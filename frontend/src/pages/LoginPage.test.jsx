import { Routes, Route } from "react-router-dom";
import { customRender, screen, userEvent, makeAuthValue } from "../test/test-utils";
import LoginPage from "./LoginPage";

function FavoritesSentinel() {
  return <div data-testid="favorites-sentinel">favorites page</div>;
}

function HomeSentinel() {
  return <div data-testid="home-sentinel">home page</div>;
}

// NOTE: LoginPage's <label> elements have no `htmlFor`/`id` association
// with their <input>s, so `getByLabelText` cannot locate them. This is a
// pre-existing accessibility gap in production markup (SUSPICIOUS, flagged
// per spec Risks policy) — not fixed here since fixing production code is
// out of scope for this characterization PR. Tests select inputs by their
// `name` attribute instead.
function renderLoginPage({ authValue, initialEntries } = {}) {
  return customRender(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/favorites" element={<FavoritesSentinel />} />
      <Route path="/" element={<HomeSentinel />} />
    </Routes>,
    { authValue, initialEntries }
  );
}

function getEmailInput(container) {
  return container.querySelector('input[name="email"]');
}

function getPasswordInput(container) {
  return container.querySelector('input[name="password"]');
}

async function fillAndSubmit(user, container, { email, password } = {}) {
  if (email !== undefined) {
    await user.type(getEmailInput(container), email);
  }
  if (password !== undefined) {
    await user.type(getPasswordInput(container), password);
  }
  await user.click(screen.getByRole("button", { name: /iniciar sesión/i }));
}

describe("LoginPage - render", () => {
  it("renders the email and password fields and the submit button", () => {
    const { container } = renderLoginPage({ initialEntries: ["/login"] });

    expect(getEmailInput(container)).toBeInTheDocument();
    expect(getPasswordInput(container)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Iniciar sesión" })).toBeInTheDocument();
  });
});

describe("LoginPage - empty submit", () => {
  it("shows field errors and does not call login", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue();
    const { container } = renderLoginPage({ authValue, initialEntries: ["/login"] });

    await fillAndSubmit(user, container, {});

    expect(screen.getByText("El email es obligatorio")).toBeInTheDocument();
    expect(screen.getByText("La contraseña es obligatoria")).toBeInTheDocument();
    expect(authValue.login).not.toHaveBeenCalled();
  });
});

describe("LoginPage - invalid email format", () => {
  it("shows an email format error", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue();
    const { container } = renderLoginPage({ authValue, initialEntries: ["/login"] });

    await fillAndSubmit(user, container, { email: "notanemail", password: "secret" });

    expect(screen.getByText("El email no es válido")).toBeInTheDocument();
    expect(authValue.login).not.toHaveBeenCalled();
  });
});

describe("LoginPage - successful login", () => {
  it("calls login and navigates to location.state.from with replace", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue({ login: vi.fn().mockResolvedValue(undefined) });
    const { container } = renderLoginPage({
      authValue,
      initialEntries: [
        { pathname: "/login", state: { from: { pathname: "/favorites" } } },
      ],
    });

    await fillAndSubmit(user, container, { email: "test@example.com", password: "secret" });

    expect(authValue.login).toHaveBeenCalledWith("test@example.com", "secret");
    expect(await screen.findByTestId("favorites-sentinel")).toBeInTheDocument();
  });

  it("navigates to / when no from state is present", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue({ login: vi.fn().mockResolvedValue(undefined) });
    const { container } = renderLoginPage({ authValue, initialEntries: ["/login"] });

    await fillAndSubmit(user, container, { email: "test@example.com", password: "secret" });

    expect(await screen.findByTestId("home-sentinel")).toBeInTheDocument();
  });
});

describe("LoginPage - failed login", () => {
  it("renders the server error message and stops loading", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue({
      login: vi.fn().mockRejectedValue(new Error("Credenciales inválidas")),
    });
    const { container } = renderLoginPage({ authValue, initialEntries: ["/login"] });

    await fillAndSubmit(user, container, { email: "test@example.com", password: "wrong" });

    expect(await screen.findByText("Credenciales inválidas")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Iniciar sesión" })).not.toBeDisabled();
  });
});

describe("LoginPage - redirect message from previous route", () => {
  it("renders the location.state.message above the form", () => {
    renderLoginPage({
      initialEntries: [
        {
          pathname: "/login",
          state: {
            message:
              "Necesitás iniciar sesión para continuar. Si no tenés cuenta, podés registrarte.",
          },
        },
      ],
    });

    expect(
      screen.getByText(
        "Necesitás iniciar sesión para continuar. Si no tenés cuenta, podés registrarte."
      )
    ).toBeInTheDocument();
  });
});
