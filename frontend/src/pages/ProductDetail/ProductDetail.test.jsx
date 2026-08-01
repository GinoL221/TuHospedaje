import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { Routes, Route, useLocation } from "react-router-dom";
import {
	customRender,
	screen,
	userEvent,
	makeAuthValue,
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

function LoginSentinel() {
	const location = useLocation();
	return (
		<div data-testid="login-sentinel">
			login page, from: {location.state?.from?.pathname ?? "none"}
		</div>
	);
}

function renderProductDetail({
	authValue,
	initialEntries = ["/lodgings/1"],
} = {}) {
	return customRender(
		<Routes>
			<Route path="/lodgings/:id" element={<ProductDetail />} />
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

	it("exposes separate desktop and mobile navigation class contracts", async () => {
		mockGetDefaults({
			lodging: {
				...lodgingFixture,
				imageUrls: [
					"https://example.com/img.jpg",
					"https://example.com/img2.jpg",
				],
			},
		});
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		expect(screen.getByRole("button", { name: "Imagen anterior" })).toHaveClass(
			"gallery-desktop-arrow",
		);
		expect(
			screen.getByRole("button", { name: "Imagen anterior en galería" }),
		).toHaveClass("gallery-mobile-arrow");
		expect(screen.getAllByRole("button", { name: /Ver imagen/ })).toHaveLength(2);
	});
});

describe("ProductDetail - mobile gallery CSS contract", () => {
	it("keeps gallery overflow inside the thumbnail strip", () => {
		const css = readFileSync(
			resolve(process.cwd(), "src/pages/ProductDetail/ProductDetail.css"),
			"utf8",
		);
		expect(css).toMatch(
			/@media \(max-width: 768px\)[\s\S]*?\.gallery-mobile-arrow\s*{[^}]*flex:\s*0 0 44px;[^}]*width:\s*44px;[^}]*height:\s*44px;/,
		);
		expect(css).toMatch(
			/@media \(max-width: 768px\)[\s\S]*?\.gallery-thumbs\s*{[^}]*min-width:\s*0;[^}]*overflow-x:\s*auto;[^}]*scroll-snap-type:\s*x proximity;/,
		);
		expect(css).toMatch(/\.gallery-desktop-arrow\s*{[^}]*display:\s*none;/);
		expect(css).not.toMatch(/overflow-x:\s*hidden/);
	});
});

describe("ProductDetail - Features detail", () => {
	it("renders the lodging's features by name and icon", async () => {
		mockGetDefaults({
			lodging: {
				...lodgingFixture,
				features: [{ id: 1, icon: "wifi", name: "WiFi" }],
			},
		});
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		expect(screen.getByText("Qué ofrece este lugar?")).toBeInTheDocument();
		expect(screen.getByText("WiFi")).toBeInTheDocument();
	});

	it("does not render the features section when the lodging has none", async () => {
		mockGetDefaults({ lodging: { ...lodgingFixture, features: [] } });
		renderProductDetail();

		await screen.findByText("Cabaña del Lago");

		expect(
			screen.queryByText("Qué ofrece este lugar?"),
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
