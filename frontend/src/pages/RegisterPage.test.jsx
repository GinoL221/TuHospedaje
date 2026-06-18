import { Routes, Route } from "react-router-dom";
import { customRender, screen, userEvent, makeAuthValue } from "../test/test-utils";
import RegisterPage from "./RegisterPage";

function HomeSentinel() {
  return <div data-testid="home-sentinel">home page</div>;
}

// NOTE: RegisterPage's <label> elements have no `htmlFor`/`id` association
// with their <input>s, so `getByLabelText` cannot locate them. This is a
// pre-existing accessibility gap in production markup (SUSPICIOUS, flagged
// per spec Risks policy) — not fixed here since fixing production code is
// out of scope for this characterization PR. Tests select inputs by their
// `name` attribute instead.
function renderRegisterPage({ authValue } = {}) {
  return customRender(
    <Routes>
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/" element={<HomeSentinel />} />
    </Routes>,
    { authValue, route: "/register" }
  );
}

function getInput(container, name) {
  return container.querySelector(`input[name="${name}"]`);
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

async function fillForm(user, container, overrides = {}) {
  const form = { ...validForm, ...overrides };
  if (form.firstName) await user.type(getInput(container, "firstName"), form.firstName);
  if (form.lastName) await user.type(getInput(container, "lastName"), form.lastName);
  if (form.email) await user.type(getInput(container, "email"), form.email);
  if (form.password) await user.type(getInput(container, "password"), form.password);
  if (form.confirmPassword) {
    await user.type(getInput(container, "confirmPassword"), form.confirmPassword);
  }
}

describe("RegisterPage - render", () => {
  it("renders all form fields and the submit button", () => {
    const { container } = renderRegisterPage();

    expect(getInput(container, "firstName")).toBeInTheDocument();
    expect(getInput(container, "lastName")).toBeInTheDocument();
    expect(getInput(container, "email")).toBeInTheDocument();
    expect(getInput(container, "password")).toBeInTheDocument();
    expect(getInput(container, "confirmPassword")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Crear cuenta" })).toBeInTheDocument();
  });
});

describe("RegisterPage - password complexity checklist", () => {
  it("toggles minLength/hasUpper/hasNumber as the password value changes", async () => {
    const user = userEvent.setup();
    const { container } = renderRegisterPage();

    const passwordInput = getInput(container, "password");
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
    const { container } = renderRegisterPage({ authValue });

    await fillForm(user, container, { confirmPassword: "Different1" });
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
    const { container } = renderRegisterPage({ authValue });

    await fillForm(user, container);
    await user.click(screen.getByRole("button", { name: "Crear cuenta" }));

    expect(await screen.findByText("Ese email ya está registrado")).toBeInTheDocument();
    expect(getInput(container, "email")).toHaveClass("input-error");
  });
});

describe("RegisterPage - generic registration failure", () => {
  it("renders the error in the generic banner when message does not match the email-duplicate pattern", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue({
      register: vi.fn().mockRejectedValue(new Error("Error de servidor")),
    });
    const { container } = renderRegisterPage({ authValue });

    await fillForm(user, container);
    await user.click(screen.getByRole("button", { name: "Crear cuenta" }));

    expect(await screen.findByText("Error de servidor")).toBeInTheDocument();
    expect(getInput(container, "email")).not.toHaveClass("input-error");
  });
});

describe("RegisterPage - successful registration", () => {
  it("calls register with form data and navigates to /", async () => {
    const user = userEvent.setup();
    const authValue = makeAuthValue({ register: vi.fn().mockResolvedValue(undefined) });
    const { container } = renderRegisterPage({ authValue });

    await fillForm(user, container);
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
