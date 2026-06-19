import { get, post, put, del } from "./api";

// NOTE: `api.js` reads `import.meta.env.VITE_API_URL` into a module-level
// `const API_BASE` at import time. Most tests below assert only the
// `endpoint` portion of the URL (the static `./api` import already has a
// fixed `API_BASE` baked in). The "request URL construction" describe block
// further down controls `API_BASE` for real via `vi.resetModules()` plus a
// dynamic `import("./api")`, which re-evaluates the module against a
// freshly stubbed `VITE_API_URL`.
function mockFetchResolved({ ok = true, status = 200, json } = {}) {
  return vi.fn().mockResolvedValue({
    ok,
    status,
    json: json ?? (async () => ({})),
  });
}

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("api service - successful requests", () => {
  it("GET resolves with the parsed JSON body", async () => {
    const fetchMock = mockFetchResolved({ json: async () => ({ data: 1 }) });
    vi.stubGlobal("fetch", fetchMock);

    const result = await get("/lodgings");

    expect(result).toEqual({ data: 1 });
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/lodgings"),
      expect.objectContaining({ method: "GET" })
    );
  });

  it("POST resolves with the parsed JSON body and serializes the request body", async () => {
    const fetchMock = mockFetchResolved({ json: async () => ({ id: 1 }) });
    vi.stubGlobal("fetch", fetchMock);

    const result = await post("/lodgings", { name: "Test" });

    expect(result).toEqual({ id: 1 });
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/lodgings"),
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ name: "Test" }),
      })
    );
  });

  it("PUT resolves with the parsed JSON body and serializes the request body", async () => {
    const fetchMock = mockFetchResolved({ json: async () => ({ id: 1, name: "Updated" }) });
    vi.stubGlobal("fetch", fetchMock);

    const result = await put("/lodgings/1", { name: "Updated" });

    expect(result).toEqual({ id: 1, name: "Updated" });
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/lodgings/1"),
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({ name: "Updated" }),
      })
    );
  });

  it("DELETE resolves with the parsed JSON body and omits a request body", async () => {
    const fetchMock = mockFetchResolved({ json: async () => ({ deleted: true }) });
    vi.stubGlobal("fetch", fetchMock);

    const result = await del("/lodgings/1");

    expect(result).toEqual({ deleted: true });
    const [, config] = fetchMock.mock.calls[0];
    expect(config.method).toBe("DELETE");
    expect(config.body).toBeUndefined();
  });
});

describe("api service - 204 No Content", () => {
  it("resolves with null and does not attempt to parse a body", async () => {
    const jsonSpy = vi.fn();
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 204, json: jsonSpy });
    vi.stubGlobal("fetch", fetchMock);

    const result = await get("/lodgings/1");

    expect(result).toBeNull();
    expect(jsonSpy).not.toHaveBeenCalled();
  });
});

describe("api service - non-OK HTTP errors", () => {
  it("rejects with the server error message when the body is JSON-parseable", async () => {
    const fetchMock = mockFetchResolved({
      ok: false,
      status: 400,
      json: async () => ({ error: "msg" }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(get("/lodgings")).rejects.toThrow("msg");
  });

  it("rejects with a generic status message when the body is unparseable", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => {
        throw new Error("invalid json");
      },
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(get("/lodgings")).rejects.toThrow("Error 500");
  });
});

describe("api service - Authorization header", () => {
  it("attaches Authorization when a token is present in localStorage", async () => {
    localStorage.setItem("token", "abc");
    const fetchMock = mockFetchResolved();
    vi.stubGlobal("fetch", fetchMock);

    await get("/lodgings");

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/lodgings"),
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer abc" }),
      })
    );
  });

  it("does not attach Authorization when there is no token", async () => {
    const fetchMock = mockFetchResolved();
    vi.stubGlobal("fetch", fetchMock);

    await get("/lodgings");

    const [, config] = fetchMock.mock.calls[0];
    expect(config.headers).not.toHaveProperty("Authorization");
  });
});

describe("api service - 401 unauthorized", () => {
  it("dispatches auth:unauthorized and rejects with 'Sesión expirada' when a token was sent", async () => {
    localStorage.setItem("token", "abc");
    const fetchMock = vi.fn().mockResolvedValue({ ok: false, status: 401, json: async () => ({}) });
    vi.stubGlobal("fetch", fetchMock);

    const eventSpy = vi.fn();
    window.addEventListener("auth:unauthorized", eventSpy);

    await expect(get("/lodgings")).rejects.toThrow("Sesión expirada");
    expect(eventSpy).toHaveBeenCalledTimes(1);
    expect(eventSpy.mock.calls[0][0]).toBeInstanceOf(CustomEvent);
    expect(eventSpy.mock.calls[0][0].type).toBe("auth:unauthorized");

    window.removeEventListener("auth:unauthorized", eventSpy);
  });

  it("rejects with the real backend error message and does NOT dispatch auth:unauthorized when no token was sent (e.g. failed login)", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ error: "Credenciales inválidas" }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const eventSpy = vi.fn();
    window.addEventListener("auth:unauthorized", eventSpy);

    await expect(post("/auth/login", { email: "a@a.com", password: "wrong" })).rejects.toThrow(
      "Credenciales inválidas"
    );
    expect(eventSpy).not.toHaveBeenCalled();

    window.removeEventListener("auth:unauthorized", eventSpy);
  });
});

describe("api service - request URL construction", () => {
  it("builds the request URL from the real VITE_API_URL evaluated at module import time", async () => {
    vi.resetModules();
    vi.stubEnv("VITE_API_URL", "http://test-base");

    const { get: scopedGet } = await import("./api");

    const fetchMock = mockFetchResolved({ json: async () => ({ data: 1 }) });
    vi.stubGlobal("fetch", fetchMock);

    await scopedGet("/lodgings");

    expect(fetchMock).toHaveBeenCalledWith(
      "http://test-base/lodgings",
      expect.objectContaining({ method: "GET" })
    );
  });
});

describe("api service - ok response with unparseable body", () => {
  it("resolves with null when res.ok is true but res.json() throws", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => {
        throw new Error("invalid json");
      },
    });
    vi.stubGlobal("fetch", fetchMock);

    const result = await get("/lodgings");

    expect(result).toBeNull();
  });
});
