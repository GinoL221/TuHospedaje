import { render, screen, act, waitFor } from "@testing-library/react";
import { MemoryRouter, Routes, Route, useLocation } from "react-router-dom";
import { AuthProvider } from "./AuthContext";
import { useAuth } from "../hooks/useAuth";
import {
	getCurrentUser,
	login,
	register,
	logout,
} from "../services/authService";

vi.mock("../services/authService");

function AuthConsumer() {
	const { user, loading, login, register, logout } = useAuth();
	return (
		<div>
			<span data-testid="loading">{loading ? "loading" : "ready"}</span>
			<span data-testid="user">{user ? user.email : "no-user"}</span>
			<button onClick={() => login("test@example.com", "secret")}>login</button>
			<button
				onClick={() => register("Test", "User", "test@example.com", "secret")}
			>
				register
			</button>
			<button onClick={() => logout()}>logout</button>
		</div>
	);
}

function LoginSentinel() {
	const location = useLocation();
	return (
		<div data-testid="login-sentinel">
			login page
			<span data-testid="login-from">
				{location.state?.from?.pathname ?? "no-from"}
			</span>
		</div>
	);
}

function renderWithProvider({ initialEntries = ["/"] } = {}) {
	return render(
		<MemoryRouter initialEntries={initialEntries}>
			<AuthProvider>
				<Routes>
					<Route path="/login" element={<LoginSentinel />} />
					<Route path="*" element={<AuthConsumer />} />
				</Routes>
			</AuthProvider>
		</MemoryRouter>,
	);
}

const meUser = {
	firstName: "Test",
	lastName: "User",
	email: "test@example.com",
	role: "USER",
	imageUrl: null,
};

beforeEach(() => {
	vi.clearAllMocks();
	getCurrentUser.mockRejectedValue(new Error("Sesión expirada"));
});

describe("AuthContext - bootstrap on mount", () => {
	it("sets the user when the current-user service resolves", async () => {
		getCurrentUser.mockResolvedValue(meUser);

		renderWithProvider();

		expect(screen.getByTestId("loading")).toHaveTextContent("loading");

		await waitFor(() => {
			expect(screen.getByTestId("loading")).toHaveTextContent("ready");
		});

		expect(getCurrentUser).toHaveBeenCalledOnce();
		expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
	});

	it("leaves the user unauthenticated without throwing when /auth/me rejects (401)", async () => {
		renderWithProvider();

		await waitFor(() => {
			expect(screen.getByTestId("loading")).toHaveTextContent("ready");
		});

		expect(screen.getByTestId("user")).toHaveTextContent("no-user");
		expect(screen.queryByTestId("login-sentinel")).not.toBeInTheDocument();
	});
});

describe("AuthContext - login", () => {
	it("sets the user directly from the response body, with no token decoding", async () => {
		login.mockResolvedValue(meUser);

		renderWithProvider();

		await waitFor(() => {
			expect(screen.getByTestId("loading")).toHaveTextContent("ready");
		});

		await act(async () => {
			screen.getByText("login").click();
		});

		expect(login).toHaveBeenCalledWith("test@example.com", "secret");
		expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
	});
});

describe("AuthContext - register", () => {
	it("sets the user directly from the response body, with no token decoding", async () => {
		register.mockResolvedValue(meUser);

		renderWithProvider();

		await waitFor(() => {
			expect(screen.getByTestId("loading")).toHaveTextContent("ready");
		});

		await act(async () => {
			screen.getByText("register").click();
		});

		expect(register).toHaveBeenCalledWith(
			"Test",
			"User",
			"test@example.com",
			"secret",
		);
		expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
	});
});

describe("AuthContext - logout", () => {
	it("clears the in-memory user state without touching localStorage", async () => {
		getCurrentUser.mockResolvedValue(meUser);
		logout.mockResolvedValue(null);
		const removeItemSpy = vi.spyOn(Storage.prototype, "removeItem");

		renderWithProvider();

		await waitFor(() => {
			expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
		});

		await act(async () => {
			screen.getByText("logout").click();
		});

		expect(logout).toHaveBeenCalledOnce();
		expect(screen.getByTestId("user")).toHaveTextContent("no-user");
		expect(removeItemSpy).not.toHaveBeenCalled();

		removeItemSpy.mockRestore();
	});
});

describe("AuthContext - auth:unauthorized event", () => {
	it("logs out and navigates to /login when the event fires", async () => {
		getCurrentUser.mockResolvedValue(meUser);

		renderWithProvider();

		await waitFor(() => {
			expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
		});

		await act(async () => {
			window.dispatchEvent(new CustomEvent("auth:unauthorized"));
		});

		expect(screen.getByTestId("login-sentinel")).toBeInTheDocument();
	});

	it("preserves the originating route as state.from so login can redirect back", async () => {
		getCurrentUser.mockResolvedValue(meUser);

		renderWithProvider({ initialEntries: ["/booking/42"] });

		await waitFor(() => {
			expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
		});

		await act(async () => {
			window.dispatchEvent(new CustomEvent("auth:unauthorized"));
		});

		expect(screen.getByTestId("login-sentinel")).toBeInTheDocument();
		expect(screen.getByTestId("login-from")).toHaveTextContent("/booking/42");
	});
});
