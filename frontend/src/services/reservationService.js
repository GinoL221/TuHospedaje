import { get } from "./api";

function appendParam(params, key, value) {
	if (value === undefined || value === null || value === "") return;
	params.set(key, String(value));
}

export function getAdminReservations({
	page = 0,
	size = 10,
	sort = "id",
	direction = "asc",
	status = "",
	q = "",
} = {}) {
	const params = new URLSearchParams();
	appendParam(params, "page", page);
	appendParam(params, "size", size);
	appendParam(params, "sort", sort);
	appendParam(params, "direction", direction);
	appendParam(params, "status", status);
	appendParam(params, "q", q.trim());

	const query = params.toString();
	return get(query ? `/reservations/admin?${query}` : "/reservations/admin");
}
