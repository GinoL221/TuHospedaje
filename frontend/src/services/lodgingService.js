import { get } from "./api";

export function searchLodgings(params) {
  const query = params ? `?${new URLSearchParams(params).toString()}` : "";
  return get(`/lodgings/search${query}`);
}

// Fixed page size of 10 matches the backend's default/maximum recommendation
// page size; the revision param is included only when the caller already
// holds one from a previous response (see design.md §1, API contract).
export function getRecommendations({ seed, page = 0, revision } = {}) {
  const params = new URLSearchParams({ seed, page: String(page), size: "10" });
  if (revision) params.set("revision", revision);
  return get(`/lodgings/recommendations?${params.toString()}`);
}
