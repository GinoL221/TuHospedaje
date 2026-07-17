import { act, render, screen } from "@testing-library/react";

vi.mock("./context/AuthContext", () => ({
  AuthProvider: ({ children }) => children,
}));
vi.mock("./components/Header/Header", () => ({ default: () => <header>Header shell</header> }));
vi.mock("./components/Footer/Footer", () => ({ default: () => <footer>Footer shell</footer> }));
vi.mock("./components/WhatsAppButton/WhatsAppButton", () => ({ default: () => null }));
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

describe("App Home route delivery", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    window.history.replaceState({}, "", "/");
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

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
