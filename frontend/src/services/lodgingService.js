import { get } from "./api";

export function searchLodgings(params) {
  const query = params ? `?${new URLSearchParams(params).toString()}` : "";
  return get(`/lodgings/search${query}`);
}

// Fixed page size of 8 fills two desktop rows of four cards; the backend
// accepts 1–10. The revision param is included only when the caller already
// holds one from a previous response (see design.md §1, API contract).
export function getRecommendations({ seed, page = 0, revision } = {}) {
  const params = new URLSearchParams({ seed, page: String(page), size: "8" });
  if (revision) params.set("revision", revision);
  return get(`/lodgings/recommendations?${params.toString()}`);
}
