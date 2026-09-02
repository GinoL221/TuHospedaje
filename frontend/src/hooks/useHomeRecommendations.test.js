import { act, renderHook, waitFor } from "@testing-library/react";
import useHomeRecommendations from "./useHomeRecommendations";
import { getRecommendations } from "../services/lodgingService";

vi.mock("../services/lodgingService", () => ({
	getRecommendations: vi.fn(),
}));

const STORAGE_ID = "tuhospedaje.recommendations.v1";
const FIXED_SEED = "11111111-1111-4111-8111-111111111111";

function page({
	lodgings = [{ id: 1, name: "Cabaña del Lago" }],
	currentPage = 0,
	totalPages = 1,
	revision = "rev-1",
	reset = false,
} = {}) {
	return { lodgings, currentPage, totalPages, revision, reset };
}

function deferred() {
	let resolve;
	let reject;
	const promise = new Promise((res, rej) => {
		resolve = res;
		reject = rej;
	});
	return { promise, resolve, reject };
}

async function settleInitial(result) {
	await waitFor(() => expect(result.current.status).toBe("idle"));
}

beforeEach(() => {
	sessionStorage.clear();
	getRecommendations.mockReset();
	vi.spyOn(crypto, "randomUUID").mockReturnValue(FIXED_SEED);
});

afterEach(() => {
	vi.restoreAllMocks();
});

describe("useHomeRecommendations", () => {
	it("restores the stored snapshot and survives unavailable storage reads and writes", async () => {
		sessionStorage.setItem(
			STORAGE_ID,
			JSON.stringify({ seed: "stored-seed-0123456789", revision: "rev-stored" }),
		);
		getRecommendations.mockResolvedValue(page());
		const { result } = renderHook(() => useHomeRecommendations());

		await settleInitial(result);
		expect(getRecommendations).toHaveBeenCalledWith({
			seed: "stored-seed-0123456789",
			page: 0,
			revision: "rev-stored",
		});

		vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
			throw new Error("storage unavailable");
		});
		vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
			throw new Error("storage unavailable");
		});
		getRecommendations.mockClear();
		const unavailableStorage = renderHook(() => useHomeRecommendations());

		await settleInitial(unavailableStorage.result);
		expect(getRecommendations).toHaveBeenCalledWith({
			seed: FIXED_SEED,
			page: 0,
			revision: undefined,
		});
	});

	it("uses fallback seeds when UUID generation fails and only replaces the snapshot on refresh", async () => {
		crypto.randomUUID.mockImplementation(() => {
			throw new Error("UUID unavailable");
		});
		getRecommendations.mockResolvedValue(page());
		const { result } = renderHook(() => useHomeRecommendations());

		await settleInitial(result);
		const initialSeed = getRecommendations.mock.calls[0][0].seed;
		expect(initialSeed).toMatch(/^[A-Za-z0-9_-]{16,64}$/);

		await act(async () => {
			result.current.refresh();
		});
		await waitFor(() => expect(getRecommendations).toHaveBeenCalledTimes(2));
		expect(getRecommendations.mock.calls[1][0]).toMatchObject({ page: 0, revision: undefined });
		expect(getRecommendations.mock.calls[1][0].seed).toMatch(/^[A-Za-z0-9_-]{16,64}$/);
	});

	it("keeps snapshot identity across pagination and applies a server reset without duplicate fetching", async () => {
		getRecommendations
			.mockResolvedValueOnce(page({ totalPages: 2 }))
			.mockResolvedValueOnce(page({ currentPage: 0, totalPages: 1, revision: "rev-2", reset: true }));
		const { result } = renderHook(() => useHomeRecommendations());

		await settleInitial(result);
		await act(async () => result.current.setPage(1));
		await waitFor(() => expect(result.current.page).toBe(0));

		expect(getRecommendations).toHaveBeenCalledTimes(2);
		expect(getRecommendations.mock.calls[1][0]).toEqual({
			seed: FIXED_SEED,
			page: 1,
			revision: "rev-1",
		});
		expect(JSON.parse(sessionStorage.getItem(STORAGE_ID))).toEqual({
			seed: FIXED_SEED,
			revision: "rev-2",
		});
	});

	it("keeps the confirmed page and snapshot visible until each pagination response settles", async () => {
		const secondPage = deferred();
		const returnedFirstPage = deferred();
		const firstPageLodgings = [{ id: 7, name: "First page" }];
		const secondPageLodgings = [{ id: 36, name: "Second page" }];
		getRecommendations
			.mockResolvedValueOnce(page({ lodgings: firstPageLodgings, totalPages: 2 }))
			.mockReturnValueOnce(secondPage.promise)
			.mockReturnValueOnce(returnedFirstPage.promise);
		const { result } = renderHook(() => useHomeRecommendations());

		await settleInitial(result);
		act(() => result.current.setPage(1));
		expect(result.current.page).toBe(0);
		expect(result.current.lodgings).toEqual(firstPageLodgings);

		await act(async () => {
			secondPage.resolve(page({ lodgings: secondPageLodgings, currentPage: 1, totalPages: 2 }));
			await secondPage.promise;
		});
		await waitFor(() => expect(result.current.page).toBe(1));

		act(() => result.current.setPage(0));
		expect(result.current.page).toBe(1);
		expect(result.current.lodgings).toEqual(secondPageLodgings);

		await act(async () => {
			returnedFirstPage.resolve(page({ lodgings: firstPageLodgings, totalPages: 2 }));
			await returnedFirstPage.promise;
		});
		await waitFor(() => expect(result.current.page).toBe(0));

		expect(result.current.lodgings).toEqual(firstPageLodgings);
		expect(getRecommendations.mock.calls.map(([request]) => request)).toEqual([
			{ seed: FIXED_SEED, page: 0, revision: undefined },
			{ seed: FIXED_SEED, page: 1, revision: "rev-1" },
			{ seed: FIXED_SEED, page: 0, revision: "rev-1" },
		]);
	});

	it("rejects an older response, advances list generation, and keeps cards during a transition", async () => {
		const next = deferred();
		getRecommendations
			.mockResolvedValueOnce(page())
			.mockReturnValueOnce(next.promise)
			.mockResolvedValueOnce(page({ lodgings: [{ id: 3, name: "Newest" }] }));
		const { result } = renderHook(() => useHomeRecommendations());

		await settleInitial(result);
		expect(result.current.listGeneration).toBe(1);
		act(() => result.current.setPage(1));
		expect(result.current.lodgings).toEqual([{ id: 1, name: "Cabaña del Lago" }]);
		expect(result.current.listBusy).toBe(true);
		act(() => result.current.setPage(0));
		await waitFor(() => expect(result.current.lodgings).toEqual([{ id: 3, name: "Newest" }]));

		await act(async () => {
			next.resolve(page({ lodgings: [{ id: 2, name: "Stale" }], currentPage: 1 }));
			await next.promise;
		});
		expect(result.current.lodgings).toEqual([{ id: 3, name: "Newest" }]);
		expect(result.current.listGeneration).toBe(2);
	});

	it("reports failures and retries without clearing an already rendered list", async () => {
		getRecommendations
			.mockResolvedValueOnce(page())
			.mockRejectedValueOnce(new Error("network down"))
			.mockResolvedValueOnce(page({ lodgings: [{ id: 8, name: "Recovered" }] }));
		const { result } = renderHook(() => useHomeRecommendations());

		await settleInitial(result);
		crypto.randomUUID.mockReturnValue("22222222-2222-4222-8222-222222222222");
		await act(async () => result.current.refresh());
		await waitFor(() => expect(result.current.status).toBe("error"));
		expect(result.current.lodgings).toEqual([{ id: 1, name: "Cabaña del Lago" }]);

		await act(async () => result.current.retry());
		await settleInitial(result);
		expect(result.current.lodgings).toEqual([{ id: 8, name: "Recovered" }]);
	});
});
