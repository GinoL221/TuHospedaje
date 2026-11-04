import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const readSource = (path) =>
	readFileSync(resolve(process.cwd(), "src", path), "utf8");

describe("account and shared surfaces visual contract", () => {
	it("keeps auth surfaces, borders, and validation feedback on scoped semantic tokens", () => {
		const auth = readSource("assets/css/auth.css");

		expect(auth).toContain("var(--surface-raised)");
		expect(auth).toContain("--auth-danger:");
		expect(auth).toContain("--auth-success:");
		expect(auth).toContain("var(--auth-danger)");
		expect(auth).toContain("var(--auth-success)");
		expect(auth).not.toMatch(/border:\s*1px solid #ccc;/);
	});

	it("keeps auth links off primary-on-light text that fails identity contrast rules", () => {
		const auth = readSource("assets/css/auth.css");

		expect(auth).toMatch(
			/\.login-box a,\s*\n\.register-box a \{[^}]*color:\s*var\(--secondary\);/,
		);
	});

	it("keeps favorites remove-action and error states off primary-on-light text", () => {
		const favorites = readSource("pages/Favorites/FavoritesPage.css");

		expect(favorites).toContain("--favorites-danger:");
		expect(favorites).toMatch(
			/\.empty-state\.error \{[^}]*color:\s*var\(--favorites-danger\);/,
		);
		expect(favorites).toMatch(
			/\.btn-remove-fav \{[^}]*color:\s*var\(--secondary\);/s,
		);
		expect(favorites).toContain("var(--action-primary-fg)");
	});

	it("keeps reservation status badges and the reservation link on scoped semantic tokens", () => {
		const reservations = readSource(
			"pages/MyReservations/MyReservationsPage.css",
		);

		expect(reservations).toContain("--reservation-success:");
		expect(reservations).toContain("--reservation-warning:");
		expect(reservations).toContain("--reservation-danger:");
		expect(reservations).toMatch(
			/\.reservation-link \{[^}]*color:\s*var\(--secondary\);/s,
		);
		expect(reservations).toContain("var(--reservation-danger)");
	});

	it("keeps the shared empty-state border off a raw gray literal", () => {
		const app = readSource("App.css");

		expect(app).toMatch(
			/\.empty-state \{[^}]*border:\s*1px dashed var\(--empty-state-border\);/s,
		);
	});

	it("preserves login, register, favorites, reservations, and unauthorized behavior contracts", () => {
		const login = readSource("pages/LoginPage.jsx");
		const register = readSource("pages/RegisterPage.jsx");
		const favorites = readSource("pages/Favorites/FavoritesPage.jsx");
		const favoriteService = readSource("services/favoriteService.js");
		const reservations = readSource(
			"pages/MyReservations/MyReservationsPage.jsx",
		);
		const unauthorized = readSource("pages/Unauthorized/Unauthorized.jsx");

		expect(login).toContain("navigate(from, { replace: true })");
		expect(register).toContain('err.code === "duplicate_email"');
		expect(favorites).toContain('from "../../services/favoriteService"');
		expect(favoriteService).toContain("del(`/favorites/${lodgingId}`)");
		expect(reservations).toContain('reservation.status === "CONFIRMED"');
		expect(unauthorized).toContain(
			"No tenés permisos para acceder a esta página.",
		);
	});
});

describe("password visibility control", () => {
	it("keeps the toggle hover on a subtle surface instead of the dark action hover", () => {
		const auth = readSource("assets/css/auth.css");

		const hover = auth.match(
			/\.login-box \.input-wrap \.pw-toggle:hover:not\(:disabled\),[\s\S]*?\{([^}]*)\}/,
		);
		expect(hover).not.toBeNull();
		expect(hover[1]).toMatch(
			/background:\s*color-mix\(in srgb, var\(--secondary\) 10%, transparent\)/,
		);
		expect(hover[1]).not.toMatch(/--action-primary-hover-bg/);
	});

	it("gives the toggle a 44px target and keeps the icon clear of the input text", () => {
		const auth = readSource("assets/css/auth.css");

		expect(auth).toMatch(
			/\.input-wrap \.pw-toggle \{[^}]*width:\s*44px;[^}]*height:\s*44px;/s,
		);
		expect(auth).toMatch(/\.input-wrap input \{[^}]*padding-right:\s*48px;/s);
	});
});
