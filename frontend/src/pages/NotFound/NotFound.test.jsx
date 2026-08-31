import { customRender, screen } from "../../test/test-utils";
import NotFound from "./NotFound";

describe("NotFound", () => {
  it("shows a Spanish not-found message and a link back home", () => {
    customRender(<NotFound />);

    expect(
      screen.getByText("La página que buscás no existe.")
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Volver al inicio" })).toHaveAttribute(
      "href",
      "/"
    );
  });
});
