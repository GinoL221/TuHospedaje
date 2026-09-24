import { act, renderHook } from "@testing-library/react";
import useHomeSearchResults from "./useHomeSearchResults";
import { searchLodgings } from "../services/lodgingService";

vi.mock("../services/lodgingService", () => ({ searchLodgings: vi.fn() }));

function deferred() {
	let resolve;
	let reject;
	const promise = new Promise((res, rej) => {
		resolve = res;
		reject = rej;
	});
	return { promise, reject, resolve };
}

function results(name) {
	return { lodgings: [{ id: name, name }], totalItems: 1, catalogItems: 1 };
}

describe("useHomeSearchResults", () => {
	beforeEach(() => searchLodgings.mockReset());

	it("loads the exact URL query and exposes its current response", async () => {
		searchLodgings.mockResolvedValueOnce(results("Salta"));
		const { result } = renderHook(() => useHomeSearchResults("?city=Salta"));

		expect(searchLodgings).toHaveBeenCalledWith("?city=Salta");
		await act(async () => await Promise.resolve());
		expect(result.current.searchResults).toEqual(results("Salta"));
	});

	it("does not load an empty query", () => {
		renderHook(() => useHomeSearchResults(""));
		expect(searchLodgings).not.toHaveBeenCalled();
	});

	it("does not refetch when rerendered with the same query", async () => {
		searchLodgings.mockResolvedValueOnce(results("Salta"));
		const { result, rerender } = renderHook(
			({ search }) => useHomeSearchResults(search),
			{ initialProps: { search: "?city=Salta" } },
		);

		rerender({ search: "?city=Salta" });
		await act(async () => await Promise.resolve());

		expect(searchLodgings).toHaveBeenCalledTimes(1);
		expect(result.current.searchResults).toEqual(results("Salta"));
	});

	it("keeps a valid empty response as data", async () => {
		const emptyResults = { lodgings: [], totalItems: 0, catalogItems: 0 };
		searchLodgings.mockResolvedValueOnce(emptyResults);
		const { result } = renderHook(() => useHomeSearchResults("?city=Salta"));

		await act(async () => await Promise.resolve());

		expect(result.current.searchResults).toEqual(emptyResults);
		expect(result.current.searchError).toBeNull();
	});

	it("exposes a fetch failure without treating it as empty data", async () => {
		const error = new Error("offline");
		searchLodgings.mockRejectedValueOnce(error);
		const { result } = renderHook(() => useHomeSearchResults("?city=Salta"));

		await act(async () => await Promise.resolve());

		expect(result.current.searchResults).toBeNull();
		expect(result.current.searchError).toBe(error);
	});

	it("retries the current query and clears its error after success", async () => {
		const error = new Error("offline");
		searchLodgings
			.mockRejectedValueOnce(error)
			.mockResolvedValueOnce(results("Salta"));
		const { result } = renderHook(() => useHomeSearchResults("?city=Salta"));

		await act(async () => await Promise.resolve());
		expect(result.current.searchError).toBe(error);

		await act(async () => {
			result.current.retrySearch();
			await Promise.resolve();
		});

		expect(searchLodgings).toHaveBeenNthCalledWith(2, "?city=Salta");
		expect(result.current.searchResults).toEqual(results("Salta"));
		expect(result.current.searchError).toBeNull();
	});

	it("keeps only the latest same-query retry completion", async () => {
		const failedSearch = deferred();
		const firstRetry = deferred();
		const secondRetry = deferred();
		searchLodgings
			.mockReturnValueOnce(failedSearch.promise)
			.mockReturnValueOnce(firstRetry.promise)
			.mockReturnValueOnce(secondRetry.promise);
		const { result } = renderHook(() => useHomeSearchResults("?city=Salta"));

		await act(async () => {
			failedSearch.reject(new Error("offline"));
			await failedSearch.promise.catch(() => undefined);
		});
		act(() => {
			result.current.retrySearch();
			result.current.retrySearch();
		});
		expect(searchLodgings).toHaveBeenCalledTimes(3);

		await act(async () => {
			secondRetry.resolve(results("latest Salta"));
			await secondRetry.promise;
		});
		await act(async () => {
			firstRetry.reject(new Error("stale retry failure"));
			await firstRetry.promise.catch(() => undefined);
		});

		expect(result.current.searchResults).toEqual(results("latest Salta"));
		expect(result.current.searchError).toBeNull();
	});

	it("rejects a superseded retry completion after the query changes", async () => {
		const failedSalta = deferred();
		const retrySalta = deferred();
		const mendoza = deferred();
		searchLodgings
			.mockReturnValueOnce(failedSalta.promise)
			.mockReturnValueOnce(retrySalta.promise)
			.mockReturnValueOnce(mendoza.promise);
		const { result, rerender } = renderHook(
			({ search }) => useHomeSearchResults(search),
			{
				initialProps: { search: "?city=Salta" },
			},
		);

		await act(async () => {
			failedSalta.reject(new Error("offline"));
			await failedSalta.promise.catch(() => undefined);
		});
		act(() => result.current.retrySearch());
		rerender({ search: "?city=Mendoza" });
		expect(result.current.searchResults).toBeNull();
		expect(result.current.searchError).toBeNull();

		await act(async () => {
			mendoza.resolve(results("Mendoza"));
			await mendoza.promise;
		});
		await act(async () => {
			retrySalta.reject(new Error("still offline"));
			await retrySalta.promise.catch(() => undefined);
		});

		expect(result.current.searchResults).toEqual(results("Mendoza"));
		expect(result.current.searchError).toBeNull();
	});

	it("does not update after unmount and has no recommendation ownership", async () => {
		const pending = deferred();
		searchLodgings.mockReturnValueOnce(pending.promise);
		const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
		const { result, unmount } = renderHook(() =>
			useHomeSearchResults("?city=Salta"),
		);

		expect(result.current.searchResults).toBeNull();
		expect(result.current.searchError).toBeNull();
		unmount();
		await act(async () => {
			pending.resolve(results("Salta"));
			await pending.promise;
		});
		expect(errorSpy).not.toHaveBeenCalled();
		errorSpy.mockRestore();
	});
});
