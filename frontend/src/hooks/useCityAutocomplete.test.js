import { act, renderHook } from "@testing-library/react";
import useCityAutocomplete from "./useCityAutocomplete";
import { getCities } from "../services/lodgingService";

vi.mock("../services/lodgingService", () => ({ getCities: vi.fn() }));

function deferred() {
	let resolve;
	let reject;
	const promise = new Promise((res, rej) => {
		resolve = res;
		reject = rej;
	});
	return { promise, resolve, reject };
}

function keyEvent(key) {
	return { key, preventDefault: vi.fn() };
}

async function advanceDebounce() {
	await act(async () => {
		await vi.advanceTimersByTimeAsync(200);
	});
}

beforeEach(() => {
	vi.useFakeTimers();
	getCities.mockReset();
});

afterEach(() => {
	vi.useRealTimers();
});

describe("useCityAutocomplete", () => {
	it("debounces city loading for 200ms and exposes successful suggestions", async () => {
		const pending = deferred();
		getCities.mockReturnValueOnce(pending.promise);
		const { result } = renderHook(() => useCityAutocomplete());

		act(() => result.current.handleCityChange("Ba"));
		expect(getCities).not.toHaveBeenCalled();
		expect(result.current.showSuggestions).toBe(false);

		await advanceDebounce();
		expect(getCities).toHaveBeenCalledWith(
			"Ba",
			expect.objectContaining({ signal: expect.any(AbortSignal) }),
		);
		expect(result.current.loadingCities).toBe(true);
		expect(result.current.showSuggestions).toBe(true);

		await act(async () => {
			pending.resolve(["Bariloche", "Baradero"]);
			await pending.promise;
		});
		expect(result.current.suggestions).toEqual(["Bariloche", "Baradero"]);
		expect(result.current.loadingCities).toBe(false);
		expect(result.current.activeSuggestionIndex).toBe(-1);
	});

	it("retains suggestions while loading, clears on errors, and clears short city text", async () => {
		getCities.mockResolvedValueOnce(["Bariloche"]);
		const { result } = renderHook(() => useCityAutocomplete());
		act(() => result.current.handleCityChange("Ba"));
		await advanceDebounce();
		await act(async () => {});

		const pending = deferred();
		getCities.mockReturnValueOnce(pending.promise);
		act(() => result.current.handleCityChange("Bar"));
		await advanceDebounce();
		expect(result.current.suggestions).toEqual(["Bariloche"]);
		expect(result.current.loadingCities).toBe(true);

		await act(async () => {
			pending.reject(new Error("network"));
			await pending.promise.catch(() => {});
		});
		expect(result.current.suggestions).toEqual([]);
		expect(result.current.loadingCities).toBe(false);

		act(() => result.current.handleCityChange("B"));
		expect(result.current.city).toBe("B");
		expect(result.current.suggestions).toEqual([]);
		expect(result.current.showSuggestions).toBe(false);
	});

	it("keeps focus and blur ordering and cancels scheduled work on cleanup", async () => {
		getCities.mockResolvedValueOnce([]);
		const { result, unmount } = renderHook(() => useCityAutocomplete());
		act(() => result.current.handleCityChange("Me"));
		act(() => result.current.handleCityFocus());
		expect(result.current.showSuggestions).toBe(true);

		act(() => result.current.handleCityBlur());
		await act(async () => {
			await vi.advanceTimersByTimeAsync(299);
		});
		expect(result.current.showSuggestions).toBe(true);
		await act(async () => {
			await vi.advanceTimersByTimeAsync(1);
		});
		expect(result.current.showSuggestions).toBe(false);

		getCities.mockClear();
		act(() => result.current.handleCityChange("Men"));
		unmount();
		await act(async () => {
			await vi.advanceTimersByTimeAsync(200);
		});
		expect(getCities).not.toHaveBeenCalled();
	});

	it("ignores an older request that resolves after the current request", async () => {
		const first = deferred();
		const second = deferred();
		getCities
			.mockReturnValueOnce(first.promise)
			.mockReturnValueOnce(second.promise);
		const { result } = renderHook(() => useCityAutocomplete());

		act(() => result.current.handleCityChange("Ba"));
		await advanceDebounce();
		const firstSignal = getCities.mock.calls[0][1].signal;

		act(() => result.current.handleCityChange("Bar"));
		await advanceDebounce();
		expect(firstSignal.aborted).toBe(true);

		await act(async () => {
			second.resolve(["Bariloche"]);
			await second.promise;
		});
		expect(result.current.suggestions).toEqual(["Bariloche"]);
		expect(result.current.loadingCities).toBe(false);

		await act(async () => {
			first.resolve(["Buenos Aires"]);
			await first.promise;
		});
		expect(result.current.suggestions).toEqual(["Bariloche"]);
		expect(result.current.loadingCities).toBe(false);
	});

	it("aborts an active request when city input becomes too short", async () => {
		const pending = deferred();
		getCities.mockReturnValueOnce(pending.promise);
		const { result } = renderHook(() => useCityAutocomplete());

		act(() => result.current.handleCityChange("Ba"));
		await advanceDebounce();
		const signal = getCities.mock.calls[0][1].signal;

		act(() => result.current.handleCityChange("B"));
		expect(signal.aborted).toBe(true);
		expect(result.current.loadingCities).toBe(false);

		await act(async () => {
			pending.resolve(["Buenos Aires"]);
			await pending.promise;
		});
		expect(result.current.suggestions).toEqual([]);
		expect(result.current.loadingCities).toBe(false);
	});

	it("aborts an active request when selecting a city", async () => {
		const pending = deferred();
		getCities.mockReturnValueOnce(pending.promise);
		const { result } = renderHook(() => useCityAutocomplete());

		act(() => result.current.handleCityChange("Ba"));
		await advanceDebounce();
		const signal = getCities.mock.calls[0][1].signal;

		act(() => result.current.selectCity("Bariloche"));
		expect(signal.aborted).toBe(true);
		expect(result.current.city).toBe("Bariloche");
		expect(result.current.loadingCities).toBe(false);
	});

	it("aborts an active request on unmount", async () => {
		const pending = deferred();
		getCities.mockReturnValueOnce(pending.promise);
		const { result, unmount } = renderHook(() => useCityAutocomplete());

		act(() => result.current.handleCityChange("Ba"));
		await advanceDebounce();
		const signal = getCities.mock.calls[0][1].signal;

		unmount();
		expect(signal.aborted).toBe(true);
	});

	it("keeps retained suggestions and loading ownership when cancellation rejects", async () => {
		getCities.mockResolvedValueOnce(["Bariloche"]);
		const cancelled = deferred();
		const current = deferred();
		getCities
			.mockReturnValueOnce(cancelled.promise)
			.mockReturnValueOnce(current.promise);
		const { result } = renderHook(() => useCityAutocomplete());

		act(() => result.current.handleCityChange("Ba"));
		await advanceDebounce();
		await act(async () => {});
		act(() => result.current.activateSuggestion(0));

		act(() => result.current.handleCityChange("Bar"));
		await advanceDebounce();
		act(() => result.current.handleCityChange("Bari"));
		await advanceDebounce();

		await act(async () => {
			cancelled.reject(new DOMException("Aborted", "AbortError"));
			await cancelled.promise.catch(() => {});
		});
		expect(result.current.suggestions).toEqual(["Bariloche"]);
		expect(result.current.activeSuggestionIndex).toBe(-1);
		expect(result.current.loadingCities).toBe(true);

		await act(async () => {
			current.resolve(["Bariloche", "Bariloche Centro"]);
			await current.promise;
		});
		expect(result.current.suggestions).toEqual([
			"Bariloche",
			"Bariloche Centro",
		]);
		expect(result.current.loadingCities).toBe(false);
	});

	it("selects cities and resets the active option", () => {
		const { result } = renderHook(() => useCityAutocomplete());
		act(() => result.current.handleCityChange("Ba"));
		act(() => result.current.selectCity("Bariloche"));

		expect(result.current.city).toBe("Bariloche");
		expect(result.current.showSuggestions).toBe(false);
		expect(result.current.activeSuggestionIndex).toBe(-1);
	});

	it("navigates arrow boundaries, selects with Enter, and dismisses with Escape", async () => {
		getCities.mockResolvedValue(["Bariloche", "Buenos Aires"]);
		const { result } = renderHook(() => useCityAutocomplete());
		act(() => result.current.handleCityChange("Ba"));
		await advanceDebounce();
		await act(async () => {});

		const down = keyEvent("ArrowDown");
		act(() => result.current.handleCityKeyDown(down));
		expect(down.preventDefault).toHaveBeenCalledOnce();
		expect(result.current.activeSuggestionIndex).toBe(0);
		act(() => result.current.handleCityKeyDown(keyEvent("ArrowUp")));
		expect(result.current.activeSuggestionIndex).toBe(1);
		act(() => result.current.handleCityKeyDown(keyEvent("ArrowDown")));
		expect(result.current.activeSuggestionIndex).toBe(0);

		const enter = keyEvent("Enter");
		act(() => result.current.handleCityKeyDown(enter));
		expect(enter.preventDefault).toHaveBeenCalledOnce();
		expect(result.current.city).toBe("Bariloche");
		expect(result.current.showSuggestions).toBe(false);
		await advanceDebounce();
		expect(getCities).toHaveBeenCalledTimes(1);
		expect(result.current.showSuggestions).toBe(false);

		act(() => result.current.handleCityChange("Bu"));
		act(() => result.current.handleCityKeyDown(keyEvent("Escape")));
		expect(result.current.showSuggestions).toBe(false);
		expect(result.current.activeSuggestionIndex).toBe(-1);
	});

	it("selects by touch without reopening or requesting the selected city", async () => {
		getCities.mockResolvedValue(["Bariloche"]);
		const { result } = renderHook(() => useCityAutocomplete());

		act(() => result.current.handleCityChange("Ba"));
		await advanceDebounce();
		await act(async () => {});

		act(() => result.current.selectCity("Bariloche"));
		expect(result.current.city).toBe("Bariloche");
		expect(result.current.showSuggestions).toBe(false);

		await advanceDebounce();
		expect(getCities).toHaveBeenCalledTimes(1);
		expect(getCities).not.toHaveBeenCalledWith("Bariloche");
		expect(result.current.showSuggestions).toBe(false);
	});
});
