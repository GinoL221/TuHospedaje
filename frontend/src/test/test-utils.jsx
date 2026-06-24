import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { AuthContext } from "../context/AuthContext";

export const mockUser = {
  firstName: "Test",
  lastName: "User",
  email: "test@example.com",
  role: "USER",
  imageUrl: null,
};
export const mockAdmin = { ...mockUser, email: "admin@example.com", role: "ADMIN" };

export function makeAuthValue(overrides = {}) {
  return {
    token: "fake-token",
    user: mockUser,
    loading: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    ...overrides,
  };
}

// authValue: pass null for logged-out; pass makeAuthValue({...}) to customize.
// Pass makeAuthValue({ loading: true }) to simulate the in-flight /me bootstrap.
// route/initialEntries: drive MemoryRouter (LoginPage reads location.state.from.pathname).
export function customRender(ui, { authValue, route = "/", initialEntries, ...options } = {}) {
  const entries = initialEntries ?? [route];
  const value =
    authValue === undefined
      ? makeAuthValue()
      : authValue === null
        ? makeAuthValue({ token: null, user: null })
        : authValue;

  function Wrapper({ children }) {
    return (
      <MemoryRouter initialEntries={entries}>
        <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
      </MemoryRouter>
    );
  }
  return { user: value, ...render(ui, { wrapper: Wrapper, ...options }) };
}

export * from "@testing-library/react";
export { default as userEvent } from "@testing-library/user-event";
