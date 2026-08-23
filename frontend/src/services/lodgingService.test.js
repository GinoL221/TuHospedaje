import { searchLodgings, getRecommendations } from "./lodgingService";
import { get } from "./api";

vi.mock("./api");

describe("lodgingService - searchLodgings", () => {
  it("builds the query string from params and calls get with it", async () => {
    get.mockResolvedValue({ lodgings: [], currentPage: 0, totalItems: 0, totalPages: 0 });

    await searchLodgings({ city: "Bariloche", page: "1" });

    expect(get).toHaveBeenCalledWith("/lodgings/search?city=Bariloche&page=1");
  });

  it("calls get with no query string when params is empty/undefined", async () => {
    get.mockResolvedValue({});

    await searchLodgings();

    expect(get).toHaveBeenCalledWith("/lodgings/search");
  });

  it("resolves with the response returned by get", async () => {
    const response = { lodgings: [{ id: 1 }], currentPage: 0, totalItems: 1, totalPages: 1 };
    get.mockResolvedValue(response);

    const result = await searchLodgings({ city: "Bariloche" });

    expect(result).toEqual(response);
  });
});

describe("lodgingService - getRecommendations", () => {
  it("builds seed, page, and fixed size 10 without a revision param", async () => {
    get.mockResolvedValue({ lodgings: [], currentPage: 0, totalItems: 0, totalPages: 0, revision: "r1", reset: false });

    await getRecommendations({ seed: "seed-value-0123456789", page: 0 });

    expect(get).toHaveBeenCalledWith(
      "/lodgings/recommendations?seed=seed-value-0123456789&page=0&size=8",
    );
  });

  it("includes the revision param only when a revision is supplied", async () => {
    get.mockResolvedValue({ lodgings: [], currentPage: 2, totalItems: 0, totalPages: 3, revision: "r2", reset: false });

    await getRecommendations({ seed: "seed-value-0123456789", page: 2, revision: "r1" });

    expect(get).toHaveBeenCalledWith(
      "/lodgings/recommendations?seed=seed-value-0123456789&page=2&size=8&revision=r1",
    );
  });

  it("resolves with the response returned by get", async () => {
    const response = { lodgings: [{ id: 1 }], currentPage: 0, totalItems: 1, totalPages: 1, revision: "r1", reset: false };
    get.mockResolvedValue(response);

    const result = await getRecommendations({ seed: "seed-value-0123456789", page: 0 });

    expect(result).toEqual(response);
  });
});
