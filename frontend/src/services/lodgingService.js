import { get } from "./api";

export function getLodging(id) {
	return get(`/lodgings/${id}`);
}

function formatDate(date) {
	const year = date.getFullYear();
	const month = String(date.getMonth() + 1).padStart(2, "0");
	const day = String(date.getDate()).padStart(2, "0");
	return `${year}-${month}-${day}`;
}

export function getAvailability(id, { checkIn, checkOut } = {}) {
	const params = new URLSearchParams();
	if (checkIn) params.set("checkIn", formatDate(checkIn));
	if (checkOut) params.set("checkOut", formatDate(checkOut));
	const query = params.size ? `?${params.toString()}` : "";
	return get(`/lodgings/${id}/availability${query}`);
}

export function searchLodgings(params) {
	const query =
		typeof params === "string"
			? params
			: params
				? `?${new URLSearchParams(params).toString()}`
				: "";
	return get(`/lodgings/search${query}`);
}

export function getCities(query) {
	return get(`/lodgings/cities?q=${encodeURIComponent(query)}`);
}

// Fixed page size of 8 fills two desktop rows of four cards; the backend
// accepts 1–10. The revision param is included only when the caller already
// holds one from a previous response (see design.md §1, API contract).
export function getRecommendations({ seed, page = 0, revision } = {}) {
	const params = new URLSearchParams({ seed, page: String(page), size: "8" });
	if (revision) params.set("revision", revision);
	return get(`/lodgings/recommendations?${params.toString()}`);
}
