import { del, get, post, put } from "./api";

function appendParam(params, key, value) {
	if (value === undefined || value === null || value === "") return;
	params.set(key, String(value));
}

export function getAdminStats() {
	return get("/admin/stats");
}

export function getAdminLodgings({
	page = 0,
	size = 10,
	sort = "id",
	direction = "asc",
	q = "",
} = {}) {
	const params = new URLSearchParams();
	appendParam(params, "page", page);
	appendParam(params, "size", size);
	appendParam(params, "sort", sort);
	appendParam(params, "direction", direction);
	appendParam(params, "q", q.trim());

	return get(`/lodgings/admin?${params.toString()}`);
}

export function getCategories() {
	return get("/categories");
}

export function createCategory(payload) {
	return post("/categories", payload);
}

export function updateCategory(id, payload) {
	return put(`/categories/${id}`, payload);
}

export function deleteCategory(id) {
	return del(`/categories/${id}`);
}

export function getFeatures() {
	return get("/features");
}

export function createFeature(payload) {
	return post("/features", payload);
}

export function updateFeature(id, payload) {
	return put(`/features/${id}`, payload);
}

export function deleteFeature(id) {
	return del(`/features/${id}`);
}

export function getPolicies() {
	return get("/policies");
}

export function createPolicy(payload) {
	return post("/policies", payload);
}

export function updatePolicy(id, payload) {
	return put(`/policies/${id}`, payload);
}

export function deletePolicy(id) {
	return del(`/policies/${id}`);
}

export function getUsers() {
	return get("/users");
}

export function updateUserRole(id, role) {
	return put(`/users/${id}/role`, { role });
}

export function createLodging(payload) {
	return post("/lodgings", payload);
}

export function updateLodging(id, payload) {
	return put(`/lodgings/${id}`, payload);
}

export function deleteLodging(id) {
	return del(`/lodgings/${id}`);
}
