import { customRender, screen, userEvent, makeAuthValue, waitFor } from "../../test/test-utils";
import SearchResults from "./SearchResults";
import { searchLodgings } from "../../services/lodgingService";
import { getCategories } from "../../services/categoryService";
import { getFavorites } from "../../services/favoriteService";

vi.mock("../../services/lodgingService");
vi.mock("../../services/categoryService");
vi.mock("../../services/favoriteService");

const lodgingFixture = (overrides = {}) => ({
  id: 1,
  name: "Cabaña del Lago",
  city: "Bariloche",
  country: "Argentina",
  description: "Una cabaña con vista al lago.",
  imageUrls: ["https://example.com/img.jpg"],
  averageRating: 0,
  categoryId: 1,
  ...overrides,
});

const categoriesFixture = [
  { id: 1, name: "Cabaña" },
  { id: 2, name: "Hotel" },
];

// The backend now returns a paginated wrapper — { lodgings, currentPage,
// totalItems, totalPages } — instead of a flat array.
function searchResponse({ lodgings = [lodgingFixture()], currentPage = 0, totalPages = 1, totalItems } = {}) {
  return {
    lodgings,
    currentPage,
    totalItems: totalItems ?? lodgings.length,
    totalPages,
  };
}

// SearchResults calls three independent services from independent effects:
// getCategories (sidebar checkboxes), getFavorites (logged-in users only)
// and searchLodgings (the actual results). Set sane defaults for all three
// so each test only needs to override what it cares about.
function mockServiceDefaults({ lodgings = [lodgingFixture()], categories = categoriesFixture, favorites = [] } = {}) {
  searchLodgings.mockResolvedValue(searchResponse({ lodgings }));
  getCategories.mockResolvedValue(categories);
  getFavorites.mockResolvedValue(favorites);
}

function renderSearchResults({ authValue, initialEntries = ["/search?city=Bariloche"] } = {}) {
  return customRender(<SearchResults />, { authValue, initialEntries });
}

describe("SearchResults - initial unfiltered search from URL params", () => {
  it("fetches the search endpoint with the city URL param and default pagination, and renders the results", async () => {
    mockServiceDefaults();
    renderSearchResults();

    expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();

    const [params] = searchLodgings.mock.calls[0];
    expect(params.toString()).toBe("city=Bariloche&page=0");
  });

  it('renders the heading with the searched city', async () => {
    mockServiceDefaults();
    renderSearchResults();

    expect(await screen.findByText('Resultados para "Bariloche"')).toBeInTheDocument();
  });
});

describe("SearchResults - empty and error states", () => {
  it("shows the empty-state message when the search resolves with no results", async () => {
    mockServiceDefaults({ lodgings: [] });
    renderSearchResults();

    expect(
      await screen.findByText("No se encontraron resultados para tu búsqueda.")
    ).toBeInTheDocument();
  });

  it("shows the error message when the search rejects", async () => {
    getCategories.mockResolvedValue(categoriesFixture);
    getFavorites.mockResolvedValue([]);
    searchLodgings.mockRejectedValue(new Error("fail"));
    renderSearchResults();

    expect(await screen.findByText("fail")).toBeInTheDocument();
  });
});

// Per the PR2/PR3 backend contract, categories are now always filtered
// server-side (no more client-side intersection): the component always
// forwards selected categories as repeated `categories` query params and
// renders whatever the backend returns for that filter set.
describe("SearchResults - categories are always filtered server-side", () => {
  it("sends both selected categories to the backend and renders the server-filtered results", async () => {
    const cabin = lodgingFixture({ id: 1, name: "Cabaña del Lago", categoryId: 1 });
    const hotel = lodgingFixture({ id: 2, name: "Hotel Centro", categoryId: 2 });
    const apart = lodgingFixture({ id: 3, name: "Apart Costa", categoryId: 3 });
    getCategories.mockResolvedValue(categoriesFixture);
    getFavorites.mockResolvedValue([]);
    searchLodgings
      .mockResolvedValueOnce(searchResponse({ lodgings: [cabin, hotel, apart] }))
      .mockResolvedValueOnce(searchResponse({ lodgings: [cabin, hotel] }));

    const user = userEvent.setup();
    renderSearchResults();

    await screen.findByText("Cabaña del Lago");
    searchLodgings.mockClear();

    await user.click(screen.getByRole("checkbox", { name: "Cabaña" }));
    await user.click(screen.getByRole("checkbox", { name: "Hotel" }));
    await user.click(screen.getByRole("button", { name: "Aplicar filtros" }));

    await waitFor(() => {
      expect(screen.queryByText("Apart Costa")).not.toBeInTheDocument();
    });
    expect(screen.getByText("Cabaña del Lago")).toBeInTheDocument();
    expect(screen.getByText("Hotel Centro")).toBeInTheDocument();

    // A single fetch carries both categories — no per-category branching left.
    expect(searchLodgings).toHaveBeenCalledTimes(1);
    const [params] = searchLodgings.mock.calls[0];
    expect(params.getAll("categories")).toEqual(["1", "2"]);
  });

  it("re-runs the search with only the remaining category after removing a chip", async () => {
    const cabin = lodgingFixture({ id: 1, name: "Cabaña del Lago", categoryId: 1 });
    const hotel = lodgingFixture({ id: 2, name: "Hotel Centro", categoryId: 2 });
    getCategories.mockResolvedValue(categoriesFixture);
    getFavorites.mockResolvedValue([]);
    searchLodgings
      .mockResolvedValueOnce(searchResponse({ lodgings: [cabin] })) // initial load
      .mockResolvedValueOnce(searchResponse({ lodgings: [cabin, hotel], totalPages: 2 })) // apply both categories
      .mockResolvedValueOnce(searchResponse({ lodgings: [hotel], totalPages: 2 })) // remove "Cabaña" chip
      .mockResolvedValueOnce(searchResponse({ lodgings: [hotel], currentPage: 1, totalPages: 2 })); // paginate

    const user = userEvent.setup();
    renderSearchResults();

    await screen.findByText("Cabaña del Lago");

    await user.click(screen.getByRole("checkbox", { name: "Cabaña" }));
    await user.click(screen.getByRole("checkbox", { name: "Hotel" }));
    await user.click(screen.getByRole("button", { name: "Aplicar filtros" }));

    await waitFor(() => {
      expect(screen.getByText("Hotel Centro")).toBeInTheDocument();
    });

    searchLodgings.mockClear();
    await user.click(screen.getByRole("button", { name: "Quitar Cabaña" }));

    await waitFor(() => {
      expect(screen.queryByRole("button", { name: "Quitar Cabaña" })).not.toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: "Quitar Hotel" })).toBeInTheDocument();
    expect(screen.getByRole("complementary", { name: "Filtros" })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Cabaña" })).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Hotel" })).toBeChecked();

    expect(searchLodgings).toHaveBeenCalledTimes(1);
    let [params] = searchLodgings.mock.calls[0];
    expect(params.getAll("categories")).toEqual(["2"]);

    await user.click(screen.getByRole("checkbox", { name: "Cabaña" }));
    expect(screen.getByRole("checkbox", { name: "Cabaña" })).toBeChecked();

    searchLodgings.mockClear();
    await user.click(screen.getByRole("button", { name: "Siguiente" }));

    await waitFor(() => {
      expect(screen.getByText("Página 2 de 2")).toBeInTheDocument();
    });
    expect(searchLodgings).toHaveBeenCalledTimes(1);
    [params] = searchLodgings.mock.calls[0];
    expect(params.getAll("categories")).toEqual(["2"]);
    expect(params.get("page")).toBe("1");
  });

  it("includes the categories param when exactly one category is selected", async () => {
    mockServiceDefaults();
    const user = userEvent.setup();
    renderSearchResults();

    await screen.findByText("Cabaña del Lago");
    searchLodgings.mockClear();

    await user.click(screen.getByRole("checkbox", { name: "Cabaña" }));
    await user.click(screen.getByRole("button", { name: "Aplicar filtros" }));

    await waitFor(() => {
      expect(searchLodgings).toHaveBeenCalledTimes(1);
    });

    expect(screen.getByRole("complementary", { name: "Filtros" })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Hotel" })).toBeInTheDocument();

    const [params] = searchLodgings.mock.calls[0];
    expect(params.getAll("categories")).toEqual(["1"]);
  });
});

describe("SearchResults - price chip removal", () => {
  it("removes the price chip and re-runs the search without price params", async () => {
    mockServiceDefaults();
    const user = userEvent.setup();
    renderSearchResults();

    await screen.findByText("Cabaña del Lago");

    const [minPriceInput, maxPriceInput] = screen.getAllByPlaceholderText("$");
    await user.type(minPriceInput, "100");
    await user.type(maxPriceInput, "200");
    await user.click(screen.getByRole("button", { name: "Aplicar filtros" }));

    const priceChipRemove = await screen.findByRole("button", { name: "Quitar filtro de precio" });
    searchLodgings.mockClear();

    await user.click(priceChipRemove);

    await waitFor(() => {
      expect(screen.queryByRole("button", { name: "Quitar filtro de precio" })).not.toBeInTheDocument();
    });

    expect(searchLodgings).toHaveBeenCalledTimes(1);
    const [params] = searchLodgings.mock.calls[0];
    expect(params.toString()).toBe("city=Bariloche&page=0");
  });
});

describe("SearchResults - date chip removal", () => {
  it("removes the date chip and re-runs the search without checkIn/checkOut params", async () => {
    mockServiceDefaults();
    const user = userEvent.setup();
    renderSearchResults({
      initialEntries: ["/search?city=Bariloche&checkIn=2026-07-01&checkOut=2026-07-05"],
    });

    await screen.findByText("Cabaña del Lago");
    const dateChipRemove = await screen.findByRole("button", { name: "Quitar fechas" });
    searchLodgings.mockClear();

    await user.click(dateChipRemove);

    await waitFor(() => {
      expect(screen.queryByRole("button", { name: "Quitar fechas" })).not.toBeInTheDocument();
    });

    expect(searchLodgings).toHaveBeenCalledTimes(1);
    const [params] = searchLodgings.mock.calls[0];
    expect(params.toString()).toBe("city=Bariloche&page=0");
  });
});

describe("SearchResults - favorites only fetched for logged-in users", () => {
  it("does not call getFavorites and keeps favoriteIds empty for anonymous users", async () => {
    mockServiceDefaults();
    renderSearchResults({ authValue: null });

    await screen.findByText("Cabaña del Lago");

    expect(getFavorites).not.toHaveBeenCalled();
    // Anonymous users don't see the favorite button at all (ProductCard's own
    // useAuth gate), which is the externally observable consequence of
    // favoriteIds staying empty.
    expect(screen.queryByRole("button", { name: "Agregar a favoritos" })).not.toBeInTheDocument();
  });

  it("calls getFavorites for a logged-in user", async () => {
    mockServiceDefaults({ favorites: [lodgingFixture({ id: 1 })] });
    renderSearchResults({ authValue: makeAuthValue() });

    await screen.findByText("Cabaña del Lago");

    expect(getFavorites).toHaveBeenCalled();
  });
});
