import { searchLodgings } from "./lodgingService";
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
