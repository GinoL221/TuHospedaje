import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import NotFound from "./pages/NotFound/NotFound";
import Unauthorized from "./pages/Unauthorized/Unauthorized";
import BookingConfirmationPage from "./pages/Booking/BookingConfirmation";
import Admin from "./pages/Admin/Admin";
import { AuthContext } from "./context/AuthContext";

describe("Semantic landmarks across views", () => {
	beforeEach(() => {
		delete window.ontouchstart;
		Object.defineProperty(navigator, "maxTouchPoints", {
			writable: true,
			configurable: true,
			value: 0,
		});
		Object.defineProperty(window, "innerWidth", {
			writable: true,
			configurable: true,
			value: 1280,
		});
	});

	it("LoginPage renders a single main landmark", () => {
		render(
			<MemoryRouter>
				<AuthContext.Provider value={{ login: vi.fn() }}>
					<LoginPage />
				</AuthContext.Provider>
			</MemoryRouter>,
		);
		expect(screen.getByRole("main")).toBeInTheDocument();
	});

	it("RegisterPage renders a single main landmark", () => {
		render(
			<MemoryRouter>
				<AuthContext.Provider value={{ register: vi.fn() }}>
					<RegisterPage />
				</AuthContext.Provider>
			</MemoryRouter>,
		);
		expect(screen.getByRole("main")).toBeInTheDocument();
	});

	it("NotFound renders a single main landmark", () => {
		render(
			<MemoryRouter>
				<NotFound />
			</MemoryRouter>,
		);
		expect(screen.getByRole("main")).toBeInTheDocument();
	});

	it("Unauthorized renders a single main landmark", () => {
		render(
			<MemoryRouter>
				<Unauthorized />
			</MemoryRouter>,
		);
		expect(screen.getByRole("main")).toBeInTheDocument();
	});

	it("BookingConfirmationPage renders a single main landmark", () => {
		render(
			<MemoryRouter
				initialEntries={[
					{
						pathname: "/booking/confirmation",
						state: {
							reservation: {
								id: 1,
								checkIn: "2026-08-01",
								checkOut: "2026-08-05",
							},
							lodging: { name: "Hotel", city: "Salta" },
						},
					},
				]}
			>
				<BookingConfirmationPage />
			</MemoryRouter>,
		);
		expect(screen.getByRole("main")).toBeInTheDocument();
	});

	it("Admin desktop shell renders a single main landmark and aside navigation", () => {
		render(
			<MemoryRouter>
				<AuthContext.Provider
					value={{ user: { firstName: "Admin" }, logout: vi.fn() }}
				>
					<Admin />
				</AuthContext.Provider>
			</MemoryRouter>,
		);
		expect(screen.getByRole("main")).toHaveClass("admin-content");
		expect(screen.getByRole("complementary")).toHaveClass("admin-sidebar");
		expect(screen.getByRole("navigation")).toHaveClass("admin-nav");
	});
});
