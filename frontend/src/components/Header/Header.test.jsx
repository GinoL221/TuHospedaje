import {
	customRender,
	makeAuthValue,
	mockAdmin,
	screen,
	userEvent,
} from "../../test/test-utils";
import Header from "./Header";

let observers = [];
let headerHeight = 72;

class ResizeObserverMock {
	constructor(callback) {
		this.callback = callback;
		observers.push(this);
	}

	observe() {}
	disconnect = vi.fn();
}

beforeEach(() => {
	observers = [];
	headerHeight = 72;
	vi.stubGlobal("ResizeObserver", ResizeObserverMock);
	vi.spyOn(HTMLElement.prototype, "getBoundingClientRect").mockImplementation(
		() => ({
			height: headerHeight,
		}),
	);
});

afterEach(() => {
	document.documentElement.style.removeProperty("--site-header-height");
	document.documentElement.style.removeProperty("--site-scroll-clearance");
	vi.restoreAllMocks();
});

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
		expect(logo).toHaveAttribute("width", "451");
		expect(logo).toHaveAttribute("height", "253");
	});

	it("navigates home through the tagline", () => {
		customRender(<Header />);

		expect(
			screen.getByRole("link", {
				name: "Encuentra tu lugar ideal al mejor precio",
			}),
		).toHaveAttribute("href", "/");
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

	it("links the admin avatar to /admin and closes the mobile menu", async () => {
		const user = userEvent.setup();
		customRender(<Header />, {
			authValue: makeAuthValue({ user: mockAdmin }),
		});

		await user.click(screen.getByRole("button", { name: "Abrir menú" }));
		const avatarLink = screen.getByRole("link", { name: "Administración" });
		expect(avatarLink).toHaveAttribute("href", "/admin");
		expect(avatarLink).toContainElement(
			screen.getByRole("img", { name: "Test" }),
		);
		await user.click(avatarLink);
		expect(screen.getByRole("button", { name: "Abrir menú" })).toHaveAttribute(
			"aria-expanded",
			"false",
		);
	});

	it("renders a non-admin name as non-interactive text", () => {
		customRender(<Header />);

		const username = screen.getByText("Test");

		expect(screen.getByRole("img", { name: "Test" }).closest("a")).toBeNull();
		expect(username).toHaveClass("nav-username");
		expect(username).not.toHaveClass("nav-link");
		expect(
			screen.queryByRole("link", { name: "Test" }),
		).not.toBeInTheDocument();
	});

	it("exposes the authenticated header styling hooks without loading an auth page", () => {
		customRender(<Header />);

		const avatar = screen.getByRole("img", { name: "Test" });
		expect(avatar).toHaveClass("avatar");
		expect(avatar).toHaveAttribute("width", "36");
		expect(avatar).toHaveAttribute("height", "36");
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
	it("publishes and cleans up current header clearance when the menu changes", async () => {
		const user = userEvent.setup();
		const { unmount } = customRender(<Header />, { authValue: null });

		expect(
			document.documentElement.style.getPropertyValue("--site-header-height"),
		).toBe("72px");
		expect(
			document.documentElement.style.getPropertyValue(
				"--site-scroll-clearance",
			),
		).toBe("calc(72px + var(--site-scroll-gap))");

		headerHeight = 184;
		await user.click(screen.getByRole("button", { name: "Abrir menú" }));
		observers.at(-1).callback();

		expect(
			document.documentElement.style.getPropertyValue("--site-header-height"),
		).toBe("184px");
		expect(screen.getByRole("button", { name: "Cerrar menú" })).toHaveAttribute(
			"aria-expanded",
			"true",
		);

		unmount();
		expect(
			document.documentElement.style.getPropertyValue("--site-header-height"),
		).toBe("");
		expect(
			observers.every((observer) => observer.disconnect.mock.calls.length > 0),
		).toBe(true);
	});

	it("opens and closes the mobile menu from the hamburger button", async () => {
		const user = userEvent.setup();
		customRender(<Header />, { authValue: null });

		const menuButton = screen.getByRole("button", { name: "Abrir menú" });
		const menu = screen.getByRole("link", {
			name: "Iniciar sesión",
		}).parentElement;

		expect(menu).not.toHaveClass("nav-links--open");
		await user.click(menuButton);
		expect(
			screen.getByRole("button", { name: "Cerrar menú" }),
		).toBeInTheDocument();
		expect(menu).toHaveClass("nav-links--open");

		await user.click(screen.getByRole("button", { name: "Cerrar menú" }));
		expect(
			screen.getByRole("button", { name: "Abrir menú" }),
		).toBeInTheDocument();
		expect(menu).not.toHaveClass("nav-links--open");
	});

	it("closes the mobile menu after navigation", async () => {
		const user = userEvent.setup();
		customRender(<Header />, { authValue: null });

		await user.click(screen.getByRole("button", { name: "Abrir menú" }));
		await user.click(screen.getByRole("link", { name: "Iniciar sesión" }));

		expect(
			screen.getByRole("button", { name: "Abrir menú" }),
		).toBeInTheDocument();
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

	it("closes the open mobile menu on Escape and returns focus to the hamburger button", async () => {
		const user = userEvent.setup();
		customRender(<Header />, { authValue: null });

		const menuButton = screen.getByRole("button", { name: "Abrir menú" });
		await user.click(menuButton);
		expect(screen.getByRole("button", { name: "Cerrar menú" })).toHaveAttribute(
			"aria-expanded",
			"true",
		);

		const loginLink = screen.getByRole("link", { name: "Iniciar sesión" });
		loginLink.focus();
		expect(loginLink).toHaveFocus();

		await user.keyboard("{Escape}");

		const reopenedButton = screen.getByRole("button", { name: "Abrir menú" });
		expect(reopenedButton).toHaveAttribute("aria-expanded", "false");
		expect(reopenedButton).toHaveFocus();
	});

	it("closes the open mobile menu when clicking outside the header without stealing focus", async () => {
		const user = userEvent.setup();
		customRender(
			<div>
				<Header />
				<button type="button">Acción externa</button>
			</div>,
			{ authValue: null },
		);

		await user.click(screen.getByRole("button", { name: "Abrir menú" }));
		expect(screen.getByRole("button", { name: "Cerrar menú" })).toHaveAttribute(
			"aria-expanded",
			"true",
		);

		const outsideButton = screen.getByRole("button", {
			name: "Acción externa",
		});
		await user.click(outsideButton);

		expect(screen.getByRole("button", { name: "Abrir menú" })).toHaveAttribute(
			"aria-expanded",
			"false",
		);
		expect(outsideButton).toHaveFocus();
	});

	it("does not close the open mobile menu when clicking a non-navigating area inside it", async () => {
		const user = userEvent.setup();
		customRender(<Header />);

		await user.click(screen.getByRole("button", { name: "Abrir menú" }));
		const menu = document.getElementById("mobile-navigation");
		expect(menu).toHaveClass("nav-links--open");

		await user.click(screen.getByText("Test"));

		expect(screen.getByRole("button", { name: "Cerrar menú" })).toHaveAttribute(
			"aria-expanded",
			"true",
		);
		expect(menu).toHaveClass("nav-links--open");
	});

	it("does not close the mobile menu or steal focus when an open dialog is present", async () => {
		const user = userEvent.setup();
		customRender(
			<div>
				<Header />
				<dialog open>
					<button type="button">Acción en diálogo</button>
				</dialog>
			</div>,
			{ authValue: null },
		);

		await user.click(screen.getByRole("button", { name: "Abrir menú" }));
		expect(screen.getByRole("button", { name: "Cerrar menú" })).toHaveAttribute(
			"aria-expanded",
			"true",
		);

		const dialogButton = screen.getByRole("button", {
			name: "Acción en diálogo",
		});
		dialogButton.focus();
		expect(dialogButton).toHaveFocus();

		await user.keyboard("{Escape}");
		expect(screen.getByRole("button", { name: "Cerrar menú" })).toHaveAttribute(
			"aria-expanded",
			"true",
		);
		expect(dialogButton).toHaveFocus();

		await user.click(dialogButton);
		expect(screen.getByRole("button", { name: "Cerrar menú" })).toHaveAttribute(
			"aria-expanded",
			"true",
		);
		expect(dialogButton).toHaveFocus();
	});

	it("does not close the mobile menu or steal focus when Escape keydown is already defaultPrevented", async () => {
		const user = userEvent.setup();
		customRender(<Header />, { authValue: null });

		await user.click(screen.getByRole("button", { name: "Abrir menú" }));
		const loginLink = screen.getByRole("link", { name: "Iniciar sesión" });
		loginLink.focus();
		expect(loginLink).toHaveFocus();

		const escapeEvent = new KeyboardEvent("keydown", {
			key: "Escape",
			bubbles: true,
			cancelable: true,
		});
		escapeEvent.preventDefault();
		document.dispatchEvent(escapeEvent);

		expect(screen.getByRole("button", { name: "Cerrar menú" })).toHaveAttribute(
			"aria-expanded",
			"true",
		);
		expect(loginLink).toHaveFocus();
	});
});
