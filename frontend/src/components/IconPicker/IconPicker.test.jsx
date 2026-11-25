import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import IconPicker from "./IconPicker";
import ModalDialog from "../ModalDialog/ModalDialog";

describe("IconPicker", () => {
	it("portals into the enclosing <dialog> when opened inside ModalDialog and selects an icon", async () => {
		const user = userEvent.setup();
		const onChange = vi.fn();
		const onClose = vi.fn();

		render(
			<ModalDialog ariaLabel="Editar característica" onClose={onClose}>
				<IconPicker value="" onChange={onChange} />
			</ModalDialog>,
		);

		const dialog = screen.getByRole("dialog", {
			name: "Editar característica",
		});
		const trigger = screen.getByTestId("icon-picker-trigger");

		await user.click(trigger);

		const popover = document.querySelector(".icon-picker-popover");
		expect(popover).not.toBeNull();
		expect(dialog.contains(popover)).toBe(true);

		const firstOption = popover.querySelector(".icon-picker-item");
		expect(firstOption).not.toBeNull();
		await user.click(firstOption);

		expect(onChange).toHaveBeenCalledTimes(1);
		expect(document.querySelector(".icon-picker-popover")).toBeNull();
		expect(onClose).not.toHaveBeenCalled();
	});

	it("closes only IconPicker and restores trigger focus on Escape inside ModalDialog", async () => {
		const user = userEvent.setup();
		const onClose = vi.fn();

		render(
			<ModalDialog ariaLabel="Editar política" onClose={onClose}>
				<IconPicker value="" onChange={vi.fn()} />
			</ModalDialog>,
		);

		const trigger = screen.getByTestId("icon-picker-trigger");
		await user.click(trigger);
		expect(document.querySelector(".icon-picker-popover")).not.toBeNull();

		await user.keyboard("{Escape}");

		expect(document.querySelector(".icon-picker-popover")).toBeNull();
		expect(onClose).not.toHaveBeenCalled();
		expect(trigger).toHaveFocus();
	});

	it("portals into document.body when rendered outside any <dialog>", async () => {
		const user = userEvent.setup();

		render(<IconPicker value="" onChange={vi.fn()} />);

		await user.click(screen.getByTestId("icon-picker-trigger"));

		const popover = document.querySelector(".icon-picker-popover");
		expect(popover).not.toBeNull();
		expect(popover.parentElement).toBe(document.body);
	});
});
