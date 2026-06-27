import { customRender, screen, makeAuthValue } from "../../test/test-utils";
import Header from "./Header";

describe("Header - authenticated user", () => {
  it("shows 'Mis reservas' link pointing to /my-reservations", () => {
    customRender(<Header />);

    expect(screen.getByRole("link", { name: "Mis reservas" })).toHaveAttribute(
      "href",
      "/my-reservations"
    );
  });

  it("shows the user's first name", () => {
    customRender(<Header />);

    expect(screen.getByText("Test")).toBeInTheDocument();
  });

  it("shows the logout button and hides login/register links", () => {
    customRender(<Header />);

    expect(screen.getByRole("button", { name: "Cerrar sesión" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Iniciar sesión" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Crear cuenta" })).not.toBeInTheDocument();
  });
});

describe("Header - unauthenticated user", () => {
  it("does not show 'Mis reservas' link", () => {
    customRender(<Header />, { authValue: null });

    expect(screen.queryByRole("link", { name: "Mis reservas" })).not.toBeInTheDocument();
  });

  it("shows login and register links", () => {
    customRender(<Header />, { authValue: null });

    expect(screen.getByRole("link", { name: "Iniciar sesión" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Crear cuenta" })).toBeInTheDocument();
  });

  it("does not show the logout button", () => {
    customRender(<Header />, { authValue: null });

    expect(screen.queryByRole("button", { name: "Cerrar sesión" })).not.toBeInTheDocument();
  });
});
