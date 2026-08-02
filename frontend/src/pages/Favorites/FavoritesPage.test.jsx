import { customRender, screen, userEvent, waitFor } from "../../test/test-utils";
import FavoritesPage from "./FavoritesPage";
import { get, del } from "../../services/api";

vi.mock("../../services/api");

const favoriteFixture = {
  id: 1,
  name: "Cabaña del Lago",
  city: "Bariloche",
  country: "Argentina",
  description: "Una cabaña con vista al lago.",
  imageUrls: ["https://example.com/img.jpg"],
};

describe("FavoritesPage - favorites list", () => {
  it("fetches and renders the user's favorites", async () => {
    get.mockResolvedValue([favoriteFixture]);
    customRender(<FavoritesPage />);

    expect(screen.getByText("Cargando...")).toBeInTheDocument();

    expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
    expect(get).toHaveBeenCalledWith("/favorites");
    expect(screen.getByRole("heading", { name: "Mis favoritos", level: 1 })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Quitar de favoritos" })).toBeInTheDocument();
  });
});

describe("FavoritesPage - empty state", () => {
  it("shows an empty-state message when there are no favorites", async () => {
    get.mockResolvedValue([]);
    customRender(<FavoritesPage />);

    expect(await screen.findByText("No tenés favoritos guardados.")).toBeInTheDocument();
  });
});

describe("FavoritesPage - fetch failure", () => {
  it("renders the error message instead of the grid when the fetch rejects", async () => {
    get.mockRejectedValue(new Error("No se pudieron cargar los favoritos."));
    customRender(<FavoritesPage />);

    expect(
      await screen.findByText("No se pudieron cargar los favoritos.")
    ).toBeInTheDocument();
  });
});

describe("FavoritesPage - removing a favorite", () => {
  it("removes the item from the grid on a successful delete", async () => {
    get.mockResolvedValue([favoriteFixture]);
    del.mockResolvedValue(undefined);
    const user = userEvent.setup();
    customRender(<FavoritesPage />);

    await screen.findByText("Cabaña del Lago");

    await user.click(screen.getByRole("button", { name: "Quitar de favoritos" }));

    expect(del).toHaveBeenCalledWith("/favorites/1");
    await waitFor(() => {
      expect(screen.queryByText("Cabaña del Lago")).not.toBeInTheDocument();
    });
  });

  it("leaves the item rendered when the delete request fails", async () => {
    get.mockResolvedValue([favoriteFixture]);
    del.mockRejectedValue(new Error("network error"));
    const user = userEvent.setup();
    // FavoritesPage's removeFavorite catch block only logs the error
    // (console.error) instead of surfacing UI error state — characterized
    // as-is per spec Risks policy, not fixed here.
    const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    customRender(<FavoritesPage />);

    await screen.findByText("Cabaña del Lago");

    await user.click(screen.getByRole("button", { name: "Quitar de favoritos" }));

    await waitFor(() => {
      expect(del).toHaveBeenCalledWith("/favorites/1");
    });
    expect(screen.getByText("Cabaña del Lago")).toBeInTheDocument();

    consoleErrorSpy.mockRestore();
  });
});
