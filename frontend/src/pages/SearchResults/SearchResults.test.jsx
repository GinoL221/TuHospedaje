import { customRender, screen, userEvent, makeAuthValue, waitFor } from "../../test/test-utils";
import SearchResults from "./SearchResults";
import { get } from "../../services/api";

vi.mock("../../services/api");

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

// SearchResults issues GET requests to three different endpoints from
// independent effects: /categories (sidebar checkboxes), /favorites
// (logged-in users only) and /lodgings/search?... (the actual results).
// Branch by endpoint so each test only needs to override what it cares about.
function mockGetDefaults({ results = [lodgingFixture()], categories = categoriesFixture, favorites = [] } = {}) {
  get.mockImplementation((endpoint) => {
    if (endpoint === "/categories") {
      return Promise.resolve(categories);
    }
    if (endpoint === "/favorites") {
      return Promise.resolve(favorites);
    }
    if (endpoint.startsWith("/lodgings/search")) {
      return Promise.resolve(results);
    }
    return Promise.resolve(null);
  });
}

function renderSearchResults({ authValue, initialEntries = ["/search?city=Bariloche"] } = {}) {
  return customRender(<SearchResults />, { authValue, initialEntries });
}

describe("SearchResults - initial unfiltered search from URL params", () => {
  it("fetches /lodgings/search with the city URL param and renders the results", async () => {
    mockGetDefaults();
    renderSearchResults();

    expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();

    expect(get).toHaveBeenCalledWith("/lodgings/search?city=Bariloche");
  });

  it('renders the heading with the searched city', async () => {
    mockGetDefaults();
    renderSearchResults();

    expect(await screen.findByText('Resultados para "Bariloche"')).toBeInTheDocument();
  });
});

describe("SearchResults - empty and error states", () => {
  it("shows the empty-state message when the search resolves with no results", async () => {
    mockGetDefaults({ results: [] });
    renderSearchResults();

    expect(
      await screen.findByText("No se encontraron resultados para tu búsqueda.")
    ).toBeInTheDocument();
  });

  it("shows the error message when the search rejects", async () => {
    get.mockImplementation((endpoint) => {
      if (endpoint === "/categories") return Promise.resolve(categoriesFixture);
      if (endpoint === "/favorites") return Promise.resolve([]);
      if (endpoint.startsWith("/lodgings/search")) return Promise.reject(new Error("fail"));
      return Promise.resolve(null);
    });
    renderSearchResults();

    expect(await screen.findByText("fail")).toBeInTheDocument();
  });
});

// Per spec Risks policy: runCategorySearch does NOT filter server-side when
// 2+ categories are selected. It fetches the full unfiltered result set from
// /lodgings/search ONCE, then intersects locally against the selected
// category ids. These tests characterize that two-step client-side flow,
// not an idealized server-side filter.
describe("SearchResults - multi-category filter is client-side intersected", () => {
  it("fetches once and filters locally to lodgings whose categoryId is in the selected set", async () => {
    const cabin = lodgingFixture({ id: 1, name: "Cabaña del Lago", categoryId: 1 });
    const hotel = lodgingFixture({ id: 2, name: "Hotel Centro", categoryId: 2 });
    const apart = lodgingFixture({ id: 3, name: "Apart Costa", categoryId: 3 });
    mockGetDefaults({ results: [cabin, hotel, apart] });
    const user = userEvent.setup();
    renderSearchResults();

    await screen.findByText("Cabaña del Lago");
    get.mockClear();

    await user.click(screen.getByRole("checkbox", { name: "Cabaña" }));
    await user.click(screen.getByRole("checkbox", { name: "Hotel" }));
    await user.click(screen.getByRole("button", { name: "Aplicar filtros" }));

    await waitFor(() => {
      expect(screen.queryByText("Apart Costa")).not.toBeInTheDocument();
    });
    expect(screen.getByText("Cabaña del Lago")).toBeInTheDocument();
    expect(screen.getByText("Hotel Centro")).toBeInTheDocument();

    // Only ONE call to /lodgings/search for the whole multi-category apply —
    // the intersection happens locally, not via a second filtered request.
    const searchCalls = get.mock.calls.filter(([endpoint]) => endpoint.startsWith("/lodgings/search"));
    expect(searchCalls).toHaveLength(1);
  });

  it("re-runs the search without a removed category chip", async () => {
    const cabin = lodgingFixture({ id: 1, name: "Cabaña del Lago", categoryId: 1 });
    const hotel = lodgingFixture({ id: 2, name: "Hotel Centro", categoryId: 2 });
    mockGetDefaults({ results: [cabin, hotel] });
    const user = userEvent.setup();
    renderSearchResults();

    await screen.findByText("Cabaña del Lago");

    await user.click(screen.getByRole("checkbox", { name: "Cabaña" }));
    await user.click(screen.getByRole("checkbox", { name: "Hotel" }));
    await user.click(screen.getByRole("button", { name: "Aplicar filtros" }));

    await waitFor(() => {
      expect(screen.getByText("Hotel Centro")).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: "Quitar Cabaña" }));

    await waitFor(() => {
      expect(screen.queryByRole("button", { name: "Quitar Cabaña" })).not.toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: "Quitar Hotel" })).toBeInTheDocument();
  });
});

describe("SearchResults - favorites only fetched for logged-in users", () => {
  it("does not call GET /favorites and keeps favoriteIds empty for anonymous users", async () => {
    mockGetDefaults();
    renderSearchResults({ authValue: null });

    await screen.findByText("Cabaña del Lago");

    expect(get).not.toHaveBeenCalledWith("/favorites");
    // Anonymous users don't see the favorite button at all (ProductCard's own
    // useAuth gate), which is the externally observable consequence of
    // favoriteIds staying empty.
    expect(screen.queryByRole("button", { name: "Agregar a favoritos" })).not.toBeInTheDocument();
  });

  it("calls GET /favorites for a logged-in user", async () => {
    mockGetDefaults({ favorites: [lodgingFixture({ id: 1 })] });
    renderSearchResults({ authValue: makeAuthValue() });

    await screen.findByText("Cabaña del Lago");

    expect(get).toHaveBeenCalledWith("/favorites");
  });
});
