import { get, post } from "./api";
import {
  createRating,
  getRatingEligibility,
  getRatingsByLodging,
} from "./ratingService";

vi.mock("./api");

describe("ratingService", () => {
  it("gets ratings and eligibility using their existing endpoints", () => {
    getRatingsByLodging(42);
    getRatingEligibility(42);

    expect(get).toHaveBeenNthCalledWith(1, "/ratings/lodging/42");
    expect(get).toHaveBeenNthCalledWith(2, "/ratings/lodging/42/eligibility");
  });

  it("posts the supplied rating payload to the existing endpoint", () => {
    const payload = { lodgingId: 42, score: 5, comment: "Excellent" };

    createRating(payload);

    expect(post).toHaveBeenCalledWith("/ratings", payload);
  });
});
