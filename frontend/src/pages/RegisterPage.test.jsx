import { Routes, Route, useLocation } from "react-router-dom";
import { customRender, screen, userEvent, makeAuthValue } from "../test/test-utils";
import RegisterPage from "./RegisterPage";

function HomeSentinel() {
  return <div data-testid="home-sentinel">home page</div>;
}

function LoginSentinel() {
  const location = useLocation();
  return (
    <div data-testid="login-sentinel">
      login page, from: {location.state?.from?.pathname ?? "none"}
    </div>
  );
}

function renderRegisterPage({ authValue, initialEntries } = {}) {
  return customRender(
    <Routes>
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/login" element={<LoginSentinel />} />
      <Route path="/" element={<HomeSentinel />} />
    </Routes>,
    { authValue, route: "/register", initialEntries }
  );
}

const labelByField = {
  firstName: "Nombre",
  lastName: "Apellido",
  email: "Email",
  password: "Contraseña",
  confirmPassword: "Confirmar contraseña",
};

function getInput(name) {
  return screen.getByLabelText(labelByField[name]);
}

function getPwCheckItem(text) {
  return screen.getByText(text, { exact: false });
}

const validForm = {
  firstName: "Test",
  lastName: "User",
  email: "test@example.com",
  password: "Secret1",
  confirmPassword: "Secret1",
};

async function fillForm(user, overrides = {}) {
  const form = { ...validForm, ...overrides };
  if (form.firstName) await user.type(getInput("firstName"), form.firstName);
  if (form.lastName) await user.type(getInput("lastName"), form.lastName);
  if (form.email) await user.type(getInput("email"), form.email);
  if (form.password) await user.type(getInput("password"), form.password);
  if (form.confirmPassword) {
    await user.type(getInput("confirmPassword"), form.confirmPassword);
  }
}

describe("RegisterPage - render", () => {
  it("renders all form fields and the submit button", () => {
    renderRegisterPage();

    expect(getInput("firstName")).toBeInTheDocument();
    expect(getInput("lastName")).toBeInTheDocument();
    expect(getInput("email")).toBeInTheDocument();
    expect(getInput("password")).toBeInTheDocument();
    expect(getInput("confirmPassword")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Crear cuenta" })).toBeInTheDocument();
  });
});

describe("RegisterPage - password complexity checklist", () => {
  it("toggles minLength/hasUpper/hasNumber as the password value changes", async () => {
    const user = userEvent.setup();
    renderRegisterPage();

    const passwordInput = getInput("password");
    await user.type(passwordInput, "a");

    expect(getPwCheckItem("Al menos 6 caracteres")).toHaveClass("pw-check-fail");
    expect(getPwCheckItem("Una letra mayúscula")).toHaveClass("pw-check-fail");
    expect(getPwCheckItem("Un número")).toHaveClass("pw-check-fail");

    await user.type(passwordInput, "bcdeF9");

    expect(getPwCheckItem("Al menos 6 caracteres")).toHaveClass("pw-check-ok");
    expect(getPwCheckItem("Una letra mayúscula")).toHaveClass("pw-check-ok");
    expect(getPwCheckItem("Un número")).toHaveClass("pw-check-ok");
  });
});

describe("RegisterPage - empty submit", () => {
  it("shows field errors and does not call register", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue();
    renderRegisterPage({ authValue });

    await user.click(screen.getByRole("button", { name: "Crear cuenta" }));

    expect(screen.getByText("El nombre es obligatorio")).toBeInTheDocument();
    expect(screen.getByText("El apellido es obligatorio")).toBeInTheDocument();
    expect(screen.getByText("El email es obligatorio")).toBeInTheDocument();
    expect(screen.getByText("La contraseña es obligatoria")).toBeInTheDocument();
    expect(screen.getByText("Confirmá tu contraseña")).toBeInTheDocument();
    expect(authValue.register).not.toHaveBeenCalled();
  });
});

describe("RegisterPage - mismatched confirm password", () => {
  it("blocks submit with a mismatch error", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue();
    renderRegisterPage({ authValue });

    await fillForm(user, { confirmPassword: "Different1" });
    await user.click(screen.getByRole("button", { name: "Crear cuenta" }));

    expect(screen.getByText("Las contraseñas no coinciden")).toBeInTheDocument();
    expect(authValue.register).not.toHaveBeenCalled();
  });
});

describe("RegisterPage - duplicate email error", () => {
  it("attaches the error to the email field, not the generic banner", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue({
      register: vi
        .fn()
        .mockRejectedValue(new Error("Ese email ya está registrado")),
    });
    renderRegisterPage({ authValue });

    await fillForm(user);
    await user.click(screen.getByRole("button", { name: "Crear cuenta" }));

    expect(await screen.findByText("Ese email ya está registrado")).toBeInTheDocument();
    expect(getInput("email")).toHaveClass("input-error");
  });
});

describe("RegisterPage - generic registration failure", () => {
  it("renders the error in the generic banner when message does not match the email-duplicate pattern", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue({
      register: vi.fn().mockRejectedValue(new Error("Error de servidor")),
    });
    renderRegisterPage({ authValue });

    await fillForm(user);
    await user.click(screen.getByRole("button", { name: "Crear cuenta" }));

    expect(await screen.findByText("Error de servidor")).toBeInTheDocument();
    expect(getInput("email")).not.toHaveClass("input-error");
  });
});

describe("RegisterPage - reciprocal login link preserves redirect origin", () => {
  it("preserves location.state.from on the 'Iniciá sesión' link, so switching to login can redirect back", async () => {
    const user = userEvent.setup();
    renderRegisterPage({
      initialEntries: [
        {
          pathname: "/register",
          state: { from: { pathname: "/favorites" } },
        },
      ],
    });

    const loginLink = screen.getByRole("link", { name: "Iniciá sesión" });
    await user.click(loginLink);

    // RegisterPage is often reached via a redirect-with-from (e.g. from
    // RequireAuth). If the user switches to login from there, the original
    // `from` must be forwarded so LoginPage can still redirect back to it
    // instead of falling back to "/".
    expect(await screen.findByTestId("login-sentinel")).toHaveTextContent(
      "from: /favorites"
    );
  });
});

describe("RegisterPage - successful registration", () => {
  it("calls register with form data and navigates to /", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue({ register: vi.fn().mockResolvedValue(undefined) });
    renderRegisterPage({ authValue });

    await fillForm(user);
    await user.click(screen.getByRole("button", { name: "Crear cuenta" }));

    expect(authValue.register).toHaveBeenCalledWith(
      "Test",
      "User",
      "test@example.com",
      "Secret1"
    );
    expect(await screen.findByTestId("home-sentinel")).toBeInTheDocument();
  });
});
