import { useState } from "react";
import { post, put } from "../../services/api";
import { customRender, fireEvent, screen, userEvent, waitFor } from "../../test/test-utils";
import LodgingFormModal from "./LodgingFormModal";

vi.mock("../../services/api");

function renderModal(overrides = {}) {
  const props = {
    lodging: null,
    categories: [],
    features: [],
    policies: [],
    onSaved: vi.fn(),
    onClose: vi.fn(),
    ...overrides,
  };
  return { ...customRender(<LodgingFormModal {...props} />), props };
}

function fillRequiredFields() {
  fireEvent.change(screen.getByTestId("field-name"), { target: { value: "Cabaña Test" } });
  fireEvent.change(screen.getByTestId("field-email"), { target: { value: "test@example.com" } });
  fireEvent.change(screen.getByTestId("field-description"), { target: { value: "Una descripción" } });
  fireEvent.change(screen.getByTestId("field-address"), { target: { value: "Calle 123" } });
  fireEvent.change(screen.getByTestId("field-city"), { target: { value: "Bariloche" } });
  fireEvent.change(screen.getByTestId("field-country"), { target: { value: "Argentina" } });
  fireEvent.change(screen.getByTestId("field-phoneNumber"), { target: { value: "1122334455" } });
  fireEvent.change(screen.getByTestId("field-pricePerNight"), { target: { value: "12500.50" } });
  fireEvent.change(screen.getByTestId("field-maxGuests"), { target: { value: "4" } });
}

function ModalHarness() {
  const [open, setOpen] = useState(false);

  return (
    <>
      <button onClick={() => setOpen(true)}>Abrir alojamiento</button>
      {open && (
        <LodgingFormModal
          lodging={null}
          categories={[]}
          features={[]}
          policies={[]}
          onSaved={vi.fn()}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
}

describe("LodgingFormModal - accessible dialog behavior", () => {
  it("exposes a named, described modal and initially focuses the name field", () => {
    renderModal();

    const dialog = screen.getByRole("dialog", { name: "Nuevo alojamiento" });
    expect(dialog).toHaveAttribute("aria-modal", "true");
    expect(dialog).toHaveAccessibleDescription("* Campos obligatorios");
    expect(screen.getByTestId("field-name")).toHaveFocus();
  });

  it("closes a clean form directly with Escape and restores focus to its opener", async () => {
    const user = userEvent.setup();
    customRender(<ModalHarness />);
    const opener = screen.getByRole("button", { name: "Abrir alojamiento" });

    await user.click(opener);
    expect(screen.getByTestId("field-name")).toHaveFocus();
    await user.keyboard("{Escape}");

    expect(screen.queryByTestId("admin-modal")).not.toBeInTheDocument();
    expect(opener).toHaveFocus();
  });

  it("traps Tab and Shift+Tab within enabled form controls", async () => {
    const user = userEvent.setup();
    renderModal();
    const firstControl = screen.getByTestId("field-name");
    const lastControl = screen.getByTestId("admin-cancel-btn");

    expect(firstControl).toHaveFocus();
    await user.tab({ shift: true });
    expect(lastControl).toHaveFocus();
    await user.tab();
    expect(firstControl).toHaveFocus();
  });

  it("does not dismiss from an interior click and closes a clean form from the overlay", async () => {
    const user = userEvent.setup();
    const { props } = renderModal();
    const modal = screen.getByTestId("admin-modal");

    await user.click(modal);
    expect(props.onClose).not.toHaveBeenCalled();

    await user.click(modal.parentElement);
    expect(props.onClose).toHaveBeenCalledTimes(1);
  });

  it("opens ConfirmDialog for a dirty Escape and restores focus when discard is canceled", async () => {
    const user = userEvent.setup();
    const { props } = renderModal();
    const nameField = screen.getByTestId("field-name");
    await user.type(nameField, "Cabaña pendiente");

    await user.keyboard("{Escape}");
    expect(screen.getByTestId("confirm-cancel-no")).toHaveFocus();
    expect(props.onClose).not.toHaveBeenCalled();

    await user.click(screen.getByTestId("confirm-cancel-no"));
    expect(screen.queryByTestId("confirm-cancel")).not.toBeInTheDocument();
    expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
    expect(nameField).toHaveFocus();
  });

  it("discards dirty changes after an overlay close request is confirmed", async () => {
    const user = userEvent.setup();
    const { props } = renderModal();
    await user.type(screen.getByTestId("field-name"), "Cabaña pendiente");

    await user.click(screen.getByTestId("admin-modal").parentElement);
    expect(screen.getByTestId("confirm-cancel")).toBeInTheDocument();
    await user.click(screen.getByTestId("confirm-cancel-yes"));

    expect(props.onClose).toHaveBeenCalledTimes(1);
  });

  it("prevents duplicate submit and close requests while submit is pending", async () => {
    let resolveRequest;
    post.mockImplementation(
      () => new Promise((resolve) => {
        resolveRequest = resolve;
      }),
    );
    const user = userEvent.setup();
    const { props } = renderModal();
    fillRequiredFields();
    const saveButton = screen.getByTestId("admin-save-btn");

    await user.click(saveButton);
    expect(saveButton).toBeDisabled();
    await user.keyboard("{Escape}");
    fireEvent.click(screen.getByTestId("admin-modal").parentElement);
    fireEvent.submit(saveButton.closest("form"));

    expect(post).toHaveBeenCalledTimes(1);
    expect(props.onClose).not.toHaveBeenCalled();
    expect(screen.queryByTestId("confirm-cancel")).not.toBeInTheDocument();

    resolveRequest({ id: 1 });
    await waitFor(() => expect(props.onClose).toHaveBeenCalledTimes(1));
  });

  it("blocks submit and close requests while an image upload is pending", async () => {
    let resolveUpload;
    global.fetch = vi.fn(
      () => new Promise((resolve) => {
        resolveUpload = resolve;
      }),
    );
    const user = userEvent.setup();
    const { props } = renderModal();
    const file = new File(["image"], "photo.png", { type: "image/png" });

    await user.upload(screen.getByLabelText(/URLs de imágenes/i), file);
    expect(screen.getByTestId("admin-save-btn")).toBeDisabled();
    await user.keyboard("{Escape}");
    fireEvent.click(screen.getByTestId("admin-modal").parentElement);

    expect(props.onClose).not.toHaveBeenCalled();
    expect(screen.queryByTestId("confirm-cancel")).not.toBeInTheDocument();

    resolveUpload({
      ok: true,
      json: async () => ({ url: "https://example.com/photo.png" }),
    });
    await waitFor(() => expect(screen.getByTestId("admin-save-btn")).not.toBeDisabled());
  });
});

describe("LodgingFormModal - price and capacity", () => {
  it("creates a lodging with numeric ARS price and guest capacity", async () => {
    const user = userEvent.setup();
    post.mockResolvedValue({ id: 1 });

    renderModal();
    fillRequiredFields();
    await user.click(screen.getByTestId("admin-save-btn"));

    expect(post).toHaveBeenCalledWith(
      "/lodgings",
      expect.objectContaining({
        pricePerNight: 12500.5,
        maxGuests: 4,
      }),
    );
  });

  it("loads and submits the existing values when editing", async () => {
    const user = userEvent.setup();
    put.mockResolvedValue({ id: 7 });
    const lodging = {
      id: 7,
      name: "Cabaña existente",
      description: "Descripción existente",
      address: "Calle 7",
      city: "Bariloche",
      country: "Argentina",
      phoneNumber: "2944000000",
      email: "cabana@example.com",
      categoryId: null,
      features: [],
      policies: [],
      imageUrls: [],
      pricePerNight: 98765.25,
      maxGuests: 6,
    };

    renderModal({ lodging });

    expect(
      screen.getByRole("spinbutton", { name: /precio por noche/i }),
    ).toHaveValue(98765.25);
    expect(
      screen.getByRole("spinbutton", { name: /capacidad máxima/i }),
    ).toHaveValue(6);

    await user.clear(screen.getByTestId("field-pricePerNight"));
    await user.type(screen.getByTestId("field-pricePerNight"), "100000.75");
    await user.clear(screen.getByTestId("field-maxGuests"));
    await user.type(screen.getByTestId("field-maxGuests"), "8");
    await user.click(screen.getByTestId("admin-save-btn"));

    expect(put).toHaveBeenCalledWith(
      "/lodgings/7",
      expect.objectContaining({
        pricePerNight: 100000.75,
        maxGuests: 8,
      }),
    );
  });

  it("shows required errors when price and capacity are empty", async () => {
    const user = userEvent.setup();
    renderModal();

    await user.click(screen.getByTestId("admin-save-btn"));

    expect(screen.getByTestId("error-pricePerNight")).toBeVisible();
    expect(screen.getByTestId("error-maxGuests")).toBeVisible();
    expect(post).not.toHaveBeenCalled();
  });

  it.each([
    ["0", "El precio por noche debe ser mayor a cero"],
    ["-1", "El precio por noche debe ser mayor a cero"],
  ])("rejects price %s", async (value, message) => {
    const user = userEvent.setup();
    renderModal();
    fillRequiredFields();
    await user.clear(screen.getByTestId("field-pricePerNight"));
    await user.type(screen.getByTestId("field-pricePerNight"), value);

    await user.click(screen.getByTestId("admin-save-btn"));

    expect(screen.getByTestId("error-pricePerNight")).toHaveTextContent(message);
    expect(post).not.toHaveBeenCalled();
  });

  it.each([
    ["0", "La capacidad máxima debe ser mayor a cero"],
    ["-1", "La capacidad máxima debe ser mayor a cero"],
    ["1.5", "La capacidad máxima debe ser un número entero"],
  ])("rejects guest capacity %s", async (value, message) => {
    const user = userEvent.setup();
    renderModal();
    fillRequiredFields();
    await user.clear(screen.getByTestId("field-maxGuests"));
    await user.type(screen.getByTestId("field-maxGuests"), value);

    await user.click(screen.getByTestId("admin-save-btn"));

    expect(screen.getByTestId("error-maxGuests")).toHaveTextContent(message);
    expect(post).not.toHaveBeenCalled();
  });
});

describe("LodgingFormModal - ImageUpload failure handling", () => {
  it("shows the upload error inside the modal and still allows submitting without an image", async () => {
    const user = userEvent.setup();
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({}),
    });
    post.mockResolvedValue({ id: 1 });

    const { props } = renderModal();

    fillRequiredFields();

    const file = new File(["fake-image"], "photo.png", { type: "image/png" });
    await user.upload(screen.getByLabelText(/URLs de imágenes/i), file);

    expect(
      await screen.findByText("No se pudo subir la imagen. Intentá de nuevo."),
    ).toBeInTheDocument();

    await user.click(screen.getByTestId("admin-save-btn"));

    expect(post).toHaveBeenCalledWith(
      "/lodgings",
      expect.objectContaining({ imageUrls: [] }),
    );
    expect(props.onSaved).toHaveBeenCalled();
  });
});
