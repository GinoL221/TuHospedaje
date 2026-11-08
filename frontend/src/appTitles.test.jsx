import { createContext } from "react";
import { render, screen } from "@testing-library/react";
import App from "./App";

vi.mock("./context/AuthContext", () => {
	const AuthContext = createContext();
	function AuthProvider({ children }) {
		return (
			<AuthContext.Provider value={{ user: null, loading: false }}>
				{children}
			</AuthContext.Provider>
		);
	}
	return { AuthContext, AuthProvider };
});
vi.mock("./components/Header/Header", () => ({
	default: () => <header>Header shell</header>,
}));
vi.mock("./components/Footer/Footer", () => ({
	default: () => <footer>Footer shell</footer>,
}));

// No page is mocked: the real lazy pages render through the real router.
describe("document title through the real App router", () => {
	beforeEach(() => {
		document.title = "TuHospedaje";
	});

	it.each([
		[
			"/no-such-route",
			"La página que buscás no existe.",
			"Página no encontrada",
		],
		[
			"/unauthorized",
			"No tenés permisos para acceder a esta página.",
			"Acceso no autorizado",
		],
	])("%s", async (path, content, title) => {
		window.history.replaceState({}, "", path);
		const { unmount } = render(<App />);

		await screen.findByText(content);

		expect(document.title).toBe(`${title} | TuHospedaje`);
		unmount();
	});
});
