// Circular by design: refreshCoordinator.js imports `post` from this module
// to call POST /auth/refresh. Both sides only reference the import inside
// function bodies (never at module-evaluation time), so ESM's hoisted
// function-declaration bindings resolve this safely.
import { ensureRefreshed } from "./refreshCoordinator";

// Evaluated once at module import time (top-level), so vi.stubEnv calls
// made after this module is first imported (e.g. in beforeEach) will not
// change API_BASE; use vi.resetModules() + a dynamic import() to re-evaluate it.
const API_BASE = import.meta.env.VITE_API_URL;

const UNSAFE_METHODS = new Set(["POST", "PUT", "DELETE", "PATCH"]);

// A backend that accepts the connection and never answers leaves fetch pending forever:
// the caller's loading state never resolves, so the UI keeps spinning with no error to
// render and no way out. This deadline turns that into an ordinary rejection the existing
// catch blocks already handle.
const REQUEST_TIMEOUT_MS = 15_000;
const TIMEOUT_MESSAGE = "La solicitud tardó demasiado. Intentá de nuevo.";

// Endpoints that legitimately return 401 for reasons unrelated to an
// expired/missing session cookie (e.g. wrong credentials on login), plus
// /auth/refresh itself (its own 401 is terminal and must not recurse back
// into the refresh coordinator). These must not trigger the global
// auth:unauthorized redirect from this same branch.
const AUTH_BOOTSTRAP_ENDPOINTS = new Set([
	"/auth/login",
	"/auth/register",
	"/auth/me",
	"/auth/csrf",
	"/auth/refresh",
]);

// Reads the XSRF-TOKEN cookie set by Spring's CookieCsrfTokenRepository
// (non-httpOnly by design, readable from JS) and URL-decodes its value —
// Spring URL-encodes the raw token before writing the cookie.
export function getCsrfToken() {
	const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
	return match ? decodeURIComponent(match[1]) : null;
}

export async function bootstrapCsrf() {
	await request("GET", "/auth/csrf");
	if (!getCsrfToken()) {
		throw new Error("CSRF token was not issued");
	}
}

/**
 * Each call gets its own controller, so the retry after a refresh is timed from when IT
 * started rather than inheriting the original attempt's already-spent budget. The timer
 * is always cleared, including on a rejection, so a settled request never leaves an armed
 * deadline behind.
 */
async function fetchWithDeadline(url, config, externalSignal) {
	const controller = new AbortController();
	let abortedByCaller = false;
	const abortFromCaller = () => {
		if (!controller.signal.aborted) {
			abortedByCaller = true;
			controller.abort();
		}
	};
	externalSignal?.addEventListener("abort", abortFromCaller, { once: true });
	if (externalSignal?.aborted) abortFromCaller();

	const deadline = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
	try {
		return await fetch(url, { ...config, signal: controller.signal });
	} catch (err) {
		if (err?.name === "AbortError") {
			if (abortedByCaller) throw err;
			throw new Error(TIMEOUT_MESSAGE, { cause: err });
		}
		throw err;
	} finally {
		clearTimeout(deadline);
		externalSignal?.removeEventListener("abort", abortFromCaller);
	}
}

async function request(
	method,
	endpoint,
	data,
	alreadyRetried = false,
	options = {},
) {
	const { multipart = false, signal } = options;
	const headers = multipart ? {} : { "Content-Type": "application/json" };
	if (UNSAFE_METHODS.has(method)) {
		const csrfToken = getCsrfToken();
		if (csrfToken) {
			headers["X-XSRF-TOKEN"] = csrfToken;
		}
	}

	const config = { method, headers, credentials: "include" };
	if (data) {
		config.body = multipart ? data : JSON.stringify(data);
	}

	const res = await fetchWithDeadline(`${API_BASE}${endpoint}`, config, signal);

	if (res.status === 401 && !AUTH_BOOTSTRAP_ENDPOINTS.has(endpoint)) {
		if (!alreadyRetried) {
			// Coalesce this 401 with any other concurrent one behind a single
			// in-flight /auth/refresh call, then retry the original request ONCE.
			// `ensureRefreshed` itself dispatches auth:unauthorized (exactly once,
			// shared across every waiting caller) if the refresh fails, so we
			// simply propagate that rejection here without dispatching again.
			await ensureRefreshed();
			return request(method, endpoint, data, true, options);
		}
		window.dispatchEvent(new CustomEvent("auth:unauthorized"));
		throw new Error("Sesión expirada");
	}

	if (!res.ok) {
		const errorData = await res.json().catch(() => ({}));
		const fieldMessages =
			errorData.fields && Object.keys(errorData.fields).length > 0
				? Object.values(errorData.fields).join(" ")
				: null;
		const serverMessage =
			fieldMessages ||
			(typeof errorData.error === "string" && errorData.error.trim()
				? errorData.error
				: null);
		const error = new Error(serverMessage || `Error ${res.status}`);
		error.code = errorData.code;
		error.status = res.status;
		error.hasServerMessage = Boolean(serverMessage);
		throw error;
	}

	if (res.status === 204) return null;

	try {
		return await res.json();
	} catch {
		return null;
	}
}

export function get(endpoint, { signal } = {}) {
	return request("GET", endpoint, undefined, false, { signal });
}

export function post(endpoint, data) {
	return request("POST", endpoint, data);
}

/**
 * Multipart requests retain credentials, CSRF, caller cancellation, and the shared
 * deadline. Do not set Content-Type here: the browser must generate the boundary.
 */
export function postMultipart(endpoint, formData, { signal } = {}) {
	return request("POST", endpoint, formData, false, {
		multipart: true,
		signal,
	});
}

export function put(endpoint, data) {
	return request("PUT", endpoint, data);
}

export function patch(endpoint, data) {
	return request("PATCH", endpoint, data);
}

export function del(endpoint) {
	return request("DELETE", endpoint);
}
