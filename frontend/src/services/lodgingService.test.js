import {
  getAvailability,
  getLodging,
  searchLodgings,
  getCities,
  getRecommendations,
} from "./lodgingService";
import { get } from "./api";

vi.mock("./api");

describe("lodgingService - getLodging", () => {
  it("requests the lodging endpoint for the supplied id", async () => {
    get.mockResolvedValue({ id: 42, name: "Cabaña del Lago" });

    await getLodging(42);

    expect(get).toHaveBeenCalledWith("/lodgings/42");
  });

  it("returns the response for a different lodging id", async () => {
    const response = { id: "abc-123", name: "Casa del Bosque" };
    get.mockResolvedValue(response);

    const result = await getLodging("abc-123");

    expect(get).toHaveBeenCalledWith("/lodgings/abc-123");
    expect(result).toEqual(response);
  });
});

describe("lodgingService - getAvailability", () => {
  it("requests availability without query parameters when no dates are supplied", async () => {
    get.mockResolvedValue({ available: true, occupiedRanges: [] });

    await getAvailability(42);

    expect(get).toHaveBeenCalledWith("/lodgings/42/availability");
  });

  it("formats local calendar dates and includes supplied check-in and check-out parameters", async () => {
    get.mockResolvedValue({ available: false, occupiedRanges: [] });

    await getAvailability("abc-123", {
      checkIn: new Date(2026, 0, 9),
      checkOut: new Date(2026, 10, 5),
    });

    expect(get).toHaveBeenCalledWith(
      "/lodgings/abc-123/availability?checkIn=2026-01-09&checkOut=2026-11-05",
    );
  });

  it("includes only the supplied check-in date", async () => {
    get.mockResolvedValue({ available: true, occupiedRanges: [] });

    await getAvailability(12, { checkIn: new Date(2026, 10, 5) });

    expect(get).toHaveBeenCalledWith(
      "/lodgings/12/availability?checkIn=2026-11-05",
    );
  });

  it("includes only the supplied check-out date", async () => {
    get.mockResolvedValue({ available: true, occupiedRanges: [] });

    await getAvailability(13, { checkOut: new Date(2026, 10, 6) });

    expect(get).toHaveBeenCalledWith(
      "/lodgings/13/availability?checkOut=2026-11-06",
    );
  });
});

describe("lodgingService - searchLodgings", () => {
  it("builds the query string from params and calls get with it", async () => {
    get.mockResolvedValue({
      lodgings: [],
      currentPage: 0,
      totalItems: 0,
      totalPages: 0,
    });

    await searchLodgings({ city: "Bariloche", page: "1" });

    expect(get).toHaveBeenCalledWith("/lodgings/search?city=Bariloche&page=1");
  });

  it("preserves an existing search string including its leading question mark", async () => {
    get.mockResolvedValue({});

    await searchLodgings("?city=San%20Mart%C3%ADn&categories=1");

    expect(get).toHaveBeenCalledWith(
      "/lodgings/search?city=San%20Mart%C3%ADn&categories=1",
    );
  });

  it("calls get with no query string when params is empty/undefined", async () => {
    get.mockResolvedValue({});

    await searchLodgings();

    expect(get).toHaveBeenCalledWith("/lodgings/search");
  });

  it("resolves with the response returned by get", async () => {
    const response = {
      lodgings: [{ id: 1 }],
      currentPage: 0,
      totalItems: 1,
      totalPages: 1,
    };
    get.mockResolvedValue(response);

    const result = await searchLodgings({ city: "Bariloche" });

    expect(result).toEqual(response);
  });
});

describe("lodgingService - getCities", () => {
  it("requests matching cities with the supplied query", async () => {
    get.mockResolvedValue(["Bariloche"]);

    await getCities("Ba");

    expect(get).toHaveBeenCalledWith("/lodgings/cities?q=Ba");
  });

  it("encodes city queries before requesting suggestions", async () => {
    get.mockResolvedValue(["San Martín"]);

    await getCities("San Martín");

    expect(get).toHaveBeenCalledWith("/lodgings/cities?q=San%20Mart%C3%ADn");
  });
});

describe("lodgingService - getRecommendations", () => {
  it("builds seed, page, and fixed size 10 without a revision param", async () => {
    get.mockResolvedValue({
      lodgings: [],
      currentPage: 0,
      totalItems: 0,
      totalPages: 0,
      revision: "r1",
      reset: false,
    });

    await getRecommendations({ seed: "seed-value-0123456789", page: 0 });

    expect(get).toHaveBeenCalledWith(
      "/lodgings/recommendations?seed=seed-value-0123456789&page=0&size=8",
    );
  });

  it("includes the revision param only when a revision is supplied", async () => {
    get.mockResolvedValue({
      lodgings: [],
      currentPage: 2,
      totalItems: 0,
      totalPages: 3,
      revision: "r2",
      reset: false,
    });

    await getRecommendations({
      seed: "seed-value-0123456789",
      page: 2,
      revision: "r1",
    });

    expect(get).toHaveBeenCalledWith(
      "/lodgings/recommendations?seed=seed-value-0123456789&page=2&size=8&revision=r1",
    );
  });

  it("resolves with the response returned by get", async () => {
    const response = {
      lodgings: [{ id: 1 }],
      currentPage: 0,
      totalItems: 1,
      totalPages: 1,
      revision: "r1",
      reset: false,
    };
    get.mockResolvedValue(response);

    const result = await getRecommendations({
      seed: "seed-value-0123456789",
      page: 0,
    });

    expect(result).toEqual(response);
  });
});
