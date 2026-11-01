import { render, screen, waitFor } from "@testing-library/react";
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

	it("renders user information and welcome email section", () => {
		render(<UserProfileModal user={user} onClose={() => {}} />);
		expect(screen.getByText("Carlos Gómez")).toBeInTheDocument();
		expect(screen.getByText("carlos@example.test")).toBeInTheDocument();
		expect(
			screen.getByRole("button", { name: /Reenviar correo de bienvenida/i }),
		).toBeInTheDocument();
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
});
