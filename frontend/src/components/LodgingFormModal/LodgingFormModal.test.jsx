import { useState } from "react";
import {
	createLodging,
	updateLodging,
} from "../../services/adminCatalogService";
import { uploadImage } from "../../services/uploadService";
import {
	customRender,
	fireEvent,
	screen,
	userEvent,
	waitFor,
} from "../../test/test-utils";
import LodgingFormModal from "./LodgingFormModal";

vi.mock("../../services/adminCatalogService");
vi.mock("../../services/uploadService");

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
	fireEvent.change(screen.getByTestId("field-name"), {
		target: { value: "Cabaña Test" },
	});
	fireEvent.change(screen.getByTestId("field-email"), {
		target: { value: "test@example.com" },
	});
	fireEvent.change(screen.getByTestId("field-description"), {
		target: { value: "Una descripción" },
	});
	fireEvent.change(screen.getByTestId("field-address"), {
		target: { value: "Calle 123" },
	});
	fireEvent.change(screen.getByTestId("field-city"), {
		target: { value: "Bariloche" },
	});
	fireEvent.change(screen.getByTestId("field-country"), {
		target: { value: "Argentina" },
	});
	fireEvent.change(screen.getByTestId("field-phoneNumber"), {
		target: { value: "1122334455" },
	});
	fireEvent.change(screen.getByTestId("field-pricePerNight"), {
		target: { value: "12500.50" },
	});
	fireEvent.change(screen.getByTestId("field-maxGuests"), {
		target: { value: "4" },
	});
}

const defaultLodgingWithImage = {
	id: null,
	imageUrls: ["https://example.com/foto1.jpg"],
};

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
	it("exposes a native modal dialog without redundant ARIA and initially focuses the name field", () => {
		renderModal();

		const dialog = screen.getByRole("dialog", { name: "Nuevo alojamiento" });
		expect(dialog.tagName).toBe("DIALOG");
		expect(dialog).toHaveAttribute("open");
		expect(dialog).toHaveAttribute("closedby", "any");
		expect(dialog).not.toHaveAttribute("role");
		expect(dialog).not.toHaveAttribute("aria-modal");
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

	it("navigates form controls with Tab and Shift+Tab", async () => {
		const user = userEvent.setup();
		renderModal();
		const firstControl = screen.getByTestId("field-name");
		const secondControl = screen.getByTestId("field-email");

		expect(firstControl).toHaveFocus();
		await user.tab();
		expect(secondControl).toHaveFocus();
		await user.tab({ shift: true });
		expect(firstControl).toHaveFocus();
	});

	it("does not dismiss from an interior click and closes a clean form from the backdrop", () => {
		const { props } = renderModal();
		const modal = screen.getByTestId("admin-modal");
		vi.spyOn(modal, "getBoundingClientRect").mockReturnValue({
			left: 100,
			right: 900,
			top: 50,
			bottom: 700,
			width: 800,
			height: 650,
		});

		const heading = screen.getByRole("heading", { name: "Nuevo alojamiento" });
		fireEvent.pointerDown(heading, { clientX: 200, clientY: 200 });
		fireEvent.pointerUp(heading, { clientX: 200, clientY: 200 });
		expect(props.onClose).not.toHaveBeenCalled();

		fireEvent.pointerDown(modal, { clientX: 20, clientY: 20 });
		fireEvent.pointerUp(modal, { clientX: 20, clientY: 20 });
		expect(props.onClose).toHaveBeenCalledTimes(1);
	});

	it("opens ConfirmDialog for a dirty Escape without manual inert/aria-hidden and restores focus when discard is canceled", async () => {
		const user = userEvent.setup();
		const { props } = renderModal();
		const modal = screen.getByTestId("admin-modal");
		const nameField = screen.getByTestId("field-name");
		await user.type(nameField, "Cabaña pendiente");

		await user.keyboard("{Escape}");
		expect(screen.getByTestId("confirm-cancel-no")).toHaveFocus();
		expect(modal).not.toHaveAttribute("aria-hidden");
		expect(modal).not.toHaveAttribute("inert");
		expect(props.onClose).not.toHaveBeenCalled();

		await user.click(screen.getByTestId("confirm-cancel-no"));
		expect(screen.queryByTestId("confirm-cancel")).not.toBeInTheDocument();
		expect(screen.getByTestId("admin-modal")).toBeInTheDocument();
		expect(nameField).toHaveFocus();
	});

	it("discards dirty changes after a backdrop close request is confirmed", async () => {
		const user = userEvent.setup();
		const { props } = renderModal();
		const modal = screen.getByTestId("admin-modal");
		await user.type(screen.getByTestId("field-name"), "Cabaña pendiente");

		vi.spyOn(modal, "getBoundingClientRect").mockReturnValue({
			left: 100,
			right: 900,
			top: 50,
			bottom: 700,
			width: 800,
			height: 650,
		});
		fireEvent.pointerDown(modal, { clientX: 20, clientY: 20 });
		fireEvent.pointerUp(modal, { clientX: 20, clientY: 20 });
		expect(screen.getByTestId("confirm-cancel")).toBeInTheDocument();
		await user.click(screen.getByTestId("confirm-cancel-yes"));

		expect(props.onClose).toHaveBeenCalledTimes(1);
	});

	it("prevents duplicate submit and close requests while submit is pending", async () => {
		let resolveRequest;
		createLodging.mockImplementation(
			() =>
				new Promise((resolve) => {
					resolveRequest = resolve;
				}),
		);
		const user = userEvent.setup();
		const { props } = renderModal({ lodging: defaultLodgingWithImage });
		const modal = screen.getByTestId("admin-modal");
		fillRequiredFields();
		const saveButton = screen.getByTestId("admin-save-btn");

		await user.click(saveButton);
		expect(saveButton).toBeDisabled();
		await user.keyboard("{Escape}");
		fireEvent.pointerDown(modal, { clientX: 0, clientY: 0 });
		fireEvent.pointerUp(modal, { clientX: 0, clientY: 0 });
		fireEvent.submit(saveButton.closest("form"));

		expect(createLodging).toHaveBeenCalledTimes(1);
		expect(props.onClose).not.toHaveBeenCalled();
		expect(screen.queryByTestId("confirm-cancel")).not.toBeInTheDocument();

		resolveRequest({ id: 1 });
		await waitFor(() => expect(props.onClose).toHaveBeenCalledTimes(1));
	});

	it("blocks submit and close requests while an image upload is pending", async () => {
		let resolveUpload;
		uploadImage.mockImplementation(
			() =>
				new Promise((resolve) => {
					resolveUpload = resolve;
				}),
		);
		const user = userEvent.setup();
		const { props } = renderModal();
		const modal = screen.getByTestId("admin-modal");
		const file = new File(["image"], "photo.png", { type: "image/png" });

		await user.upload(screen.getByLabelText(/URLs de imágenes/i), file);
		expect(screen.getByTestId("admin-save-btn")).toBeDisabled();
		await user.keyboard("{Escape}");
		fireEvent.pointerDown(modal, { clientX: 0, clientY: 0 });
		fireEvent.pointerUp(modal, { clientX: 0, clientY: 0 });

		expect(props.onClose).not.toHaveBeenCalled();
		expect(screen.queryByTestId("confirm-cancel")).not.toBeInTheDocument();

		resolveUpload({
			ok: true,
			json: async () => ({ url: "https://example.com/photo.png" }),
		});
		await waitFor(() =>
			expect(screen.getByTestId("admin-save-btn")).not.toBeDisabled(),
		);
	});
});

describe("LodgingFormModal - price and capacity", () => {
	it("creates a lodging with numeric ARS price and guest capacity", async () => {
		const user = userEvent.setup();
		createLodging.mockResolvedValue({ id: 1 });

		renderModal({ lodging: defaultLodgingWithImage });
		fillRequiredFields();
		await user.click(screen.getByTestId("admin-save-btn"));

		expect(createLodging).toHaveBeenCalledWith(
			expect.objectContaining({
				pricePerNight: 12500.5,
				maxGuests: 4,
			}),
		);
	});

	it("loads and submits the existing values when editing", async () => {
		const user = userEvent.setup();
		updateLodging.mockResolvedValue({ id: 7 });
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
			imageUrls: ["https://example.com/foto-existente.jpg"],
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

		expect(updateLodging).toHaveBeenCalledWith(
			7,
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
		expect(createLodging).not.toHaveBeenCalled();
	});

	it.each([
		["0", "El precio por noche debe ser mayor a cero"],
		["-1", "El precio por noche debe ser mayor a cero"],
	])("rejects price %s", async (value, message) => {
		const user = userEvent.setup();
		renderModal({ lodging: defaultLodgingWithImage });
		fillRequiredFields();
		await user.clear(screen.getByTestId("field-pricePerNight"));
		await user.type(screen.getByTestId("field-pricePerNight"), value);

		await user.click(screen.getByTestId("admin-save-btn"));

		expect(screen.getByTestId("error-pricePerNight")).toHaveTextContent(
			message,
		);
		expect(createLodging).not.toHaveBeenCalled();
	});

	it.each([
		["0", "La capacidad máxima debe ser mayor a cero"],
		["-1", "La capacidad máxima debe ser mayor a cero"],
		["1.5", "La capacidad máxima debe ser un número entero"],
	])("rejects guest capacity %s", async (value, message) => {
		const user = userEvent.setup();
		renderModal({ lodging: defaultLodgingWithImage });
		fillRequiredFields();
		await user.clear(screen.getByTestId("field-maxGuests"));
		await user.type(screen.getByTestId("field-maxGuests"), value);

		await user.click(screen.getByTestId("admin-save-btn"));

		expect(screen.getByTestId("error-maxGuests")).toHaveTextContent(message);
		expect(createLodging).not.toHaveBeenCalled();
	});
});

describe("LodgingFormModal - image requirements and error display", () => {
	it("shows required error when no image is added and rejects submission", async () => {
		const user = userEvent.setup();
		renderModal({ lodging: { id: null, imageUrls: [] } });

		fillRequiredFields();

		await user.click(screen.getByTestId("admin-save-btn"));

		expect(
			screen.getByText("Debés agregar al menos una imagen"),
		).toBeInTheDocument();
		expect(createLodging).not.toHaveBeenCalled();
	});

	it("submits the uploaded image URLs in the payload", async () => {
		const user = userEvent.setup();
		createLodging.mockResolvedValue({ id: 99 });
		uploadImage.mockResolvedValueOnce({ url: "https://example.com/foto1.jpg" });
		renderModal({ lodging: { id: null, imageUrls: [] } });

		fillRequiredFields();

		const file = new File(["fake-image"], "photo.png", { type: "image/png" });
		await user.upload(screen.getByLabelText(/URLs de imágenes/i), file);

		await user.click(screen.getByTestId("admin-save-btn"));

		expect(createLodging).toHaveBeenCalledWith(
			expect.objectContaining({
				imageUrls: ["https://example.com/foto1.jpg"],
			}),
		);
	});

	it("displays server error message when createLodging fails with duplicate name", async () => {
		const user = userEvent.setup();
		createLodging.mockRejectedValue(
			new Error("Ya existe un alojamiento con el nombre: Hotel Test"),
		);
		renderModal({ lodging: defaultLodgingWithImage });

		fillRequiredFields();

		await user.click(screen.getByTestId("admin-save-btn"));

		expect(
			await screen.findByText(
				"Ya existe un alojamiento con el nombre: Hotel Test",
			),
		).toBeInTheDocument();
	});
});

describe("LodgingFormModal - form semantics, autofill, and direct ref focus", () => {
	it("exposes explicit name, htmlFor/id, autofill hints, and aria-invalid=false before validation", () => {
		renderModal();

		const nameInput = screen.getByLabelText("Nombre del alojamiento");
		const emailInput = screen.getByLabelText("Correo electrónico");
		const descriptionInput = screen.getByLabelText("Descripción");
		const addressInput = screen.getByLabelText("Dirección");
		const cityInput = screen.getByLabelText("Ciudad");
		const countryInput = screen.getByLabelText("País");
		const phoneInput = screen.getByLabelText("Teléfono");
		const categorySelect = screen.getByLabelText("Categoría");

		expect(nameInput).toHaveAttribute("name", "name");
		expect(nameInput).toHaveAttribute("enterkeyhint", "next");
		expect(nameInput).toHaveAttribute("aria-invalid", "false");
		expect(nameInput.id).toBeTruthy();

		expect(emailInput).toHaveAttribute("name", "email");
		expect(emailInput).toHaveAttribute("autocomplete", "email");
		expect(emailInput).toHaveAttribute("spellcheck", "false");
		expect(emailInput).toHaveAttribute("enterkeyhint", "next");
		expect(emailInput).toHaveAttribute("aria-invalid", "false");

		expect(descriptionInput).toHaveAttribute("name", "description");
		expect(descriptionInput).toHaveAttribute("aria-invalid", "false");

		expect(addressInput).toHaveAttribute("name", "address");
		expect(addressInput).toHaveAttribute("autocomplete", "street-address");
		expect(addressInput).toHaveAttribute("enterkeyhint", "next");

		expect(cityInput).toHaveAttribute("name", "city");
		expect(cityInput).toHaveAttribute("autocomplete", "address-level2");
		expect(cityInput).toHaveAttribute("enterkeyhint", "next");

		expect(countryInput).toHaveAttribute("name", "country");
		expect(countryInput).toHaveAttribute("autocomplete", "country-name");
		expect(countryInput).toHaveAttribute("enterkeyhint", "next");

		expect(phoneInput).toHaveAttribute("name", "phoneNumber");
		expect(phoneInput).toHaveAttribute("autocomplete", "tel");
		expect(phoneInput).toHaveAttribute("enterkeyhint", "next");

		expect(categorySelect).toHaveAttribute("name", "categoryId");
	});

	it("associates validation errors with role=alert and aria-describedby including imageUrls", async () => {
		const user = userEvent.setup();
		renderModal();

		await user.click(screen.getByTestId("admin-save-btn"));

		const nameError = screen.getByTestId("error-name");
		expect(nameError).toHaveAttribute("role", "alert");
		expect(screen.getByTestId("field-name")).toHaveAttribute(
			"aria-describedby",
			nameError.id,
		);

		const imageError = screen.getByTestId("error-imageUrls");
		expect(imageError).toHaveAttribute("role", "alert");
		expect(imageError.id).toBeTruthy();
		const imageGroup = screen.getByRole("group", { name: "Imágenes" });
		expect(imageGroup).toHaveAttribute("aria-invalid", "true");
		expect(imageGroup).toHaveAttribute("aria-describedby", imageError.id);
	});

	it("focuses the first invalid field synchronously without scheduling setTimeout", async () => {
		const user = userEvent.setup();
		renderModal();

		const timeoutSpy = vi.spyOn(globalThis, "setTimeout");
		fireEvent.click(screen.getByTestId("admin-save-btn"));

		expect(timeoutSpy).not.toHaveBeenCalledWith(expect.any(Function), 100);
		expect(screen.getByTestId("field-name")).toHaveFocus();

		await user.type(screen.getByTestId("field-name"), "Cabaña Test");
		fireEvent.click(screen.getByTestId("admin-save-btn"));
		expect(screen.getByTestId("field-email")).toHaveFocus();

		timeoutSpy.mockRestore();
	});
});
