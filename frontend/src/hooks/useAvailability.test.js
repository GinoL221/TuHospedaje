import { renderHook, act } from "@testing-library/react";
import useAvailability from "./useAvailability";
import { getAvailability } from "../services/lodgingService";

vi.mock("../services/lodgingService");

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

function availabilityResponse(occupiedRanges = []) {
  return { available: occupiedRanges.length === 0, occupiedRanges };
}

describe("useAvailability - idle to loading to ready", () => {
  it("starts idle, moves to loading while the request is in flight, then ready with the response's occupied ranges", async () => {
    const first = deferred();
    getAvailability.mockReturnValue(first.promise);
    const { result } = renderHook(() => useAvailability(1));

    expect(result.current.status).toBe("idle");
    expect(result.current.occupiedRanges).toEqual([]);

    act(() => {
      result.current.load({});
    });

    expect(result.current.status).toBe("loading");
    expect(getAvailability).toHaveBeenCalledWith(1, {});

    await act(async () => {
      first.resolve(
        availabilityResponse([
          { checkIn: "2026-08-01", checkOut: "2026-08-05" },
        ]),
      );
      await first.promise;
    });

    expect(result.current.status).toBe("ready");
    expect(result.current.occupiedRanges).toEqual([
      { checkIn: "2026-08-01", checkOut: "2026-08-05" },
    ]);
    expect(result.current.error).toBeNull();
    expect(
      result.current.isRangeAvailable(
        new Date("2026-08-02"),
        new Date("2026-08-03"),
      ),
    ).toBe(false);
    expect(
      result.current.isRangeAvailable(
        new Date("2026-09-02"),
        new Date("2026-09-03"),
      ),
    ).toBe(true);
  });
});

describe("useAvailability - zero-occupied ready state", () => {
  it("becomes ready with an empty occupiedRanges list, treating every range as available", async () => {
    getAvailability.mockResolvedValueOnce(availabilityResponse([]));
    const { result } = renderHook(() => useAvailability(2));

    await act(async () => {
      await result.current.load({});
    });

    expect(result.current.status).toBe("ready");
    expect(result.current.occupiedRanges).toEqual([]);
    expect(
      result.current.isRangeAvailable(
        new Date("2026-10-02"),
        new Date("2026-10-03"),
      ),
    ).toBe(true);
  });
});

describe("useAvailability - isRangeAvailable before a ready result", () => {
  it("never asserts availability while idle, loading, or errored", async () => {
    const { result } = renderHook(() => useAvailability(3));

    expect(
      result.current.isRangeAvailable(
        new Date("2026-01-01"),
        new Date("2026-01-02"),
      ),
    ).toBe(false);

    getAvailability.mockRejectedValueOnce(new Error("boom"));
    await act(async () => {
      await result.current.load({});
    });

    expect(result.current.status).toBe("error");
    expect(
      result.current.isRangeAvailable(
        new Date("2026-01-01"),
        new Date("2026-01-02"),
      ),
    ).toBe(false);
  });
});

describe("useAvailability - initial and repeated failures", () => {
  it("transitions to error on an initial failure, with no ready data to protect", async () => {
    getAvailability.mockRejectedValueOnce(new Error("network down"));
    const { result } = renderHook(() => useAvailability(4));

    await act(async () => {
      await result.current.load({});
    });

    expect(result.current.status).toBe("error");
    expect(result.current.error).toBe("network down");
    expect(result.current.occupiedRanges).toEqual([]);
  });

  it("stays in error, never stale, when the initial request and a retry both fail", async () => {
    getAvailability.mockRejectedValueOnce(new Error("first failure"));
    const { result } = renderHook(() => useAvailability(4));

    await act(async () => {
      await result.current.load({});
    });
    expect(result.current.status).toBe("error");

    getAvailability.mockRejectedValueOnce(new Error("second failure"));
    await act(async () => {
      await result.current.retry();
    });

    expect(result.current.status).toBe("error");
    expect(result.current.error).toBe("second failure");
  });
});

describe("useAvailability - retrying the last request", () => {
  it("retry() reissues the exact same last requested date range", async () => {
    getAvailability.mockRejectedValueOnce(new Error("network down"));
    const { result } = renderHook(() => useAvailability(5));

    await act(async () => {
      await result.current.load({
        checkIn: new Date(2026, 7, 1),
        checkOut: new Date(2026, 7, 3),
      });
    });
    expect(result.current.status).toBe("error");

    getAvailability.mockResolvedValueOnce(availabilityResponse([]));
    await act(async () => {
      await result.current.retry();
    });

    expect(getAvailability).toHaveBeenLastCalledWith(5, {
      checkIn: new Date(2026, 7, 1),
      checkOut: new Date(2026, 7, 3),
    });
    expect(result.current.status).toBe("ready");
  });
});

describe("useAvailability - availability query dates", () => {
  it("uses local calendar fields rather than UTC serialization for a single-digit month and day", async () => {
    const utcSpy = vi
      .spyOn(Date.prototype, "toISOString")
      .mockReturnValue("1999-12-31T00:00:00.000Z");
    try {
      getAvailability.mockResolvedValueOnce(availabilityResponse([]));
      const { result } = renderHook(() => useAvailability(11));

      await act(async () => {
        await result.current.load({
          checkIn: new Date(2026, 0, 9),
          checkOut: new Date(2026, 0, 10),
        });
      });

      expect(getAvailability).toHaveBeenCalledWith(11, {
        checkIn: new Date(2026, 0, 9),
        checkOut: new Date(2026, 0, 10),
      });
    } finally {
      utcSpy.mockRestore();
    }
  });

  it("includes only checkIn when checkOut is omitted", async () => {
    getAvailability.mockResolvedValueOnce(availabilityResponse([]));
    const { result } = renderHook(() => useAvailability(12));

    await act(async () => {
      await result.current.load({ checkIn: new Date(2026, 10, 5) });
    });

    expect(getAvailability).toHaveBeenCalledWith(12, {
      checkIn: new Date(2026, 10, 5),
    });
  });

  it("includes only checkOut when checkIn is omitted", async () => {
    getAvailability.mockResolvedValueOnce(availabilityResponse([]));
    const { result } = renderHook(() => useAvailability(13));

    await act(async () => {
      await result.current.load({ checkOut: new Date(2026, 10, 6) });
    });

    expect(getAvailability).toHaveBeenCalledWith(13, {
      checkOut: new Date(2026, 10, 6),
    });
  });
});

describe("useAvailability - stale refresh", () => {
  it("keeps the previous occupied ranges but flags stale when a refresh after ready fails", async () => {
    getAvailability.mockResolvedValueOnce(
      availabilityResponse([{ checkIn: "2026-09-01", checkOut: "2026-09-05" }]),
    );
    const { result } = renderHook(() => useAvailability(6));

    await act(async () => {
      await result.current.load({});
    });
    expect(result.current.status).toBe("ready");

    getAvailability.mockRejectedValueOnce(new Error("refresh failed"));
    await act(async () => {
      await result.current.load({});
    });

    expect(result.current.status).toBe("stale");
    expect(result.current.occupiedRanges).toEqual([
      { checkIn: "2026-09-01", checkOut: "2026-09-05" },
    ]);
    expect(
      result.current.isRangeAvailable(
        new Date("2026-10-01"),
        new Date("2026-10-02"),
      ),
    ).toBe(false);
  });
});

describe("useAvailability - out-of-order and unmount protection", () => {
  it("ignores a slow first response that resolves after a newer request already resolved", async () => {
    const slow = deferred();
    const fast = deferred();
    getAvailability.mockReturnValueOnce(slow.promise);
    const { result } = renderHook(() => useAvailability(9));

    act(() => {
      result.current.load({
        checkIn: new Date("2026-08-01T00:00:00Z"),
        checkOut: new Date("2026-08-03T00:00:00Z"),
      });
    });

    getAvailability.mockReturnValueOnce(fast.promise);
    act(() => {
      result.current.load({
        checkIn: new Date("2026-08-10T00:00:00Z"),
        checkOut: new Date("2026-08-12T00:00:00Z"),
      });
    });

    await act(async () => {
      fast.resolve(
        availabilityResponse([
          { checkIn: "2026-08-10", checkOut: "2026-08-12" },
        ]),
      );
      await fast.promise;
    });

    expect(result.current.status).toBe("ready");
    expect(result.current.occupiedRanges).toEqual([
      { checkIn: "2026-08-10", checkOut: "2026-08-12" },
    ]);

    await act(async () => {
      slow.resolve(
        availabilityResponse([
          { checkIn: "2026-01-01", checkOut: "2026-01-02" },
        ]),
      );
      await slow.promise;
    });

    expect(result.current.status).toBe("ready");
    expect(result.current.occupiedRanges).toEqual([
      { checkIn: "2026-08-10", checkOut: "2026-08-12" },
    ]);
  });

  it("does not update state after the component unmounts", async () => {
    const pending = deferred();
    getAvailability.mockReturnValueOnce(pending.promise);
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    const { result, unmount } = renderHook(() => useAvailability(10));

    act(() => {
      result.current.load({});
    });

    unmount();

    await act(async () => {
      pending.resolve(availabilityResponse([]));
      await pending.promise;
    });

    expect(errorSpy).not.toHaveBeenCalled();
    errorSpy.mockRestore();
  });
});
