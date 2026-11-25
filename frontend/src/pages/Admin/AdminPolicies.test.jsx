import {
	customRender,
	fireEvent,
	screen,
	userEvent,
	waitFor,
} from "../../test/test-utils";
import AdminPolicies from "./AdminPolicies";
import { get, post, put, del } from "../../services/api";

vi.mock("../../services/api");

const policyFixture = (overrides = {}) => ({
	id: 1,
	name: "Check-in flexible",
	description: "Ingreso a partir de las 14hs.",
	icon: "fa-solid fa-clock",
	...overrides,
});

function renderAdminPolicies(options) {
	return customRender(<AdminPolicies />, options);
}

describe("AdminPolicies - listing", () => {
	it("renders the empty state when there are no policies", async () => {
		get.mockResolvedValue([]);
		renderAdminPolicies();

		expect(
			await screen.findByText(
				"No hay políticas cargadas todavía. ¡Creá la primera!",
			),
		).toBeInTheDocument();
	});

	it("shows an alert instead of the empty state when the initial load fails", async () => {
		get.mockRejectedValue(new Error("Network error"));
		const consoleErrorSpy = vi
			.spyOn(console, "error")
			.mockImplementation(() => {});
		renderAdminPolicies();

		expect(await screen.findByRole("alert")).toHaveTextContent(
			"No pudimos cargar las políticas.",
		);
		expect(
			screen.getByRole("button", { name: "Reintentar" }),
		).toBeInTheDocument();
		expect(
			screen.queryByText(
				"No hay políticas cargadas todavía. ¡Creá la primera!",
			),
		).not.toBeInTheDocument();
		consoleErrorSpy.mockRestore();
	});

	it("retries the initial load and restores the successful empty state", async () => {
		get
			.mockRejectedValueOnce(new Error("Network error"))
			.mockResolvedValueOnce([]);
		const consoleErrorSpy = vi
			.spyOn(console, "error")
			.mockImplementation(() => {});
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByRole("alert");
		await user.click(screen.getByRole("button", { name: "Reintentar" }));

		expect(
			await screen.findByText(
				"No hay políticas cargadas todavía. ¡Creá la primera!",
			),
		).toBeInTheDocument();
		expect(screen.queryByRole("alert")).not.toBeInTheDocument();
		expect(get).toHaveBeenCalledTimes(2);
		consoleErrorSpy.mockRestore();
	});

	it("renders a row per policy with name and description", async () => {
		get.mockResolvedValue([
			policyFixture({ id: 1, name: "Check-in flexible" }),
			policyFixture({
				id: 2,
				name: "No mascotas",
				description: "No se permiten mascotas.",
			}),
		]);
		renderAdminPolicies();

		expect(await screen.findByText("Check-in flexible")).toBeInTheDocument();
		expect(screen.getByText("No mascotas")).toBeInTheDocument();
		expect(screen.getByText("No se permiten mascotas.")).toBeInTheDocument();
	});

	it("wraps the policies table in a keyboard-scrollable named region", async () => {
		get.mockResolvedValue([policyFixture()]);
		renderAdminPolicies();

		await screen.findByText("Check-in flexible");
		const region = screen.getByRole("region", {
			name: "Tabla de políticas",
		});
		expect(region).toHaveAttribute("tabindex", "0");
		expect(region.querySelector("table")).toBeInTheDocument();
	});

	it("fetches from GET /policies on mount", async () => {
		get.mockResolvedValue([policyFixture()]);
		renderAdminPolicies();

		await screen.findByText("Check-in flexible");
		expect(get).toHaveBeenCalledWith("/policies");
	});
});

describe("AdminPolicies - create", () => {
	it("opens the modal in create mode when clicking the add button", async () => {
		get.mockResolvedValue([]);
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByText(
			"No hay políticas cargadas todavía. ¡Creá la primera!",
		);
		await user.click(screen.getByTestId("admin-add-btn"));

		expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
		expect(
			screen.getByRole("heading", { name: "Nueva política" }),
		).toBeInTheDocument();
		expect(screen.getByTestId("field-name")).toHaveValue("");
	});

	it("shows inline required-field errors for name AND icon, and makes no request when submitting empty", async () => {
		get.mockResolvedValue([]);
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByText(
			"No hay políticas cargadas todavía. ¡Creá la primera!",
		);
		await user.click(screen.getByTestId("admin-add-btn"));
		await user.click(screen.getByTestId("admin-save-btn"));

		// AdminPolicies.validate() requires BOTH name and icon (like
		// AdminFeatures), and ALSO carries a description field (like
		// AdminCategories) — re-verified individually from validate(), not
		// assumed shared across the four modules.
		expect(await screen.findByTestId("error-name")).toHaveTextContent(
			"El nombre es obligatorio",
		);
		expect(screen.getByTestId("error-icon")).toHaveTextContent(
			"El ícono es obligatorio",
		);
		expect(post).not.toHaveBeenCalled();
	});

	it("submits the form and refreshes the list on success", async () => {
		get.mockResolvedValue([]);
		post.mockResolvedValue(policyFixture());
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByText(
			"No hay políticas cargadas todavía. ¡Creá la primera!",
		);
		await user.click(screen.getByTestId("admin-add-btn"));

		await user.type(screen.getByTestId("field-name"), "Check-in flexible");
		await user.type(
			screen.getByTestId("field-description"),
			"Ingreso a partir de las 14hs.",
		);
		await user.click(screen.getByTestId("icon-picker-trigger"));
		await user.click(screen.getByTestId("icon-picker-item-clock"));

		get.mockResolvedValue([policyFixture()]);

		await user.click(screen.getByTestId("admin-save-btn"));

		expect(post).toHaveBeenCalledWith("/policies", {
			name: "Check-in flexible",
			description: "Ingreso a partir de las 14hs.",
			icon: "clock",
		});

		await waitFor(() => {
			expect(screen.queryByTestId("admin-modal")).not.toBeInTheDocument();
		});
		expect(await screen.findByText("Check-in flexible")).toBeInTheDocument();
	});
});

describe("AdminPolicies - edit", () => {
	it("opens the modal pre-filled with the selected policy's data", async () => {
		get.mockResolvedValue([
			policyFixture({ id: 1, name: "Check-in flexible" }),
		]);
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByText("Check-in flexible");
		await user.click(screen.getByTestId("row-edit-btn"));

		expect(
			screen.getByRole("heading", { name: "Editar política" }),
		).toBeInTheDocument();
		expect(screen.getByTestId("field-name")).toHaveValue("Check-in flexible");
	});

	it("shows an inline form error (not an alert) when the update request rejects", async () => {
		get.mockResolvedValue([
			policyFixture({ id: 1, name: "Check-in flexible" }),
		]);
		put.mockRejectedValue(new Error("No se pudo actualizar"));
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByText("Check-in flexible");
		await user.click(screen.getByTestId("row-edit-btn"));
		await user.click(screen.getByTestId("admin-save-btn"));

		expect(
			await screen.findByText("No se pudo actualizar"),
		).toBeInTheDocument();
		expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
	});

	it("asks for confirm-cancel when there are unsaved changes and keeps the modal open on dismiss", async () => {
		get.mockResolvedValue([
			policyFixture({ id: 1, name: "Check-in flexible" }),
		]);
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByText("Check-in flexible");
		await user.click(screen.getByTestId("row-edit-btn"));

		await user.clear(screen.getByTestId("field-name"));
		await user.type(screen.getByTestId("field-name"), "Check-in estricto");
		await user.click(screen.getByTestId("admin-cancel-btn"));

		expect(screen.getByTestId("confirm-cancel")).toBeInTheDocument();

		await user.click(screen.getByTestId("confirm-cancel-no"));

		expect(screen.queryByTestId("confirm-cancel")).not.toBeInTheDocument();
		expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
	});
});

describe("AdminPolicies - delete", () => {
	it("shows the in-app confirm dialog and calls DELETE /policies/:id on accept, then refreshes the list", async () => {
		get.mockResolvedValue([
			policyFixture({ id: 1, name: "Check-in flexible" }),
		]);
		del.mockResolvedValue({});
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByText("Check-in flexible");
		await user.click(screen.getByTestId("row-delete-btn"));

		// Uses the in-app ConfirmDialog (testId confirm-delete), kept independent
		// from the confirm-cancel dialog (unsaved-edit guard) that already exists
		// in this same screen via useConfirmCancel.
		expect(screen.getByTestId("confirm-delete")).toHaveTextContent(
			'¿Eliminar política "Check-in flexible"?',
		);
		expect(del).not.toHaveBeenCalled();

		get.mockResolvedValue([]);
		await user.click(screen.getByTestId("confirm-delete-yes"));

		expect(del).toHaveBeenCalledWith("/policies/1");
		await waitFor(() => {
			expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
		});
		expect(
			await screen.findByText(
				"No hay políticas cargadas todavía. ¡Creá la primera!",
			),
		).toBeInTheDocument();
	});

	it("makes no DELETE request when the confirmation is dismissed", async () => {
		get.mockResolvedValue([
			policyFixture({ id: 1, name: "Check-in flexible" }),
		]);
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByText("Check-in flexible");
		await user.click(screen.getByTestId("row-delete-btn"));
		await user.click(screen.getByTestId("confirm-delete-no"));

		expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
		expect(del).not.toHaveBeenCalled();
		expect(screen.getByText("Check-in flexible")).toBeInTheDocument();
	});

	it("surfaces the request error via alert when DELETE rejects", async () => {
		get.mockResolvedValue([
			policyFixture({ id: 1, name: "Check-in flexible" }),
		]);
		del.mockRejectedValue(new Error("No se pudo eliminar"));
		const alertSpy = vi.spyOn(window, "alert").mockImplementation(() => {});
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByText("Check-in flexible");
		await user.click(screen.getByTestId("row-delete-btn"));
		await user.click(screen.getByTestId("confirm-delete-yes"));

		await waitFor(() => {
			expect(alertSpy).toHaveBeenCalledWith("No se pudo eliminar");
		});
		expect(screen.getByText("Check-in flexible")).toBeInTheDocument();
		expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
	});

	it("keeps the dialog message scoped to the most recently clicked row when reopened for a different policy", async () => {
		get.mockResolvedValue([
			policyFixture({ id: 1, name: "Check-in flexible" }),
			policyFixture({
				id: 2,
				name: "No mascotas",
				description: "No se permiten mascotas.",
			}),
		]);
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByText("Check-in flexible");

		await user.click(
			screen
				.getByTestId("row-1")
				.querySelector("[data-testid='row-delete-btn']"),
		);
		expect(screen.getByTestId("confirm-delete")).toHaveTextContent(
			'¿Eliminar política "Check-in flexible"?',
		);
		await user.click(screen.getByTestId("confirm-delete-no"));

		await user.click(
			screen
				.getByTestId("row-2")
				.querySelector("[data-testid='row-delete-btn']"),
		);
		expect(screen.getByTestId("confirm-delete")).toHaveTextContent(
			'¿Eliminar política "No mascotas"?',
		);
		expect(del).not.toHaveBeenCalled();
	});
});

describe("AdminPolicies - form semantics and direct ref focus", () => {
	it("exposes explicit name, htmlFor/id, enterKeyHint, aria-invalid/aria-describedby, and focuses invalid fields without setTimeout", async () => {
		get.mockResolvedValue([]);
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByText(
			"No hay políticas cargadas todavía. ¡Creá la primera!",
		);
		await user.click(screen.getByTestId("admin-add-btn"));

		const nameField = screen.getByLabelText("Nombre");
		expect(nameField).toHaveAttribute("name", "name");
		expect(nameField).toHaveAttribute("enterkeyhint", "next");
		expect(nameField).toHaveAttribute("aria-invalid", "false");
		expect(screen.getByLabelText("Descripción")).toHaveAttribute(
			"name",
			"description",
		);

		const timeoutSpy = vi.spyOn(globalThis, "setTimeout");
		fireEvent.click(screen.getByTestId("admin-save-btn"));

		expect(timeoutSpy).not.toHaveBeenCalledWith(expect.any(Function), 100);
		expect(nameField).toHaveFocus();

		const nameError = screen.getByTestId("error-name");
		expect(nameError).toHaveAttribute("role", "alert");
		expect(nameField).toHaveAttribute("aria-invalid", "true");
		expect(nameField).toHaveAttribute("aria-describedby", nameError.id);

		const iconError = screen.getByTestId("error-icon");
		expect(iconError).toHaveAttribute("role", "alert");

		await user.type(nameField, "Check-in flexible");
		fireEvent.click(screen.getByTestId("admin-save-btn"));
		expect(screen.getByTestId("icon-picker-trigger")).toHaveFocus();

		timeoutSpy.mockRestore();
	});

	it("exposes an accessible native dialog with its heading name and closes on Escape", async () => {
		get.mockResolvedValue([]);
		const user = userEvent.setup();
		renderAdminPolicies();

		await screen.findByText(
			"No hay políticas cargadas todavía. ¡Creá la primera!",
		);
		await user.click(screen.getByTestId("admin-add-btn"));

		const dialog = screen.getByRole("dialog", { name: "Nueva política" });
		expect(dialog.tagName).toBe("DIALOG");
		expect(dialog).toHaveAttribute("open");
		expect(dialog).not.toHaveAttribute("role");
		expect(dialog).not.toHaveAttribute("aria-modal");

		await user.keyboard("{Escape}");

		expect(screen.queryByTestId("admin-modal")).not.toBeInTheDocument();
	});
});
