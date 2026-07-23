import { bootstrapCsrf, getCsrfToken } from "./api";

describe("bootstrapCsrf", () => {
  beforeEach(() => {
    document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
  });

  it("resolves only after a successful response leaves a readable token", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true, status: 204 }));
    document.cookie = "XSRF-TOKEN=csrf-ready; path=/";

    await expect(bootstrapCsrf()).resolves.toBeUndefined();
    expect(getCsrfToken()).toBe("csrf-ready");
  });

  it("rejects HTTP, network, and absent-cookie failures", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
      json: async () => ({ error: "Unavailable" }),
    }));
    await expect(bootstrapCsrf()).rejects.toThrow("Unavailable");

    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("Network down")));
    await expect(bootstrapCsrf()).rejects.toThrow("Network down");

    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true, status: 204 }));
    await expect(bootstrapCsrf()).rejects.toThrow("CSRF token");
  });

  it("does not dispatch auth:unauthorized when the bootstrap endpoint itself returns 401", async () => {
    const dispatchSpy = vi.spyOn(window, "dispatchEvent");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ error: "Unauthorized" }),
    }));

    await expect(bootstrapCsrf()).rejects.toThrow();
    expect(dispatchSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: "auth:unauthorized" }),
    );
  });
});
