import { act, renderHook } from "@testing-library/react";
import useCityAutocomplete from "./useCityAutocomplete";
import { get } from "../services/api";

vi.mock("../services/api", () => ({ get: vi.fn() }));

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
	get.mockReset();
});

afterEach(() => {
	vi.useRealTimers();
});

describe("useCityAutocomplete", () => {
	it("debounces city loading for 200ms and exposes successful suggestions", async () => {
		const pending = deferred();
		get.mockReturnValueOnce(pending.promise);
		const { result } = renderHook(() => useCityAutocomplete());

		act(() => result.current.handleCityChange("Ba"));
		expect(get).not.toHaveBeenCalled();
		expect(result.current.showSuggestions).toBe(false);

		await advanceDebounce();
		expect(get).toHaveBeenCalledWith("/lodgings/cities?q=Ba");
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
		get.mockResolvedValueOnce(["Bariloche"]);
		const { result } = renderHook(() => useCityAutocomplete());
		act(() => result.current.handleCityChange("Ba"));
		await advanceDebounce();
		await act(async () => {});

		const pending = deferred();
		get.mockReturnValueOnce(pending.promise);
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
		get.mockResolvedValueOnce([]);
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

		get.mockClear();
		act(() => result.current.handleCityChange("Men"));
		unmount();
		await act(async () => {
			await vi.advanceTimersByTimeAsync(200);
		});
		expect(get).not.toHaveBeenCalled();
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
		get.mockResolvedValueOnce(["Bariloche", "Buenos Aires"]);
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

		act(() => result.current.handleCityChange("Bu"));
		act(() => result.current.handleCityKeyDown(keyEvent("Escape")));
		expect(result.current.showSuggestions).toBe(false);
		expect(result.current.activeSuggestionIndex).toBe(-1);
	});
});
