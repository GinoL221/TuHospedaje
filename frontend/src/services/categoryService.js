import { get } from "./api";

export function getCategories() {
  return get("/categories");
}
