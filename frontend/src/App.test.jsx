import { createContext } from "react";
import { act, render, screen } from "@testing-library/react";

const authState = vi.hoisted(() => ({ value: { token: null, user: null } }));

vi.mock("./context/AuthContext", () => {
  const AuthContext = createContext();
  function AuthProvider({ children }) {
    return <AuthContext.Provider value={authState.value}>{children}</AuthContext.Provider>;
  }
  return { AuthContext, AuthProvider };
});
vi.mock("./components/Header/Header", () => ({ default: () => <header>Header shell</header> }));
vi.mock("./components/Footer/Footer", () => ({ default: () => <footer>Footer shell</footer> }));
vi.mock("./components/WhatsAppButton/WhatsAppButton", () => ({
  default: () => <div>WhatsApp shell</div>,
}));
vi.mock("./components/RequireAuth", () => ({ default: () => null }));
vi.mock("./components/RequireAdmin", () => ({ default: ({ children }) => children }));
vi.mock("./pages/LoginPage", () => ({ default: () => null }));
vi.mock("./pages/RegisterPage", () => ({ default: () => null }));
vi.mock("./pages/ProductDetail/ProductDetail", () => ({ default: () => null }));
vi.mock("./pages/Admin/Admin", () => ({ default: () => null }));
vi.mock("./pages/SearchResults/SearchResults", () => ({ default: () => null }));
vi.mock("./pages/Favorites/FavoritesPage", () => ({ default: () => null }));
vi.mock("./pages/Booking/BookingPage", () => ({ default: () => null }));
vi.mock("./pages/Booking/BookingConfirmation", () => ({ default: () => null }));
vi.mock("./pages/MyReservations/MyReservationsPage", () => ({ default: () => null }));

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  promise.catch(() => {});
  return { promise, resolve, reject };
}

async function renderAppWithHome(homeModule) {
  vi.resetModules();
  vi.doMock("./pages/Home/Home", () => homeModule);
  const { default: App } = await import("./App");
  return render(<App />);
}

const UNAUTHENTICATED = { token: null, user: null };
const AUTHENTICATED_USER = { token: "fake-token", user: { role: "USER" } };
const AUTHENTICATED_ADMIN = { token: "fake-token", user: { role: "ADMIN" } };

// Renders App at an arbitrary route with a controllable auth context, real or
// inert guards, and per-test page module overrides. Every call re-declares
// the guard mocks and a sane default for every lazy page so behavior never
// depends on what a previous test left registered.
async function renderAppAt({ path, authValue = UNAUTHENTICATED, guards = "inert", pages = {} } = {}) {
  vi.resetModules();
  authState.value = authValue;
  window.history.replaceState({}, "", path);

  vi.doMock(
    "./components/RequireAuth",
    guards === "real"
      ? () => vi.importActual("./components/RequireAuth")
      : () => ({ default: () => null })
  );
  vi.doMock(
    "./components/RequireAdmin",
    guards === "real"
      ? () => vi.importActual("./components/RequireAdmin")
      : () => ({ default: ({ children }) => children })
  );

  const defaultPages = {
    "./pages/Home/Home": { default: () => <main>Home shell</main> },
  };
  for (const [pagePath, moduleOrPromise] of Object.entries({ ...defaultPages, ...pages })) {
    vi.doMock(pagePath, () => moduleOrPromise);
  }

  const { default: App } = await import("./App");
  return render(<App />);
}

beforeEach(() => {
  vi.useFakeTimers();
  window.history.replaceState({}, "", "/");
});

afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("App Home route delivery", () => {
  it("keeps the shell visible, delays feedback, and replaces it immediately on resolution", async () => {
    const home = deferred();
    await renderAppWithHome(home.promise);

    expect(screen.getByText("Header shell")).toBeInTheDocument();
    expect(screen.getByText("Footer shell")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();

    act(() => vi.advanceTimersByTime(150));
    expect(screen.getByRole("status")).toHaveTextContent("Cargando página…");

    await act(async () => home.resolve({ default: () => <main>Home resolved</main> }));
    expect(screen.getByText("Home resolved")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("does not flash loading feedback when Home resolves quickly", async () => {
    await renderAppWithHome({ default: () => <main>Fast Home</main> });
    await act(async () => {});

    expect(screen.getByText("Fast Home")).toBeInTheDocument();
    act(() => vi.runAllTimers());
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("shows manual recovery after Home rejects without reloading automatically", async () => {
    const home = deferred();
    const reload = vi.fn();
    vi.stubGlobal("location", { reload });
    vi.spyOn(console, "error").mockImplementation(() => {});
    await renderAppWithHome(home.promise);

    await act(async () => home.reject(new Error("chunk failed")));

    expect(screen.getByRole("button", { name: "Recargar página" })).toBeInTheDocument();
    expect(reload).not.toHaveBeenCalled();
  });
});

describe("App SearchResults route delivery", () => {
  it("keeps the shell visible, delays feedback, and replaces it immediately on resolution", async () => {
    const search = deferred();
    await renderAppAt({
      path: "/search",
      pages: { "./pages/SearchResults/SearchResults": search.promise },
    });

    expect(screen.getByText("Header shell")).toBeInTheDocument();
    expect(screen.getByText("Footer shell")).toBeInTheDocument();
    expect(screen.getByText("WhatsApp shell")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();

    act(() => vi.advanceTimersByTime(150));
    expect(screen.getByRole("status")).toHaveTextContent("Cargando página…");

    await act(async () => search.resolve({ default: () => <main>Search resolved</main> }));
    expect(screen.getByText("Search resolved")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.getByText("Header shell")).toBeInTheDocument();
  });

  it("does not flash loading feedback when SearchResults resolves quickly", async () => {
    await renderAppAt({
      path: "/search",
      pages: { "./pages/SearchResults/SearchResults": { default: () => <main>Fast Search</main> } },
    });
    await act(async () => {});

    expect(screen.getByText("Fast Search")).toBeInTheDocument();
    act(() => vi.runAllTimers());
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("shows manual recovery after SearchResults rejects without reloading automatically", async () => {
    const search = deferred();
    const reload = vi.fn();
    vi.stubGlobal("location", { reload });
    vi.spyOn(console, "error").mockImplementation(() => {});
    await renderAppAt({
      path: "/search",
      pages: { "./pages/SearchResults/SearchResults": search.promise },
    });

    await act(async () => search.reject(new Error("chunk failed")));

    expect(screen.getByRole("button", { name: "Recargar página" })).toBeInTheDocument();
    expect(reload).not.toHaveBeenCalled();
    expect(screen.getByText("Header shell")).toBeInTheDocument();
  });
});

describe("App protected route authorization", () => {
  it("redirects an unauthenticated visitor to /login before the deferred page resolves", async () => {
    const reservations = deferred();
    await renderAppAt({
      path: "/my-reservations",
      authValue: UNAUTHENTICATED,
      guards: "real",
      pages: { "./pages/MyReservations/MyReservationsPage": reservations.promise },
    });

    await act(async () => {});

    expect(window.location.pathname).toBe("/login");
    expect(screen.queryByText("Reservations resolved")).not.toBeInTheDocument();
  });

  it("renders the authorized destination once an authenticated user's page resolves", async () => {
    const reservations = deferred();
    await renderAppAt({
      path: "/my-reservations",
      authValue: AUTHENTICATED_USER,
      guards: "real",
      pages: { "./pages/MyReservations/MyReservationsPage": reservations.promise },
    });

    expect(screen.getByText("Header shell")).toBeInTheDocument();
    await act(async () => reservations.resolve({ default: () => <main>Reservations resolved</main> }));

    expect(screen.getByText("Reservations resolved")).toBeInTheDocument();
    expect(window.location.pathname).toBe("/my-reservations");
  });
});

describe("App admin route authorization", () => {
  it("denies an authenticated non-admin visitor before the deferred Admin page resolves", async () => {
    const admin = deferred();
    await renderAppAt({
      path: "/admin",
      authValue: AUTHENTICATED_USER,
      guards: "real",
      pages: { "./pages/Admin/Admin": admin.promise },
    });

    await act(async () => {});

    expect(window.location.pathname).toBe("/");
    expect(screen.queryByText("Admin resolved")).not.toBeInTheDocument();
    expect(screen.getByText("Header shell")).toBeInTheDocument();
  });

  it("keeps the shell suppressed, delays feedback, and replaces it immediately for an authorized admin", async () => {
    const admin = deferred();
    await renderAppAt({
      path: "/admin",
      authValue: AUTHENTICATED_ADMIN,
      guards: "real",
      pages: { "./pages/Admin/Admin": admin.promise },
    });

    expect(screen.queryByText("Header shell")).not.toBeInTheDocument();
    expect(screen.queryByText("Footer shell")).not.toBeInTheDocument();
    expect(screen.queryByText("WhatsApp shell")).not.toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();

    act(() => vi.advanceTimersByTime(150));
    expect(screen.getByRole("status")).toHaveTextContent("Cargando página…");

    await act(async () => admin.resolve({ default: () => <main>Admin resolved</main> }));
    expect(screen.getByText("Admin resolved")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.queryByText("Header shell")).not.toBeInTheDocument();
  });

  it("does not flash loading feedback when the admin page resolves quickly", async () => {
    await renderAppAt({
      path: "/admin",
      authValue: AUTHENTICATED_ADMIN,
      guards: "real",
      pages: { "./pages/Admin/Admin": { default: () => <main>Fast Admin</main> } },
    });
    await act(async () => {});

    expect(screen.getByText("Fast Admin")).toBeInTheDocument();
    act(() => vi.runAllTimers());
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.queryByText("Header shell")).not.toBeInTheDocument();
  });

  it("shows manual recovery after the admin page rejects, keeping the shell suppressed and without reloading automatically", async () => {
    const admin = deferred();
    const reload = vi.fn();
    vi.spyOn(console, "error").mockImplementation(() => {});
    await renderAppAt({
      path: "/admin",
      authValue: AUTHENTICATED_ADMIN,
      guards: "real",
      pages: { "./pages/Admin/Admin": admin.promise },
    });
    vi.stubGlobal("location", { reload });

    await act(async () => admin.reject(new Error("chunk failed")));

    expect(screen.getByRole("button", { name: "Recargar página" })).toBeInTheDocument();
    expect(reload).not.toHaveBeenCalled();
    expect(screen.queryByText("Header shell")).not.toBeInTheDocument();
  });
});

describe("App document title", () => {
  it("stays fixed and route-independent across public, authenticated, and admin resolution", async () => {
    const search = deferred();
    const publicRender = await renderAppAt({
      path: "/search",
      pages: { "./pages/SearchResults/SearchResults": search.promise },
    });
    await act(async () => search.resolve({ default: () => <main>Search resolved</main> }));
    const publicTitle = document.title;
    publicRender.unmount();

    const reservations = deferred();
    const protectedRender = await renderAppAt({
      path: "/my-reservations",
      authValue: AUTHENTICATED_USER,
      guards: "real",
      pages: { "./pages/MyReservations/MyReservationsPage": reservations.promise },
    });
    await act(async () => reservations.resolve({ default: () => <main>Reservations resolved</main> }));
    const protectedTitle = document.title;
    protectedRender.unmount();

    const admin = deferred();
    const adminRender = await renderAppAt({
      path: "/admin",
      authValue: AUTHENTICATED_ADMIN,
      guards: "real",
      pages: { "./pages/Admin/Admin": admin.promise },
    });
    await act(async () => admin.resolve({ default: () => <main>Admin resolved</main> }));
    const adminTitle = document.title;
    adminRender.unmount();

    expect(publicTitle).toBe(protectedTitle);
    expect(protectedTitle).toBe(adminTitle);
  });
});
