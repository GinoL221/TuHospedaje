import { customRender, fireEvent, screen, userEvent, waitFor } from "../../test/test-utils";
import AdminCategories from "./AdminCategories";
import { get, post, put, del } from "../../services/api";

vi.mock("../../services/api");

const categoryFixture = (overrides = {}) => ({
  id: 1,
  name: "Cabañas",
  description: "Alojamientos tipo cabaña.",
  icon: "fa-solid fa-tree",
  imageUrl: "https://img.example.com/cabanas.jpg",
  ...overrides,
});

function renderAdminCategories(options) {
  return customRender(<AdminCategories />, options);
}

describe("AdminCategories - listing", () => {
  it("renders the empty state when there are no categories", async () => {
    get.mockResolvedValue([]);
    renderAdminCategories();

    expect(
      await screen.findByText("No hay categorías cargadas todavía. ¡Creá la primera!")
    ).toBeInTheDocument();
  });

  it("renders a row per category with name and description", async () => {
    get.mockResolvedValue([
      categoryFixture({ id: 1, name: "Cabañas" }),
      categoryFixture({ id: 2, name: "Hoteles", description: "Alojamientos tipo hotel." }),
    ]);
    renderAdminCategories();

    expect(await screen.findByText("Cabañas")).toBeInTheDocument();
    expect(screen.getByText("Hoteles")).toBeInTheDocument();
    expect(screen.getByText("Alojamientos tipo hotel.")).toBeInTheDocument();
  });

  it("fetches from GET /categories on mount", async () => {
    get.mockResolvedValue([categoryFixture()]);
    renderAdminCategories();

    await screen.findByText("Cabañas");
    expect(get).toHaveBeenCalledWith("/categories");
  });
});

describe("AdminCategories - create", () => {
  it("opens the modal in create mode when clicking the add button", async () => {
    get.mockResolvedValue([]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("No hay categorías cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));

    expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Nueva categoría" })).toBeInTheDocument();
    expect(screen.getByTestId("field-name")).toHaveValue("");
  });

  it("shows an inline required-name error and makes no request when submitting empty", async () => {
    get.mockResolvedValue([]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("No hay categorías cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));
    await user.click(screen.getByTestId("admin-save-btn"));

    expect(await screen.findByTestId("error-name")).toHaveTextContent("El nombre es obligatorio");
    expect(post).not.toHaveBeenCalled();
  });

  it("focuses the first invalid field without scheduling delayed DOM work", async () => {
    get.mockResolvedValue([]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("No hay categorías cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));
    const timeoutSpy = vi.spyOn(globalThis, "setTimeout");

    fireEvent.click(screen.getByTestId("admin-save-btn"));

    expect(timeoutSpy).not.toHaveBeenCalledWith(expect.any(Function), 100);
    expect(screen.getByTestId("field-name")).toHaveFocus();
  });

  it("submits the form and refreshes the list on success", async () => {
    get.mockResolvedValue([]);
    post.mockResolvedValue(categoryFixture());
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("No hay categorías cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));

    await user.type(screen.getByTestId("field-name"), "Cabañas");
    await user.type(screen.getByTestId("field-description"), "Alojamientos tipo cabaña.");
    await user.type(screen.getByTestId("field-image-url"), "https://img.example.com/cabanas.jpg");

    get.mockResolvedValue([categoryFixture()]);

    await user.click(screen.getByTestId("admin-save-btn"));

    expect(post).toHaveBeenCalledWith(
      "/categories",
      expect.objectContaining({
        name: "Cabañas",
        description: "Alojamientos tipo cabaña.",
        imageUrl: "https://img.example.com/cabanas.jpg",
      })
    );

    await waitFor(() => {
      expect(screen.queryByTestId("admin-modal")).not.toBeInTheDocument();
    });
    expect(await screen.findByText("Cabañas")).toBeInTheDocument();
  });
});

describe("AdminCategories - edit", () => {
  it("opens the modal pre-filled with the selected category's data", async () => {
    get.mockResolvedValue([categoryFixture({ id: 1, name: "Cabañas" })]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("Cabañas");
    await user.click(screen.getByTestId("row-edit-btn"));

    expect(screen.getByRole("heading", { name: "Editar categoría" })).toBeInTheDocument();
    expect(screen.getByTestId("field-name")).toHaveValue("Cabañas");
  });

  it("shows an inline form error (not an alert) when the update request rejects", async () => {
    get.mockResolvedValue([categoryFixture({ id: 1, name: "Cabañas" })]);
    put.mockRejectedValue(new Error("No se pudo actualizar"));
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("Cabañas");
    await user.click(screen.getByTestId("row-edit-btn"));
    await user.click(screen.getByTestId("admin-save-btn"));

    // SUSPICIOUS: AdminCategories surfaces save failures via an inline
    // `.form-error` string inside the modal (handleSubmit's .catch sets
    // `error` state) — same mechanism as AdminLodgings, but its OWN delete
    // failure path below uses window.alert instead. Two different error
    // mechanisms coexist within this single file. Asserted as-is per spec
    // Risks; not unified here (no production change in this change).
    expect(await screen.findByText("No se pudo actualizar")).toBeInTheDocument();
    expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
  });

  it("asks for confirm-cancel when there are unsaved changes and keeps the modal open on dismiss", async () => {
    get.mockResolvedValue([categoryFixture({ id: 1, name: "Cabañas" })]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("Cabañas");
    await user.click(screen.getByTestId("row-edit-btn"));

    await user.clear(screen.getByTestId("field-name"));
    await user.type(screen.getByTestId("field-name"), "Cabañas Renovadas");
    await user.click(screen.getByTestId("admin-cancel-btn"));

    expect(screen.getByTestId("confirm-cancel")).toBeInTheDocument();

    await user.click(screen.getByTestId("confirm-cancel-no"));

    expect(screen.queryByTestId("confirm-cancel")).not.toBeInTheDocument();
    expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
  });
});

describe("AdminCategories - delete", () => {
  it("shows the in-app confirm dialog and calls DELETE /categories/:id on accept, then refreshes the list", async () => {
    get.mockResolvedValue([categoryFixture({ id: 1, name: "Cabañas" })]);
    del.mockResolvedValue({});
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("Cabañas");
    await user.click(screen.getByTestId("row-delete-btn"));

    // Uses the in-app ConfirmDialog (testId confirm-delete), NOT
    // window.confirm — same mechanism as AdminLodgings, but different from
    // AdminFeatures/AdminPolicies which use window.confirm for delete.
    expect(screen.getByTestId("confirm-delete")).toHaveTextContent(
      '¿Eliminar la categoría "Cabañas"? Los alojamientos asociados quedarán sin categoría.'
    );

    get.mockResolvedValue([]);

    await user.click(screen.getByTestId("confirm-delete-yes"));

    expect(del).toHaveBeenCalledWith("/categories/1");
    await waitFor(() => {
      expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
    });
    expect(
      await screen.findByText("No hay categorías cargadas todavía. ¡Creá la primera!")
    ).toBeInTheDocument();
  });

  it("makes no DELETE request when the confirmation is dismissed", async () => {
    get.mockResolvedValue([categoryFixture({ id: 1, name: "Cabañas" })]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("Cabañas");
    await user.click(screen.getByTestId("row-delete-btn"));
    await user.click(screen.getByTestId("confirm-delete-no"));

    expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
    expect(del).not.toHaveBeenCalled();
    expect(screen.getByText("Cabañas")).toBeInTheDocument();
  });

  it("surfaces the request error via alert when DELETE rejects", async () => {
    get.mockResolvedValue([categoryFixture({ id: 1, name: "Cabañas" })]);
    del.mockRejectedValue(new Error("No se pudo eliminar"));
    const alertSpy = vi.spyOn(window, "alert").mockImplementation(() => {});
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("Cabañas");
    await user.click(screen.getByTestId("row-delete-btn"));
    await user.click(screen.getByTestId("confirm-delete-yes"));

    // SUSPICIOUS: confirmDelete's own failure path uses window.alert, while
    // the create/edit failure path in the SAME file uses an inline
    // `.form-error` string. Asserted as-is per spec Risks; not unified here.
    await waitFor(() => {
      expect(alertSpy).toHaveBeenCalledWith("No se pudo eliminar");
    });
    expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
  });
});

describe("AdminCategories - representative image", () => {
  it("marks the representative image as required when creating a category", async () => {
    get.mockResolvedValue([]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("No hay categorías cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));

    expect(screen.getByTestId("field-image-url")).toBeRequired();
  });

  it("does not mark the representative image as required when editing a category", async () => {
    get.mockResolvedValue([categoryFixture()]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("Cabañas");
    await user.click(screen.getByTestId("row-edit-btn"));

    expect(screen.getByTestId("field-image-url")).not.toBeRequired();
  });

  it("renders a representative-image URL field in the create form", async () => {
    get.mockResolvedValue([]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("No hay categorías cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));

    expect(screen.getByTestId("field-image-url")).toHaveValue("");
  });

  it("rejects create submission without a representative image and does not call the API", async () => {
    get.mockResolvedValue([]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("No hay categorías cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));
    await user.type(screen.getByTestId("field-name"), "Cabañas");
    await user.click(screen.getByTestId("admin-save-btn"));

    const fieldError = await screen.findByTestId("error-image-url");
    expect(fieldError).toHaveTextContent("La imagen representativa es obligatoria");
    const field = screen.getByTestId("field-image-url");
    expect(field).toHaveAttribute("aria-invalid", "true");
    expect(field).toHaveAttribute("aria-describedby", "error-image-url");
    expect(post).not.toHaveBeenCalled();
  });

  it("rejects a malformed representative-image URL and does not call the API", async () => {
    get.mockResolvedValue([]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("No hay categorías cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));
    await user.type(screen.getByTestId("field-name"), "Cabañas");
    await user.type(screen.getByTestId("field-image-url"), "not-a-url");
    await user.click(screen.getByTestId("admin-save-btn"));

    expect(await screen.findByTestId("error-image-url")).toHaveTextContent(
      "La imagen debe ser una URL https válida"
    );
    expect(post).not.toHaveBeenCalled();
  });

  it("shows a live preview once a syntactically valid https URL is entered", async () => {
    get.mockResolvedValue([]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("No hay categorías cargadas todavía. ¡Creá la primera!");
    await user.click(screen.getByTestId("admin-add-btn"));

    expect(screen.queryByTestId("image-url-preview")).not.toBeInTheDocument();

    await user.type(screen.getByTestId("field-image-url"), "https://img.example.com/cabana.jpg");

    expect(screen.getByTestId("image-url-preview")).toHaveAttribute(
      "src",
      "https://img.example.com/cabana.jpg"
    );
  });

  it("prefills the representative-image field when editing an existing category", async () => {
    get.mockResolvedValue([categoryFixture({ imageUrl: "https://img.example.com/existing.jpg" })]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("Cabañas");
    await user.click(screen.getByTestId("row-edit-btn"));

    expect(screen.getByTestId("field-image-url")).toHaveValue(
      "https://img.example.com/existing.jpg"
    );
  });

  it("preserves the stored image when an edit omits the representative-image field", async () => {
    get.mockResolvedValue([categoryFixture({ imageUrl: "https://img.example.com/existing.jpg" })]);
    put.mockResolvedValue({});
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("Cabañas");
    await user.click(screen.getByTestId("row-edit-btn"));
    await user.clear(screen.getByTestId("field-image-url"));
    await user.click(screen.getByTestId("admin-save-btn"));

    expect(put).toHaveBeenCalledWith(
      "/categories/1",
      expect.objectContaining({ imageUrl: null })
    );
    expect(screen.queryByTestId("error-image-url")).not.toBeInTheDocument();
  });

  it("rejects an invalid replacement on edit without erasing the existing valid image", async () => {
    get.mockResolvedValue([categoryFixture({ imageUrl: "https://img.example.com/existing.jpg" })]);
    const user = userEvent.setup();
    renderAdminCategories();

    await screen.findByText("Cabañas");
    await user.click(screen.getByTestId("row-edit-btn"));
    await user.clear(screen.getByTestId("field-image-url"));
    await user.type(screen.getByTestId("field-image-url"), "not-a-url");
    await user.click(screen.getByTestId("admin-save-btn"));

    expect(await screen.findByTestId("error-image-url")).toBeInTheDocument();
    expect(put).not.toHaveBeenCalled();
  });
});
