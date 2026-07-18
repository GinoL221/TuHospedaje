import { customRender, screen } from "../../test/test-utils";
import Unauthorized from "./Unauthorized";

describe("Unauthorized", () => {
  it("shows a Spanish permission message and a link back home", () => {
    customRender(<Unauthorized />);

    expect(
      screen.getByText("No tenés permisos para acceder a esta página.")
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Volver al inicio" })).toHaveAttribute(
      "href",
      "/"
    );
  });
});
