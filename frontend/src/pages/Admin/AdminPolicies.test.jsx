import { customRender, fireEvent, screen, userEvent, waitFor } from "../../test/test-utils";
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
      await screen.findByText("No hay políticas cargadas todavía. ¡Creá la primera!")
    ).toBeInTheDocument();
  });

  it("renders a row per policy with name and description", async () => {
    get.mockResolvedValue([
      policyFixture({ id: 1, name: "Check-in flexible" }),
      policyFixture({ id: 2, name: "No mascotas", description: "No se permiten mascotas." }),
    ]);
    renderAdminPolicies();

    expect(await screen.findByText("Check-in flexible")).toBeInTheDocument();
    expect(screen.getByText("No mascotas")).toBeInTheDocument();
    expect(screen.getByText("No se permiten mascotas.")).toBeInTheDocument();
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

    await screen.findByText("No hay políticas cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));

    expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Nueva política" })).toBeInTheDocument();
    expect(screen.getByTestId("field-name")).toHaveValue("");
  });

  it("shows inline required-field errors for name AND icon, and makes no request when submitting empty", async () => {
    get.mockResolvedValue([]);
    const user = userEvent.setup();
    renderAdminPolicies();

    await screen.findByText("No hay políticas cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));
    await user.click(screen.getByTestId("admin-save-btn"));

    // AdminPolicies.validate() requires BOTH name and icon (like
    // AdminFeatures), and ALSO carries a description field (like
    // AdminCategories) — re-verified individually from validate(), not
    // assumed shared across the four modules.
    expect(await screen.findByTestId("error-name")).toHaveTextContent("El nombre es obligatorio");
    expect(screen.getByTestId("error-icon")).toHaveTextContent("El ícono es obligatorio");
    expect(post).not.toHaveBeenCalled();
  });

  it("submits the form and refreshes the list on success", async () => {
    get.mockResolvedValue([]);
    post.mockResolvedValue(policyFixture());
    const user = userEvent.setup();
    renderAdminPolicies();

    await screen.findByText("No hay políticas cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));

    await user.type(screen.getByTestId("field-name"), "Check-in flexible");
    await user.type(screen.getByTestId("field-description"), "Ingreso a partir de las 14hs.");
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
    get.mockResolvedValue([policyFixture({ id: 1, name: "Check-in flexible" })]);
    const user = userEvent.setup();
    renderAdminPolicies();

    await screen.findByText("Check-in flexible");
    await user.click(screen.getByTestId("row-edit-btn"));

    expect(screen.getByRole("heading", { name: "Editar política" })).toBeInTheDocument();
    expect(screen.getByTestId("field-name")).toHaveValue("Check-in flexible");
  });

  it("shows an inline form error (not an alert) when the update request rejects", async () => {
    get.mockResolvedValue([policyFixture({ id: 1, name: "Check-in flexible" })]);
    put.mockRejectedValue(new Error("No se pudo actualizar"));
    const user = userEvent.setup();
    renderAdminPolicies();

    await screen.findByText("Check-in flexible");
    await user.click(screen.getByTestId("row-edit-btn"));
    await user.click(screen.getByTestId("admin-save-btn"));

    expect(await screen.findByText("No se pudo actualizar")).toBeInTheDocument();
    expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
  });

  it("asks for confirm-cancel when there are unsaved changes and keeps the modal open on dismiss", async () => {
    get.mockResolvedValue([policyFixture({ id: 1, name: "Check-in flexible" })]);
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
    get.mockResolvedValue([policyFixture({ id: 1, name: "Check-in flexible" })]);
    del.mockResolvedValue({});
    const user = userEvent.setup();
    renderAdminPolicies();

    await screen.findByText("Check-in flexible");
    await user.click(screen.getByTestId("row-delete-btn"));

    // Uses the in-app ConfirmDialog (testId confirm-delete), kept independent
    // from the confirm-cancel dialog (unsaved-edit guard) that already exists
    // in this same screen via useConfirmCancel.
    expect(screen.getByTestId("confirm-delete")).toHaveTextContent(
      '¿Eliminar política "Check-in flexible"?'
    );
    expect(del).not.toHaveBeenCalled();

    get.mockResolvedValue([]);
    await user.click(screen.getByTestId("confirm-delete-yes"));

    expect(del).toHaveBeenCalledWith("/policies/1");
    await waitFor(() => {
      expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
    });
    expect(
      await screen.findByText("No hay políticas cargadas todavía. ¡Creá la primera!")
    ).toBeInTheDocument();
  });

  it("makes no DELETE request when the confirmation is dismissed", async () => {
    get.mockResolvedValue([policyFixture({ id: 1, name: "Check-in flexible" })]);
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
    get.mockResolvedValue([policyFixture({ id: 1, name: "Check-in flexible" })]);
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
      policyFixture({ id: 2, name: "No mascotas", description: "No se permiten mascotas." }),
    ]);
    const user = userEvent.setup();
    renderAdminPolicies();

    await screen.findByText("Check-in flexible");

    await user.click(screen.getByTestId("row-1").querySelector("[data-testid='row-delete-btn']"));
    expect(screen.getByTestId("confirm-delete")).toHaveTextContent('¿Eliminar política "Check-in flexible"?');
    await user.click(screen.getByTestId("confirm-delete-no"));

    await user.click(screen.getByTestId("row-2").querySelector("[data-testid='row-delete-btn']"));
    expect(screen.getByTestId("confirm-delete")).toHaveTextContent('¿Eliminar política "No mascotas"?');
    expect(del).not.toHaveBeenCalled();
  });
});

describe("AdminPolicies - invalid-field focus timeout cleanup", () => {
  it("clears the pending focus timeout on unmount so it never fires after teardown", async () => {
    get.mockResolvedValue([]);
    const user = userEvent.setup();
    const { unmount } = renderAdminPolicies();

    await screen.findByText("No hay políticas cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));

    const clearTimeoutSpy = vi.spyOn(window, "clearTimeout");
    fireEvent.click(screen.getByTestId("admin-save-btn"));
    unmount();

    expect(clearTimeoutSpy).toHaveBeenCalled();
    clearTimeoutSpy.mockRestore();
  });
});
