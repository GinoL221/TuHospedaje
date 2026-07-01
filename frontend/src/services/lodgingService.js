import { get } from "./api";

export function searchLodgings(params) {
  const query = params ? `?${new URLSearchParams(params).toString()}` : "";
  return get(`/lodgings/search${query}`);
}
