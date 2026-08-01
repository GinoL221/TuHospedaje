import { createContext } from "react";
import { act, render, screen } from "@testing-library/react";

const authState = vi.hoisted(() => ({ value: { user: null, loading: false } }));

vi.mock("./context/AuthContext", () => {
  const AuthContext = createContext();
  function AuthProvider({ children }) {
    return <AuthContext.Provider value={authState.value}>{children}</AuthContext.Provider>;
  }
  return { AuthContext, AuthProvider };
});
vi.mock("./components/Header/Header", () => ({ default: () => <header>Header shell</header> }));
vi.mock("./components/Footer/Footer", () => ({ default: () => <footer>Footer shell</footer> }));
vi.mock("./components/RequireAuth", () => vi.importActual("./components/RequireAuth"));
vi.mock("./components/RequireAdmin", () => vi.importActual("./components/RequireAdmin"));
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

const UNAUTHENTICATED = { user: null, loading: false };
const AUTHENTICATED_USER = { user: { role: "USER" }, loading: false };
const AUTHENTICATED_ADMIN = { user: { role: "ADMIN" }, loading: false };

// Renders App at an arbitrary route with a controllable auth context and
// per-test page module overrides, exercising the real RequireAuth/RequireAdmin
// guards. Every call re-declares a sane default for every lazy page so
// behavior never depends on what a previous test left registered.
async function renderAppAt({ path, authValue = UNAUTHENTICATED, pages = {} } = {}) {
  vi.resetModules();
  authState.value = authValue;
  window.history.replaceState({}, "", path);

  const defaultPages = {
    "./pages/Home/Home": { default: () => <main>Home shell</main> },
    "./pages/Unauthorized/Unauthorized": { default: () => <main>Unauthorized shell</main> },
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
      pages: { "./pages/Admin/Admin": admin.promise },
    });

    await act(async () => {});

    expect(window.location.pathname).toBe("/unauthorized");
    expect(screen.queryByText("Admin resolved")).not.toBeInTheDocument();
    expect(screen.getByText("Header shell")).toBeInTheDocument();
  });

  it("keeps the shell suppressed, delays feedback, and replaces it immediately for an authorized admin", async () => {
    const admin = deferred();
    await renderAppAt({
      path: "/admin",
      authValue: AUTHENTICATED_ADMIN,
      pages: { "./pages/Admin/Admin": admin.promise },
    });

    expect(screen.queryByText("Header shell")).not.toBeInTheDocument();
    expect(screen.queryByText("Footer shell")).not.toBeInTheDocument();
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
      pages: { "./pages/Admin/Admin": admin.promise },
    });
    vi.stubGlobal("location", { reload });

    await act(async () => admin.reject(new Error("chunk failed")));

    expect(screen.getByRole("button", { name: "Recargar página" })).toBeInTheDocument();
    expect(reload).not.toHaveBeenCalled();
    expect(screen.queryByText("Header shell")).not.toBeInTheDocument();
  });
});

describe("App auth loading state", () => {
  it("renders neither protected nor admin content while auth is still loading, without redirecting or exposing the deferred page", async () => {
    const reservations = deferred();
    const { unmount } = await renderAppAt({
      path: "/my-reservations",
      authValue: { user: null, loading: true },
      pages: { "./pages/MyReservations/MyReservationsPage": reservations.promise },
    });

    await act(async () => {});

    expect(window.location.pathname).toBe("/my-reservations");
    expect(screen.queryByText("Reservations resolved")).not.toBeInTheDocument();
    unmount();

    const admin = deferred();
    await renderAppAt({
      path: "/admin",
      authValue: { user: null, loading: true },
      pages: { "./pages/Admin/Admin": admin.promise },
    });

    await act(async () => {});

    expect(window.location.pathname).toBe("/admin");
    expect(screen.queryByText("Admin resolved")).not.toBeInTheDocument();
  });
});

describe("App document title", () => {
  it("never sets document.title while resolving public, authenticated, and admin routes", async () => {
    const titleSetter = vi.spyOn(document, "title", "set");

    const search = deferred();
    const publicRender = await renderAppAt({
      path: "/search",
      pages: { "./pages/SearchResults/SearchResults": search.promise },
    });
    await act(async () => search.resolve({ default: () => <main>Search resolved</main> }));
    publicRender.unmount();

    const reservations = deferred();
    const protectedRender = await renderAppAt({
      path: "/my-reservations",
      authValue: AUTHENTICATED_USER,
      pages: { "./pages/MyReservations/MyReservationsPage": reservations.promise },
    });
    await act(async () => reservations.resolve({ default: () => <main>Reservations resolved</main> }));
    protectedRender.unmount();

    const admin = deferred();
    const adminRender = await renderAppAt({
      path: "/admin",
      authValue: AUTHENTICATED_ADMIN,
      pages: { "./pages/Admin/Admin": admin.promise },
    });
    await act(async () => admin.resolve({ default: () => <main>Admin resolved</main> }));
    adminRender.unmount();

    expect(titleSetter).not.toHaveBeenCalled();
  });
});
