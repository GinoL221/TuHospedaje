import { render, screen, act, waitFor } from "@testing-library/react";
import { MemoryRouter, Routes, Route, useLocation } from "react-router-dom";
import { AuthProvider } from "./AuthContext";
import { useAuth } from "../hooks/useAuth";
import { get, post } from "../services/api";

vi.mock("../services/api");

function AuthConsumer() {
  const { user, loading, login, register, logout } = useAuth();
  return (
    <div>
      <span data-testid="loading">{loading ? "loading" : "ready"}</span>
      <span data-testid="user">{user ? user.email : "no-user"}</span>
      <button onClick={() => login("test@example.com", "secret")}>login</button>
      <button
        onClick={() =>
          register("Test", "User", "test@example.com", "secret")
        }
      >
        register
      </button>
      <button onClick={() => logout()}>logout</button>
    </div>
  );
}

function LoginSentinel() {
  const location = useLocation();
  return (
    <div data-testid="login-sentinel">
      login page
      <span data-testid="login-from">
        {location.state?.from?.pathname ?? "no-from"}
      </span>
    </div>
  );
}

function renderWithProvider({ initialEntries = ["/"] } = {}) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginSentinel />} />
          <Route path="*" element={<AuthConsumer />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  );
}

const meUser = {
  firstName: "Test",
  lastName: "User",
  email: "test@example.com",
  role: "USER",
  imageUrl: null,
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("AuthContext - bootstrap on mount", () => {
  it("calls GET /auth/me and sets the user when it resolves (200)", async () => {
    get.mockResolvedValue(meUser);

    renderWithProvider();

    expect(screen.getByTestId("loading")).toHaveTextContent("loading");

    await waitFor(() => {
      expect(screen.getByTestId("loading")).toHaveTextContent("ready");
    });

    expect(get).toHaveBeenCalledWith("/auth/me");
    expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
  });

  it("leaves the user unauthenticated without throwing when /auth/me rejects (401)", async () => {
    get.mockRejectedValue(new Error("Sesión expirada"));

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId("loading")).toHaveTextContent("ready");
    });

    expect(screen.getByTestId("user")).toHaveTextContent("no-user");
  });
});

describe("AuthContext - login", () => {
  it("sets the user directly from the response body, with no token decoding", async () => {
    get.mockRejectedValue(new Error("Sesión expirada"));
    post.mockResolvedValue(meUser);

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId("loading")).toHaveTextContent("ready");
    });

    await act(async () => {
      screen.getByText("login").click();
    });

    expect(post).toHaveBeenCalledWith("/auth/login", {
      email: "test@example.com",
      password: "secret",
    });
    expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
  });
});

describe("AuthContext - register", () => {
  it("sets the user directly from the response body, with no token decoding", async () => {
    get.mockRejectedValue(new Error("Sesión expirada"));
    post.mockResolvedValue(meUser);

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId("loading")).toHaveTextContent("ready");
    });

    await act(async () => {
      screen.getByText("register").click();
    });

    expect(post).toHaveBeenCalledWith("/auth/register", {
      firstName: "Test",
      lastName: "User",
      email: "test@example.com",
      password: "secret",
    });
    expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
  });
});

describe("AuthContext - logout", () => {
  it("calls POST /auth/logout and clears the in-memory user state without touching localStorage", async () => {
    get.mockResolvedValue(meUser);
    post.mockResolvedValue(null);
    const removeItemSpy = vi.spyOn(Storage.prototype, "removeItem");

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
    });

    await act(async () => {
      screen.getByText("logout").click();
    });

    expect(post).toHaveBeenCalledWith("/auth/logout");
    expect(screen.getByTestId("user")).toHaveTextContent("no-user");
    expect(removeItemSpy).not.toHaveBeenCalled();

    removeItemSpy.mockRestore();
  });
});

describe("AuthContext - auth:unauthorized event", () => {
  it("logs out and navigates to /login when the event fires", async () => {
    get.mockResolvedValue(meUser);
    post.mockResolvedValue(null);

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
    });

    await act(async () => {
      window.dispatchEvent(new CustomEvent("auth:unauthorized"));
    });

    expect(screen.getByTestId("login-sentinel")).toBeInTheDocument();
  });

  it("preserves the originating route as state.from so login can redirect back", async () => {
    get.mockResolvedValue(meUser);

    renderWithProvider({ initialEntries: ["/booking/42"] });

    await waitFor(() => {
      expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
    });

    await act(async () => {
      window.dispatchEvent(new CustomEvent("auth:unauthorized"));
    });

    expect(screen.getByTestId("login-sentinel")).toBeInTheDocument();
    expect(screen.getByTestId("login-from")).toHaveTextContent("/booking/42");
  });
});
