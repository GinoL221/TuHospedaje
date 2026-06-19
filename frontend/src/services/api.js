// Evaluated once at module import time (top-level), so vi.stubEnv calls
// made after this module is first imported (e.g. in beforeEach) will not
// change API_BASE; use vi.resetModules() + a dynamic import() to re-evaluate it.
const API_BASE = import.meta.env.VITE_API_URL;

async function request(method, endpoint, data) {
  const token = localStorage.getItem("token");
  const headers = { "Content-Type": "application/json" };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const config = { method, headers };
  if (data) {
    config.body = JSON.stringify(data);
  }

  const res = await fetch(`${API_BASE}${endpoint}`, config);

  if (res.status === 401 && token) {
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

export function del(endpoint) {
  return request("DELETE", endpoint);
}
