import { get } from "./api";

// These tests exercise the FULL integration between api.js's request() 401
// handling and the single-flight refreshCoordinator: they mock only the
// global `fetch`, never `./api` itself, so the real retry-once logic runs.

async function flushMicrotasks(times = 10) {
  for (let i = 0; i < times; i += 1) {
    await Promise.resolve();
  }
}

beforeEach(() => {
  document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
});

describe("refresh coordinator - single-flight coalescing", () => {
  it("coalesces 3 concurrent 401s into exactly one /auth/refresh call and replays all 3 requests only after it resolves", async () => {
    const hitCounts = {};
    let resolveRefresh;
    const refreshPromise = new Promise((resolve) => {
      resolveRefresh = () => resolve({ ok: true, status: 200, json: async () => ({}) });
    });

    const fetchMock = vi.fn((url) => {
      if (url.includes("/auth/refresh")) {
        return refreshPromise;
      }
      hitCounts[url] = (hitCounts[url] || 0) + 1;
      if (hitCounts[url] === 1) {
        return Promise.resolve({ ok: false, status: 401, json: async () => ({}) });
      }
      return Promise.resolve({ ok: true, status: 200, json: async () => ({ url }) });
    });
    vi.stubGlobal("fetch", fetchMock);

    const p1 = get("/a");
    const p2 = get("/b");
    const p3 = get("/c");

    await flushMicrotasks();

    const refreshCalls = fetchMock.mock.calls.filter(([url]) => url.includes("/auth/refresh"));
    expect(refreshCalls).toHaveLength(1);

    let settled = false;
    Promise.all([p1, p2, p3]).then(() => {
      settled = true;
    });
    await flushMicrotasks();
    expect(settled).toBe(false);

    resolveRefresh();

    const results = await Promise.all([p1, p2, p3]);
    expect(results).toEqual([
      { url: expect.stringContaining("/a") },
      { url: expect.stringContaining("/b") },
      { url: expect.stringContaining("/c") },
    ]);

    const refreshCallsAfter = fetchMock.mock.calls.filter(([url]) => url.includes("/auth/refresh"));
    expect(refreshCallsAfter).toHaveLength(1);
    // Each of /a, /b, /c was hit twice: once for the original 401, once for the replay.
    Object.values(hitCounts).forEach((count) => expect(count).toBe(2));
  });
});

describe("refresh coordinator - failed refresh", () => {
  it("rejects all queued requests without retry, dispatches auth:unauthorized exactly once, and never retries the refresh call", async () => {
    const hitCounts = {};
    // The backend's non-disclosing /auth/refresh 401 body (per design ADR):
    // a generic error message, same shape as any other rejected refresh.
    const fetchMock = vi.fn((url) => {
      if (url.includes("/auth/refresh")) {
        hitCounts["/auth/refresh"] = (hitCounts["/auth/refresh"] || 0) + 1;
        return Promise.resolve({
          ok: false,
          status: 401,
          json: async () => ({ error: "Sesión inválida" }),
        });
      }
      hitCounts[url] = (hitCounts[url] || 0) + 1;
      return Promise.resolve({ ok: false, status: 401, json: async () => ({}) });
    });
    vi.stubGlobal("fetch", fetchMock);

    const eventSpy = vi.fn();
    window.addEventListener("auth:unauthorized", eventSpy);

    const results = await Promise.allSettled([get("/a"), get("/b"), get("/c")]);

    results.forEach((result) => {
      expect(result.status).toBe("rejected");
      expect(result.reason.message).toBe("Sesión inválida");
    });

    expect(hitCounts["/auth/refresh"]).toBe(1);
    Object.entries(hitCounts).forEach(([url, count]) => {
      if (url !== "/auth/refresh") {
        expect(count).toBe(1);
      }
    });
    expect(eventSpy).toHaveBeenCalledTimes(1);

    window.removeEventListener("auth:unauthorized", eventSpy);
  });
});
