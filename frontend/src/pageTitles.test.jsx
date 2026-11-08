import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import NotFound from "./pages/NotFound/NotFound";
import Unauthorized from "./pages/Unauthorized/Unauthorized";
import BookingConfirmationPage from "./pages/Booking/BookingConfirmation";
import Admin from "./pages/Admin/Admin";
import { AuthContext } from "./context/AuthContext";

const BRAND = "TuHospedaje";

const confirmationState = {
	reservation: { id: 1, checkIn: "2026-08-01", checkOut: "2026-08-05" },
	lodging: { name: "Hotel", city: "Salta" },
};

// Each case renders the real page component, so removing the hook from a page
// makes exactly that case fail.
const PAGES = [
	{
		title: "Iniciar sesión",
		ui: (
			<AuthContext.Provider value={{ login: vi.fn() }}>
				<LoginPage />
			</AuthContext.Provider>
		),
	},
	{
		title: "Crear cuenta",
		ui: (
			<AuthContext.Provider value={{ register: vi.fn() }}>
				<RegisterPage />
			</AuthContext.Provider>
		),
	},
	{ title: "Página no encontrada", ui: <NotFound /> },
	{ title: "Acceso no autorizado", ui: <Unauthorized /> },
	{
		title: "Reserva confirmada",
		ui: <BookingConfirmationPage />,
		entry: { pathname: "/booking/confirmation", state: confirmationState },
	},
	{
		title: "Panel de administración",
		ui: (
			<AuthContext.Provider
				value={{ user: { firstName: "Admin" }, logout: vi.fn() }}
			>
				<Admin />
			</AuthContext.Provider>
		),
	},
];

describe("page titles", () => {
	beforeEach(() => {
		document.title = BRAND;
		delete window.ontouchstart;
		Object.defineProperty(navigator, "maxTouchPoints", {
			configurable: true,
			value: 0,
		});
		Object.defineProperty(window, "innerWidth", {
			configurable: true,
			value: 1280,
		});
	});

	it.each(PAGES)("sets the title for $title", ({ title, ui, entry }) => {
		const { unmount } = render(
			<MemoryRouter initialEntries={[entry ?? "/"]}>{ui}</MemoryRouter>,
		);

		expect(document.title).toBe(`${title} | ${BRAND}`);

		unmount();
		expect(document.title).toBe(BRAND);
	});
});
