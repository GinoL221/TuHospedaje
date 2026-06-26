import { Routes, Route, useLocation } from "react-router-dom";
import { customRender, screen, makeAuthValue } from "../test/test-utils";
import RequireAuth from "./RequireAuth";

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

function ProtectedSentinel() {
  return <div data-testid="protected-sentinel">protected content</div>;
}

function renderGuardedRoute({ authValue, route = "/protected" } = {}) {
  return customRender(
    <Routes>
      <Route path="/login" element={<LoginSentinel />} />
      <Route element={<RequireAuth />}>
        <Route path="/protected" element={<ProtectedSentinel />} />
      </Route>
    </Routes>,
    { authValue, route }
  );
}

describe("RequireAuth - unauthenticated user", () => {
  it("redirects to /login passing state.from and a Spanish prompt message", () => {
    renderGuardedRoute({ authValue: null });

    expect(screen.getByTestId("login-sentinel")).toBeInTheDocument();
    expect(screen.queryByTestId("protected-sentinel")).not.toBeInTheDocument();
    expect(screen.getByTestId("redirect-from")).toHaveTextContent("/protected");
    expect(screen.getByTestId("redirect-message")).toHaveTextContent(
      "Necesitás iniciar sesión para continuar. Si no tenés cuenta, podés registrarte."
    );
  });
});

describe("RequireAuth - authenticated user", () => {
  it("renders the nested Outlet content, no redirect occurs", () => {
    renderGuardedRoute();

    expect(screen.getByTestId("protected-sentinel")).toBeInTheDocument();
    expect(screen.queryByTestId("login-sentinel")).not.toBeInTheDocument();
  });
});

describe("RequireAuth - session bootstrap still in flight (loading=true, user=null)", () => {
  it("does not redirect to /login while loading, even though user is still null", () => {
    renderGuardedRoute({ authValue: makeAuthValue({ user: null, loading: true }) });

    expect(screen.queryByTestId("login-sentinel")).not.toBeInTheDocument();
    expect(screen.queryByTestId("protected-sentinel")).not.toBeInTheDocument();
  });
});
