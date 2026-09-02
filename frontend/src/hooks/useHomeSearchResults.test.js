import { act, renderHook } from "@testing-library/react";
import useHomeSearchResults from "./useHomeSearchResults";
import { get } from "../services/api";

vi.mock("../services/api");

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
	beforeEach(() => get.mockReset());

	it("loads the exact URL query and exposes its current response", async () => {
		get.mockResolvedValueOnce(results("Salta"));
		const { result } = renderHook(() => useHomeSearchResults("?city=Salta"));

		expect(get).toHaveBeenCalledWith("/lodgings/search?city=Salta");
		await act(async () => await Promise.resolve());
		expect(result.current.searchResults).toEqual(results("Salta"));
	});

	it("does not load an empty query", () => {
		renderHook(() => useHomeSearchResults(""));
		expect(get).not.toHaveBeenCalled();
	});

	it("uses the existing empty payload after a current-query failure", async () => {
		get.mockRejectedValueOnce(new Error("offline"));
		const { result } = renderHook(() => useHomeSearchResults("?city=Salta"));

		await act(async () => await Promise.resolve());
		expect(result.current.searchResults).toEqual({
			lodgings: [],
			totalItems: 0,
			catalogItems: 0,
		});
	});

	it("rejects an older query response after a newer query settles", async () => {
		const salta = deferred();
		const mendoza = deferred();
		get.mockReturnValueOnce(salta.promise).mockReturnValueOnce(mendoza.promise);
		const { result, rerender } = renderHook(({ search }) => useHomeSearchResults(search), {
			initialProps: { search: "?city=Salta" },
		});

		rerender({ search: "?city=Mendoza" });
		await act(async () => {
			mendoza.resolve(results("Mendoza"));
			await mendoza.promise;
		});
		await act(async () => {
			salta.resolve(results("Salta"));
			await salta.promise;
		});

		expect(result.current.searchResults).toEqual(results("Mendoza"));
	});

	it("does not update after unmount and has no recommendation ownership", async () => {
		const pending = deferred();
		get.mockReturnValueOnce(pending.promise);
		const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
		const { result, unmount } = renderHook(() => useHomeSearchResults("?city=Salta"));

		expect(result.current).toEqual({ searchResults: null });
		unmount();
		await act(async () => {
			pending.resolve(results("Salta"));
			await pending.promise;
		});
		expect(errorSpy).not.toHaveBeenCalled();
		errorSpy.mockRestore();
	});
});
