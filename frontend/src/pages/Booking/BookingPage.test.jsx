import { Routes, Route } from "react-router-dom";
import {
	customRender,
	screen,
	userEvent,
	makeAuthValue,
	waitFor,
	fireEvent,
} from "../../test/test-utils";
import BookingPage from "./BookingPage";
import { get, post } from "../../services/api";
import {
	getDateCellByLabelPart,
	selectDateByLabelPart,
} from "../../test/date-picker-utils";

vi.mock("../../services/api");

function ConfirmationSentinel() {
	return <div data-testid="confirmation-sentinel">confirmation page</div>;
}

const lodgingFixture = {
	id: 1,
	name: "Cabaña del Lago",
	city: "Bariloche",
	country: "Argentina",
	pricePerNight: 100,
	description: "Una cabaña con vista al lago.",
	imageUrls: ["https://example.com/img.jpg"],
	features: [{ id: 1, icon: "wifi", name: "WiFi" }],
};

function mockGetDefaults({
	lodging = lodgingFixture,
	myReservations = [],
	availability = { available: true, occupiedRanges: [] },
} = {}) {
	get.mockImplementation((endpoint) => {
		if (endpoint === "/reservations/my") {
			return Promise.resolve(myReservations);
		}
		if (endpoint.startsWith(`/lodgings/${lodging?.id ?? 1}/availability`)) {
			return Promise.resolve(availability);
		}
		if (endpoint === `/lodgings/${lodging?.id ?? 1}`) {
			return Promise.resolve(lodging);
		}
		return Promise.resolve(null);
	});
}

function deferred() {
	let resolve;
	let reject;
	const promise = new Promise((res, rej) => {
		resolve = res;
		reject = rej;
	});
	return { promise, resolve, reject };
}

// Lets a test control each successive call to the availability endpoint
// independently (initial load, a dated re-load, and the submit-time
// preflight all hit the same endpoint).
function mockGetSequenced({
	lodging = lodgingFixture,
	myReservations = [],
	availabilityResponses = [{}],
} = {}) {
	let availabilityCallIndex = 0;
	get.mockImplementation((endpoint) => {
		if (endpoint === "/reservations/my") {
			return Promise.resolve(myReservations);
		}
		if (endpoint.startsWith(`/lodgings/${lodging.id}/availability`)) {
			const entry =
				availabilityResponses[
					Math.min(availabilityCallIndex, availabilityResponses.length - 1)
				];
			availabilityCallIndex += 1;
			return typeof entry === "function" ? entry() : Promise.resolve(entry);
		}
		if (endpoint === `/lodgings/${lodging.id}`) return Promise.resolve(lodging);
		return Promise.resolve(null);
	});
}

function renderBookingPage({
	authValue,
	initialEntries = ["/booking/1"],
	route = "/booking/:lodgingId",
} = {}) {
	return customRender(
		<Routes>
			<Route path={route} element={<BookingPage />} />
			<Route path="/booking/confirmation" element={<ConfirmationSentinel />} />
		</Routes>,
		{ authValue, initialEntries },
	);
}

describe("BookingPage - loading and summary", () => {
	it("shows a loading state before the lodging resolves, then renders the summary", async () => {
		mockGetDefaults();
		renderBookingPage();

		expect(screen.getByText("Cargando...")).toBeInTheDocument();

		expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
		expect(screen.getByText("Bariloche, Argentina")).toBeInTheDocument();
		expect(
			screen.getByText("Una cabaña con vista al lago."),
		).toBeInTheDocument();
	});

	it("renders the lodging's image and features in the summary", async () => {
		mockGetDefaults();
		renderBookingPage();

		await screen.findByText("Cabaña del Lago");

		const image = screen.getByRole("img", { name: "Cabaña del Lago" });
		expect(image).toHaveAttribute("src", "https://example.com/img.jpg");

		expect(screen.getByText("WiFi")).toBeInTheDocument();
	});

	it("omits the image and features blocks when the lodging has none", async () => {
		mockGetDefaults({
			lodging: { ...lodgingFixture, imageUrls: [], features: [] },
		});
		renderBookingPage();

		await screen.findByText("Cabaña del Lago");

		expect(
			screen.queryByRole("img", { name: "Cabaña del Lago" }),
		).not.toBeInTheDocument();
		expect(screen.queryByText("WiFi")).not.toBeInTheDocument();
	});
});

describe("BookingPage - guest phone prefill", () => {
	it("prefills guestPhone from the latest prior reservation", async () => {
		// API returns reservations ordered by checkIn DESC: most recent first
		mockGetDefaults({
			myReservations: [
				{ id: 2, guestPhone: "222222" }, // most recent (comes first in DESC list)
				{ id: 1, guestPhone: "111111" }, // older reservation
			],
		});
		renderBookingPage();

		await screen.findByText("Cabaña del Lago");

		await waitFor(() => {
			const phoneInput = screen.getByLabelText("Teléfono");
			expect(phoneInput.value).toBe("222222");
		});
	});

	it("leaves guestPhone empty when there are no prior reservations", async () => {
		mockGetDefaults({ myReservations: [] });
		renderBookingPage();

		await screen.findByText("Cabaña del Lago");

		const phoneInput = screen.getByLabelText("Teléfono");
		expect(phoneInput.value).toBe("");
	});
});

describe("BookingPage - total computed from nights x price", () => {
	it("computes and displays the total for a 3-night stay preloaded via location.state", async () => {
		mockGetDefaults();
		const authValue = makeAuthValue();
		renderBookingPage({
			authValue,
			initialEntries: [
				{
					pathname: "/booking/1",
					state: { checkIn: "2026-07-01", checkOut: "2026-07-04" },
				},
			],
		});

		await screen.findByText("Cabaña del Lago");

		expect(screen.getByText(/3 noches/)).toBeInTheDocument();
		expect(screen.getByText("$300")).toBeInTheDocument();
	});

	it("uses the singular '1 noche' label for a 1-night stay", async () => {
		mockGetDefaults();
		const authValue = makeAuthValue();
		const { container } = renderBookingPage({
			authValue,
			initialEntries: [
				{
					pathname: "/booking/1",
					state: { checkIn: "2026-07-01", checkOut: "2026-07-02" },
				},
			],
		});

		await screen.findByText("Cabaña del Lago");

		const totalText = container.querySelector(".booking-total").textContent;
		expect(totalText).toMatch(/1 noche\b/);
		expect(totalText).not.toMatch(/1 noches/);
		expect(totalText).toContain("$100");
	});

	it("computes a total of $0 when the lodging has no pricePerNight", async () => {
		mockGetDefaults({ lodging: { ...lodgingFixture, pricePerNight: 0 } });
		const authValue = makeAuthValue();
		const { container } = renderBookingPage({
			authValue,
			initialEntries: [
				{
					pathname: "/booking/1",
					state: { checkIn: "2026-07-01", checkOut: "2026-07-04" },
				},
			],
		});

		await screen.findByText("Cabaña del Lago");

		const totalText = container.querySelector(".booking-total").textContent;
		expect(totalText).toMatch(/3 noches/);
		expect(totalText).toContain("$0");
	});
});

// NOTE: This describe block only verifies the submit button's disabled state
// based on whether checkIn/checkOut are present. It does NOT cover the actual
// occupied-dates filtering logic (isDateOccupied / the filterDate prop passed
// to react-datepicker). Testing that would require interacting with the
// react-datepicker calendar widget directly, which is a known coverage gap.
describe("BookingPage - submit button enablement", () => {
	it("disables the submit button until both dates are selected", async () => {
		mockGetDefaults({
			availability: {
				occupiedRanges: [{ checkIn: "2026-07-10", checkOut: "2026-07-15" }],
			},
		});
		renderBookingPage();

		await screen.findByText("Cabaña del Lago");

		expect(
			screen.getByRole("button", { name: "Confirmar reserva" }),
		).toBeDisabled();
	});

	it("enables the submit button once both dates are preloaded via location.state", async () => {
		mockGetDefaults();
		renderBookingPage({
			initialEntries: [
				{
					pathname: "/booking/1",
					state: { checkIn: "2026-07-01", checkOut: "2026-07-04" },
				},
			],
		});

		await screen.findByText("Cabaña del Lago");

		expect(
			screen.getByRole("button", { name: "Confirmar reserva" }),
		).not.toBeDisabled();
	});
});

describe("BookingPage - submit without dates", () => {
	it("shows an inline error and does not call post when dates are missing", async () => {
		mockGetDefaults();
		const { container } = renderBookingPage();

		await screen.findByText("Cabaña del Lago");

		// The submit button is disabled with no dates selected, so we dispatch the
		// form's submit event directly to exercise the handleSubmit guard clause
		// (the same code path the spec requires us to characterize).
		fireEvent.submit(container.querySelector("form.booking-form"));

		expect(
			await screen.findByText("Seleccioná un rango de fechas."),
		).toBeInTheDocument();
		expect(post).not.toHaveBeenCalled();
	});
});

describe("BookingPage - successful reservation", () => {
	it("navigates to /booking/confirmation with reservation and lodging state", async () => {
		mockGetDefaults();
		const reservationFixture = {
			id: 99,
			checkIn: "2026-07-01",
			checkOut: "2026-07-04",
		};
		post.mockResolvedValue(reservationFixture);
		const user = userEvent.setup();
		const authValue = makeAuthValue();
		renderBookingPage({
			authValue,
			initialEntries: [
				{
					pathname: "/booking/1",
					state: { checkIn: "2026-07-01", checkOut: "2026-07-04" },
				},
			],
		});

		await screen.findByText("Cabaña del Lago");

		const phoneInput = screen.getByLabelText("Teléfono");
		await user.type(phoneInput, "123456");
		await user.click(screen.getByRole("button", { name: "Confirmar reserva" }));

		expect(post).toHaveBeenCalledWith("/reservations", {
			lodgingId: 1,
			checkIn: "2026-07-01",
			checkOut: "2026-07-04",
			guestName: `${authValue.user.firstName} ${authValue.user.lastName}`,
			guestEmail: authValue.user.email,
			guestPhone: "123456",
		});
		expect(
			await screen.findByTestId("confirmation-sentinel"),
		).toBeInTheDocument();
	});
});

describe("BookingPage - reservation submit error", () => {
	it("renders the server error message and stops loading on a failed reservation", async () => {
		mockGetDefaults();
		post.mockRejectedValue(
			new Error("Las fechas seleccionadas ya no están disponibles."),
		);
		const { container } = renderBookingPage({
			initialEntries: [
				{
					pathname: "/booking/1",
					state: { checkIn: "2026-07-01", checkOut: "2026-07-04" },
				},
			],
		});

		await screen.findByText("Cabaña del Lago");

		fireEvent.submit(container.querySelector("form.booking-form"));

		expect(
			await screen.findByText(
				"Las fechas seleccionadas ya no están disponibles.",
			),
		).toBeInTheDocument();
		expect(
			screen.getByRole("button", { name: "Confirmar reserva" }),
		).not.toBeDisabled();
	});
});

// NOTE: minCheckoutDate's date-arithmetic contract (day-after-checkIn,
// month/year rollover, null/undefined fallback) is unit-tested exhaustively
// in src/utils/dateRange.test.js. The test below only checks the thin
// page-specific wiring: that the check-out DatePicker's minDate prop is
// actually connected to minCheckoutDate(checkIn), not duplicating the
// date-math assertions already owned by dateRange.test.js.
describe("BookingPage - check-out calendar minimum date", () => {
	beforeEach(() => {
		vi.useFakeTimers({ shouldAdvanceTime: true });
		vi.setSystemTime(new Date("2026-07-15"));
	});

	afterEach(() => {
		vi.useRealTimers();
	});

	it("disables the same day as check-in in the check-out calendar, requiring at least one night", async () => {
		mockGetDefaults();
		const authValue = makeAuthValue();
		const user = userEvent.setup();
		renderBookingPage({ authValue });

		await screen.findByText("Cabaña del Lago");
		await waitFor(() =>
			expect(screen.getByLabelText("Check-in")).not.toBeDisabled(),
		);

		await selectDateByLabelPart(
			user,
			screen.getByLabelText("Check-in"),
			"July 15th, 2026",
		);

		// Open the check-out calendar: the same day must now be disabled,
		// proving minDate={minCheckoutDate(checkIn)} is actually wired up
		// (a booking requires at least one night, so checkOut > checkIn).
		await user.click(screen.getByLabelText("Check-out"));
		const sameDayInCheckoutCalendar = getDateCellByLabelPart("July 15th, 2026");

		expect(sameDayInCheckoutCalendar).toHaveAttribute("aria-disabled", "true");
	});
});

// Covers isDateOccupied / the filterDate prop wired into both DatePickers:
// dates that overlap an existing reservation must be blocked, using the same
// [checkIn, checkOut) exclusive-end semantics as the backend
// (LodgingServiceImpl.checkAvailability). There was previously zero test
// coverage of this filtering behavior.
describe("BookingPage - occupied date filtering", () => {
	beforeEach(() => {
		vi.useFakeTimers({ shouldAdvanceTime: true });
		// Use a midday local time, not UTC midnight: "2026-07-01" parses as UTC
		// midnight, which in a UTC-3 timezone is "2026-06-30T21:00" local time,
		// shifting the DatePicker's default-open month to June and breaking
		// any assertion on July dates.
		vi.setSystemTime(new Date("2026-07-01T12:00:00"));
	});

	afterEach(() => {
		vi.useRealTimers();
	});

	it("blocks dates inside an existing reservation while leaving its checkout day and outside dates selectable", async () => {
		// Existing reservation occupies July 20-22 (checkOut 23 is exclusive,
		// i.e. the 3-night stay is [2026-07-20, 2026-07-23)).
		mockGetDefaults({
			availability: {
				occupiedRanges: [{ checkIn: "2026-07-20", checkOut: "2026-07-23" }],
			},
		});
		const authValue = makeAuthValue();
		const user = userEvent.setup();
		renderBookingPage({ authValue });

		await screen.findByText("Cabaña del Lago");
		await waitFor(() =>
			expect(screen.getByLabelText("Check-in")).not.toBeDisabled(),
		);

		const checkInInput = screen.getByLabelText("Check-in");
		await user.click(checkInInput);

		const july20 = getDateCellByLabelPart("July 20th, 2026");
		const july21 = getDateCellByLabelPart("July 21st, 2026");
		const july22 = getDateCellByLabelPart("July 22nd, 2026");
		const july23 = getDateCellByLabelPart("July 23rd, 2026");
		const july25 = getDateCellByLabelPart("July 25th, 2026");

		// Strictly inside the occupied range: blocked.
		expect(july20).toHaveAttribute("aria-disabled", "true");
		expect(july21).toHaveAttribute("aria-disabled", "true");
		expect(july22).toHaveAttribute("aria-disabled", "true");

		// The reservation's checkout day is the exclusive upper bound, so it
		// must remain selectable (this is what makes same-day turnover work).
		expect(july23).toHaveAttribute("aria-disabled", "false");

		// A date outside the occupied range entirely is also selectable.
		expect(july25).toHaveAttribute("aria-disabled", "false");
	});
});

describe("BookingPage - availability preflight and conflict recovery", () => {
	const preloadedDatesEntry = {
		pathname: "/booking/1",
		state: { checkIn: "2026-07-01", checkOut: "2026-07-04" },
	};

	it("keeps the submit button disabled while preloaded dates await the current availability load", async () => {
		const pending = deferred();
		mockGetSequenced({ availabilityResponses: [() => pending.promise] });
		renderBookingPage({
			authValue: makeAuthValue(),
			initialEntries: [preloadedDatesEntry],
		});

		await screen.findByText("Cabaña del Lago");

		expect(screen.getByRole("status")).toHaveTextContent(
			"Comprobando disponibilidad",
		);
		expect(
			screen.getByRole("button", { name: "Confirmar reserva" }),
		).toBeDisabled();

		pending.resolve({ available: true, occupiedRanges: [] });

		await waitFor(() =>
			expect(
				screen.getByRole("button", { name: "Confirmar reserva" }),
			).not.toBeDisabled(),
		);
	});

	it("blocks a direct form submit and shows an inline message until availability reports ready, without calling post", async () => {
		const pending = deferred();
		mockGetSequenced({ availabilityResponses: [() => pending.promise] });
		const { container } = renderBookingPage({
			authValue: makeAuthValue(),
			initialEntries: [preloadedDatesEntry],
		});

		await screen.findByText("Cabaña del Lago");

		fireEvent.submit(container.querySelector("form.booking-form"));

		expect(
			await screen.findByText(
				"Estamos verificando la disponibilidad. Probá de nuevo en un instante.",
			),
		).toBeInTheDocument();
		expect(post).not.toHaveBeenCalled();

		pending.resolve({ available: true, occupiedRanges: [] });
	});

	it("runs a successful range preflight before creating the reservation", async () => {
		mockGetSequenced({
			availabilityResponses: [
				{ available: true, occupiedRanges: [] },
				{ available: true, occupiedRanges: [] },
			],
		});
		const reservationFixture = {
			id: 99,
			checkIn: "2026-07-01",
			checkOut: "2026-07-04",
		};
		post.mockResolvedValue(reservationFixture);
		const authValue = makeAuthValue();
		const user = userEvent.setup();
		renderBookingPage({
			authValue,
			initialEntries: [preloadedDatesEntry],
		});

		await screen.findByText("Cabaña del Lago");
		await waitFor(() =>
			expect(
				screen.getByRole("button", { name: "Confirmar reserva" }),
			).not.toBeDisabled(),
		);

		await user.type(screen.getByLabelText("Teléfono"), "123456");
		await user.click(screen.getByRole("button", { name: "Confirmar reserva" }));

		expect(post).toHaveBeenCalledWith("/reservations", {
			lodgingId: 1,
			checkIn: "2026-07-01",
			checkOut: "2026-07-04",
			guestName: `${authValue.user.firstName} ${authValue.user.lastName}`,
			guestEmail: authValue.user.email,
			guestPhone: "123456",
		});
		expect(
			await screen.findByTestId("confirmation-sentinel"),
		).toBeInTheDocument();
	});

	it("shows an inline conflict message and never calls post when the preflight finds the range unavailable", async () => {
		mockGetSequenced({
			availabilityResponses: [
				{ available: true, occupiedRanges: [] },
				{
					available: false,
					occupiedRanges: [{ checkIn: "2026-07-01", checkOut: "2026-07-04" }],
				},
			],
		});
		const authValue = makeAuthValue();
		const user = userEvent.setup();
		renderBookingPage({
			authValue,
			initialEntries: [preloadedDatesEntry],
		});

		await screen.findByText("Cabaña del Lago");
		await waitFor(() =>
			expect(
				screen.getByRole("button", { name: "Confirmar reserva" }),
			).not.toBeDisabled(),
		);

		await user.type(screen.getByLabelText("Teléfono"), "123456");
		await user.click(screen.getByRole("button", { name: "Confirmar reserva" }));

		expect(
			await screen.findByText(
				"Las fechas seleccionadas ya no están disponibles. Elegí otro rango.",
			),
		).toHaveAttribute("role", "alert");
		expect(post).not.toHaveBeenCalled();
		expect(
			screen.queryByTestId("confirmation-sentinel"),
		).not.toBeInTheDocument();
	});

	it("keeps the backend overlap rejection as final authority, shows an inline conflict, and refreshes availability for recovery", async () => {
		mockGetSequenced({
			availabilityResponses: [
				{ available: true, occupiedRanges: [] },
				{ available: true, occupiedRanges: [] },
				{
					available: false,
					occupiedRanges: [{ checkIn: "2026-07-01", checkOut: "2026-07-04" }],
				},
			],
		});
		post.mockRejectedValue(
			new Error("Las fechas seleccionadas ya no están disponibles."),
		);
		const authValue = makeAuthValue();
		const user = userEvent.setup();
		renderBookingPage({
			authValue,
			initialEntries: [preloadedDatesEntry],
		});

		await screen.findByText("Cabaña del Lago");
		await waitFor(() =>
			expect(
				screen.getByRole("button", { name: "Confirmar reserva" }),
			).not.toBeDisabled(),
		);

		await user.type(screen.getByLabelText("Teléfono"), "123456");
		await user.click(screen.getByRole("button", { name: "Confirmar reserva" }));

		expect(
			await screen.findByText(
				"Las fechas seleccionadas ya no están disponibles.",
			),
		).toHaveAttribute("role", "alert");
		expect(post).toHaveBeenCalledTimes(1);
		expect(
			screen.queryByTestId("confirmation-sentinel"),
		).not.toBeInTheDocument();

		// Recovery refresh: a third availability call (beyond the initial load
		// and the pre-post preflight) proves the hook re-fetched current
		// occupied ranges after the backend rejected the booking.
		await waitFor(() =>
			expect(
				get.mock.calls.filter((call) =>
					call[0].startsWith("/lodgings/1/availability"),
				).length,
			).toBe(3),
		);
	});

	it("shows an accessible error with Retry when the initial availability load fails, and clears it on a successful retry", async () => {
		mockGetSequenced({
			availabilityResponses: [
				() => Promise.reject(new Error("down")),
				{ available: true, occupiedRanges: [] },
			],
		});
		const user = userEvent.setup();
		renderBookingPage({
			authValue: makeAuthValue(),
			initialEntries: [preloadedDatesEntry],
		});

		await screen.findByText("Cabaña del Lago");

		const alert = await screen.findByRole("alert");
		expect(alert).toHaveTextContent("No pudimos obtener la disponibilidad");
		expect(
			screen.getByRole("button", { name: "Confirmar reserva" }),
		).toBeDisabled();
		expect(screen.getByLabelText("Check-in")).not.toBeDisabled();
		expect(screen.getByLabelText("Check-out")).not.toBeDisabled();

		await user.click(screen.getByRole("button", { name: "Reintentar" }));

		await screen.findByText("Todas las fechas están disponibles.");
		expect(screen.queryByRole("alert")).not.toBeInTheDocument();
		expect(
			screen.getByRole("button", { name: "Confirmar reserva" }),
		).not.toBeDisabled();
	});

	it("keeps date inputs editable after a stale refresh while submission remains blocked", async () => {
		vi.useFakeTimers({ shouldAdvanceTime: true });
		vi.setSystemTime(new Date("2026-07-01T12:00:00"));
		try {
			mockGetSequenced({
				availabilityResponses: [
					{ available: true, occupiedRanges: [] },
					() => Promise.reject(new Error("down")),
				],
			});
			const user = userEvent.setup();
			renderBookingPage({
				authValue: makeAuthValue(),
				initialEntries: [preloadedDatesEntry],
			});

			await screen.findByText("Cabaña del Lago");
			await waitFor(() =>
				expect(screen.getByRole("button", { name: "Confirmar reserva" })).not.toBeDisabled(),
			);
			await selectDateByLabelPart(user, screen.getByLabelText("Check-in"), "July 2nd, 2026");

			await screen.findByRole("alert");
			expect(screen.getByLabelText("Check-in")).not.toBeDisabled();
			expect(screen.getByLabelText("Check-out")).not.toBeDisabled();
			expect(screen.getByRole("button", { name: "Confirmar reserva" })).toBeDisabled();
		} finally {
			vi.useRealTimers();
		}
	});

	it("asks the user to retry when the preflight returns no technical result", async () => {
		mockGetSequenced({
			availabilityResponses: [{ available: true, occupiedRanges: [] }, null],
		});
		const user = userEvent.setup();
		renderBookingPage({
			authValue: makeAuthValue(),
			initialEntries: [preloadedDatesEntry],
		});

		await screen.findByText("Cabaña del Lago");
		await waitFor(() =>
			expect(screen.getByRole("button", { name: "Confirmar reserva" })).not.toBeDisabled(),
		);
		await user.type(screen.getByLabelText("Teléfono"), "123456");
		await user.click(screen.getByRole("button", { name: "Confirmar reserva" }));

		expect(await screen.findByText("No pudimos verificar la disponibilidad. Reintentá antes de confirmar la reserva.")).toHaveAttribute("role", "alert");
		expect(post).not.toHaveBeenCalled();
	});
});

describe("BookingPage - current user via useAuth", () => {
	it("renders the authenticated user's name and email as read-only fields", async () => {
		mockGetDefaults();
		const authValue = makeAuthValue();
		renderBookingPage({ authValue });

		await screen.findByText("Cabaña del Lago");

		expect(screen.getByLabelText("Nombre")).toHaveValue(
			authValue.user.firstName,
		);
		expect(screen.getByLabelText("Apellido")).toHaveValue(
			authValue.user.lastName,
		);
		expect(screen.getByLabelText("Email")).toHaveValue(authValue.user.email);
	});
});
