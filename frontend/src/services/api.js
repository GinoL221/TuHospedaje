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

  if (res.status === 401) {
    localStorage.removeItem("token");
    if (token) {
      window.location.href = "/login";
      throw new Error("Sesión expirada");
    }
  }

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Error ${res.status}`);
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
