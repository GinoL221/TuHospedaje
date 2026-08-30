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

async function request(method, endpoint, data, alreadyRetried = false) {
  const headers = { "Content-Type": "application/json" };
  if (UNSAFE_METHODS.has(method)) {
    const csrfToken = getCsrfToken();
    if (csrfToken) {
      headers["X-XSRF-TOKEN"] = csrfToken;
    }
  }

  const config = { method, headers, credentials: "include" };
  if (data) {
    config.body = JSON.stringify(data);
  }

  const res = await fetch(`${API_BASE}${endpoint}`, config);

  if (res.status === 401 && !AUTH_BOOTSTRAP_ENDPOINTS.has(endpoint)) {
    if (!alreadyRetried) {
      // Coalesce this 401 with any other concurrent one behind a single
      // in-flight /auth/refresh call, then retry the original request ONCE.
      // `ensureRefreshed` itself dispatches auth:unauthorized (exactly once,
      // shared across every waiting caller) if the refresh fails, so we
      // simply propagate that rejection here without dispatching again.
      await ensureRefreshed();
      return request(method, endpoint, data, true);
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
    const error = new Error(fieldMessages || errorData.error || `Error ${res.status}`);
    error.code = errorData.code;
    throw error;
  }

  if (res.status === 204) return null;

  try {
    return await res.json();
  } catch {
    return null;
  }
}

export function get(endpoint) {
  return request("GET", endpoint);
}

export function post(endpoint, data) {
  return request("POST", endpoint, data);
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
