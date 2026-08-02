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

const lodgingFixture = {
	id: 1,
	name: "Cabaña del Lago",
	pricePerNight: 100,
	imageUrls: [],
};
const categoryFixture = { id: 1, name: "Cabaña", icon: null };

function mockGetDefaults({
	lodgings = [lodgingFixture],
	totalPages = 1,
	categories = [],
	favorites = [],
} = {}) {
	get.mockImplementation((endpoint) => {
		if (endpoint.startsWith("/lodgings?category="))
			return Promise.resolve(lodgings);
		if (endpoint.startsWith("/lodgings?page="))
			return Promise.resolve({ lodgings, totalPages });
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

describe("Home - lodgings render", () => {
	it("renders lodging cards fetched on mount", async () => {
		mockGetDefaults();
		renderHome();

		expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
		expect(get).toHaveBeenCalledWith(
			expect.stringContaining("/lodgings?page="),
		);
	});

	it("shows the empty state when there are no lodgings", async () => {
		mockGetDefaults({ lodgings: [] });
		renderHome();

		expect(
			await screen.findByText(
				"No hay alojamientos cargados todavía. Volvé más tarde.",
			),
		).toBeInTheDocument();
	});
});

describe("Home - category filter", () => {
	it("calls the category-filtered endpoint when a category is clicked", async () => {
		mockGetDefaults({ categories: [categoryFixture] });
		const user = userEvent.setup();
		renderHome();

		await screen.findByText("Cabaña del Lago");

		await user.click(screen.getByRole("button", { name: /Cabaña/ }));

		await waitFor(() =>
			expect(get).toHaveBeenCalledWith("/lodgings?category=1"),
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
});

describe("Home - search form", () => {
	it("supports mouse and keyboard selection with listbox semantics", async () => {
		mockGetDefaults();
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/cities"))
				return Promise.resolve(["Bariloche", "Buenos Aires"]);
			if (endpoint.startsWith("/lodgings?page="))
				return Promise.resolve({ lodgings: [lodgingFixture], totalPages: 1 });
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

	it("closes city suggestions with Escape", async () => {
		mockGetDefaults();
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/cities")) return Promise.resolve(["Mendoza"]);
			if (endpoint.startsWith("/lodgings?page="))
				return Promise.resolve({ lodgings: [lodgingFixture], totalPages: 1 });
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
		mockGetDefaults({ totalPages: 2 });
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
