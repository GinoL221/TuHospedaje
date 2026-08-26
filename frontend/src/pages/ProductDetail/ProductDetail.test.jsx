import { Routes, Route, useLocation, useNavigate } from "react-router-dom";
import {
	customRender,
	screen,
	userEvent,
	makeAuthValue,
	waitFor,
} from "../../test/test-utils";
import ProductDetail from "./ProductDetail";
import { get } from "../../services/api";
import {
	getDateCellByLabelPart,
	selectDateByLabelPart,
} from "../../test/date-picker-utils";

vi.mock("../../services/api");

function BookingSentinel() {
	return <div data-testid="booking-sentinel">booking page</div>;
}

function LodgingNavigation() {
	const navigate = useNavigate();
	return (
		<>
			<button type="button" onClick={() => navigate("/lodgings/2")}>Ver otro alojamiento</button>
			<button type="button" onClick={() => navigate("/lodgings/1")}>Volver al alojamiento</button>
		</>
	);
}

const lodgingFixture = {
	id: 1,
	name: "Cabaña del Lago",
	city: "Bariloche",
	country: "Argentina",
	description: "Una cabaña con vista al lago.",
	pricePerNight: 100,
	imageUrls: ["https://example.com/img.jpg"],
};

function mockGetDefaults({
	lodging = lodgingFixture,
	availability = {},
	ratings = {},
} = {}) {
	get.mockImplementation((endpoint) => {
		if (endpoint.startsWith(`/lodgings/${lodging?.id ?? 1}/availability`)) {
			return Promise.resolve(availability);
		}
		if (endpoint === `/lodgings/${lodging?.id ?? 1}`) {
			return Promise.resolve(lodging);
		}
		if (endpoint.startsWith("/ratings/lodging/")) {
			return Promise.resolve({ average: 0, count: 0, ratings: [], ...ratings });
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
// independently (e.g. the initial dateless load vs. a later dated
// re-fetch triggered by selecting both check-in/check-out).
function mockGetSequenced({
	lodging = lodgingFixture,
	ratings = {},
	availabilityResponses = [{}],
} = {}) {
	let availabilityCallIndex = 0;
	get.mockImplementation((endpoint) => {
		if (endpoint.startsWith(`/lodgings/${lodging.id}/availability`)) {
			const entry =
				availabilityResponses[
					Math.min(availabilityCallIndex, availabilityResponses.length - 1)
				];
			availabilityCallIndex += 1;
			return typeof entry === "function" ? entry() : Promise.resolve(entry);
		}
		if (endpoint === `/lodgings/${lodging.id}`) return Promise.resolve(lodging);
		if (endpoint.startsWith("/ratings/lodging/"))
			return Promise.resolve({ average: 0, count: 0, ratings: [], ...ratings });
		return Promise.resolve(null);
	});
}

function LoginSentinel() {
	const location = useLocation();
	return (
		<div data-testid="login-sentinel">
			login page, from: {location.state?.from?.pathname ?? "none"}
			{location.state?.message && `, message: ${location.state.message}`}
		</div>
	);
}

function renderProductDetail({
	authValue,
	initialEntries = ["/lodgings/1"],
} = {}) {
	return customRender(
		<Routes>
			<Route
				path="/lodgings/:id"
				element={
					<>
						<LodgingNavigation />
						<ProductDetail />
					</>
				}
			/>
			<Route path="/booking/:id" element={<BookingSentinel />} />
			<Route path="/login" element={<LoginSentinel />} />
		</Routes>,
		{ authValue, initialEntries },
	);
}

describe("ProductDetail - rendering lodging detail", () => {
	it("shows a loading state before the lodging resolves, then renders the summary", async () => {
		mockGetDefaults();
		renderProductDetail();

		expect(screen.getByText("Cargando...")).toBeInTheDocument();

		expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
		expect(get).toHaveBeenCalledWith("/lodgings/1");
		expect(screen.getByText("Bariloche, Argentina")).toBeInTheDocument();
		expect(
			screen.getByText("Una cabaña con vista al lago."),
		).toBeInTheDocument();
	});
});

describe("ProductDetail - header navigation placement", () => {
	it("renders the back action after Share so it is the rightmost header control", async () => {
		mockGetDefaults();
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");
		const header = screen.getByRole("heading", { name: "Cabaña del Lago" }).parentElement.parentElement;

		expect(Array.from(header.children).map((child) => child.getAttribute("aria-label") || child.textContent))
			.toEqual(["Cabaña del LagoBariloche, Argentina", "Compartir", "Volver"]);
		expect(screen.getByRole("main", { name: "Cabaña del Lago" })).toHaveAttribute("aria-labelledby", "product-detail-title");
		expect(screen.getByRole("region", { name: "Reservar este alojamiento" })).toBeInTheDocument();
		expect(screen.getByRole("button", { name: "Volver" }).querySelector("svg")).toHaveAttribute("aria-hidden", "true");
	});

	it("does not override the header controls' DOM order through Share CSS", () => {
		const css = readFileSync(
			resolve(process.cwd(), "src/pages/ProductDetail/ProductDetail.css"),
			"utf8",
		);

		const shareRule = css.match(/\.btn-share\s*{([^}]*)}/)?.[1] ?? "";
		expect(shareRule).not.toMatch(/\border\s*:/);
		expect(css.match(/\.btn-share\s*\{/g)).toHaveLength(1);
		expect(css).toMatch(/\.product-detail\s*\{[^}]*overflow-x:\s*hidden/);
		expect(css).toMatch(/\.product-detail \.product-datepicker-popper[^}]*width:\s*min\(320px, 100vw\)/);
		expect(css).toMatch(/@media \(max-width: 768px\)[\s\S]*\.date-pickers[^}]*grid-template-columns:\s*1fr/);
		expect(css).toMatch(/@media \(prefers-reduced-motion: reduce\)/);
	});
});

describe("ProductDetail - useAuth integration for the reserve CTA", () => {
	it("shows a login prompt instead of the reserve button for anonymous users", async () => {
		mockGetDefaults();
		renderProductDetail({ authValue: null });

		await screen.findByText("Cabaña del Lago");

		expect(screen.getByRole("link", { name: "Iniciá sesión" })).toHaveAttribute(
			"href",
			"/login",
		);
		expect(
			screen.queryByRole("button", { name: "Reservar" }),
		).not.toBeInTheDocument();
	});

	it("preserves the current location as state.from on the login link, so login can redirect back here", async () => {
		mockGetDefaults();
		const user = userEvent.setup();
		renderProductDetail({ authValue: null, initialEntries: ["/lodgings/1"] });

		await screen.findByText("Cabaña del Lago");

		const loginLink = screen.getByRole("link", { name: "Iniciá sesión" });
		await user.click(loginLink);

		// Same pattern RequireAuth already uses: the login link must carry
		// state.from = current location, so LoginPage's
		// `location.state?.from?.pathname` resolves back here instead of
		// falling back to "/".
		expect(await screen.findByTestId("login-sentinel")).toHaveTextContent(
			"from: /lodgings/1",
		);
	});

	it("explains the mandatory login and registration path after selecting reserve", async () => {
		mockGetDefaults();
		const user = userEvent.setup();
		renderProductDetail({ authValue: null, initialEntries: ["/lodgings/1"] });

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByRole("link", { name: "Iniciá sesión" }));

		expect(await screen.findByTestId("login-sentinel")).toHaveTextContent(
			"message: Para reservar necesitás iniciar sesión. Si no tenés cuenta, registrate.",
		);
	});

	it("disables the reserve button for a logged-in user until both dates are selected", async () => {
		mockGetDefaults();
		const authValue = makeAuthValue();
		renderProductDetail({ authValue });

		await screen.findByText("Cabaña del Lago");

		expect(screen.getByRole("button", { name: "Reservar" })).toBeDisabled();
	});
});

describe("ProductDetail - navigation to booking", () => {
	beforeEach(() => {
		vi.useFakeTimers({ shouldAdvanceTime: true });
		vi.setSystemTime(new Date("2026-07-15"));
	});

	afterEach(() => {
		vi.useRealTimers();
	});

	it("navigates to /booking/:id with checkIn/checkOut state once dates are selected", async () => {
		mockGetDefaults();
		const authValue = makeAuthValue();
		const user = userEvent.setup();
		renderProductDetail({ authValue });

		await screen.findByText("Cabaña del Lago");

		await selectDateByLabelPart(
			user,
			screen.getByLabelText("Check-in"),
			"July 15th, 2026",
		);
		await selectDateByLabelPart(
			user,
			screen.getByLabelText("Check-out"),
			"July 16th, 2026",
		);

		const reserveButton = screen.getByRole("button", { name: "Reservar" });
		expect(reserveButton).not.toBeDisabled();
		await user.click(reserveButton);

		expect(await screen.findByTestId("booking-sentinel")).toBeInTheDocument();
	});

	// NOTE: minCheckoutDate's date-arithmetic contract (day-after-checkIn,
	// month/year rollover, null/undefined fallback) is unit-tested
	// exhaustively in src/utils/dateRange.test.js. This test only checks the
	// thin page-specific wiring: that the check-out DatePicker's minDate prop
	// is actually connected to minCheckoutDate(checkIn), not duplicating the
	// date-math assertions already owned by dateRange.test.js.
	it("disables the same day as check-in in the check-out calendar, requiring at least one night", async () => {
		mockGetDefaults();
		const authValue = makeAuthValue();
		const user = userEvent.setup();
		renderProductDetail({ authValue });

		await screen.findByText("Cabaña del Lago");

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

describe("ProductDetail - availability state machine", () => {
	beforeEach(() => {
		vi.useFakeTimers({ shouldAdvanceTime: true });
		vi.setSystemTime(new Date("2026-07-15"));
	});

	afterEach(() => {
		vi.useRealTimers();
	});

	it("shows an accessible loading status while availability resolves, then enables the date pickers", async () => {
		const pending = deferred();
		mockGetSequenced({ availabilityResponses: [() => pending.promise] });
		const authValue = makeAuthValue();
		renderProductDetail({ authValue });

		await screen.findByText("Cabaña del Lago");

		expect(screen.getByText("Comprobando disponibilidad...")).toHaveAttribute(
			"role",
			"status",
		);
		expect(screen.getByLabelText("Check-in")).toBeDisabled();
		expect(screen.getByLabelText("Check-out")).toBeDisabled();
		expect(screen.getByRole("button", { name: "Reservar" })).toBeDisabled();

		pending.resolve({ available: true, occupiedRanges: [] });
		await screen.findByText("Todas las fechas están disponibles.");

		expect(screen.getByLabelText("Check-in")).not.toBeDisabled();
		expect(screen.getByLabelText("Check-out")).not.toBeDisabled();
	});

	it("shows a usable status with zero occupied ranges instead of an error or indefinite loading state", async () => {
		mockGetSequenced({ availabilityResponses: [{ available: true, occupiedRanges: [] }] });
		renderProductDetail({ authValue: makeAuthValue() });

		await screen.findByText("Cabaña del Lago");

		expect(
			await screen.findByText("Todas las fechas están disponibles."),
		).toHaveAttribute("role", "status");
		expect(screen.getByText("Todas las fechas están disponibles.")).toHaveAttribute("id", "product-availability-status");
		expect(screen.getByRole("group", { name: "Fechas de la estadía" })).toHaveAttribute("aria-describedby", "product-availability-status");
		for (const label of ["Check-in", "Check-out"]) {
			const input = screen.getByLabelText(label);
			expect(input).toBeRequired();
			expect(input).toHaveAttribute("aria-required", "true");
			expect(input).toHaveAttribute("aria-describedby", "product-availability-status");
		}
	});

	it("disables occupied dates in the check-in calendar and blocks their selection", async () => {
		mockGetSequenced({
			availabilityResponses: [
				{
					available: false,
					occupiedRanges: [{ checkIn: "2026-07-15", checkOut: "2026-07-16" }],
				},
			],
		});
		const user = userEvent.setup();
		renderProductDetail({ authValue: makeAuthValue() });

		await screen.findByText("Cabaña del Lago");
		await waitFor(() =>
			expect(screen.getByLabelText("Check-in")).not.toBeDisabled(),
		);

		await user.click(screen.getByLabelText("Check-in"));
		const occupiedCell = getDateCellByLabelPart("July 15th, 2026");

		expect(occupiedCell).toHaveAttribute("aria-disabled", "true");
	});

	it("shows an accessible error with Retry on initial failure and keeps controls disabled", async () => {
		mockGetSequenced({ availabilityResponses: [() => Promise.reject(new Error("down"))] });
		renderProductDetail({ authValue: makeAuthValue() });

		await screen.findByText("Cabaña del Lago");

		const alert = await screen.findByRole("alert");
		expect(alert).toHaveTextContent("No pudimos obtener la disponibilidad");
		expect(alert).toHaveAttribute("id", "product-availability-alert");
		expect(screen.getByLabelText("Check-in")).toHaveAttribute("aria-describedby", "product-availability-alert");
		expect(screen.getByLabelText("Check-in")).toBeDisabled();
		expect(screen.getByRole("button", { name: "Reservar" })).toBeDisabled();
	});

	it("clears the error and enables controls when the user retries successfully", async () => {
		mockGetSequenced({
			availabilityResponses: [
				() => Promise.reject(new Error("down")),
				{ available: true, occupiedRanges: [] },
			],
		});
		const user = userEvent.setup();
		renderProductDetail({ authValue: makeAuthValue() });

		await screen.findByText("Cabaña del Lago");
		await screen.findByRole("alert");

		await user.click(screen.getByRole("button", { name: "Reintentar" }));

		await screen.findByText("Todas las fechas están disponibles.");
		expect(screen.queryByRole("alert")).not.toBeInTheDocument();
		expect(screen.getByLabelText("Check-in")).not.toBeDisabled();
	});

	it("keeps actionable failure feedback when the initial request and a retry both fail", async () => {
		mockGetSequenced({
			availabilityResponses: [
				() => Promise.reject(new Error("down")),
				() => Promise.reject(new Error("still down")),
			],
		});
		const user = userEvent.setup();
		renderProductDetail({ authValue: makeAuthValue() });

		await screen.findByText("Cabaña del Lago");
		await screen.findByRole("alert");

		await user.click(screen.getByRole("button", { name: "Reintentar" }));

		const alert = await screen.findByRole("alert");
		expect(alert).toHaveTextContent("No pudimos obtener la disponibilidad");
		expect(screen.getByRole("button", { name: "Reintentar" })).toBeInTheDocument();
	});

	it("marks a stale refresh as non-authoritative and keeps Reserve disabled after a ready state", async () => {
		mockGetSequenced({
			availabilityResponses: [
				{ available: true, occupiedRanges: [] },
				() => Promise.reject(new Error("refresh failed")),
			],
		});
		const user = userEvent.setup();
		renderProductDetail({ authValue: makeAuthValue() });

		await screen.findByText("Cabaña del Lago");
		await screen.findByText("Todas las fechas están disponibles.");

		await selectDateByLabelPart(
			user,
			screen.getByLabelText("Check-in"),
			"July 15th, 2026",
		);
		await selectDateByLabelPart(
			user,
			screen.getByLabelText("Check-out"),
			"July 16th, 2026",
		);

		const alert = await screen.findByRole("alert");
		expect(alert).toHaveTextContent(
			"No pudimos actualizar la disponibilidad",
		);
		expect(screen.getByRole("button", { name: "Reservar" })).toBeDisabled();
	});

	it("clears an already-selected range and shows a conflict message once a refreshed response marks it occupied", async () => {
		mockGetSequenced({
			availabilityResponses: [
				{ available: true, occupiedRanges: [] },
				{
					available: false,
					occupiedRanges: [{ checkIn: "2026-07-15", checkOut: "2026-07-16" }],
				},
			],
		});
		const user = userEvent.setup();
		renderProductDetail({ authValue: makeAuthValue() });

		await screen.findByText("Cabaña del Lago");
		await screen.findByText("Todas las fechas están disponibles.");

		await selectDateByLabelPart(
			user,
			screen.getByLabelText("Check-in"),
			"July 15th, 2026",
		);
		await selectDateByLabelPart(
			user,
			screen.getByLabelText("Check-out"),
			"July 16th, 2026",
		);

		expect(
			await screen.findByText(
				"Las fechas seleccionadas ya no están disponibles. Elegí otro rango.",
			),
		).toHaveAttribute("role", "alert");
		expect(screen.getByLabelText("Check-in")).toHaveValue("");
		expect(screen.getByLabelText("Check-out")).toHaveValue("");
		expect(screen.getByRole("button", { name: "Reservar" })).toBeDisabled();
	});

	it("clears a selection conflict when navigation starts loading another lodging", async () => {
		let availabilityCallIndex = 0;
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/") && endpoint.includes("/availability")) {
				const responses = [
					{ available: true, occupiedRanges: [] },
					{ available: false, occupiedRanges: [{ checkIn: "2026-07-15", checkOut: "2026-07-16" }] },
				];
				return Promise.resolve(responses[Math.min(availabilityCallIndex++, 1)]);
			}
			if (endpoint === "/lodgings/1") return Promise.resolve(lodgingFixture);
			if (endpoint === "/lodgings/2")
				return Promise.resolve({ ...lodgingFixture, id: 2, name: "Cabaña del Bosque" });
			if (endpoint.startsWith("/ratings/lodging/"))
				return Promise.resolve({ average: 0, count: 0, ratings: [] });
			return Promise.resolve(null);
		});
		const user = userEvent.setup();
		renderProductDetail({ authValue: makeAuthValue() });

		await screen.findByText("Todas las fechas están disponibles.");
		await selectDateByLabelPart(user, screen.getByLabelText("Check-in"), "July 15th, 2026");
		await selectDateByLabelPart(user, screen.getByLabelText("Check-out"), "July 16th, 2026");
		await screen.findByText("Las fechas seleccionadas ya no están disponibles. Elegí otro rango.");

		await user.click(screen.getByRole("button", { name: "Ver otro alojamiento" }));

		await waitFor(() =>
			expect(screen.queryByText("Las fechas seleccionadas ya no están disponibles. Elegí otro rango.")).not.toBeInTheDocument(),
		);

		await user.click(screen.getByRole("button", { name: "Volver al alojamiento" }));
		expect(screen.queryByText("Las fechas seleccionadas ya no están disponibles. Elegí otro rango.")).not.toBeInTheDocument();
	});

	it("clears a selection conflict after the user reselects a valid date range", async () => {
		mockGetSequenced({
			availabilityResponses: [
				{ available: true, occupiedRanges: [] },
				{ available: false, occupiedRanges: [{ checkIn: "2026-07-15", checkOut: "2026-07-16" }] },
			],
		});
		const user = userEvent.setup();
		renderProductDetail({ authValue: makeAuthValue() });

		await screen.findByText("Todas las fechas están disponibles.");
		await selectDateByLabelPart(user, screen.getByLabelText("Check-in"), "July 15th, 2026");
		await selectDateByLabelPart(user, screen.getByLabelText("Check-out"), "July 16th, 2026");
		await screen.findByText("Las fechas seleccionadas ya no están disponibles. Elegí otro rango.");

		await selectDateByLabelPart(user, screen.getByLabelText("Check-in"), "July 17th, 2026");
		await selectDateByLabelPart(user, screen.getByLabelText("Check-out"), "July 18th, 2026");

		expect(screen.queryByText("Las fechas seleccionadas ya no están disponibles. Elegí otro rango.")).not.toBeInTheDocument();
	});
});

describe("ProductDetail - ShareModal", () => {
	it("opens ShareModal with the current lodging's data and the real page URL", async () => {
		mockGetDefaults();
		const user = userEvent.setup();
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		expect(
			screen.queryByRole("heading", { name: "Compartir" }),
		).not.toBeInTheDocument();

		await user.click(screen.getByRole("button", { name: "Compartir" }));

		expect(
			screen.getByRole("heading", { name: "Compartir" }),
		).toBeInTheDocument();
		// Not a hardcoded placeholder: proves the modal is wired to this lodging.
		expect(screen.getByText("Bariloche")).toBeInTheDocument();
		expect(
			screen.getByRole("button", { name: "Copiar enlace" }),
		).toBeInTheDocument();
	});

	it("closes ShareModal when its close button is clicked", async () => {
		mockGetDefaults();
		const user = userEvent.setup();
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		await user.click(screen.getByRole("button", { name: "Compartir" }));
		expect(
			screen.getByRole("heading", { name: "Compartir" }),
		).toBeInTheDocument();

		await user.click(screen.getByRole("button", { name: "Cerrar" }));
		expect(
			screen.queryByRole("heading", { name: "Compartir" }),
		).not.toBeInTheDocument();
	});
});

describe("ProductDetail - GalleryModal", () => {
	it("delegates the first-five preview and full gallery to LodgingGallery", async () => {
		const imageUrls = Array.from({ length: 6 }, (_, index) => `https://example.com/image-${index + 1}.jpg`);
		mockGetDefaults({ lodging: { ...lodgingFixture, imageUrls } });
		const user = userEvent.setup();
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");
		expect(screen.getAllByRole("img", { name: /Cabaña del Lago - \d/ })).toHaveLength(5);
		await user.click(screen.getByRole("button", { name: "Ver más" }));
		expect(screen.getByText("1 / 6")).toBeInTheDocument();
	});

	it("opens GalleryModal showing the real image from the lodging's data", async () => {
		mockGetDefaults();
		const user = userEvent.setup();
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		expect(
			screen.queryByRole("dialog", { name: "Galería de imágenes" }),
		).not.toBeInTheDocument();

		await user.click(screen.getByRole("button", { name: "Abrir galería" }));

		const dialog = screen.getByRole("dialog", { name: "Galería de imágenes" });
		expect(dialog).toBeInTheDocument();
		expect(screen.getByRole("img", { name: "1 de 1" })).toHaveAttribute(
			"src",
			"https://example.com/img.jpg",
		);
	});

	it("navigates to the clicked thumbnail's real image when multiple images exist", async () => {
		const multiImageLodging = {
			...lodgingFixture,
			imageUrls: [
				"https://example.com/img.jpg",
				"https://example.com/img2.jpg",
			],
		};
		mockGetDefaults({ lodging: multiImageLodging });
		const user = userEvent.setup();
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		await user.click(screen.getByRole("button", { name: "Ver imagen 2" }));

		expect(screen.getByRole("img", { name: "2 de 2" })).toHaveAttribute(
			"src",
			"https://example.com/img2.jpg",
		);
	});

	it("changes the main image with bounded arrows without opening the modal", async () => {
		const multiImageLodging = {
			...lodgingFixture,
			imageUrls: [
				"https://example.com/img.jpg",
				"https://example.com/img2.jpg",
			],
		};
		mockGetDefaults({ lodging: multiImageLodging });
		const user = userEvent.setup();
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");
		const previous = screen.getByRole("button", { name: "Imagen anterior" });
		const next = screen.getByRole("button", { name: "Imagen siguiente" });

		expect(previous).toBeDisabled();
		expect(next).not.toBeDisabled();
		await user.click(next);

		expect(
			screen
				.getByRole("button", { name: "Abrir galería" })
				.querySelector("img"),
		).toHaveAttribute("src", "https://example.com/img2.jpg");
		expect(next).toBeDisabled();
		expect(previous).not.toBeDisabled();
		expect(
			screen.queryByRole("dialog", { name: "Galería de imágenes" }),
		).not.toBeInTheDocument();
	});

	it("centers the selected mobile thumbnail and disables motion when requested", async () => {
		const scrollIntoView = vi.fn();
		const originalScrollIntoView = Element.prototype.scrollIntoView;
		const originalMatchMedia = window.matchMedia;
		Element.prototype.scrollIntoView = scrollIntoView;
		window.matchMedia = vi.fn((query) => ({
			matches:
				query === "(max-width: 768px)" ||
				query === "(prefers-reduced-motion: reduce)",
		}));

		try {
			mockGetDefaults({
				lodging: {
					...lodgingFixture,
					imageUrls: [
						"https://example.com/img.jpg",
						"https://example.com/img2.jpg",
					],
				},
			});
			const user = userEvent.setup();
			renderProductDetail();

			await screen.findByText("Cabaña del Lago");
			await user.click(
				screen.getByRole("button", { name: "Imagen siguiente en galería" }),
			);

			expect(scrollIntoView).toHaveBeenLastCalledWith({
				inline: "center",
				block: "nearest",
				behavior: "auto",
			});
			expect(screen.getByRole("button", { name: "Ver imagen 2" })).toHaveAttribute(
				"aria-current",
				"true",
			);
		} finally {
			Element.prototype.scrollIntoView = originalScrollIntoView;
			window.matchMedia = originalMatchMedia;
		}
	});

});

describe("ProductDetail - Features detail", () => {
	it("renders only existing images when the gallery has fewer than five", async () => {
		mockGetDefaults({
			lodging: {
				...lodgingFixture,
				imageUrls: [
					"https://example.com/one.jpg",
					"https://example.com/two.jpg",
					"https://example.com/three.jpg",
				],
			},
		});
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		expect(screen.getAllByRole("img", { name: /Cabaña del Lago - \d/ }))
			.toHaveLength(3);
		expect(screen.getAllByRole("button", { name: /Ver imagen/ }))
			.toHaveLength(2);
	});

	it("renders up to four existing secondary images and opens every image from Ver más", async () => {
		const imageUrls = Array.from(
			{ length: 6 },
			(_, index) => `https://example.com/image-${index + 1}.jpg`,
		);
		mockGetDefaults({ lodging: { ...lodgingFixture, imageUrls } });
		const user = userEvent.setup();
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		expect(screen.getAllByRole("img", { name: /Cabaña del Lago - \d/ }))
			.toHaveLength(5);
		expect(screen.getByRole("button", { name: "Ver más" })).toBeInTheDocument();

		await user.click(screen.getByRole("button", { name: "Ver más" }));

		expect(screen.getByRole("dialog", { name: "Galería de imágenes" })).toBeInTheDocument();
		expect(screen.getByText("1 / 6")).toBeInTheDocument();
	});

	it("renders the lodging's features by name and icon", async () => {
		mockGetDefaults({
			lodging: {
				...lodgingFixture,
				features: [
					{ id: 1, icon: "wifi", name: "WiFi" },
					{ id: 2, icon: "car", name: "Estacionamiento" },
				],
			},
		});
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		expect(screen.getByText("Características")).toBeInTheDocument();
		expect(screen.getByText("WiFi")).toBeInTheDocument();
		expect(screen.getByText("Estacionamiento")).toBeInTheDocument();
		expect(screen.getByRole("img", { name: "Ícono de WiFi" })).toBeInTheDocument();
		expect(
			screen.getByRole("img", { name: "Ícono de Estacionamiento" }),
		).toBeInTheDocument();
	});

	it("does not render the features section when the lodging has none", async () => {
		mockGetDefaults({ lodging: { ...lodgingFixture, features: [] } });
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		expect(
			screen.queryByText("Características"),
		).not.toBeInTheDocument();
	});
});

describe("ProductDetail - Policies detail", () => {
	it("renders the lodging's policies by name and description", async () => {
		mockGetDefaults({
			lodging: {
				...lodgingFixture,
				policies: [
					{
						id: 1,
						icon: "ban",
						name: "No fumar",
						description: "Prohibido fumar dentro del alojamiento.",
					},
				],
			},
		});
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		expect(screen.getByText("Políticas")).toBeInTheDocument();
		expect(screen.getByText("No fumar")).toBeInTheDocument();
		expect(
			screen.getByText("Prohibido fumar dentro del alojamiento."),
		).toBeInTheDocument();
	});

	it("does not render the policies section when the lodging has none", async () => {
		mockGetDefaults({ lodging: { ...lodgingFixture, policies: [] } });
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		expect(screen.queryByText("Políticas")).not.toBeInTheDocument();
	});
});

describe("ProductDetail - ReviewsSection integration", () => {
	it("renders the nested ReviewsSection with data fetched for this lodging", async () => {
		mockGetDefaults({ ratings: { average: 4.2, count: 5, ratings: [] } });
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		expect(get).toHaveBeenCalledWith("/ratings/lodging/1");
		expect(await screen.findByText("4.2")).toBeInTheDocument();
		expect(screen.getByText("(5 reseñas)")).toBeInTheDocument();
	});
});
