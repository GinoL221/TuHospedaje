import { Routes, Route, useLocation } from "react-router-dom";
import { fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { customRender, screen, waitFor } from "../../test/test-utils";
import Home from "./Home";
import { get } from "../../services/api";

vi.mock("../../services/api");

const { registerLocaleMock } = vi.hoisted(() => ({
	registerLocaleMock: vi.fn(),
}));

vi.mock("react-datepicker", () => ({
	registerLocale: registerLocaleMock,
	default: ({ placeholderText, onChange, locale, popperClassName }) => (
		<input
			aria-label={placeholderText}
			data-testid={`datepicker-${placeholderText}`}
			data-locale={locale}
			data-popper-class={popperClassName}
			onChange={(e) =>
				onChange(e.target.value ? new Date(e.target.value) : null)
			}
		/>
	),
}));

vi.mock("../../components/ProductCard/ProductCard", () => ({
	default: ({ lodging }) => (
		<div data-testid="product-card">{lodging.name}</div>
	),
}));

const FIXED_SEED = "11111111-1111-4111-8111-111111111111";

const lodgingFixture = {
	id: 1,
	name: "Cabaña del Lago",
	pricePerNight: 100,
	imageUrls: [],
};
const categoryFixture = { id: 1, name: "Cabaña", icon: "tree-pine", imageUrl: null };

function recommendationsPage({
	lodgings = [lodgingFixture],
	currentPage = 0,
	totalPages = 1,
	revision = "rev-1",
	reset = false,
} = {}) {
	return {
		lodgings,
		currentPage,
		totalItems: lodgings.length,
		totalPages,
		revision,
		reset,
	};
}

function mockGetDefaults({
	recommendations = recommendationsPage(),
	categories = [],
	favorites = [],
	categoryLodgings = [lodgingFixture],
} = {}) {
	get.mockImplementation((endpoint) => {
		if (endpoint.startsWith("/lodgings/recommendations")) {
			// Echo the requested page back as currentPage, like the real
			// backend does for an in-range request (see RecommendationPageResponse).
			const requestedPage =
				Number(new URL(endpoint, "http://localhost").searchParams.get("page")) || 0;
			return Promise.resolve({ ...recommendations, currentPage: requestedPage });
		}
		if (endpoint.startsWith("/lodgings?category="))
			return Promise.resolve(categoryLodgings);
		if (endpoint === "/categories") return Promise.resolve(categories);
		if (endpoint === "/favorites") return Promise.resolve(favorites);
		if (endpoint.startsWith("/lodgings/cities")) return Promise.resolve([]);
		return Promise.resolve(null);
	});
}

function SearchSentinel() {
	const location = useLocation();
	return <div data-testid="search-sentinel">{location.search}</div>;
}

function renderHome({ authValue } = {}) {
	return customRender(
		<Routes>
			<Route path="/" element={<Home />} />
			<Route path="/search" element={<SearchSentinel />} />
		</Routes>,
		{ authValue, route: "/" },
	);
}

beforeEach(() => {
	sessionStorage.clear();
	vi.spyOn(crypto, "randomUUID").mockReturnValue(FIXED_SEED);
});

describe("Home - lodgings render", () => {
	it("renders lodging cards fetched from the recommendations endpoint on mount", async () => {
		mockGetDefaults();
		renderHome();

		expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
		expect(get).toHaveBeenCalledWith(
			`/lodgings/recommendations?seed=${FIXED_SEED}&page=0&size=8`,
		);
	});

	it("shows the empty state when the recommendations catalog is empty", async () => {
		mockGetDefaults({
			recommendations: recommendationsPage({ lodgings: [], totalPages: 0 }),
		});
		renderHome();

		expect(
			await screen.findByText(
				"No hay alojamientos cargados todavía. Volvé más tarde.",
			),
		).toBeInTheDocument();
	});
});

describe("Home - recommendation snapshot persistence", () => {
	it("uses valid fallback seeds when randomUUID throws during initial load and refresh", async () => {
		crypto.randomUUID.mockImplementation(() => {
			throw new Error("UUID unavailable");
		});
		mockGetDefaults();
		const user = userEvent.setup();
		renderHome();

		await screen.findByText("Cabaña del Lago");
		await user.click(
			screen.getByRole("button", { name: "Actualizar recomendaciones" }),
		);
		await waitFor(() =>
			expect(
				get.mock.calls.filter(([endpoint]) =>
					endpoint.startsWith("/lodgings/recommendations"),
				),
			).toHaveLength(2),
		);

		for (const [endpoint] of get.mock.calls.filter(([path]) =>
			path.startsWith("/lodgings/recommendations"),
		)) {
			const seed = new URL(endpoint, "http://localhost").searchParams.get("seed");
			expect(seed).toMatch(/^[A-Za-z0-9_-]{16,64}$/);
		}
	});

	it("persists the generated seed under the tab-scoped sessionStorage key", async () => {
		mockGetDefaults();
		renderHome();

		await screen.findByText("Cabaña del Lago");

		const stored = JSON.parse(
			sessionStorage.getItem("tuhospedaje.recommendations.v1"),
		);
		expect(stored.seed).toBe(FIXED_SEED);
	});

	it("reuses the stored seed and revision instead of generating a new one", async () => {
		sessionStorage.setItem(
			"tuhospedaje.recommendations.v1",
			JSON.stringify({ seed: "stored-seed-0123456789", revision: "rev-stored" }),
		);
		mockGetDefaults();
		renderHome();

		await screen.findByText("Cabaña del Lago");

		expect(get).toHaveBeenCalledWith(
			"/lodgings/recommendations?seed=stored-seed-0123456789&page=0&size=8&revision=rev-stored",
		);
		expect(crypto.randomUUID).not.toHaveBeenCalled();
	});
});

describe("Home - recommendation pagination stability", () => {
	it("keeps forward/back navigation stable and reuses page 1 identities on return", async () => {
		mockGetDefaults({
			recommendations: recommendationsPage({
				lodgings: [lodgingFixture],
				totalPages: 2,
			}),
		});
		const user = userEvent.setup();
		renderHome();

		await screen.findByText("Cabaña del Lago");

		const page2Fixture = { ...lodgingFixture, id: 2, name: "Casa de Playa" };
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/recommendations?"))
				return Promise.resolve(
					endpoint.includes("page=1")
						? recommendationsPage({
								lodgings: [page2Fixture],
								currentPage: 1,
								totalPages: 2,
							})
						: recommendationsPage({
								lodgings: [lodgingFixture],
								currentPage: 0,
								totalPages: 2,
							}),
				);
			if (endpoint === "/categories") return Promise.resolve([]);
			if (endpoint === "/favorites") return Promise.resolve([]);
			return Promise.resolve(null);
		});

		await user.click(screen.getByRole("button", { name: "Siguiente" }));
		expect(await screen.findByText("Casa de Playa")).toBeInTheDocument();

		await user.click(screen.getByRole("button", { name: "Anterior" }));
		expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
	});

	it("requests the last page when Última is clicked and first page when Inicio is clicked", async () => {
		mockGetDefaults({
			recommendations: recommendationsPage({ totalPages: 3 }),
		});
		const user = userEvent.setup();
		renderHome();

		await screen.findByText("Cabaña del Lago");

		await user.click(screen.getByRole("button", { name: "Última" }));
		await waitFor(() =>
			expect(get).toHaveBeenCalledWith(
				expect.stringContaining("page=2"),
			),
		);

		await user.click(screen.getByRole("button", { name: "Inicio" }));
		await waitFor(() =>
			expect(get).toHaveBeenCalledWith(
				expect.stringContaining("page=0"),
			),
		);
	});
});

function deferred() {
	let resolve;
	const promise = new Promise((res) => {
		resolve = res;
	});
	return { promise, resolve };
}

describe("Home - pending list transition", () => {
	it("keeps the previous recommendation cards visible and marks the list busy while refresh is in flight", async () => {
		mockGetDefaults();
		const user = userEvent.setup();
		renderHome();
		await screen.findByText("Cabaña del Lago");

		const pending = deferred();
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/recommendations"))
				return pending.promise;
			if (endpoint === "/categories") return Promise.resolve([]);
			return Promise.resolve(null);
		});

		crypto.randomUUID.mockReturnValue("22222222-2222-4222-8222-222222222222");
		await user.click(
			screen.getByRole("button", { name: "Actualizar recomendaciones" }),
		);

		const list = screen.getByRole("list", { name: "Recomendaciones" });
		expect(screen.getByText("Cabaña del Lago")).toBeInTheDocument();
		expect(list).toHaveAttribute("aria-busy", "true");
		expect(list).toHaveClass("is-pending");
		expect(screen.getByRole("status")).toHaveTextContent(
			"Cargando recomendaciones...",
		);

		pending.resolve(
			recommendationsPage({
				lodgings: [{ ...lodgingFixture, id: 8, name: "Hotel La Perla" }],
			}),
		);

		expect(await screen.findByText("Hotel La Perla")).toBeInTheDocument();
		expect(screen.queryByText("Cabaña del Lago")).not.toBeInTheDocument();
		expect(screen.getByRole("list", { name: "Recomendaciones" })).toHaveAttribute(
			"aria-busy",
			"false",
		);
	});

	it("keeps the previous page visible and busy until the next page arrives", async () => {
		mockGetDefaults({
			recommendations: recommendationsPage({ totalPages: 2 }),
		});
		const user = userEvent.setup();
		renderHome();
		await screen.findByText("Cabaña del Lago");

		const pending = deferred();
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/recommendations"))
				return pending.promise;
			if (endpoint === "/categories") return Promise.resolve([]);
			return Promise.resolve(null);
		});

		await user.click(screen.getByRole("button", { name: "Siguiente" }));

		expect(screen.getByText("Cabaña del Lago")).toBeInTheDocument();
		expect(screen.getByRole("list", { name: "Recomendaciones" })).toHaveAttribute(
			"aria-busy",
			"true",
		);

		pending.resolve(
			recommendationsPage({
				lodgings: [{ ...lodgingFixture, id: 2, name: "Casa de Playa" }],
				currentPage: 1,
				totalPages: 2,
			}),
		);

		expect(await screen.findByText("Casa de Playa")).toBeInTheDocument();
		expect(screen.queryByText("Cabaña del Lago")).not.toBeInTheDocument();
	});

	it("keeps recommendation cards visible and busy until category lodgings arrive", async () => {
		mockGetDefaults({ categories: [categoryFixture] });
		const user = userEvent.setup();
		renderHome();
		await screen.findByText("Cabaña del Lago");

		const pending = deferred();
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings?category=")) return pending.promise;
			if (endpoint.startsWith("/lodgings/recommendations"))
				return Promise.resolve(recommendationsPage());
			if (endpoint === "/categories") return Promise.resolve([categoryFixture]);
			return Promise.resolve(null);
		});

		await user.click(screen.getByRole("button", { name: /Cabaña/ }));

		expect(screen.getByText("Cabaña del Lago")).toBeInTheDocument();
		expect(screen.getByRole("list")).toHaveAttribute("aria-busy", "true");

		pending.resolve([{ ...lodgingFixture, id: 4, name: "Cabaña Filtrada" }]);

		expect(await screen.findByText("Cabaña Filtrada")).toBeInTheDocument();
		expect(screen.queryByText("Cabaña del Lago")).not.toBeInTheDocument();
	});
});

describe("Home - explicit refresh and catalog reset", () => {
	it("replaces the seed, clears the revision, and resets to page 0 on explicit refresh", async () => {
		mockGetDefaults({
			recommendations: recommendationsPage({ totalPages: 2 }),
		});
		const user = userEvent.setup();
		renderHome();

		await screen.findByText("Cabaña del Lago");

		crypto.randomUUID.mockReturnValue("22222222-2222-4222-8222-222222222222");
		await user.click(
			screen.getByRole("button", { name: "Actualizar recomendaciones" }),
		);

		await waitFor(() =>
			expect(get).toHaveBeenCalledWith(
				"/lodgings/recommendations?seed=22222222-2222-4222-8222-222222222222&page=0&size=8",
			),
		);
	});

	it("adopts an atomic reset from a stale nonzero page without issuing another recommendation request", async () => {
		const pageOneFixture = { ...lodgingFixture, id: 2, name: "Casa de Playa" };
		const resetFixture = { ...lodgingFixture, id: 9, name: "Depto Centro" };
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/recommendations")) {
				const requestedPage = new URL(endpoint, "http://localhost").searchParams.get("page");
				return Promise.resolve(
					requestedPage === "1"
						? recommendationsPage({
								lodgings: [resetFixture],
								currentPage: 0,
								totalPages: 1,
								revision: "rev-2",
								reset: true,
							})
						: recommendationsPage({
								lodgings: [pageOneFixture],
								currentPage: 0,
								totalPages: 2,
								revision: "rev-1",
							}),
				);
			}
			if (endpoint === "/categories") return Promise.resolve([]);
			if (endpoint === "/favorites") return Promise.resolve([]);
			return Promise.resolve(null);
		});
		const user = userEvent.setup();
		renderHome();

		await screen.findByText("Casa de Playa");
		await user.click(screen.getByRole("button", { name: "Siguiente" }));

		expect(await screen.findByText("Depto Centro")).toBeInTheDocument();
		expect(screen.queryByText("Casa de Playa")).not.toBeInTheDocument();
		expect(screen.queryByText(/Página/)).not.toBeInTheDocument();
		expect(JSON.parse(sessionStorage.getItem("tuhospedaje.recommendations.v1"))).toEqual({
			seed: FIXED_SEED,
			revision: "rev-2",
		});
		expect(
			get.mock.calls.filter(([endpoint]) => endpoint.startsWith("/lodgings/recommendations")),
		).toHaveLength(2);
	});

	it("keeps later pagination available after a reset already served at page 0", async () => {
		const secondPageFixture = { ...lodgingFixture, id: 10, name: "Loft Norte" };
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/recommendations")) {
				const url = new URL(endpoint, "http://localhost");
				return Promise.resolve(
					url.searchParams.get("page") === "1"
						? recommendationsPage({
								lodgings: [secondPageFixture],
								currentPage: 1,
								totalPages: 2,
								revision: "rev-2",
							})
						: recommendationsPage({
								currentPage: 0,
								totalPages: 2,
								revision: "rev-2",
								reset: true,
							}),
				);
			}
			if (endpoint === "/categories") return Promise.resolve([]);
			if (endpoint === "/favorites") return Promise.resolve([]);
			return Promise.resolve(null);
		});
		const user = userEvent.setup();
		renderHome();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByRole("button", { name: "Siguiente" }));

		expect(await screen.findByText("Loft Norte")).toBeInTheDocument();
		expect(get).toHaveBeenCalledWith(
			`/lodgings/recommendations?seed=${FIXED_SEED}&page=1&size=8&revision=rev-2`,
		);
	});
});

describe("Home - category filter compatibility", () => {
	it("calls the category-filtered endpoint (not recommendations) when a category is clicked", async () => {
		mockGetDefaults({ categories: [categoryFixture] });
		const user = userEvent.setup();
		renderHome();

		await screen.findByText("Cabaña del Lago");
		get.mockClear();

		await user.click(screen.getByRole("button", { name: /Cabaña/ }));

		await waitFor(() =>
			expect(get).toHaveBeenCalledWith("/lodgings?category=1"),
		);
		expect(get).not.toHaveBeenCalledWith(
			expect.stringContaining("/lodgings/recommendations"),
		);
	});

	it("shows Mostrar todos when a category is active and hides it after deselection", async () => {
		mockGetDefaults({ categories: [categoryFixture] });
		const user = userEvent.setup();
		renderHome();

		await screen.findByText("Cabaña del Lago");

		const categoryBtn = screen.getByRole("button", { name: /Cabaña/ });
		await user.click(categoryBtn);
		expect(
			await screen.findByRole("button", { name: "Mostrar todos" }),
		).toBeInTheDocument();

		await user.click(categoryBtn);
		await waitFor(() =>
			expect(
				screen.queryByRole("button", { name: "Mostrar todos" }),
			).not.toBeInTheDocument(),
		);
	});

	it("renders the category Lucide icon and preserves filter behavior on click", async () => {
		const hotelCategory = {
			id: 2,
			name: "Hoteles",
			icon: "hotel",
		};
		mockGetDefaults({ categories: [hotelCategory] });
		const user = userEvent.setup();
		const { container } = renderHome();

		await screen.findByText("Cabaña del Lago");
		expect(container.querySelector("svg.lucide-hotel")).toBeInTheDocument();

		get.mockClear();
		await user.click(screen.getByRole("button", { name: /Hoteles/ }));

		await waitFor(() =>
			expect(get).toHaveBeenCalledWith("/lodgings?category=2"),
		);
	});

	it("renders the seeded category icon on the home filter chips", async () => {
		mockGetDefaults({ categories: [categoryFixture] });
		const { container } = renderHome();

		await screen.findByText("Cabaña del Lago");
		expect(container.querySelector("svg.lucide-tree-pine")).toBeInTheDocument();
		expect(
			screen.queryByRole("img", { name: /Imagen/ }),
		).not.toBeInTheDocument();
	});
});

describe("Home - stale response rejection", () => {
	it("ignores an out-of-order response from an earlier (page 1) request that resolves after a later (page 0) one", async () => {
		let resolvePage1;
		const pendingPage1 = new Promise((resolve) => {
			resolvePage1 = resolve;
		});
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/recommendations")) {
				const requestedPage = Number(
					new URL(endpoint, "http://localhost").searchParams.get("page"),
				);
				if (requestedPage === 0)
					return Promise.resolve(
						recommendationsPage({
							lodgings: [{ ...lodgingFixture, id: 2, name: "Casa de Playa" }],
							currentPage: 0,
							totalPages: 2,
						}),
					);
				return pendingPage1;
			}
			if (endpoint === "/categories") return Promise.resolve([]);
			if (endpoint === "/favorites") return Promise.resolve([]);
			return Promise.resolve(null);
		});
		const user = userEvent.setup();
		renderHome();
		await screen.findByText("Casa de Playa");

		// Navigate to page 1 (request stays pending), then back to page 0
		// (resolves immediately) before the page 1 response ever arrives.
		await user.click(screen.getByRole("button", { name: "Siguiente" }));
		await user.click(screen.getByRole("button", { name: "Anterior" }));
		await screen.findByText("Casa de Playa");

		resolvePage1(
			recommendationsPage({
				lodgings: [{ ...lodgingFixture, id: 3, name: "Producto Viejo" }],
				currentPage: 1,
				totalPages: 2,
			}),
		);
		await Promise.resolve();
		await Promise.resolve();

		expect(screen.queryByText("Producto Viejo")).not.toBeInTheDocument();
		expect(screen.getByText("Casa de Playa")).toBeInTheDocument();
	});
});

describe("Home - loading failure, retry, and repeated failure", () => {
	it("shows an accessible alert with Retry on load failure and clears it on successful retry", async () => {
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/recommendations"))
				return Promise.reject(new Error("network error"));
			if (endpoint === "/categories") return Promise.resolve([]);
			if (endpoint === "/favorites") return Promise.resolve([]);
			return Promise.resolve(null);
		});
		const user = userEvent.setup();
		renderHome();

		expect(await screen.findByRole("alert")).toHaveTextContent(
			"No pudimos cargar las recomendaciones.",
		);
		expect(screen.queryByTestId("product-card")).not.toBeInTheDocument();

		mockGetDefaults();
		await user.click(screen.getByRole("button", { name: "Reintentar" }));

		expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
		expect(screen.queryByRole("alert")).not.toBeInTheDocument();
	});

	it("keeps showing the failure state without stale products after a repeated failure", async () => {
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/recommendations"))
				return Promise.reject(new Error("network error"));
			if (endpoint === "/categories") return Promise.resolve([]);
			if (endpoint === "/favorites") return Promise.resolve([]);
			return Promise.resolve(null);
		});
		const user = userEvent.setup();
		renderHome();

		await screen.findByRole("alert");
		await user.click(screen.getByRole("button", { name: "Reintentar" }));

		expect(await screen.findByRole("alert")).toBeInTheDocument();
		expect(screen.queryByTestId("product-card")).not.toBeInTheDocument();
		expect(
			screen.queryByText("No hay alojamientos cargados todavía. Volvé más tarde."),
		).not.toBeInTheDocument();
	});
});

describe("Home - search form", () => {
	it("supports mouse and keyboard selection with listbox semantics", async () => {
		mockGetDefaults();
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/cities"))
				return Promise.resolve(["Bariloche", "Buenos Aires"]);
			if (endpoint.startsWith("/lodgings/recommendations"))
				return Promise.resolve(recommendationsPage());
			if (endpoint === "/categories") return Promise.resolve([]);
			return Promise.resolve(null);
		});
		const user = userEvent.setup();
		renderHome();
		const input = screen.getByRole("combobox", { name: "" });

		await user.type(input, "Ba");
		const listbox = await screen.findByRole("listbox", {
			name: "Sugerencias de ciudades",
		});
		expect(input).toHaveAttribute("aria-expanded", "true");
		expect(input).toHaveAttribute("aria-controls", listbox.id);

		await user.keyboard("{ArrowDown}");
		const activeOption = screen.getByRole("option", { name: "Bariloche" });
		expect(activeOption).toHaveClass("is-active");
		expect(activeOption).toHaveAttribute("aria-selected", "true");
		expect(input).toHaveAttribute("aria-activedescendant", activeOption.id);

		await user.keyboard("{ArrowDown}{ArrowUp}{Enter}");
		expect(input).toHaveValue("Bariloche");
		expect(input).toHaveAttribute("aria-expanded", "false");

		await user.clear(input);
		await user.type(input, "Bu");
		await screen.findByRole("option", { name: "Buenos Aires" });
		fireEvent.mouseDown(screen.getByRole("option", { name: "Buenos Aires" }));
		expect(input).toHaveValue("Buenos Aires");
	});

	it("keeps previous city suggestions visible while a new search is in flight", async () => {
		mockGetDefaults();
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/cities"))
				return Promise.resolve(["Bariloche"]);
			if (endpoint.startsWith("/lodgings/recommendations"))
				return Promise.resolve(recommendationsPage());
			if (endpoint === "/categories") return Promise.resolve([]);
			return Promise.resolve(null);
		});
		const user = userEvent.setup();
		renderHome();
		const input = screen.getByRole("combobox");
		await user.type(input, "Ba");
		await screen.findByRole("option", { name: "Bariloche" });

		const pending = deferred();
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/cities")) return pending.promise;
			if (endpoint.startsWith("/lodgings/recommendations"))
				return Promise.resolve(recommendationsPage());
			return Promise.resolve([]);
		});

		await user.type(input, "r");
		expect(await screen.findByRole("status")).toHaveTextContent("Buscando...");
		expect(screen.getByRole("option", { name: "Bariloche" })).toBeInTheDocument();

		pending.resolve(["Bariloche", "Baradero"]);
		expect(await screen.findByRole("option", { name: "Baradero" })).toBeInTheDocument();
		expect(screen.queryByText("Buscando...")).not.toBeInTheDocument();
	});

	it("closes city suggestions with Escape", async () => {
		mockGetDefaults();
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/cities")) return Promise.resolve(["Mendoza"]);
			if (endpoint.startsWith("/lodgings/recommendations"))
				return Promise.resolve(recommendationsPage());
			if (endpoint === "/categories") return Promise.resolve([]);
			return Promise.resolve(null);
		});
		const user = userEvent.setup();
		renderHome();
		const input = screen.getByRole("combobox");

		await user.type(input, "Me");
		await screen.findByRole("option", { name: "Mendoza" });
		await user.keyboard("{Escape}");

		expect(input).toHaveAttribute("aria-expanded", "false");
		expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
	});

	it("registers and applies the Spanish locale to both date pickers", async () => {
		mockGetDefaults();
		renderHome();

		await screen.findByText("Cabaña del Lago");

		expect(screen.getByTestId("datepicker-Check-in")).toHaveAttribute(
			"data-locale",
			"es",
		);
		expect(screen.getByTestId("datepicker-Check-out")).toHaveAttribute(
			"data-locale",
			"es",
		);
		expect(screen.getByTestId("datepicker-Check-in")).toHaveAttribute(
			"data-popper-class",
			"home-datepicker-popper",
		);
	});

	it("navigates to /search with the city query param when submitted", async () => {
		mockGetDefaults();
		const user = userEvent.setup();
		renderHome();

		await screen.findByText("Cabaña del Lago");

		await user.type(screen.getByPlaceholderText("Ciudad"), "Bariloche");
		await user.click(screen.getByRole("button", { name: "Buscar" }));

		expect(await screen.findByTestId("search-sentinel")).toHaveTextContent(
			"city=Bariloche",
		);
	});

	it("shows a validation error when checkOut is not after checkIn", async () => {
		mockGetDefaults();
		const user = userEvent.setup();
		renderHome();

		await screen.findByText("Cabaña del Lago");

		fireEvent.change(screen.getByTestId("datepicker-Check-in"), {
			target: { value: "2026-07-10" },
		});
		fireEvent.change(screen.getByTestId("datepicker-Check-out"), {
			target: { value: "2026-07-05" },
		});

		await user.click(screen.getByRole("button", { name: "Buscar" }));

		expect(
			screen.getByText("La fecha de check-out debe ser posterior al check-in"),
		).toBeInTheDocument();
	});
});

describe("Home - pagination", () => {
	it("enables Siguiente on page 0 and disables it after reaching the last page", async () => {
		mockGetDefaults({
			recommendations: recommendationsPage({ totalPages: 2 }),
		});
		const user = userEvent.setup();
		renderHome();

		await screen.findByText("Cabaña del Lago");

		const nextBtn = screen.getByRole("button", { name: "Siguiente" });
		expect(nextBtn).not.toBeDisabled();

		await user.click(nextBtn);

		await waitFor(() =>
			expect(screen.getByRole("button", { name: "Siguiente" })).toBeDisabled(),
		);
	});
});

describe("Home - favorites", () => {
	it("fetches favorites when the user is logged in", async () => {
		mockGetDefaults();
		renderHome();

		await screen.findByText("Cabaña del Lago");

		expect(get).toHaveBeenCalledWith("/favorites");
	});

	it("does not fetch favorites when the user is logged out", async () => {
		mockGetDefaults();
		renderHome({ authValue: null });

		await screen.findByText("Cabaña del Lago");

		expect(get).not.toHaveBeenCalledWith("/favorites");
	});
});
