import { render, screen, act, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { StrictMode, useState } from "react";
import { AuthProvider } from "./AuthContext";
import { useAuth } from "../hooks/useAuth";
import { get, post, bootstrapCsrf } from "../services/api";

vi.mock("../services/api");

function Consumer() {
  const { user, loading, login, register, logout, logoutError } = useAuth();
  const [lastError, setLastError] = useState("");
  return <div>
    <span data-testid="status">{loading ? "loading" : user ? user.email : "anonymous"}</span>
    <span data-testid="last-error">{lastError}</span>
    <span data-testid="logout-error">{logoutError}</span>
    <button onClick={() => login("test@example.com", "secret").catch((e) => setLastError(e.message))}>login</button>
    <button onClick={() => register("Test", "User", "test@example.com", "secret").catch((e) => setLastError(e.message))}>register</button>
    <button onClick={() => logout().catch(() => {})}>logout</button>
  </div>;
}

const user = { email: "test@example.com", firstName: "Test", role: "USER" };

function renderProvider({ strictMode = false } = {}) {
  const provider = <MemoryRouter initialEntries={["/"]}><AuthProvider><Routes>
    <Route path="*" element={<Consumer />} />
  </Routes></AuthProvider></MemoryRouter>;
  return render(strictMode ? <StrictMode>{provider}</StrictMode> : provider);
}

beforeEach(() => vi.clearAllMocks());

describe("authenticated CSRF sequencing", () => {
  it("does not publish login or register before bootstrap resolves", async () => {
    get.mockRejectedValue(new Error("No session"));
    post.mockResolvedValue(user);
    let resolveBootstrap;
    bootstrapCsrf.mockImplementation(() => new Promise((resolve) => { resolveBootstrap = resolve; }));
    renderProvider();
    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("anonymous"));

    await act(async () => screen.getByText("login").click());
    expect(screen.getByTestId("status")).not.toHaveTextContent(user.email);
    resolveBootstrap();
    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent(user.email));
  });

  it("keeps the user anonymous when bootstrap fails and preserves user on logout failure", async () => {
    get.mockRejectedValue(new Error("No session"));
    post.mockResolvedValue(user);
    bootstrapCsrf.mockRejectedValue(new Error("CSRF unavailable"));
    renderProvider();
    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("anonymous"));
    await act(async () => screen.getByText("register").click());
    expect(screen.getByTestId("status")).not.toHaveTextContent("stale@example.com");

    bootstrapCsrf.mockResolvedValue(undefined);
    await act(async () => screen.getByText("login").click());
    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent(user.email));
    post.mockRejectedValue(new Error("Logout rejected"));
    await act(async () => screen.getByText("logout").click());
    expect(screen.getByTestId("status")).toHaveTextContent(user.email);
    expect(screen.getByTestId("logout-error")).toHaveTextContent("Logout rejected");
  });

  it("surfaces a clear retry-via-login message when register succeeds but bootstrap fails", async () => {
    get.mockRejectedValue(new Error("No session"));
    post.mockResolvedValue(user);
    bootstrapCsrf.mockRejectedValue(new Error("CSRF unavailable"));
    renderProvider();
    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("anonymous"));

    await act(async () => screen.getByText("register").click());
    expect(screen.getByTestId("last-error")).not.toHaveTextContent("email ya está registrado");
    expect(screen.getByTestId("last-error")).toHaveTextContent(/iniciar sesión/i);
  });

  it("does not publish a late /me response before the current bootstrap resolves", async () => {
    let resolveMe;
    let resolveBootstrap;
    get.mockImplementation(() => new Promise((resolve) => { resolveMe = resolve; }));
    bootstrapCsrf.mockImplementation(() => new Promise((resolve) => { resolveBootstrap = resolve; }));
    renderProvider();

    resolveMe(user);
    await act(async () => {});
    expect(screen.getByTestId("status")).not.toHaveTextContent(user.email);

    resolveBootstrap();
    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent(user.email));
  });

  it("ignores the first StrictMode auth response when the duplicate effect is newer", async () => {
    const responses = [];
    get.mockImplementation(() => new Promise((resolve) => responses.push(resolve)));
    bootstrapCsrf.mockResolvedValue(undefined);
    renderProvider({ strictMode: true });

    await waitFor(() => expect(responses).toHaveLength(2));
    responses[0]({ ...user, email: "stale@example.com" });
    await act(async () => {});
    expect(screen.getByTestId("status")).not.toHaveTextContent("stale@example.com");

    responses[1](user);
    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent(user.email));
  });
});
