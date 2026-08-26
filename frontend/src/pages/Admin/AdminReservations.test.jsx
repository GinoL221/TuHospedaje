import { customRender, screen, userEvent, waitFor } from "../../test/test-utils";
import AdminReservations from "./AdminReservations";
import { getAdminReservations } from "../../services/reservationService";

vi.mock("../../services/reservationService");

beforeEach(() => {
	getAdminReservations.mockReset();
});

const reservationFixture = (overrides = {}) => ({
	id: 1,
	lodgingName: "Cabaña del Lago",
	guestName: "Ana Gomez",
	checkIn: "2026-07-01",
	checkOut: "2026-07-05",
	totalPrice: 400,
  status: "CONFIRMED",
  createdAt: "2026-06-20T14:30:00",
  createdAtDerived: false,
  notes: "Necesito una cuna",
	...overrides,
});

function renderAdminReservations(options) {
	return customRender(<AdminReservations />, options);
}

describe("AdminReservations - listing", () => {
	it("shows a loading state before the fetch resolves", async () => {
		let resolveFetch;
		getAdminReservations.mockImplementation(
			() =>
				new Promise((resolve) => {
					resolveFetch = resolve;
				})
		);

		renderAdminReservations();

		expect(screen.getByText("Cargando reservas...")).toBeInTheDocument();

		resolveFetch({ items: [], currentPage: 0, totalItems: 0, totalPages: 0 });
		await waitFor(() => {
			expect(screen.queryByText("Cargando reservas...")).not.toBeInTheDocument();
		});
	});

	it("renders the empty state when there are no reservations", async () => {
		getAdminReservations.mockResolvedValue({ items: [], currentPage: 0, totalItems: 0, totalPages: 0 });
		renderAdminReservations();

		expect(await screen.findByText("No hay reservas registradas.")).toBeInTheDocument();
	});

  it("renders a row per reservation with lodging, guest, dates, total and status", async () => {
		getAdminReservations.mockResolvedValue({
			items: [
				reservationFixture({ id: 1, lodgingName: "Cabaña del Lago", status: "CONFIRMED" }),
				reservationFixture({ id: 2, lodgingName: "Hotel Centro", status: "CANCELLED" }),
			],
			currentPage: 0,
			totalItems: 2,
			totalPages: 1,
		});
		renderAdminReservations();

		expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
		expect(screen.getByText("Hotel Centro")).toBeInTheDocument();
		expect(screen.getAllByText("Ana Gomez")).toHaveLength(2);

		const row1 = screen.getByTestId("row-1");
		const row2 = screen.getByTestId("row-2");
		expect(row1).toHaveTextContent("Confirmada");
		expect(row2).toHaveTextContent("Cancelada");
		expect(row1.querySelector(".status-badge")).toHaveClass("status-confirmed");
		expect(row2.querySelector(".status-badge")).toHaveClass("status-cancelled");
	});

	it("shows exact creation time and notes for each reservation", async () => {
		getAdminReservations.mockResolvedValue({
			items: [reservationFixture()], currentPage: 0, totalItems: 1, totalPages: 1,
		});
		renderAdminReservations();

		const row = await screen.findByTestId("row-1");
		expect(row).toHaveTextContent("Fecha de creación: 20/06/2026 14:30");
		expect(row).toHaveTextContent("Necesito una cuna");
	});

	it("labels derived creation time as estimated and omits blank notes", async () => {
		getAdminReservations.mockResolvedValue({
			items: [reservationFixture({ createdAt: "2026-07-01T00:00:00", createdAtDerived: true, notes: " " })],
			currentPage: 0, totalItems: 1, totalPages: 1,
		});
		renderAdminReservations();

		const row = await screen.findByTestId("row-1");
		expect(row).toHaveTextContent("Fecha estimada: 01/07/2026 00:00");
		expect(row).toHaveTextContent("-");
	});

	it("fetches from the admin reservations endpoint on mount", async () => {
		getAdminReservations.mockResolvedValue({ items: [reservationFixture()], currentPage: 0, totalItems: 1, totalPages: 1 });
		renderAdminReservations();

		await screen.findByText("Cabaña del Lago");
		expect(getAdminReservations).toHaveBeenCalledWith({
			page: 0,
			size: 10,
			sort: "id",
			direction: "asc",
			status: "",
			q: "",
		});
	});

	it("shows an error state and recovers after retry", async () => {
		const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
		getAdminReservations
			.mockRejectedValueOnce(new Error("fail"))
			.mockResolvedValueOnce({ items: [reservationFixture()], currentPage: 0, totalItems: 1, totalPages: 1 });

		const user = userEvent.setup();
		renderAdminReservations();

		expect(await screen.findByRole("alert")).toHaveTextContent("No se pudieron cargar las reservas.");
		expect(screen.queryByText("No hay reservas registradas.")).not.toBeInTheDocument();

		await user.click(screen.getByRole("button", { name: "Reintentar" }));
		expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
		expect(consoleErrorSpy).toHaveBeenCalled();
		consoleErrorSpy.mockRestore();
	});

	it("fetches the next page when pagination changes", async () => {
		getAdminReservations.mockResolvedValue({
			items: [reservationFixture()],
			currentPage: 0,
			totalItems: 20,
			totalPages: 2,
		});
		const user = userEvent.setup();
		renderAdminReservations();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByRole("button", { name: "Siguiente" }));

		await waitFor(() => {
			expect(getAdminReservations).toHaveBeenCalledWith(
				expect.objectContaining({ page: 1, sort: "id", direction: "asc" }),
			);
		});
	});

	it("keeps display columns from triggering unsupported sort requests", async () => {
		getAdminReservations.mockResolvedValue({
			items: [reservationFixture()],
			currentPage: 0,
			totalItems: 1,
			totalPages: 1,
		});
		const user = userEvent.setup();
		renderAdminReservations();

		await screen.findByText("Cabaña del Lago");
		const initialCalls = getAdminReservations.mock.calls.length;

		await user.click(screen.getByRole("columnheader", { name: "Alojamiento" }));
		await user.click(screen.getByRole("columnheader", { name: "Huésped" }));

		expect(getAdminReservations).toHaveBeenCalledTimes(initialCalls);
	});

	it("fetches sorted data and resets to the first page when a sortable header is clicked", async () => {
		getAdminReservations.mockImplementation((params) => {
			if (params.sort === "status" && params.direction === "asc") {
				return Promise.resolve({
					items: [reservationFixture({ id: 2, lodgingName: "Hotel Centro", status: "CANCELLED" })],
					currentPage: 0,
					totalItems: 1,
					totalPages: 1,
				});
			}

			return Promise.resolve({
				items: [reservationFixture()],
				currentPage: 0,
				totalItems: 1,
				totalPages: 1,
			});
		});
		const user = userEvent.setup();
		renderAdminReservations();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByRole("columnheader", { name: /Estado/ }));

		await waitFor(() => {
			expect(getAdminReservations).toHaveBeenCalledWith(
				expect.objectContaining({ page: 0, sort: "status", direction: "asc" }),
			);
		});
		expect(await screen.findByText("Hotel Centro")).toBeInTheDocument();
	});

	it("refetches even when reset returns to the current defaults", async () => {
		let resolveRefetch;
		getAdminReservations
			.mockResolvedValueOnce({ items: [reservationFixture()], currentPage: 0, totalItems: 1, totalPages: 1 })
			.mockImplementationOnce(
				() =>
					new Promise((resolve) => {
						resolveRefetch = resolve;
					})
				);

		const user = userEvent.setup();
		renderAdminReservations();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByRole("button", { name: "Limpiar filtros" }));

		await waitFor(() => {
			expect(getAdminReservations).toHaveBeenCalledTimes(2);
		});
		expect(screen.getByText("Cargando reservas...")).toBeInTheDocument();

		resolveRefetch({ items: [reservationFixture()], currentPage: 0, totalItems: 1, totalPages: 1 });
		expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
		expect(screen.queryByText("Cargando reservas...")).not.toBeInTheDocument();
	});

	it("fetches filtered data and resets the page when search and status change", async () => {
		getAdminReservations.mockImplementation((params) => {
			if (params.status === "CONFIRMED" && params.q === "juan") {
				return Promise.resolve({
					items: [reservationFixture({ id: 3, guestName: "Juan Perez" })],
					currentPage: 0,
					totalItems: 1,
					totalPages: 1,
				});
			}

			return Promise.resolve({
				items: [reservationFixture()],
				currentPage: 1,
				totalItems: 20,
				totalPages: 2,
			});
		});
		const user = userEvent.setup();
		renderAdminReservations();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByRole("button", { name: "Siguiente" }));
		await screen.findByText("Cabaña del Lago");
		await user.selectOptions(screen.getByLabelText("Filtrar por estado"), "CONFIRMED");
		await user.type(screen.getByLabelText("Buscar reservas"), "juan");

		await waitFor(() => {
			expect(getAdminReservations).toHaveBeenCalledWith(
				expect.objectContaining({ page: 0, status: "CONFIRMED", q: "juan" }),
			);
		});
		expect(await screen.findByText("Juan Perez")).toBeInTheDocument();
		expect(screen.queryByText("Hotel Centro")).not.toBeInTheDocument();
	});
});

describe("AdminReservations - read-only scope", () => {
	it("renders no create/edit/delete controls anywhere in the document", async () => {
		getAdminReservations.mockResolvedValue({ items: [reservationFixture()], currentPage: 0, totalItems: 1, totalPages: 1 });
		renderAdminReservations();

		await screen.findByText("Cabaña del Lago");

		expect(screen.queryByTestId("admin-add-btn")).not.toBeInTheDocument();
		expect(screen.queryByTestId("admin-modal")).not.toBeInTheDocument();
		expect(screen.queryByTestId("row-edit-btn")).not.toBeInTheDocument();
		expect(screen.queryByTestId("row-delete-btn")).not.toBeInTheDocument();
		expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
		expect(screen.queryByRole("button", { name: /eliminar|editar|agregar/i })).not.toBeInTheDocument();
	});
});
