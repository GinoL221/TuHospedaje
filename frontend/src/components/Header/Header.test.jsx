import {
	customRender,
	makeAuthValue,
	mockAdmin,
	screen,
	userEvent,
} from "../../test/test-utils";
import Header from "./Header";

describe("Header - authenticated user", () => {
	it("uses the official isologotype as the accessible home link", () => {
		customRender(<Header />);

		const homeLink = screen.getByRole("link", { name: "TuHospedaje — Inicio" });
		const logo = screen.getByRole("img", { name: "TuHospedaje — Inicio" });
		const header = screen.getByRole("banner");

		expect(header).toHaveClass("site-header");
		expect(homeLink).toHaveAttribute("href", "/");
		expect(homeLink).toContainElement(logo);
		expect(logo).toHaveAttribute(
			"src",
			expect.stringContaining("TuHospedaje_Isologotipo.png"),
		);
	});

	it("shows 'Mis reservas' link pointing to /my-reservations", () => {
		customRender(<Header />);

		expect(screen.getByRole("link", { name: "Mis reservas" })).toHaveAttribute(
			"href",
			"/my-reservations",
		);
	});

	it("renders an admin name as navigation pointing to the canonical /administración route", () => {
		customRender(<Header />, {
			authValue: makeAuthValue({ user: mockAdmin }),
		});

		const adminLink = screen.getByRole("link", { name: "Test" });

		expect(adminLink).toHaveAttribute("href", "/administración");
		expect(adminLink).toHaveClass("nav-link", "nav-username");
	});

	it("renders a non-admin name as non-interactive text", () => {
		customRender(<Header />);

		const username = screen.getByText("Test");

		expect(username).toHaveClass("nav-username");
		expect(username).not.toHaveClass("nav-link");
		expect(screen.queryByRole("link", { name: "Test" })).not.toBeInTheDocument();
	});

	it("exposes the authenticated header styling hooks without loading an auth page", () => {
		customRender(<Header />);

		expect(screen.getByRole("img", { name: "Test" })).toHaveClass("avatar");
		expect(screen.getByText("Test")).toHaveClass("nav-username");
		expect(screen.getByRole("button", { name: "Cerrar sesión" })).toHaveClass(
			"btn-logout",
		);
	});

	it("shows the logout button and hides login/register links", () => {
		customRender(<Header />);

		expect(
			screen.getByRole("button", { name: "Cerrar sesión" }),
		).toBeInTheDocument();
		expect(
			screen.queryByRole("link", { name: "Iniciar sesión" }),
		).not.toBeInTheDocument();
		expect(
			screen.queryByRole("link", { name: "Crear cuenta" }),
		).not.toBeInTheDocument();
	});
});

describe("Header - unauthenticated user", () => {
	it("opens and closes the mobile menu from the hamburger button", async () => {
		const user = userEvent.setup();
		customRender(<Header />, { authValue: null });

		const menuButton = screen.getByRole("button", { name: "Abrir menú" });
		const menu = screen.getByRole("link", { name: "Iniciar sesión" }).parentElement;

		expect(menu).not.toHaveClass("nav-links--open");
		await user.click(menuButton);
		expect(screen.getByRole("button", { name: "Cerrar menú" })).toBeInTheDocument();
		expect(menu).toHaveClass("nav-links--open");

		await user.click(screen.getByRole("button", { name: "Cerrar menú" }));
		expect(screen.getByRole("button", { name: "Abrir menú" })).toBeInTheDocument();
		expect(menu).not.toHaveClass("nav-links--open");
	});

	it("closes the mobile menu after navigation", async () => {
		const user = userEvent.setup();
		customRender(<Header />, { authValue: null });

		await user.click(screen.getByRole("button", { name: "Abrir menú" }));
		await user.click(screen.getByRole("link", { name: "Iniciar sesión" }));

		expect(screen.getByRole("button", { name: "Abrir menú" })).toBeInTheDocument();
	});

	it("does not show 'Mis reservas' link", () => {
		customRender(<Header />, { authValue: null });

		expect(
			screen.queryByRole("link", { name: "Mis reservas" }),
		).not.toBeInTheDocument();
	});

	it("shows login and register links", () => {
		customRender(<Header />, { authValue: null });

		expect(
			screen.getByRole("link", { name: "Iniciar sesión" }),
		).toBeInTheDocument();
		const registerLink = screen.getByRole("link", { name: "Crear cuenta" });
		expect(registerLink).toHaveAttribute("href", "/register");
		expect(registerLink).toHaveClass("btn-secondary", "header-register-cta");
	});

	it("does not show the logout button", () => {
		customRender(<Header />, { authValue: null });

		expect(
			screen.queryByRole("button", { name: "Cerrar sesión" }),
		).not.toBeInTheDocument();
	});
});
