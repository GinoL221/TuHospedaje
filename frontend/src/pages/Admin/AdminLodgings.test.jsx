import {
	customRender,
	screen,
	userEvent,
	waitFor,
} from "../../test/test-utils";
import AdminLodgings from "./AdminLodgings";
import { get, post, put, del } from "../../services/api";

vi.mock("../../services/api");

const lodgingFixture = (overrides = {}) => ({
	id: 1,
	name: "Cabaña del Lago",
	description: "Una cabaña con vista al lago.",
	address: "Ruta 40 km 5",
	city: "Bariloche",
	country: "Argentina",
	phoneNumber: "1122334455",
	email: "contacto@cabana.com",
	categoryId: 1,
	features: [],
	policies: [],
	imageUrls: [],
	...overrides,
});

// AdminLodgings fires 4 independent GET effects on mount: /lodgings/admin
// (the server-paginated table data), plus /categories, /features, /policies
// (modal dropdown/checkbox data). Branch by endpoint so each test only
// overrides what it cares about — same pattern as SearchResults.test.jsx.
function pageResponse(items, overrides = {}) {
	return {
		items,
		currentPage: 0,
		totalItems: items.length,
		totalPages: items.length > 0 ? 1 : 0,
		...overrides,
	};
}

function mockGetDefaults({
	lodgings = [lodgingFixture()],
	categories = [],
	features = [],
	policies = [],
	page = {},
} = {}) {
	get.mockImplementation((endpoint) => {
		if (endpoint.startsWith("/lodgings/admin"))
			return Promise.resolve(pageResponse(lodgings, page));
		if (endpoint === "/categories") return Promise.resolve(categories);
		if (endpoint === "/features") return Promise.resolve(features);
		if (endpoint === "/policies") return Promise.resolve(policies);
		return Promise.resolve(null);
	});
}

function renderAdminLodgings(options) {
	return customRender(<AdminLodgings />, options);
}

describe("AdminLodgings - listing", () => {
	it("renders the empty state when there are no lodgings", async () => {
		mockGetDefaults({ lodgings: [] });
		renderAdminLodgings();

		expect(
			await screen.findByText(
				"No hay alojamientos cargados todavía. ¡Agregá el primero!",
			),
		).toBeInTheDocument();
	});

	it("renders a row per lodging with name and description", async () => {
		mockGetDefaults({
			lodgings: [
				lodgingFixture({ id: 1, name: "Cabaña del Lago" }),
				lodgingFixture({
					id: 2,
					name: "Hotel Centro",
					description: "En el centro de la ciudad.",
				}),
			],
		});
		renderAdminLodgings();

		expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
		expect(screen.getByText("Hotel Centro")).toBeInTheDocument();
		expect(screen.getByText("En el centro de la ciudad.")).toBeInTheDocument();
	});

	it("fetches a server-paginated lodging page on mount", async () => {
		mockGetDefaults();
		renderAdminLodgings();

		await screen.findByText("Cabaña del Lago");
		expect(get).toHaveBeenCalledWith(
			"/lodgings/admin?page=0&size=10&sort=id&direction=asc",
		);
	});

	it("fetches the next server page when pagination changes", async () => {
		mockGetDefaults({ page: { totalPages: 2 } });
		const user = userEvent.setup();
		renderAdminLodgings();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByRole("button", { name: "Siguiente" }));

		await waitFor(() => {
			expect(get).toHaveBeenCalledWith(
				"/lodgings/admin?page=1&size=10&sort=id&direction=asc",
			);
		});
	});

	it("refetches the last valid page when the current server page becomes empty", async () => {
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/admin")) {
				const params = new URLSearchParams(endpoint.split("?")[1]);
				if (params.get("page") === "1") {
					return Promise.resolve(
						pageResponse([], { currentPage: 1, totalItems: 10, totalPages: 1 }),
					);
				}
				return Promise.resolve(
					pageResponse([lodgingFixture()], { totalPages: 2 }),
				);
			}
			if (endpoint === "/categories") return Promise.resolve([]);
			if (endpoint === "/features") return Promise.resolve([]);
			if (endpoint === "/policies") return Promise.resolve([]);
			return Promise.resolve(null);
		});
		const user = userEvent.setup();
		renderAdminLodgings();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByRole("button", { name: "Siguiente" }));

		await waitFor(() => {
			expect(get).toHaveBeenCalledWith(
				"/lodgings/admin?page=1&size=10&sort=id&direction=asc",
			);
		});
		await waitFor(() => {
			expect(
				get.mock.calls.filter(
					([endpoint]) =>
						endpoint === "/lodgings/admin?page=0&size=10&sort=id&direction=asc",
				),
			).toHaveLength(2);
		});
	});

	it("fetches and renders sorted server data when a sortable header is clicked", async () => {
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/admin")) {
				const params = new URLSearchParams(endpoint.split("?")[1]);
				if (params.get("sort") === "name") {
					return Promise.resolve(
						pageResponse([
							lodgingFixture({
								id: 2,
								name: "Apart Hotel Sur",
								description: "Resultado ordenado por nombre.",
							}),
						]),
					);
				}
				return Promise.resolve(pageResponse([lodgingFixture()]));
			}
			if (endpoint === "/categories") return Promise.resolve([]);
			if (endpoint === "/features") return Promise.resolve([]);
			if (endpoint === "/policies") return Promise.resolve([]);
			return Promise.resolve(null);
		});
		const user = userEvent.setup();
		renderAdminLodgings();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByRole("columnheader", { name: /Nombre/ }));

		await waitFor(() => {
			expect(get).toHaveBeenCalledWith(
				"/lodgings/admin?page=0&size=10&sort=name&direction=asc",
			);
		});
		expect(await screen.findByText("Apart Hotel Sur")).toBeInTheDocument();
		expect(
			screen.getByText("Resultado ordenado por nombre."),
		).toBeInTheDocument();
		expect(screen.queryByText("Cabaña del Lago")).not.toBeInTheDocument();
	});

	it("fetches and renders a searched server page and resets to the first page", async () => {
		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/admin")) {
				const params = new URLSearchParams(endpoint.split("?")[1]);
				if (params.get("q")) {
					return Promise.resolve(
						pageResponse([
							lodgingFixture({
								id: 3,
								name: "Lago Azul",
								description: "Resultado filtrado por búsqueda.",
							}),
						]),
					);
				}
				if (params.get("page") === "1") {
					return Promise.resolve(
						pageResponse(
							[
								lodgingFixture({
									id: 2,
									name: "Hotel Centro",
									description: "Segunda página sin filtrar.",
								}),
							],
							{ currentPage: 1, totalItems: 20, totalPages: 2 },
						),
					);
				}
				return Promise.resolve(
					pageResponse([lodgingFixture()], { totalItems: 20, totalPages: 2 }),
				);
			}
			if (endpoint === "/categories") return Promise.resolve([]);
			if (endpoint === "/features") return Promise.resolve([]);
			if (endpoint === "/policies") return Promise.resolve([]);
			return Promise.resolve(null);
		});
		const user = userEvent.setup();
		renderAdminLodgings();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByRole("button", { name: "Siguiente" }));
		await screen.findByText("Hotel Centro");
		await user.type(screen.getByLabelText("Buscar alojamientos"), "lago");

		await waitFor(() => {
			expect(get).toHaveBeenCalledWith(
				"/lodgings/admin?page=0&size=10&sort=id&direction=asc&q=lago",
			);
		});
		expect(await screen.findByText("Lago Azul")).toBeInTheDocument();
		expect(
			screen.getByText("Resultado filtrado por búsqueda."),
		).toBeInTheDocument();
		expect(screen.queryByText("Hotel Centro")).not.toBeInTheDocument();
	});
});

describe("AdminLodgings - create", () => {
	it("opens the modal in create mode when clicking the add button", async () => {
		mockGetDefaults({ lodgings: [] });
		const user = userEvent.setup();
		renderAdminLodgings();

		await screen.findByText(
			"No hay alojamientos cargados todavía. ¡Agregá el primero!",
		);
		await user.click(screen.getByTestId("admin-add-btn"));

		expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
		expect(
			screen.getByRole("heading", { name: "Nuevo alojamiento" }),
		).toBeInTheDocument();
		expect(screen.getByTestId("field-name")).toHaveValue("");
	});

	it("shows inline required-field errors and makes no request when submitting empty", async () => {
		mockGetDefaults({ lodgings: [] });
		const user = userEvent.setup();
		renderAdminLodgings();

		await screen.findByText(
			"No hay alojamientos cargados todavía. ¡Agregá el primero!",
		);
		await user.click(screen.getByTestId("admin-add-btn"));
		await user.click(screen.getByTestId("admin-save-btn"));

		expect(await screen.findByTestId("error-name")).toHaveTextContent(
			"El nombre es obligatorio",
		);
		expect(screen.getByTestId("error-email")).toHaveTextContent(
			"El email es obligatorio",
		);
		expect(post).not.toHaveBeenCalled();
	});

	it("submits the form and refreshes the list on success", async () => {
		mockGetDefaults({ lodgings: [] });
		post.mockResolvedValue(lodgingFixture());
		const user = userEvent.setup();
		renderAdminLodgings();

		await screen.findByText(
			"No hay alojamientos cargados todavía. ¡Agregá el primero!",
		);
		await user.click(screen.getByTestId("admin-add-btn"));

		await user.type(screen.getByTestId("field-name"), "Cabaña del Lago");
		await user.type(screen.getByTestId("field-email"), "contacto@cabana.com");
		await user.type(
			screen.getByTestId("field-description"),
			"Una cabaña con vista al lago.",
		);
		await user.type(screen.getByTestId("field-address"), "Ruta 40 km 5");
		await user.type(screen.getByTestId("field-city"), "Bariloche");
		await user.type(screen.getByTestId("field-country"), "Argentina");
		await user.type(screen.getByTestId("field-phoneNumber"), "1122334455");

		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/admin"))
				return Promise.resolve(pageResponse([lodgingFixture()]));
			return Promise.resolve([]);
		});

		await user.click(screen.getByTestId("admin-save-btn"));

		expect(post).toHaveBeenCalledWith(
			"/lodgings",
			expect.objectContaining({
				name: "Cabaña del Lago",
				email: "contacto@cabana.com",
			}),
		);

		await waitFor(() => {
			expect(screen.queryByTestId("admin-modal")).not.toBeInTheDocument();
		});
		expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
	});
});

describe("AdminLodgings - edit", () => {
	it("opens the modal pre-filled with the selected lodging's data", async () => {
		mockGetDefaults({
			lodgings: [lodgingFixture({ id: 1, name: "Cabaña del Lago" })],
		});
		const user = userEvent.setup();
		renderAdminLodgings();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByTestId("row-edit-btn"));

		expect(
			screen.getByRole("heading", { name: "Editar alojamiento" }),
		).toBeInTheDocument();
		expect(screen.getByTestId("field-name")).toHaveValue("Cabaña del Lago");
		expect(screen.getByTestId("field-email")).toHaveValue(
			"contacto@cabana.com",
		);
	});

	it("shows an inline form error (not an alert) when the update request rejects", async () => {
		mockGetDefaults({
			lodgings: [lodgingFixture({ id: 1, name: "Cabaña del Lago" })],
		});
		put.mockRejectedValue(new Error("No se pudo actualizar"));
		const user = userEvent.setup();
		renderAdminLodgings();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByTestId("row-edit-btn"));
		await user.click(screen.getByTestId("admin-save-btn"));

		// SUSPICIOUS: AdminLodgings surfaces save failures as an inline
		// `.form-error` string inside the modal, while AdminUsers (same PR) uses
		// a blocking window.alert for its own failure path — the two Admin CRUD
		// screens use different, inconsistent error UX. Asserted as-is per spec
		// Risks; not unified here (no production change in this change).
		expect(
			await screen.findByText("No se pudo actualizar"),
		).toBeInTheDocument();
		expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
	});

	it("asks for confirm-cancel when there are unsaved changes and keeps the modal open on dismiss", async () => {
		mockGetDefaults({
			lodgings: [lodgingFixture({ id: 1, name: "Cabaña del Lago" })],
		});
		const user = userEvent.setup();
		renderAdminLodgings();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByTestId("row-edit-btn"));

		await user.clear(screen.getByTestId("field-name"));
		await user.type(screen.getByTestId("field-name"), "Cabaña Renovada");
		await user.click(screen.getByTestId("admin-cancel-btn"));

		expect(screen.getByTestId("confirm-cancel")).toBeInTheDocument();

		await user.click(screen.getByTestId("confirm-cancel-no"));

		expect(screen.queryByTestId("confirm-cancel")).not.toBeInTheDocument();
		expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
	});
});

describe("AdminLodgings - delete", () => {
	it("shows the confirm dialog and calls DELETE /lodgings/:id on accept, then refreshes the list", async () => {
		mockGetDefaults({
			lodgings: [lodgingFixture({ id: 1, name: "Cabaña del Lago" })],
		});
		del.mockResolvedValue({});
		const user = userEvent.setup();
		renderAdminLodgings();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByTestId("row-delete-btn"));

		expect(screen.getByTestId("confirm-delete")).toHaveTextContent(
			'¿Eliminar el alojamiento "Cabaña del Lago"?',
		);

		get.mockImplementation((endpoint) => {
			if (endpoint.startsWith("/lodgings/admin"))
				return Promise.resolve(pageResponse([]));
			return Promise.resolve([]);
		});

		await user.click(screen.getByTestId("confirm-delete-yes"));

		expect(del).toHaveBeenCalledWith("/lodgings/1");
		await waitFor(() => {
			expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
		});
		expect(
			await screen.findByText(
				"No hay alojamientos cargados todavía. ¡Agregá el primero!",
			),
		).toBeInTheDocument();
	});

	it("makes no DELETE request when the confirmation is dismissed", async () => {
		mockGetDefaults({
			lodgings: [lodgingFixture({ id: 1, name: "Cabaña del Lago" })],
		});
		const user = userEvent.setup();
		renderAdminLodgings();

		await screen.findByText("Cabaña del Lago");
		await user.click(screen.getByTestId("row-delete-btn"));
		await user.click(screen.getByTestId("confirm-delete-no"));

		expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
		expect(del).not.toHaveBeenCalled();
		expect(screen.getByText("Cabaña del Lago")).toBeInTheDocument();
	});
});
