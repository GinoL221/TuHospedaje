import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import WelcomeEmailBanner from "./WelcomeEmailBanner";
import * as authService from "../../services/authService";

describe("WelcomeEmailBanner", () => {
	const user = { firstName: "Ana", email: "ana@example.test" };

	beforeEach(() => {
		vi.restoreAllMocks();
	});

	it("renders welcome text and resend action", () => {
		render(<WelcomeEmailBanner user={user} />);
		expect(
			screen.getByText(/¡Te damos la bienvenida a TuHospedaje, Ana!/i),
		).toBeInTheDocument();
		expect(screen.getByText(/ana@example.test/i)).toBeInTheDocument();
		expect(
			screen.getByRole("button", { name: /Reenviar correo de bienvenida/i }),
		).toBeInTheDocument();
	});

	it("shows success feedback when resend is scheduled", async () => {
		vi.spyOn(authService, "resendWelcomeEmail").mockResolvedValue(null);
		const userEv = userEvent.setup();
		render(<WelcomeEmailBanner user={user} />);

		const button = screen.getByRole("button", {
			name: /Reenviar correo de bienvenida/i,
		});
		await userEv.click(button);

		await waitFor(() => {
			expect(
				screen.getByText(/¡Listo! Te reenviamos el correo de bienvenida/i),
			).toBeInTheDocument();
		});
		expect(button).toBeDisabled();
	});

	it("shows pending feedback when resend is already pending", async () => {
		vi.spyOn(authService, "resendWelcomeEmail").mockResolvedValue({
			status: "pending",
		});
		const userEv = userEvent.setup();
		render(<WelcomeEmailBanner user={user} />);

		await userEv.click(
			screen.getByRole("button", { name: /Reenviar correo de bienvenida/i }),
		);

		await waitFor(() => {
			expect(
				screen.getByText(/Tu correo de bienvenida ya está en camino/i),
			).toBeInTheDocument();
		});
	});

	it("shows cooldown feedback when response is 429", async () => {
		const error = new Error("Rate limit exceeded");
		error.status = 429;
		vi.spyOn(authService, "resendWelcomeEmail").mockRejectedValue(error);
		const userEv = userEvent.setup();
		render(<WelcomeEmailBanner user={user} />);

		await userEv.click(
			screen.getByRole("button", { name: /Reenviar correo de bienvenida/i }),
		);

		await waitFor(() => {
			expect(
				screen.getByText(/Ya solicitaste un reenvío recientemente/i),
			).toBeInTheDocument();
		});
	});
});
