import { get, post } from "./api";

export function getRatingsByLodging(lodgingId) {
	return get(`/ratings/lodging/${lodgingId}`);
}

export function getRatingEligibility(lodgingId) {
	return get(`/ratings/lodging/${lodgingId}/eligibility`);
}

export function createRating(payload) {
	return post("/ratings", payload);
}
