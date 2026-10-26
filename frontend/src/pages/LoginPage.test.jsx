import { Routes, Route, useLocation } from "react-router-dom";
import {
	customRender,
	screen,
	userEvent,
	makeAuthValue,
	mockUser,
} from "../test/test-utils";
import { AuthProvider } from "../context/AuthContext";
import RequireAuth from "../components/RequireAuth";
import { getCurrentUser, login as loginRequest } from "../services/authService";
import LoginPage from "./LoginPage";

vi.mock("../services/authService", () => ({
	getCurrentUser: vi.fn(),
	login: vi.fn(),
	register: vi.fn(),
	logout: vi.fn(),
}));

function BookingSentinel() {
	const location = useLocation();
	return <div data-testid="booking-sentinel">{location.pathname}</div>;
}

function FavoritesSentinel() {
	return <div data-testid="favorites-sentinel">favorites page</div>;
}

function HomeSentinel() {
	return <div data-testid="home-sentinel">home page</div>;
}

function renderLoginPage({ authValue, initialEntries } = {}) {
	return customRender(
		<Routes>
			<Route path="/login" element={<LoginPage />} />
			<Route path="/favorites" element={<FavoritesSentinel />} />
			<Route
				path="/register"
				element={<div data-testid="register-sentinel">registration page</div>}
			/>
			<Route path="/" element={<HomeSentinel />} />
		</Routes>,
		{ authValue, initialEntries },
	);
}

function getEmailInput() {
	return screen.getByLabelText("Email");
}

function getPasswordInput() {
	return screen.getByLabelText("Contraseña");
}

async function fillAndSubmit(user, { email, password } = {}) {
	if (email !== undefined) {
		await user.type(getEmailInput(), email);
	}
	if (password !== undefined) {
		await user.type(getPasswordInput(), password);
	}
	await user.click(screen.getByRole("button", { name: /iniciar sesión/i }));
}

describe("LoginPage - render", () => {
	it("renders the email and password fields and the submit button", () => {
		renderLoginPage({ initialEntries: ["/login"] });

		expect(getEmailInput()).toBeInTheDocument();
		expect(getPasswordInput()).toBeInTheDocument();
		expect(
			screen.getByRole("button", { name: "Iniciar sesión" }),
		).toBeInTheDocument();
	});
});

describe("LoginPage - booking authentication journey", () => {
	it("redirects an anonymous booking visit to login and returns after simulated authentication", async () => {
		getCurrentUser.mockResolvedValue(null);
		loginRequest.mockResolvedValue(mockUser);
		const user = userEvent.setup();
		customRender(
			<AuthProvider>
				<Routes>
					<Route path="/login" element={<LoginPage />} />
					<Route element={<RequireAuth />}>
						<Route path="/booking/:lodgingId" element={<BookingSentinel />} />
					</Route>
				</Routes>
			</AuthProvider>,
			{ route: "/booking/1", authValue: null },
		);

		expect(
			await screen.findByText(
				"Necesitás iniciar sesión para continuar. Si no tenés cuenta, podés registrarte.",
			),
		).toBeInTheDocument();
		expect(screen.queryByTestId("booking-sentinel")).not.toBeInTheDocument();
		expect(loginRequest).not.toHaveBeenCalled();
		expect(screen.getByRole("link", { name: "Crear cuenta" })).toHaveAttribute(
			"href",
			"/register",
		);

		await fillAndSubmit(user, {
			email: "test@example.com",
			password: "test-password",
		});

		expect(loginRequest).toHaveBeenCalledExactlyOnceWith(
			"test@example.com",
			"test-password",
		);
		expect(await screen.findByTestId("booking-sentinel")).toHaveTextContent(
			"/booking/1",
		);
		expect(
			screen.queryByRole("heading", { name: "Iniciar sesión" }),
		).not.toBeInTheDocument();
	});

	it("opens registration from the create-account link", async () => {
		const user = userEvent.setup();
		const authValue = makeAuthValue({ user: null, token: null });
		renderLoginPage({ authValue, initialEntries: ["/login"] });

		const link = screen.getByRole("link", { name: "Crear cuenta" });
		expect(link).toHaveAttribute("href", "/register");
		await user.click(link);

		expect(await screen.findByTestId("register-sentinel")).toBeInTheDocument();
		expect(authValue.login).not.toHaveBeenCalled();
		expect(authValue.register).not.toHaveBeenCalled();
	});
});

describe("LoginPage - empty submit", () => {
	it("shows field errors and does not call login", async () => {
		const user = userEvent.setup();
		const authValue = makeAuthValue();
		renderLoginPage({ authValue, initialEntries: ["/login"] });

		await fillAndSubmit(user, {});

		expect(screen.getByText("El email es obligatorio")).toBeInTheDocument();
		expect(
			screen.getByText("La contraseña es obligatoria"),
		).toBeInTheDocument();
		expect(authValue.login).not.toHaveBeenCalled();
	});
});

describe("LoginPage - invalid email format", () => {
	it("shows an email format error", async () => {
		const user = userEvent.setup();
		const authValue = makeAuthValue();
		renderLoginPage({ authValue, initialEntries: ["/login"] });

		await fillAndSubmit(user, { email: "notanemail", password: "secret" });

		expect(screen.getByText("El email no es válido")).toBeInTheDocument();
		expect(authValue.login).not.toHaveBeenCalled();
	});
});

describe("LoginPage - successful login", () => {
	it("calls login and navigates to location.state.from with replace", async () => {
		const user = userEvent.setup();
		const authValue = makeAuthValue({
			login: vi.fn().mockResolvedValue(undefined),
		});
		renderLoginPage({
			authValue,
			initialEntries: [
				{ pathname: "/login", state: { from: { pathname: "/favorites" } } },
			],
		});

		await fillAndSubmit(user, {
			email: "test@example.com",
			password: "secret",
		});

		expect(authValue.login).toHaveBeenCalledWith("test@example.com", "secret");
		expect(await screen.findByTestId("favorites-sentinel")).toBeInTheDocument();
	});

	it("navigates to / when no from state is present", async () => {
		const user = userEvent.setup();
		const authValue = makeAuthValue({
			login: vi.fn().mockResolvedValue(undefined),
		});
		renderLoginPage({ authValue, initialEntries: ["/login"] });

		await fillAndSubmit(user, {
			email: "test@example.com",
			password: "secret",
		});

		expect(await screen.findByTestId("home-sentinel")).toBeInTheDocument();
	});
});

describe("LoginPage - failed login", () => {
	it("renders the server error message and stops loading", async () => {
		const user = userEvent.setup();
		const authValue = makeAuthValue({
			login: vi.fn().mockRejectedValue(new Error("Credenciales inválidas")),
		});
		renderLoginPage({ authValue, initialEntries: ["/login"] });

		await fillAndSubmit(user, { email: "test@example.com", password: "wrong" });

		expect(
			await screen.findByText("Credenciales inválidas"),
		).toBeInTheDocument();
		expect(
			screen.getByRole("button", { name: "Iniciar sesión" }),
		).not.toBeDisabled();
	});
});

describe("LoginPage - redirect message from previous route", () => {
	it("renders the location.state.message above the form", () => {
		renderLoginPage({
			initialEntries: [
				{
					pathname: "/login",
					state: {
						message:
							"Necesitás iniciar sesión para continuar. Si no tenés cuenta, podés registrarte.",
					},
				},
			],
		});

		expect(
			screen.getByText(
				"Necesitás iniciar sesión para continuar. Si no tenés cuenta, podés registrarte.",
			),
		).toBeInTheDocument();
	});
});
