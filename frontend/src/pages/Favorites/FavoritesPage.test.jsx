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

  it("leaves the item rendered and shows a per-item alert when the delete request fails", async () => {
    get.mockResolvedValue([favoriteFixture]);
    del.mockRejectedValue(new Error("network error"));
    const user = userEvent.setup();
    customRender(<FavoritesPage />);

    await screen.findByText("Cabaña del Lago");

    await user.click(screen.getByRole("button", { name: "Quitar de favoritos" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("network error");
    expect(screen.getByText("Cabaña del Lago")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Quitar de favoritos" })).toBeEnabled();
  });

  it("scopes the removal error to the failing item only", async () => {
    const secondFixture = {
      ...favoriteFixture,
      id: 2,
      name: "Departamento Centro",
    };
    get.mockResolvedValue([favoriteFixture, secondFixture]);
    del.mockImplementation((path) =>
      path === "/favorites/1"
        ? Promise.reject(new Error("network error"))
        : Promise.resolve(undefined)
    );
    const user = userEvent.setup();
    customRender(<FavoritesPage />);

    await screen.findByText("Cabaña del Lago");

    const removeButtons = screen.getAllByRole("button", { name: "Quitar de favoritos" });
    await user.click(removeButtons[0]);

    const alerts = await screen.findAllByRole("alert");
    expect(alerts).toHaveLength(1);
    expect(alerts[0].closest(".favorite-item")).toHaveTextContent("Cabaña del Lago");
  });

  it("clears the error and removes the item on a successful retry", async () => {
    get.mockResolvedValue([favoriteFixture]);
    del.mockRejectedValueOnce(new Error("network error"));
    del.mockResolvedValueOnce(undefined);
    const user = userEvent.setup();
    customRender(<FavoritesPage />);

    await screen.findByText("Cabaña del Lago");

    const removeButton = screen.getByRole("button", { name: "Quitar de favoritos" });
    await user.click(removeButton);
    expect(await screen.findByRole("alert")).toBeInTheDocument();

    await user.click(removeButton);

    await waitFor(() => {
      expect(screen.queryByText("Cabaña del Lago")).not.toBeInTheDocument();
    });
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});
