// Evaluated once at module import time (top-level), so vi.stubEnv calls
// made after this module is first imported (e.g. in beforeEach) will not
// change API_BASE; use vi.resetModules() + a dynamic import() to re-evaluate it.
const API_BASE = import.meta.env.VITE_API_URL;

const UNSAFE_METHODS = new Set(["POST", "PUT", "DELETE", "PATCH"]);

// Endpoints that legitimately return 401 for reasons unrelated to an
// expired/missing session cookie (e.g. wrong credentials on login). These
// must not trigger the global auth:unauthorized redirect.
const AUTH_BOOTSTRAP_ENDPOINTS = new Set([
  "/auth/login",
  "/auth/register",
  "/auth/me",
  "/auth/csrf",
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

async function request(method, endpoint, data) {
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
    window.dispatchEvent(new CustomEvent("auth:unauthorized"));
    throw new Error("Sesión expirada");
  }

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    const fieldMessages =
      errorData.fields && Object.keys(errorData.fields).length > 0
        ? Object.values(errorData.fields).join(" ")
        : null;
    throw new Error(fieldMessages || errorData.error || `Error ${res.status}`);
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
