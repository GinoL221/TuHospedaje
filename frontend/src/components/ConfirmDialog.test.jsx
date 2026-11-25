import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ConfirmDialog from "./ConfirmDialog";

describe("ConfirmDialog - visibility", () => {
	it("renders nothing when show is false", () => {
		render(
			<ConfirmDialog
				show={false}
				message="¿Confirmar?"
				onConfirm={vi.fn()}
				onCancel={vi.fn()}
			/>,
		);

		expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
	});

	it("exposes a named modal dialog and initially focuses cancel", () => {
		render(
			<ConfirmDialog
				show={true}
				message="¿Eliminar alojamiento?"
				onConfirm={vi.fn()}
				onCancel={vi.fn()}
			/>,
		);

		const dialog = screen.getByRole("dialog", {
			name: "¿Eliminar alojamiento?",
		});
		expect(dialog.tagName).toBe("DIALOG");
		expect(dialog).toHaveAttribute("open");
		expect(dialog).not.toHaveAttribute("role");
		expect(dialog).not.toHaveAttribute("aria-modal");
		expect(screen.getByTestId("confirm-delete-no")).toHaveFocus();
	});
});

describe("ConfirmDialog - keyboard and focus", () => {
	it("moves keyboard focus between dialog controls and delegates trapping to native showModal()", async () => {
		const showModalSpy = vi.spyOn(HTMLDialogElement.prototype, "showModal");
		const user = userEvent.setup();
		render(
			<ConfirmDialog
				show={true}
				message="¿Confirmar?"
				onConfirm={vi.fn()}
				onCancel={vi.fn()}
			/>,
		);

		const confirmButton = screen.getByTestId("confirm-delete-yes");
		const cancelButton = screen.getByTestId("confirm-delete-no");

		expect(showModalSpy).toHaveBeenCalledTimes(1);
		expect(cancelButton).toHaveFocus();
		await user.tab({ shift: true });
		expect(confirmButton).toHaveFocus();
		await user.tab();
		expect(cancelButton).toHaveFocus();
		showModalSpy.mockRestore();
	});

	it("cancels on Escape", async () => {
		const onCancel = vi.fn();
		const user = userEvent.setup();
		render(
			<ConfirmDialog
				show={true}
				message="¿Confirmar?"
				onConfirm={vi.fn()}
				onCancel={onCancel}
			/>,
		);

		await user.keyboard("{Escape}");

		expect(onCancel).toHaveBeenCalledTimes(1);
	});

	it("restores focus to the trigger when it closes", async () => {
		const user = userEvent.setup();
		const { rerender } = render(
			<>
				<button>Eliminar</button>
				<ConfirmDialog
					show={false}
					message="¿Confirmar?"
					onConfirm={vi.fn()}
					onCancel={vi.fn()}
				/>
			</>,
		);
		const trigger = screen.getByRole("button", { name: "Eliminar" });
		await user.click(trigger);

		rerender(
			<>
				<button>Eliminar</button>
				<ConfirmDialog
					show={true}
					message="¿Confirmar?"
					onConfirm={vi.fn()}
					onCancel={vi.fn()}
				/>
			</>,
		);
		expect(screen.getByTestId("confirm-delete-no")).toHaveFocus();

		rerender(
			<>
				<button>Eliminar</button>
				<ConfirmDialog
					show={false}
					message="¿Confirmar?"
					onConfirm={vi.fn()}
					onCancel={vi.fn()}
				/>
			</>,
		);
		expect(trigger).toHaveFocus();
	});
});

describe("ConfirmDialog - overlay and propagation", () => {
	it("calls onCancel when the overlay (outside the modal) is clicked", async () => {
		const onCancel = vi.fn();
		const user = userEvent.setup();
		render(
			<ConfirmDialog
				show={true}
				message="¿Confirmar?"
				onConfirm={vi.fn()}
				onCancel={onCancel}
			/>,
		);

		await user.click(screen.getByTestId("confirm-delete"));

		expect(onCancel).toHaveBeenCalledTimes(1);
	});

	it("does not call onCancel when clicking inside the modal", async () => {
		const onCancel = vi.fn();
		const user = userEvent.setup();
		render(
			<ConfirmDialog
				show={true}
				message="¿Confirmar?"
				onConfirm={vi.fn()}
				onCancel={onCancel}
			/>,
		);

		await user.click(screen.getByText("¿Confirmar?"));

		expect(onCancel).not.toHaveBeenCalled();
	});
});

describe("ConfirmDialog - basic confirm/cancel actions", () => {
	it("calls onConfirm when the confirm button is clicked", async () => {
		const onConfirm = vi.fn().mockResolvedValue(undefined);
		const user = userEvent.setup();
		render(
			<ConfirmDialog
				show={true}
				message="¿Confirmar?"
				onConfirm={onConfirm}
				onCancel={vi.fn()}
			/>,
		);

		await user.click(screen.getByTestId("confirm-delete-yes"));

		expect(onConfirm).toHaveBeenCalledTimes(1);
	});

	it("calls onCancel when the cancel button is clicked", async () => {
		const onCancel = vi.fn();
		const user = userEvent.setup();
		render(
			<ConfirmDialog
				show={true}
				message="¿Confirmar?"
				onConfirm={vi.fn().mockResolvedValue(undefined)}
				onCancel={onCancel}
			/>,
		);

		await user.click(screen.getByTestId("confirm-delete-no"));

		expect(onCancel).toHaveBeenCalledTimes(1);
	});
});

describe("ConfirmDialog - double-click guard while onConfirm is in flight", () => {
	it("calls onConfirm only once when the confirm button is double-clicked before it resolves", async () => {
		let resolveConfirm;
		const onConfirm = vi.fn(
			() =>
				new Promise((resolve) => {
					resolveConfirm = resolve;
				}),
		);
		const user = userEvent.setup();
		render(
			<ConfirmDialog
				show={true}
				message="¿Confirmar?"
				onConfirm={onConfirm}
				onCancel={vi.fn()}
			/>,
		);

		const confirmButton = screen.getByTestId("confirm-delete-yes");
		await user.click(confirmButton);
		await user.click(confirmButton);

		expect(onConfirm).toHaveBeenCalledTimes(1);

		resolveConfirm();
		await waitFor(() => expect(confirmButton).not.toBeDisabled());
	});

	it("disables the confirm button while onConfirm is pending and re-enables it after it resolves", async () => {
		let resolveConfirm;
		const onConfirm = vi.fn(
			() =>
				new Promise((resolve) => {
					resolveConfirm = resolve;
				}),
		);
		const user = userEvent.setup();
		render(
			<ConfirmDialog
				show={true}
				message="¿Confirmar?"
				onConfirm={onConfirm}
				onCancel={vi.fn()}
			/>,
		);

		const confirmButton = screen.getByTestId("confirm-delete-yes");
		await user.click(confirmButton);

		expect(confirmButton).toBeDisabled();

		resolveConfirm();
		await waitFor(() => expect(confirmButton).not.toBeDisabled());
	});

	it("re-enables the confirm button after onConfirm rejects", async () => {
		let rejectConfirm;
		const onConfirm = vi.fn(
			() =>
				new Promise((_resolve, reject) => {
					rejectConfirm = reject;
				}),
		);
		const user = userEvent.setup();
		render(
			<ConfirmDialog
				show={true}
				message="¿Confirmar?"
				onConfirm={onConfirm}
				onCancel={vi.fn()}
			/>,
		);

		const confirmButton = screen.getByTestId("confirm-delete-yes");
		await user.click(confirmButton);

		expect(confirmButton).toBeDisabled();

		rejectConfirm(new Error("request failed"));
		await waitFor(() => expect(confirmButton).not.toBeDisabled());
	});

	it("ignores clicks on cancel while onConfirm is pending", async () => {
		let resolveConfirm;
		const onConfirm = vi.fn(
			() =>
				new Promise((resolve) => {
					resolveConfirm = resolve;
				}),
		);
		const onCancel = vi.fn();
		const user = userEvent.setup();
		render(
			<ConfirmDialog
				show={true}
				message="¿Confirmar?"
				onConfirm={onConfirm}
				onCancel={onCancel}
			/>,
		);

		await user.click(screen.getByTestId("confirm-delete-yes"));
		await user.click(screen.getByTestId("confirm-delete-no"));

		expect(onCancel).not.toHaveBeenCalled();

		resolveConfirm();
		await waitFor(() =>
			expect(screen.getByTestId("confirm-delete-no")).not.toBeDisabled(),
		);
	});

	it("ignores Escape and overlay cancellation while onConfirm is pending", async () => {
		let resolveConfirm;
		const onConfirm = vi.fn(
			() =>
				new Promise((resolve) => {
					resolveConfirm = resolve;
				}),
		);
		const onCancel = vi.fn();
		const user = userEvent.setup();
		render(
			<ConfirmDialog
				show={true}
				message="¿Confirmar?"
				onConfirm={onConfirm}
				onCancel={onCancel}
			/>,
		);

		await user.click(screen.getByTestId("confirm-delete-yes"));
		await user.keyboard("{Escape}");
		await user.click(screen.getByTestId("confirm-delete"));

		expect(onConfirm).toHaveBeenCalledTimes(1);
		expect(onCancel).not.toHaveBeenCalled();

		resolveConfirm();
		await waitFor(() =>
			expect(screen.getByTestId("confirm-delete-no")).not.toBeDisabled(),
		);
	});

	it("keeps the dialog open on Escape, backdrop click, and non-cancelable native close while onConfirm is pending", async () => {
		let resolveConfirm;
		const onConfirm = vi.fn(
			() =>
				new Promise((resolve) => {
					resolveConfirm = resolve;
				}),
		);
		const onCancel = vi.fn();
		const user = userEvent.setup();
		render(
			<ConfirmDialog
				show={true}
				message="¿Confirmar?"
				onConfirm={onConfirm}
				onCancel={onCancel}
			/>,
		);

		const dialog = screen.getByRole("dialog", { name: "¿Confirmar?" });
		await user.click(screen.getByTestId("confirm-delete-yes"));

		await user.keyboard("{Escape}");
		await user.click(dialog);

		expect(onCancel).not.toHaveBeenCalled();
		expect(dialog).toHaveAttribute("open");
		expect(dialog.open).toBe(true);

		dialog.dispatchEvent(new Event("cancel", { cancelable: false }));
		dialog.close();

		await waitFor(() => {
			expect(dialog.open).toBe(true);
			expect(dialog).toHaveAttribute("open");
		});
		expect(onCancel).not.toHaveBeenCalled();

		resolveConfirm();
		await waitFor(() =>
			expect(screen.getByTestId("confirm-delete-no")).not.toBeDisabled(),
		);
	});
});
