import { act, renderHook } from "@testing-library/react";
import useRatingEligibility from "./useRatingEligibility";
import { get } from "../services/api";

vi.mock("../services/api");

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe("useRatingEligibility - unmount protection", () => {
  it("does not update state after an eligibility request resolves post-unmount", async () => {
    const pending = deferred();
    get.mockReturnValueOnce(pending.promise);
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    const { result, unmount } = renderHook(() => useRatingEligibility(10));

    act(() => {
      result.current.load();
    });
    expect(result.current.status).toBe("loading");

    unmount();

    await act(async () => {
      pending.resolve({ eligible: true });
      await pending.promise;
    });

    expect(errorSpy).not.toHaveBeenCalled();
    errorSpy.mockRestore();
  });

  it("does not update state after an eligibility request rejects post-unmount", async () => {
    const pending = deferred();
    get.mockReturnValueOnce(pending.promise);
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    const { result, unmount } = renderHook(() => useRatingEligibility(11));

    act(() => {
      result.current.load();
    });
    expect(result.current.status).toBe("loading");

    unmount();

    await act(async () => {
      pending.reject(new Error("network"));
      try {
        await pending.promise;
      } catch {
        // The hook consumes the rejection; this only settles the deferred promise.
      }
    });

    expect(errorSpy).not.toHaveBeenCalled();
    errorSpy.mockRestore();
  });
});
