import { get, post, del } from "./api";

export function getFavorites() {
  return get("/favorites");
}

export function addFavorite(lodgingId) {
  return post(`/favorites/${lodgingId}`);
}

export function removeFavorite(lodgingId) {
  return del(`/favorites/${lodgingId}`);
}
