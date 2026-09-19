import {
	cancelReservation,
	createReservation,
	getAdminReservations,
	getMyReservations,
} from "./reservationService";
import { get, patch, post } from "./api";

vi.mock("./api");

describe("reservationService - getAdminReservations", () => {
	it("builds the server query string from table state and calls get", async () => {
		get.mockResolvedValue({
			items: [],
			currentPage: 0,
			totalItems: 0,
			totalPages: 0,
		});

		await getAdminReservations({
			page: 2,
			size: 10,
			sort: "status",
			direction: "desc",
			status: "CONFIRMED",
			q: "  juan  ",
		});

		expect(get).toHaveBeenCalledWith(
			"/reservations/admin?page=2&size=10&sort=status&direction=desc&status=CONFIRMED&q=juan",
		);
	});

	it("omits blank search and filter values", async () => {
		get.mockResolvedValue({
			items: [],
			currentPage: 0,
			totalItems: 0,
			totalPages: 0,
		});

		await getAdminReservations();

		expect(get).toHaveBeenCalledWith(
			"/reservations/admin?page=0&size=10&sort=id&direction=asc",
		);
	});
});

describe("reservationService - createReservation", () => {
	it("posts the booking payload to the reservations endpoint", async () => {
		const payload = {
			lodgingId: 1,
			checkIn: "2026-07-01",
			checkOut: "2026-07-04",
			guestPhone: "123456",
		};
		post.mockResolvedValue({ id: 99 });

		await expect(createReservation(payload)).resolves.toEqual({ id: 99 });
		expect(post).toHaveBeenCalledWith("/reservations", payload);
	});

	it("passes optional notes without adding identity fields", async () => {
		const payload = {
			lodgingId: 1,
			checkIn: "2026-07-01",
			checkOut: "2026-07-04",
			guestPhone: "123456",
			notes: "Late arrival",
		};
		post.mockResolvedValue({ id: 100 });

		await createReservation(payload);
		expect(post).toHaveBeenCalledWith("/reservations", payload);
		expect(post.mock.calls[0][1]).not.toHaveProperty("guestName");
		expect(post.mock.calls[0][1]).not.toHaveProperty("guestEmail");
	});
});

describe("reservationService - getMyReservations", () => {
	it("gets the authenticated user's reservations", async () => {
		const reservations = [{ id: 7 }];
		get.mockResolvedValue(reservations);

		await expect(getMyReservations()).resolves.toEqual(reservations);
		expect(get).toHaveBeenCalledWith("/reservations/my");
	});

	it("returns an empty list response unchanged", async () => {
		get.mockResolvedValue([]);

		await expect(getMyReservations()).resolves.toEqual([]);
		expect(get).toHaveBeenCalledWith("/reservations/my");
	});
});

describe("reservationService - cancelReservation", () => {
	it("calls the owner cancellation endpoint", async () => {
		patch.mockResolvedValue({ id: 7, status: "CANCELLED" });

		await expect(cancelReservation(7)).resolves.toEqual({
			id: 7,
			status: "CANCELLED",
		});
		expect(patch).toHaveBeenCalledWith("/reservations/7/cancel");
	});
});
