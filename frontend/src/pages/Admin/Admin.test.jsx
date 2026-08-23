import { Routes, Route } from "react-router-dom";
import userEvent from "@testing-library/user-event";
import { customRender, screen, makeAuthValue, mockAdmin } from "../../test/test-utils";
import Admin from "./Admin";

vi.mock("./AdminDashboard", () => ({ default: () => <div data-testid="admin-dashboard" /> }));
vi.mock("./AdminLodgings", () => ({ default: () => <div data-testid="admin-lodgings" /> }));
vi.mock("./AdminCategories", () => ({ default: () => <div data-testid="admin-categories" /> }));
vi.mock("./AdminFeatures", () => ({ default: () => <div data-testid="admin-features" /> }));
vi.mock("./AdminPolicies", () => ({ default: () => <div data-testid="admin-policies" /> }));
vi.mock("./AdminUsers", () => ({ default: () => <div data-testid="admin-users" /> }));
vi.mock("./AdminReservations", () => ({ default: () => <div data-testid="admin-reservations" /> }));

// jsdom has `ontouchstart` in window by default and innerWidth = 1024 (≤ 1024),
// which makes isMobile = true for all tests. Force a desktop viewport so
// non-mobile tests render the admin shell, and only the mobile describe
// overrides innerWidth back to a narrow value.
beforeEach(() => {
  Object.defineProperty(window, "innerWidth", {
    value: 1280,
    writable: true,
    configurable: true,
  });
});

function HomeSentinel() {
  return <div data-testid="home-sentinel">home</div>;
}

function renderAdmin({ authValue } = {}) {
  return customRender(
    <Routes>
      <Route path="/admin" element={<Admin />} />
      <Route path="/" element={<HomeSentinel />} />
    </Routes>,
    { authValue: authValue ?? makeAuthValue({ user: mockAdmin }), route: "/admin" }
  );
}

describe("Admin - default tab", () => {
  it("renders AdminDashboard by default", () => {
    renderAdmin();
    expect(screen.getByTestId("admin-dashboard")).toBeInTheDocument();
    expect(screen.queryByTestId("admin-lodgings")).not.toBeInTheDocument();
  });
});

describe("Admin - tab switching", () => {
  it("switches to Alojamientos panel when its nav button is clicked", async () => {
    const user = userEvent.setup();
    renderAdmin();

    await user.click(screen.getByTestId("admin-nav-lodgings"));

    expect(screen.getByTestId("admin-lodgings")).toBeInTheDocument();
    expect(screen.queryByTestId("admin-dashboard")).not.toBeInTheDocument();
  });

  it("switches to Reservas panel when its nav button is clicked", async () => {
    const user = userEvent.setup();
    renderAdmin();

    await user.click(screen.getByTestId("admin-nav-reservations"));

    expect(screen.getByTestId("admin-reservations")).toBeInTheDocument();
    expect(screen.queryByTestId("admin-dashboard")).not.toBeInTheDocument();
  });
});

describe("Admin - logout", () => {
  it("calls logout when the Salir button is clicked", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue({ user: mockAdmin });
    renderAdmin({ authValue });

    await user.click(screen.getByRole("button", { name: /Salir/ }));

    expect(authValue.logout).toHaveBeenCalledTimes(1);
  });
});

describe("Admin - navigation to home", () => {
  it("navigates to / when Ir al inicio is clicked", async () => {
    const user = userEvent.setup();
    renderAdmin();

    await user.click(screen.getByRole("button", { name: /Ir al inicio/ }));

    expect(await screen.findByTestId("home-sentinel")).toBeInTheDocument();
  });
});

describe("Admin - mobile block", () => {
  beforeEach(() => {
    Object.defineProperty(window, "innerWidth", {
      value: 768,
      writable: true,
      configurable: true,
    });
  });

  afterEach(() => {
    Object.defineProperty(window, "innerWidth", {
      value: 1280,
      writable: true,
      configurable: true,
    });
  });

  it("renders the mobile-unavailable message when the viewport is narrow", () => {
    // jsdom always has ontouchstart; setting innerWidth ≤ 1024 is enough
    renderAdmin();

    expect(screen.getByText("Panel no disponible en móvil")).toBeInTheDocument();
    expect(screen.queryByTestId("admin-dashboard")).not.toBeInTheDocument();
  });

  it("announces the unavailable state as an accessible status and focuses its heading for direct navigation", () => {
    renderAdmin();

    const status = screen.getByRole("status");
    const heading = screen.getByRole("heading", {
      name: "Panel no disponible en móvil",
    });

    expect(status).toContainElement(heading);
    expect(heading).toHaveFocus();
  });
});
