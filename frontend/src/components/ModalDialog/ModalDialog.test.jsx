import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ModalDialog from "./ModalDialog";

describe("ModalDialog - native dialog semantics", () => {
	it("opens imperatively via showModal() without redundant role or aria-modal", () => {
		const showModalSpy = vi.spyOn(HTMLDialogElement.prototype, "showModal");

		render(
			<ModalDialog ariaLabel="Confirmar acción" onClose={vi.fn()}>
				<p>Contenido</p>
			</ModalDialog>,
		);

		const dialog = screen.getByRole("dialog", { name: "Confirmar acción" });
		expect(dialog.tagName).toBe("DIALOG");
		expect(dialog).toHaveAttribute("open");
		expect(dialog).not.toHaveAttribute("role");
		expect(dialog).not.toHaveAttribute("aria-modal");
		expect(dialog).toHaveAttribute("closedby", "any");
		expect(showModalSpy).toHaveBeenCalledTimes(1);
		showModalSpy.mockRestore();
	});

	it("supports ariaLabelledBy, ariaDescribedBy, custom className, and closedBy", () => {
		render(
			<ModalDialog
				ariaLabelledBy="modal-title"
				ariaDescribedBy="modal-desc"
				className="custom-modal"
				closedBy="closerequest"
				onClose={vi.fn()}
			>
				<h2 id="modal-title">Título</h2>
				<p id="modal-desc">Descripción</p>
			</ModalDialog>,
		);

		const dialog = screen.getByRole("dialog", { name: "Título" });
		expect(dialog).toHaveAttribute("aria-labelledby", "modal-title");
		expect(dialog).toHaveAttribute("aria-describedby", "modal-desc");
		expect(dialog).toHaveClass("modal-dialog", "custom-modal");
		expect(dialog).toHaveAttribute("closedby", "closerequest");
	});

	it("resets dialog.returnValue when native close fires", () => {
		render(
			<ModalDialog ariaLabel="Reset test" onClose={vi.fn()}>
				<p>Body</p>
			</ModalDialog>,
		);

		const dialog = screen.getByRole("dialog", { name: "Reset test" });
		dialog.close("submitted");
		expect(dialog.returnValue).toBe("");
	});
});

describe("ModalDialog - Escape and cancel handling", () => {
	it("invokes onClose when Escape is pressed", async () => {
		const onClose = vi.fn();
		const user = userEvent.setup();

		render(
			<ModalDialog ariaLabel="Escape test" onClose={onClose}>
				<button type="button">Acción</button>
			</ModalDialog>,
		);

		await user.keyboard("{Escape}");

		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it("keeps the dialog open when onCancel calls event.preventDefault()", async () => {
		const onClose = vi.fn();
		const onCancel = vi.fn((event) => event.preventDefault());
		const user = userEvent.setup();

		render(
			<ModalDialog
				ariaLabel="Guarded modal"
				onCancel={onCancel}
				onClose={onClose}
			>
				<button type="button">Acción</button>
			</ModalDialog>,
		);

		await user.keyboard("{Escape}");

		expect(onCancel).toHaveBeenCalledTimes(1);
		expect(onClose).not.toHaveBeenCalled();
		expect(
			screen.getByRole("dialog", { name: "Guarded modal" }),
		).toHaveAttribute("open");
	});
});

describe("ModalDialog - Safari light-dismiss fallback", () => {
	const rect = {
		left: 100,
		right: 500,
		top: 100,
		bottom: 400,
		width: 400,
		height: 300,
	};

	it("dismisses when both pointerdown and pointerup occur outside dialog bounds", () => {
		const onClose = vi.fn();
		render(
			<ModalDialog ariaLabel="Backdrop test" closedBy="any" onClose={onClose}>
				<p>Contenido</p>
			</ModalDialog>,
		);

		const dialog = screen.getByRole("dialog", { name: "Backdrop test" });
		vi.spyOn(dialog, "getBoundingClientRect").mockReturnValue(rect);

		fireEvent.pointerDown(dialog, { clientX: 50, clientY: 50 });
		fireEvent.pointerUp(dialog, { clientX: 50, clientY: 50 });

		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it("does not dismiss when a drag starts inside and ends outside dialog bounds", () => {
		const onClose = vi.fn();
		render(
			<ModalDialog ariaLabel="Drag test" closedBy="any" onClose={onClose}>
				<p>Texto seleccionable</p>
			</ModalDialog>,
		);

		const dialog = screen.getByRole("dialog", { name: "Drag test" });
		vi.spyOn(dialog, "getBoundingClientRect").mockReturnValue(rect);

		fireEvent.pointerDown(dialog, { clientX: 200, clientY: 200 });
		fireEvent.pointerUp(dialog, { clientX: 20, clientY: 20 });

		expect(onClose).not.toHaveBeenCalled();
	});

	it("does not light-dismiss when closedBy is not 'any'", () => {
		const onClose = vi.fn();
		render(
			<ModalDialog
				ariaLabel="No backdrop test"
				closedBy="closerequest"
				onClose={onClose}
			>
				<p>Contenido</p>
			</ModalDialog>,
		);

		const dialog = screen.getByRole("dialog", { name: "No backdrop test" });
		vi.spyOn(dialog, "getBoundingClientRect").mockReturnValue(rect);

		fireEvent.pointerDown(dialog, { clientX: 10, clientY: 10 });
		fireEvent.pointerUp(dialog, { clientX: 10, clientY: 10 });

		expect(onClose).not.toHaveBeenCalled();
	});
});
