import { customRender, screen, userEvent, waitFor } from "../../test/test-utils";
import AdminFeatures from "./AdminFeatures";
import { get, post, put, del } from "../../services/api";

vi.mock("../../services/api");

const featureFixture = (overrides = {}) => ({
  id: 1,
  name: "WiFi",
  icon: "fa-solid fa-wifi",
  ...overrides,
});

function renderAdminFeatures(options) {
  return customRender(<AdminFeatures />, options);
}

describe("AdminFeatures - listing", () => {
  it("renders the empty state when there are no features", async () => {
    get.mockResolvedValue([]);
    renderAdminFeatures();

    expect(
      await screen.findByText("No hay características cargadas todavía. ¡Creá la primera!")
    ).toBeInTheDocument();
  });

  it("renders a row per feature with name and icon code", async () => {
    get.mockResolvedValue([
      featureFixture({ id: 1, name: "WiFi" }),
      featureFixture({ id: 2, name: "Pileta", icon: "fa-solid fa-water-ladder" }),
    ]);
    renderAdminFeatures();

    expect(await screen.findByText("WiFi")).toBeInTheDocument();
    expect(screen.getByText("Pileta")).toBeInTheDocument();
    expect(screen.getByText("fa-solid fa-water-ladder")).toBeInTheDocument();
  });

  it("fetches from GET /features on mount", async () => {
    get.mockResolvedValue([featureFixture()]);
    renderAdminFeatures();

    await screen.findByText("WiFi");
    expect(get).toHaveBeenCalledWith("/features");
  });
});

describe("AdminFeatures - create", () => {
  it("opens the modal in create mode when clicking the add button", async () => {
    get.mockResolvedValue([]);
    const user = userEvent.setup();
    renderAdminFeatures();

    await screen.findByText("No hay características cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));

    expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Nueva característica" })).toBeInTheDocument();
    expect(screen.getByTestId("field-name")).toHaveValue("");
  });

  it("shows inline required-field errors for name AND icon, and makes no request when submitting empty", async () => {
    get.mockResolvedValue([]);
    const user = userEvent.setup();
    renderAdminFeatures();

    await screen.findByText("No hay características cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));
    await user.click(screen.getByTestId("admin-save-btn"));

    // AdminFeatures.validate() requires BOTH name and icon, unlike
    // AdminCategories (only name required) and AdminPolicies (name + icon,
    // same as here) — each module's required-field set was re-verified
    // individually from its own validate() function, not assumed shared.
    expect(await screen.findByTestId("error-name")).toHaveTextContent("El nombre es obligatorio");
    expect(screen.getByTestId("error-icon")).toHaveTextContent("El ícono es obligatorio");
    expect(post).not.toHaveBeenCalled();
  });

  it("submits the form and refreshes the list on success", async () => {
    get.mockResolvedValue([]);
    post.mockResolvedValue(featureFixture());
    const user = userEvent.setup();
    renderAdminFeatures();

    await screen.findByText("No hay características cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));

    await user.type(screen.getByTestId("field-name"), "WiFi");
    // icon is a required field here (unlike AdminCategories) — select a real
    // entry through the IconPicker widget instead of leaving it empty.
    await user.click(screen.getByTestId("icon-picker-trigger"));
    await user.click(screen.getByTestId("icon-picker-item-wifi"));

    get.mockResolvedValue([featureFixture()]);

    await user.click(screen.getByTestId("admin-save-btn"));

    expect(post).toHaveBeenCalledWith("/features", { name: "WiFi", icon: "wifi" });

    await waitFor(() => {
      expect(screen.queryByTestId("admin-modal")).not.toBeInTheDocument();
    });
    expect(await screen.findByText("WiFi")).toBeInTheDocument();
  });
});

describe("AdminFeatures - edit", () => {
  it("opens the modal pre-filled with the selected feature's data", async () => {
    get.mockResolvedValue([featureFixture({ id: 1, name: "WiFi" })]);
    const user = userEvent.setup();
    renderAdminFeatures();

    await screen.findByText("WiFi");
    await user.click(screen.getByTestId("row-edit-btn"));

    expect(screen.getByRole("heading", { name: "Editar característica" })).toBeInTheDocument();
    expect(screen.getByTestId("field-name")).toHaveValue("WiFi");
  });

  it("shows an inline form error (not an alert) when the update request rejects", async () => {
    get.mockResolvedValue([featureFixture({ id: 1, name: "WiFi" })]);
    put.mockRejectedValue(new Error("No se pudo actualizar"));
    const user = userEvent.setup();
    renderAdminFeatures();

    await screen.findByText("WiFi");
    await user.click(screen.getByTestId("row-edit-btn"));
    await user.click(screen.getByTestId("admin-save-btn"));

    expect(await screen.findByText("No se pudo actualizar")).toBeInTheDocument();
    expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
  });

  it("asks for confirm-cancel when there are unsaved changes and keeps the modal open on dismiss", async () => {
    get.mockResolvedValue([featureFixture({ id: 1, name: "WiFi" })]);
    const user = userEvent.setup();
    renderAdminFeatures();

    await screen.findByText("WiFi");
    await user.click(screen.getByTestId("row-edit-btn"));

    await user.clear(screen.getByTestId("field-name"));
    await user.type(screen.getByTestId("field-name"), "WiFi 5G");
    await user.click(screen.getByTestId("admin-cancel-btn"));

    expect(screen.getByTestId("confirm-cancel")).toBeInTheDocument();

    await user.click(screen.getByTestId("confirm-cancel-no"));

    expect(screen.queryByTestId("confirm-cancel")).not.toBeInTheDocument();
    expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
  });
});

describe("AdminFeatures - delete", () => {
  it("asks for native confirmation and calls DELETE /features/:id on accept, then refreshes the list", async () => {
    get.mockResolvedValue([featureFixture({ id: 1, name: "WiFi" })]);
    del.mockResolvedValue({});
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const user = userEvent.setup();
    renderAdminFeatures();

    await screen.findByText("WiFi");
    get.mockResolvedValue([]);
    await user.click(screen.getByTestId("row-delete-btn"));

    // SUSPICIOUS: AdminFeatures uses native window.confirm for delete (NOT
    // the in-app ConfirmDialog used by AdminCategories/AdminLodgings) — a
    // third inconsistent confirmation mechanism across Admin CRUD screens.
    // Asserted as-is per spec Risks; not unified here.
    expect(window.confirm).toHaveBeenCalledWith('¿Eliminar característica "WiFi"?');

    await waitFor(() => {
      expect(del).toHaveBeenCalledWith("/features/1");
    });
    expect(
      await screen.findByText("No hay características cargadas todavía. ¡Creá la primera!")
    ).toBeInTheDocument();
  });

  it("makes no DELETE request when the native confirmation is rejected", async () => {
    get.mockResolvedValue([featureFixture({ id: 1, name: "WiFi" })]);
    vi.spyOn(window, "confirm").mockReturnValue(false);
    const user = userEvent.setup();
    renderAdminFeatures();

    await screen.findByText("WiFi");
    await user.click(screen.getByTestId("row-delete-btn"));

    expect(window.confirm).toHaveBeenCalled();
    expect(del).not.toHaveBeenCalled();
    expect(screen.getByText("WiFi")).toBeInTheDocument();
  });

  it("surfaces the request error via alert when DELETE rejects", async () => {
    get.mockResolvedValue([featureFixture({ id: 1, name: "WiFi" })]);
    del.mockRejectedValue(new Error("No se pudo eliminar"));
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const alertSpy = vi.spyOn(window, "alert").mockImplementation(() => {});
    const user = userEvent.setup();
    renderAdminFeatures();

    await screen.findByText("WiFi");
    await user.click(screen.getByTestId("row-delete-btn"));

    await waitFor(() => {
      expect(alertSpy).toHaveBeenCalledWith("No se pudo eliminar");
    });
    expect(screen.getByText("WiFi")).toBeInTheDocument();
  });
});
