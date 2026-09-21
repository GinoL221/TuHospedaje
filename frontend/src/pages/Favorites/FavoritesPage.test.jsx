import {
	customRender,
	screen,
	userEvent,
	waitFor,
} from "../../test/test-utils";
import FavoritesPage from "./FavoritesPage";
import { getFavorites, removeFavorite } from "../../services/favoriteService";

vi.mock("../../services/favoriteService", () => ({
	getFavorites: vi.fn(),
	removeFavorite: vi.fn(),
}));

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
		getFavorites.mockResolvedValue([favoriteFixture]);
		customRender(<FavoritesPage />);

		expect(screen.getByRole("status")).toHaveTextContent("Cargando...");

		expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
		expect(getFavorites).toHaveBeenCalledWith();
		expect(
			screen.getByRole("heading", { name: "Mis favoritos", level: 1 }),
		).toBeInTheDocument();
		expect(
			screen.getByRole("button", { name: "Quitar de favoritos" }),
		).toBeInTheDocument();
	});
});

describe("FavoritesPage - empty state", () => {
	it("shows an empty-state message when there are no favorites", async () => {
		getFavorites.mockResolvedValue([]);
		customRender(<FavoritesPage />);

		expect(
			await screen.findByText("No tenés favoritos guardados."),
		).toBeInTheDocument();
	});
});

describe("FavoritesPage - fetch failure", () => {
	it("renders the error message instead of the grid when the fetch rejects", async () => {
		getFavorites.mockRejectedValue(
			new Error("No se pudieron cargar los favoritos."),
		);
		customRender(<FavoritesPage />);

		expect(await screen.findByRole("alert")).toHaveTextContent(
			"No se pudieron cargar los favoritos.",
		);
	});
});

describe("FavoritesPage - removing a favorite", () => {
	it("removes the item from the grid on a successful delete", async () => {
		getFavorites.mockResolvedValue([favoriteFixture]);
		removeFavorite.mockResolvedValue(undefined);
		const user = userEvent.setup();
		customRender(<FavoritesPage />);

		await screen.findByText("Cabaña del Lago");

		await user.click(
			screen.getByRole("button", { name: "Quitar de favoritos" }),
		);

		expect(removeFavorite).toHaveBeenCalledWith(1);
		await waitFor(() => {
			expect(screen.queryByText("Cabaña del Lago")).not.toBeInTheDocument();
		});
	});

	it("prevents duplicate delete requests while removal is pending", async () => {
		let resolveDelete;
		getFavorites.mockResolvedValue([favoriteFixture]);
		removeFavorite.mockImplementation(
			() =>
				new Promise((resolve) => {
					resolveDelete = resolve;
				}),
		);
		const user = userEvent.setup();
		customRender(<FavoritesPage />);

		await screen.findByText("Cabaña del Lago");
		const removeButton = screen.getByRole("button", {
			name: "Quitar de favoritos",
		});

		await user.click(removeButton);
		expect(removeButton).toBeDisabled();
		expect(removeButton).toHaveAccessibleName("Quitando de favoritos");
		await user.click(removeButton);
		expect(removeFavorite).toHaveBeenCalledTimes(1);

		resolveDelete();
		await waitFor(() => {
			expect(screen.queryByText("Cabaña del Lago")).not.toBeInTheDocument();
		});
	});

	it("leaves the item rendered and shows a per-item alert when the delete request fails", async () => {
		getFavorites.mockResolvedValue([favoriteFixture]);
		removeFavorite.mockRejectedValue(new Error("network error"));
		const user = userEvent.setup();
		customRender(<FavoritesPage />);

		await screen.findByText("Cabaña del Lago");

		await user.click(
			screen.getByRole("button", { name: "Quitar de favoritos" }),
		);

		expect(await screen.findByRole("alert")).toHaveTextContent("network error");
		expect(screen.getByText("Cabaña del Lago")).toBeInTheDocument();
		expect(
			screen.getByRole("button", { name: "Quitar de favoritos" }),
		).toBeEnabled();
	});

	it("scopes the removal error to the failing item only", async () => {
		const secondFixture = {
			...favoriteFixture,
			id: 2,
			name: "Departamento Centro",
		};
		getFavorites.mockResolvedValue([favoriteFixture, secondFixture]);
		removeFavorite.mockImplementation((id) =>
			id === 1
				? Promise.reject(new Error("network error"))
				: Promise.resolve(undefined),
		);
		const user = userEvent.setup();
		customRender(<FavoritesPage />);

		await screen.findByText("Cabaña del Lago");

		const removeButtons = screen.getAllByRole("button", {
			name: "Quitar de favoritos",
		});
		await user.click(removeButtons[0]);

		const alerts = await screen.findAllByRole("alert");
		expect(alerts).toHaveLength(1);
		expect(alerts[0].closest(".favorite-item")).toHaveTextContent(
			"Cabaña del Lago",
		);
	});

	it("clears the error and removes the item on a successful retry", async () => {
		getFavorites.mockResolvedValue([favoriteFixture]);
		removeFavorite.mockRejectedValueOnce(new Error("network error"));
		removeFavorite.mockResolvedValueOnce(undefined);
		const user = userEvent.setup();
		customRender(<FavoritesPage />);

		await screen.findByText("Cabaña del Lago");

		const removeButton = screen.getByRole("button", {
			name: "Quitar de favoritos",
		});
		await user.click(removeButton);
		expect(await screen.findByRole("alert")).toBeInTheDocument();

		await user.click(removeButton);

		await waitFor(() => {
			expect(screen.queryByText("Cabaña del Lago")).not.toBeInTheDocument();
		});
		expect(screen.queryByRole("alert")).not.toBeInTheDocument();
	});
});
