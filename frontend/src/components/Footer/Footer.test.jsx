import { render, screen } from "@testing-library/react";
import Footer from "./Footer";

describe("Footer - content", () => {
  it("renders the brand logo with alt text", () => {
    render(<Footer />);
    expect(screen.getByRole("img", { name: "TuHospedaje" })).toBeInTheDocument();
  });

  it("renders the copyright notice", () => {
    render(<Footer />);
    expect(
      screen.getByText("© 2026 TuHospedaje. Todos los derechos reservados."),
    ).toBeInTheDocument();
  });

  it("renders Facebook and Instagram links pointing to the right destinations", () => {
    render(<Footer />);

    const facebookLink = screen.getByRole("link", {
      name: "Facebook",
    });
    expect(facebookLink).toHaveAttribute("href", "https://facebook.com");
    expect(facebookLink).toHaveAttribute("target", "_blank");

    const instagramLink = screen.getByRole("link", {
      name: "Instagram",
    });
    expect(instagramLink).toHaveAttribute("href", "https://instagram.com");
    expect(instagramLink).toHaveAttribute("target", "_blank");
  });
});
