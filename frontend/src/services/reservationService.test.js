import { cancelReservation, getAdminReservations } from "./reservationService";
import { get, patch } from "./api";

vi.mock("./api");

describe("reservationService - getAdminReservations", () => {
	it("builds the server query string from table state and calls get", async () => {
		get.mockResolvedValue({ items: [], currentPage: 0, totalItems: 0, totalPages: 0 });

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
		get.mockResolvedValue({ items: [], currentPage: 0, totalItems: 0, totalPages: 0 });

		await getAdminReservations();

		expect(get).toHaveBeenCalledWith(
			"/reservations/admin?page=0&size=10&sort=id&direction=asc",
		);
	});
});

describe("reservationService - cancelReservation", () => {
	it("calls the owner cancellation endpoint", async () => {
		patch.mockResolvedValue({ id: 7, status: "CANCELLED" });

		await expect(cancelReservation(7)).resolves.toEqual({ id: 7, status: "CANCELLED" });
		expect(patch).toHaveBeenCalledWith("/reservations/7/cancel");
	});
});
