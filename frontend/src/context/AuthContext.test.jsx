import { render, screen, act } from "@testing-library/react";
import { MemoryRouter, Routes, Route, useLocation } from "react-router-dom";
import { jwtDecode } from "jwt-decode";
import { AuthProvider } from "./AuthContext";
import { useAuth } from "../hooks/useAuth";
import { post } from "../services/api";

vi.mock("jwt-decode");
vi.mock("../services/api");

function AuthConsumer() {
  const { user, token, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="user">{user ? user.email : "no-user"}</span>
      <span data-testid="token">{token ?? "no-token"}</span>
      <button onClick={() => login("test@example.com", "secret")}>login</button>
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

const decodedPayload = {
  firstName: "Test",
  lastName: "User",
  sub: "test@example.com",
  role: "USER",
  imageUrl: null,
  exp: Math.floor(Date.now() / 1000) + 3600,
};

beforeEach(() => {
  localStorage.clear();
});

describe("AuthContext - login", () => {
  it("updates token/user and persists the token to localStorage", async () => {
    post.mockResolvedValue({ token: "fresh-jwt" });
    jwtDecode.mockReturnValue(decodedPayload);

    renderWithProvider();

    expect(screen.getByTestId("user")).toHaveTextContent("no-user");

    await act(async () => {
      screen.getByText("login").click();
    });

    expect(post).toHaveBeenCalledWith("/auth/login", {
      email: "test@example.com",
      password: "secret",
    });
    expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
    expect(screen.getByTestId("token")).toHaveTextContent("fresh-jwt");
    expect(localStorage.getItem("token")).toBe("fresh-jwt");
  });
});

describe("AuthContext - logout", () => {
  it("clears token/user from state and removes the token from localStorage", async () => {
    localStorage.setItem("token", "existing-jwt");
    jwtDecode.mockReturnValue(decodedPayload);

    renderWithProvider();

    expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");

    await act(async () => {
      screen.getByText("logout").click();
    });

    expect(screen.getByTestId("user")).toHaveTextContent("no-user");
    expect(screen.getByTestId("token")).toHaveTextContent("no-token");
    expect(localStorage.getItem("token")).toBeNull();
  });
});

describe("AuthContext - initial mount with a valid token", () => {
  it("restores user/token from a non-expired token in localStorage", () => {
    localStorage.setItem("token", "valid-jwt");
    jwtDecode.mockReturnValue(decodedPayload);

    renderWithProvider();

    expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
    expect(screen.getByTestId("token")).toHaveTextContent("valid-jwt");
    expect(localStorage.getItem("token")).toBe("valid-jwt");
  });
});

describe("AuthContext - initial mount with an expired token", () => {
  it("discards the token and starts logged-out", () => {
    localStorage.setItem("token", "expired-jwt");
    jwtDecode.mockReturnValue({ ...decodedPayload, exp: Math.floor(Date.now() / 1000) - 10 });

    renderWithProvider();

    expect(screen.getByTestId("user")).toHaveTextContent("no-user");
    expect(screen.getByTestId("token")).toHaveTextContent("no-token");
    expect(localStorage.getItem("token")).toBeNull();
  });
});

describe("AuthContext - initial mount with a malformed token", () => {
  // SUSPICIOUS: getInitialAuth swallows the decode error silently (bare
  // catch-all) and just falls back to logged-out state instead of
  // surfacing the error anywhere. Characterizing current behavior as-is,
  // per spec Risks section — not treating this as the ideal contract.
  it("silently discards an unparseable token and starts logged-out", () => {
    localStorage.setItem("token", "not-a-real-jwt");
    jwtDecode.mockImplementation(() => {
      throw new Error("invalid token");
    });

    renderWithProvider();

    expect(screen.getByTestId("user")).toHaveTextContent("no-user");
    expect(screen.getByTestId("token")).toHaveTextContent("no-token");
    expect(localStorage.getItem("token")).toBeNull();
  });
});

describe("AuthContext - auth:unauthorized event", () => {
  it("logs out and navigates to /login when the event fires", async () => {
    localStorage.setItem("token", "valid-jwt");
    jwtDecode.mockReturnValue(decodedPayload);

    renderWithProvider();

    expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");

    await act(async () => {
      window.dispatchEvent(new CustomEvent("auth:unauthorized"));
    });

    expect(screen.getByTestId("login-sentinel")).toBeInTheDocument();
    expect(localStorage.getItem("token")).toBeNull();
  });

  it("preserves the originating route as state.from so login can redirect back", async () => {
    localStorage.setItem("token", "valid-jwt");
    jwtDecode.mockReturnValue(decodedPayload);

    renderWithProvider({ initialEntries: ["/booking/42"] });

    await act(async () => {
      window.dispatchEvent(new CustomEvent("auth:unauthorized"));
    });

    expect(screen.getByTestId("login-sentinel")).toBeInTheDocument();
    expect(screen.getByTestId("login-from")).toHaveTextContent("/booking/42");
  });
});
