import { render, screen, act, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { StrictMode, useState } from "react";
import { AuthProvider } from "./AuthContext";
import { useAuth } from "../hooks/useAuth";
import {
	getCurrentUser,
	login,
	register,
	logout,
} from "../services/authService";

vi.mock("../services/authService");

function Consumer() {
	const { user, loading, login, register, logout, logoutError } = useAuth();
	const [lastError, setLastError] = useState("");
	return (
		<div>
			<span data-testid="status">
				{loading ? "loading" : user ? user.email : "anonymous"}
			</span>
			<span data-testid="last-error">{lastError}</span>
			<span data-testid="logout-error">{logoutError}</span>
			<button
				onClick={() =>
					login("test@example.com", "secret").catch((e) =>
						setLastError(e.message),
					)
				}
			>
				login
			</button>
			<button
				onClick={() =>
					register("Test", "User", "test@example.com", "secret").catch((e) =>
						setLastError(e.message),
					)
				}
			>
				register
			</button>
			<button onClick={() => logout().catch(() => {})}>logout</button>
		</div>
	);
}

const user = { email: "test@example.com", firstName: "Test", role: "USER" };

function renderProvider({ strictMode = false } = {}) {
	const provider = (
		<MemoryRouter initialEntries={["/"]}>
			<AuthProvider>
				<Routes>
					<Route path="*" element={<Consumer />} />
				</Routes>
			</AuthProvider>
		</MemoryRouter>
	);
	return render(strictMode ? <StrictMode>{provider}</StrictMode> : provider);
}

beforeEach(() => {
	vi.clearAllMocks();
	getCurrentUser.mockRejectedValue(new Error("No session"));
});

describe("authenticated service sequencing", () => {
	it("does not publish login before the service resolves", async () => {
		let resolveLogin;
		login.mockImplementation(
			() =>
				new Promise((resolve) => {
					resolveLogin = resolve;
				}),
		);
		renderProvider();
		await waitFor(() =>
			expect(screen.getByTestId("status")).toHaveTextContent("anonymous"),
		);

		await act(async () => screen.getByText("login").click());
		expect(screen.getByTestId("status")).not.toHaveTextContent(user.email);
		resolveLogin(user);
		await waitFor(() =>
			expect(screen.getByTestId("status")).toHaveTextContent(user.email),
		);
	});

	it("keeps the user anonymous when register fails and preserves user on logout failure", async () => {
		register.mockRejectedValue(new Error("CSRF unavailable"));
		renderProvider();
		await waitFor(() =>
			expect(screen.getByTestId("status")).toHaveTextContent("anonymous"),
		);
		await act(async () => screen.getByText("register").click());
		expect(screen.getByTestId("status")).not.toHaveTextContent(
			"stale@example.com",
		);

		login.mockResolvedValue(user);
		await act(async () => screen.getByText("login").click());
		await waitFor(() =>
			expect(screen.getByTestId("status")).toHaveTextContent(user.email),
		);
		logout.mockRejectedValue(new Error("Logout rejected"));
		await act(async () => screen.getByText("logout").click());
		expect(screen.getByTestId("status")).toHaveTextContent(user.email);
		expect(screen.getByTestId("logout-error")).toHaveTextContent(
			"Logout rejected",
		);
	});

	it("surfaces the retry-via-login message from the register service", async () => {
		register.mockRejectedValue(
			new Error(
				"Tu cuenta se creó correctamente, pero no pudimos iniciar sesión automáticamente. Iniciá sesión con tus credenciales.",
			),
		);
		renderProvider();
		await waitFor(() =>
			expect(screen.getByTestId("status")).toHaveTextContent("anonymous"),
		);

		await act(async () => screen.getByText("register").click());
		expect(screen.getByTestId("last-error")).not.toHaveTextContent(
			"email ya está registrado",
		);
		expect(screen.getByTestId("last-error")).toHaveTextContent(
			/iniciar sesión/i,
		);
	});

	it("does not publish a late current-user response before the service resolves", async () => {
		let resolveCurrentUser;
		getCurrentUser.mockImplementation(
			() =>
				new Promise((resolve) => {
					resolveCurrentUser = resolve;
				}),
		);
		renderProvider();

		expect(screen.getByTestId("status")).not.toHaveTextContent(user.email);
		resolveCurrentUser(user);
		await waitFor(() =>
			expect(screen.getByTestId("status")).toHaveTextContent(user.email),
		);
	});

	it("ignores the first StrictMode auth response when the duplicate effect is newer", async () => {
		const responses = [];
		getCurrentUser.mockImplementation(
			() => new Promise((resolve) => responses.push(resolve)),
		);
		renderProvider({ strictMode: true });

		await waitFor(() => expect(responses).toHaveLength(2));
		responses[0]({ ...user, email: "stale@example.com" });
		await act(async () => {});
		expect(screen.getByTestId("status")).not.toHaveTextContent(
			"stale@example.com",
		);

		responses[1](user);
		await waitFor(() =>
			expect(screen.getByTestId("status")).toHaveTextContent(user.email),
		);
	});
});
