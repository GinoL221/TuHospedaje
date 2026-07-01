import { Routes, Route, useLocation } from "react-router-dom";
import { customRender, screen, makeAuthValue, mockAdmin } from "../test/test-utils";
import RequireAdmin from "./RequireAdmin";

function LoginSentinel() {
  const location = useLocation();
  return (
    <div data-testid="login-sentinel">
      login page
      <span data-testid="redirect-from">{location.state?.from?.pathname}</span>
      <span data-testid="redirect-message">{location.state?.message}</span>
    </div>
  );
}

function UnauthorizedSentinel() {
  return <div data-testid="unauthorized-sentinel">unauthorized page</div>;
}

function AdminSentinel() {
  return <div data-testid="admin-sentinel">admin content</div>;
}

function renderGuardedRoute({ authValue, route = "/admin" } = {}) {
  return customRender(
    <Routes>
      <Route path="/login" element={<LoginSentinel />} />
      <Route path="/unauthorized" element={<UnauthorizedSentinel />} />
      <Route element={<RequireAdmin />}>
        <Route path="/admin" element={<AdminSentinel />} />
      </Route>
    </Routes>,
    { authValue, route }
  );
}

describe("RequireAdmin - unauthenticated user", () => {
  it("redirects to /login passing state.from and a Spanish prompt message", () => {
    renderGuardedRoute({ authValue: null });

    expect(screen.getByTestId("login-sentinel")).toBeInTheDocument();
    expect(screen.queryByTestId("admin-sentinel")).not.toBeInTheDocument();
    expect(screen.getByTestId("redirect-from")).toHaveTextContent("/admin");
    expect(screen.getByTestId("redirect-message")).toHaveTextContent(
      "Necesitás iniciar sesión para continuar. Si no tenés cuenta, podés registrarte."
    );
  });
});

describe("RequireAdmin - authenticated non-admin user", () => {
  it("redirects to /unauthorized without exposing the admin content", () => {
    renderGuardedRoute();

    expect(screen.getByTestId("unauthorized-sentinel")).toBeInTheDocument();
    expect(screen.queryByTestId("admin-sentinel")).not.toBeInTheDocument();
  });
});

describe("RequireAdmin - authenticated admin user", () => {
  it("renders the nested Outlet content, no redirect occurs", () => {
    renderGuardedRoute({ authValue: makeAuthValue({ user: mockAdmin }) });

    expect(screen.getByTestId("admin-sentinel")).toBeInTheDocument();
    expect(screen.queryByTestId("login-sentinel")).not.toBeInTheDocument();
    expect(screen.queryByTestId("unauthorized-sentinel")).not.toBeInTheDocument();
  });
});

describe("RequireAdmin - session bootstrap still in flight (loading=true, user=null)", () => {
  it("does not redirect to /login while loading, even though user is still null", () => {
    renderGuardedRoute({ authValue: makeAuthValue({ user: null, loading: true }) });

    expect(screen.queryByTestId("login-sentinel")).not.toBeInTheDocument();
    expect(screen.queryByTestId("admin-sentinel")).not.toBeInTheDocument();
  });
});
