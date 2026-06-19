import { post } from "../../services/api";
import { customRender, screen, userEvent } from "../../test/test-utils";
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

async function fillRequiredFields(user) {
  await user.type(screen.getByTestId("field-name"), "Cabaña Test");
  await user.type(screen.getByTestId("field-email"), "test@example.com");
  await user.type(screen.getByTestId("field-description"), "Una descripción");
  await user.type(screen.getByTestId("field-address"), "Calle 123");
  await user.type(screen.getByTestId("field-city"), "Bariloche");
  await user.type(screen.getByTestId("field-country"), "Argentina");
  await user.type(screen.getByTestId("field-phoneNumber"), "1122334455");
}

describe("LodgingFormModal - ImageUpload failure handling", () => {
  it("shows the upload error inside the modal and still allows submitting without an image", async () => {
    const user = userEvent.setup();
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({}),
    });
    post.mockResolvedValue({ id: 1 });

    const { props } = renderModal();

    await fillRequiredFields(user);

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
