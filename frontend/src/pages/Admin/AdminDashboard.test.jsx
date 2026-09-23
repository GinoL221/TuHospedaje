import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import AdminDashboard from "./AdminDashboard";
import { get } from "../../services/api";

vi.mock("../../services/api");

const EMPTY_STATS = {
	lodgings: 0,
	categories: 0,
	features: 0,
	users: 0,
	reservations: 0,
};

function page(items) {
	return { items, currentPage: 0, totalItems: items.length, totalPages: 1 };
}

function mockGetDefaults({
	stats = {},
	recentLodgings = [],
	reservations = [],
} = {}) {
	get.mockImplementation((endpoint) => {
		if (endpoint === "/admin/stats")
			return Promise.resolve({ ...EMPTY_STATS, ...stats });
		if (endpoint.startsWith("/lodgings/admin"))
			return Promise.resolve(page(recentLodgings));
		if (endpoint.startsWith("/reservations/admin"))
			return Promise.resolve(page(reservations));
		return Promise.resolve([]);
	});
}

describe("AdminDashboard - stat count", () => {
	it("shows … initially then the count once the fetch resolves", async () => {
		mockGetDefaults({ stats: { lodgings: 2 } });
		render(<AdminDashboard onTabChange={vi.fn()} />);

		expect(screen.getAllByText("…").length).toBeGreaterThan(0);
		expect(await screen.findByText("2")).toBeInTheDocument();
	});

	/**
	 * The defect this replaced: counts were the .length of listing payloads, and the
	 * lodgings listing caps its result set — so past that cap the card displayed the cap
	 * rather than the real total. The count must come from the server's own tally, which
	 * is why a total far larger than any page renders exactly.
	 */
	it("renders a total larger than any page, because counts come from the server not from list lengths", async () => {
		mockGetDefaults({
			stats: { lodgings: 250 },
			recentLodgings: [{ id: 1, name: "Hotel Sol" }],
		});
		render(<AdminDashboard onTabChange={vi.fn()} />);

		expect(await screen.findByText("250")).toBeInTheDocument();
	});

	it("asks only for counts and the two recent pages, never for a whole table", async () => {
		mockGetDefaults();
		render(<AdminDashboard onTabChange={vi.fn()} />);

		await waitFor(() => expect(get).toHaveBeenCalledWith("/admin/stats"));

		const requested = get.mock.calls.map(([endpoint]) => endpoint);
		expect(requested).toHaveLength(3);
		expect(requested).not.toContain("/lodgings");
		expect(requested).not.toContain("/reservations");
		expect(requested).not.toContain("/users");
		expect(requested).not.toContain("/categories");
		expect(requested).not.toContain("/features");
	});

	it("shows — on every card when the stats request rejects", async () => {
		get.mockImplementation((endpoint) => {
			if (endpoint === "/admin/stats") return Promise.reject(new Error("fail"));
			return Promise.resolve(page([]));
		});
		render(<AdminDashboard onTabChange={vi.fn()} />);

		await waitFor(() => expect(screen.getAllByText("—")).toHaveLength(5));
	});

	it("keeps successful recent sections visible when lodgings fail", async () => {
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/admin"))
				return Promise.reject(new Error("fail"));
			if (endpoint.startsWith("/reservations/admin")) {
				return Promise.resolve(
					page([
						{
							id: 1,
							lodgingName: "Cabaña",
							guestName: "Ana García",
							checkIn: "2026-07-01",
							checkOut: "2026-07-04",
							totalPrice: 300,
							status: "CONFIRMED",
						},
					]),
				);
			}
			return Promise.resolve({ ...EMPTY_STATS, lodgings: 7 });
		});
		render(<AdminDashboard onTabChange={vi.fn()} />);

		expect(await screen.findByText("7")).toBeInTheDocument();
		expect(screen.getByText("Últimas reservas")).toBeInTheDocument();
		expect(
			screen.getByText("No pudimos cargar los alojamientos recientes."),
		).toBeInTheDocument();
	});

	it("retries each failed section independently and recovers only that section", async () => {
		const requests = { stats: 0, lodgings: 0, reservations: 0 };
		get.mockImplementation((endpoint) => {
			if (endpoint === "/admin/stats") {
				requests.stats += 1;
				return requests.stats === 1
					? Promise.reject(new Error("fail"))
					: Promise.resolve({ ...EMPTY_STATS, lodgings: 9 });
			}
			if (endpoint.startsWith("/lodgings/admin")) {
				requests.lodgings += 1;
				return requests.lodgings === 1
					? Promise.reject(new Error("fail"))
					: Promise.resolve(page([{ id: 2, name: "Hotel Sol" }]));
			}
			if (endpoint.startsWith("/reservations/admin")) {
				requests.reservations += 1;
				return requests.reservations === 1
					? Promise.reject(new Error("fail"))
					: Promise.resolve(
							page([
								{
									id: 3,
									lodgingName: "Hostal",
									guestName: "Luis Pérez",
									checkIn: "2026-08-01",
									checkOut: "2026-08-03",
									totalPrice: 200,
									status: "CONFIRMED",
								},
							]),
						);
			}
			return Promise.resolve([]);
		});
		const user = userEvent.setup();
		render(<AdminDashboard onTabChange={vi.fn()} />);

		await screen.findByText("No pudimos cargar las estadísticas.");
		expect(
			screen.getByText("No pudimos cargar los alojamientos recientes."),
		).toBeInTheDocument();
		expect(
			screen.getByText("No pudimos cargar las reservas recientes."),
		).toBeInTheDocument();

		const retryButtons = screen.getAllByRole("button", { name: "Reintentar" });
		await user.click(retryButtons[0]);
		expect(await screen.findByText("9")).toBeInTheDocument();
		expect(
			screen.queryByText("No pudimos cargar las estadísticas."),
		).not.toBeInTheDocument();
		expect(requests).toEqual({ stats: 2, lodgings: 1, reservations: 1 });

		await user.click(retryButtons[1]);
		expect(await screen.findByText("Hotel Sol")).toBeInTheDocument();
		expect(
			screen.queryByText("No pudimos cargar los alojamientos recientes."),
		).not.toBeInTheDocument();
		expect(requests).toEqual({ stats: 2, lodgings: 2, reservations: 1 });

		await user.click(retryButtons[2]);
		expect(await screen.findByText("Hostal")).toBeInTheDocument();
		expect(
			screen.queryByText("No pudimos cargar las reservas recientes."),
		).not.toBeInTheDocument();
		expect(requests).toEqual({ stats: 2, lodgings: 2, reservations: 2 });
	});
});

describe("AdminDashboard - tab navigation", () => {
	it("uses a native button and calls onTabChange once when a stat card is clicked", async () => {
		mockGetDefaults();
		const onTabChange = vi.fn();
		const user = userEvent.setup();
		render(<AdminDashboard onTabChange={onTabChange} />);

		const card = screen.getByRole("button", { name: /Alojamientos/ });
		expect(card.tagName).toBe("BUTTON");
		await user.click(card);
		expect(onTabChange).toHaveBeenCalledTimes(1);
		expect(onTabChange).toHaveBeenCalledWith("lodgings");
	});

	it("calls onTabChange once when a stat card is activated with Enter", async () => {
		mockGetDefaults();
		const onTabChange = vi.fn();
		const user = userEvent.setup();
		render(<AdminDashboard onTabChange={onTabChange} />);

		const card = screen.getByRole("button", { name: /Categorías/ });
		await user.click(card);
		onTabChange.mockClear();
		await user.keyboard("{Enter}");
		expect(onTabChange).toHaveBeenCalledTimes(1);
		expect(onTabChange).toHaveBeenCalledWith("categories");
	});

	it("calls onTabChange once when a stat card is activated with Space", async () => {
		mockGetDefaults();
		const onTabChange = vi.fn();
		const user = userEvent.setup();
		render(<AdminDashboard onTabChange={onTabChange} />);

		const card = screen.getByText("Reservas").closest(".stat-card");
		expect(card).not.toBeNull();
		await user.click(card);
		onTabChange.mockClear();
		await user.keyboard("[Space]");
		expect(onTabChange).toHaveBeenCalledTimes(1);
		expect(onTabChange).toHaveBeenCalledWith("reservations");
	});
});

describe("AdminDashboard - recent lodgings table", () => {
	it("renders the recent lodgings table when there are recent lodgings", async () => {
		mockGetDefaults({ recentLodgings: [{ id: 1, name: "Hotel Sol" }] });
		render(<AdminDashboard onTabChange={vi.fn()} />);

		expect(await screen.findByText("Últimos alojamientos")).toBeInTheDocument();
		expect(screen.getByText("Hotel Sol")).toBeInTheDocument();
	});

	it("does not render the recent lodgings table when the list is empty", async () => {
		mockGetDefaults();
		render(<AdminDashboard onTabChange={vi.fn()} />);

		await waitFor(() => expect(get).toHaveBeenCalled());
		expect(screen.queryByText("Últimos alojamientos")).not.toBeInTheDocument();
	});
});

describe("AdminDashboard - recent reservations table", () => {
	it("does not render the recent reservations table when the list is empty", async () => {
		mockGetDefaults();
		render(<AdminDashboard onTabChange={vi.fn()} />);

		await waitFor(() => expect(get).toHaveBeenCalled());
		expect(screen.queryByText("Últimas reservas")).not.toBeInTheDocument();
	});

	it("renders status badges — Confirmada for CONFIRMED and Cancelada otherwise", async () => {
		const reservations = [
			{
				id: 1,
				lodgingName: "Cabaña",
				guestName: "Ana García",
				checkIn: "2026-07-01",
				checkOut: "2026-07-04",
				totalPrice: 300,
				status: "CONFIRMED",
				createdAt: "2026-06-20T14:30:00",
				createdAtDerived: false,
				notes: "Necesito una cuna",
			},
			{
				id: 2,
				lodgingName: "Hostal",
				guestName: "Luis Pérez",
				checkIn: "2026-08-01",
				checkOut: "2026-08-03",
				totalPrice: 200,
				status: "CANCELLED",
			},
		];
		mockGetDefaults({ reservations });
		render(<AdminDashboard onTabChange={vi.fn()} />);

		expect(await screen.findByText("Últimas reservas")).toBeInTheDocument();
		expect(screen.getByText("Confirmada")).toBeInTheDocument();
		expect(screen.getByText("Cancelada")).toBeInTheDocument();
	});

	it("shows reservation notes and labels derived creation times as estimated", async () => {
		mockGetDefaults({
			reservations: [
				{
					id: 1,
					lodgingName: "Cabaña",
					guestName: "Ana García",
					checkIn: "2026-07-01",
					checkOut: "2026-07-04",
					totalPrice: 300,
					status: "CONFIRMED",
					createdAt: "2026-07-01T00:00:00",
					createdAtDerived: true,
					notes: "Llegada tarde",
				},
			],
		});
		render(<AdminDashboard onTabChange={vi.fn()} />);

		expect(
			await screen.findByText("Fecha estimada: 01/07/2026 00:00"),
		).toBeInTheDocument();
		expect(screen.getByText("Llegada tarde")).toBeInTheDocument();
	});

	it("omits empty notes from recent reservations", async () => {
		mockGetDefaults({
			reservations: [
				{
					id: 1,
					lodgingName: "Cabaña",
					guestName: "Ana García",
					checkIn: "2026-07-01",
					checkOut: "2026-07-04",
					totalPrice: 300,
					status: "CONFIRMED",
					createdAt: "2026-06-20T14:30:00",
					createdAtDerived: false,
					notes: " ",
				},
			],
		});
		render(<AdminDashboard onTabChange={vi.fn()} />);

		expect(
			await screen.findByText("Fecha de creación: 20/06/2026 14:30"),
		).toBeInTheDocument();
		expect(
			screen.getByText("Fecha de creación: 20/06/2026 14:30").closest("tr"),
		).toHaveTextContent("-");
	});
});
