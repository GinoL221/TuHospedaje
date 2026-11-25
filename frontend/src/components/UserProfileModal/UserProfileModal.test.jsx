import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import UserProfileModal from "./UserProfileModal";
import * as authService from "../../services/authService";

describe("UserProfileModal", () => {
	const user = {
		firstName: "Carlos",
		lastName: "Gómez",
		email: "carlos@example.test",
		role: "USER",
	};

	beforeEach(() => {
		vi.restoreAllMocks();
	});

	it("renders native modal dialog semantics with user information and without mutating body overflow", () => {
		document.body.style.overflow = "scroll";
		const { unmount } = render(
			<UserProfileModal user={user} onClose={() => {}} />,
		);

		const dialog = screen.getByRole("dialog", { name: "Carlos Gómez" });
		expect(dialog.tagName).toBe("DIALOG");
		expect(dialog).toHaveAttribute("open");
		expect(dialog).toHaveAttribute("closedby", "any");
		expect(dialog).not.toHaveAttribute("role");
		expect(dialog).not.toHaveAttribute("aria-modal");
		expect(document.body.style.overflow).toBe("scroll");
		expect(screen.getByText("Carlos Gómez")).toBeInTheDocument();
		expect(screen.getByText("carlos@example.test")).toBeInTheDocument();
		expect(
			screen.getByRole("button", { name: /Reenviar correo de bienvenida/i }),
		).toBeInTheDocument();

		unmount();
		expect(document.body.style.overflow).toBe("scroll");
		document.body.style.overflow = "";
	});

	it("triggers resend and shows confirmation message", async () => {
		vi.spyOn(authService, "resendWelcomeEmail").mockResolvedValue(null);
		const userEv = userEvent.setup();
		render(<UserProfileModal user={user} onClose={() => {}} />);

		const resendBtn = screen.getByRole("button", {
			name: /Reenviar correo de bienvenida/i,
		});
		await userEv.click(resendBtn);

		await waitFor(() => {
			expect(
				screen.getByText(/¡Listo! Te reenviamos el correo de bienvenida/i),
			).toBeInTheDocument();
		});
		expect(resendBtn).toBeDisabled();
	});

	it("calls onClose when close button is clicked", async () => {
		const onClose = vi.fn();
		const userEv = userEvent.setup();
		render(<UserProfileModal user={user} onClose={onClose} />);

		const closeBtn = screen.getByRole("button", {
			name: /Cerrar ventana de perfil/i,
		});
		await userEv.click(closeBtn);
		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it("closes on Escape key press", async () => {
		const onClose = vi.fn();
		const userEv = userEvent.setup();
		render(<UserProfileModal user={user} onClose={onClose} />);

		await userEv.keyboard("{Escape}");
		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it("navigates focus across modal controls with Tab and Shift+Tab", async () => {
		const userEv = userEvent.setup();
		render(<UserProfileModal user={user} onClose={() => {}} />);

		const closeBtn = screen.getByRole("button", {
			name: /Cerrar ventana de perfil/i,
		});
		const resendBtn = screen.getByRole("button", {
			name: /Reenviar correo de bienvenida/i,
		});

		expect(closeBtn).toHaveFocus();

		await userEv.tab();
		expect(resendBtn).toHaveFocus();

		await userEv.tab({ shift: true });
		expect(closeBtn).toHaveFocus();
	});

	it("closes on backdrop click but not on an interior click", () => {
		const onClose = vi.fn();
		render(<UserProfileModal user={user} onClose={onClose} />);
		const dialog = screen.getByRole("dialog", { name: "Carlos Gómez" });
		vi.spyOn(dialog, "getBoundingClientRect").mockReturnValue({
			left: 100,
			right: 540,
			top: 100,
			bottom: 500,
			width: 440,
			height: 400,
		});

		const heading = screen.getByRole("heading", { name: "Carlos Gómez" });
		fireEvent.pointerDown(heading, { clientX: 200, clientY: 200 });
		fireEvent.pointerUp(heading, { clientX: 200, clientY: 200 });
		expect(onClose).not.toHaveBeenCalled();

		fireEvent.pointerDown(dialog, { clientX: 20, clientY: 20 });
		fireEvent.pointerUp(dialog, { clientX: 20, clientY: 20 });
		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it("restores focus to the opener when unmounted", async () => {
		const userEv = userEvent.setup();
		const { rerender } = render(<button type="button">Abrir perfil</button>);
		const trigger = screen.getByRole("button", { name: "Abrir perfil" });
		await userEv.click(trigger);

		rerender(
			<>
				<button type="button">Abrir perfil</button>
				<UserProfileModal user={user} onClose={vi.fn()} />
			</>,
		);
		expect(
			screen.getByRole("button", { name: /Cerrar ventana de perfil/i }),
		).toHaveFocus();

		rerender(<button type="button">Abrir perfil</button>);
		expect(trigger).toHaveFocus();
	});
});
