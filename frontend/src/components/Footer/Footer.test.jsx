import { render, screen } from "@testing-library/react";
import Footer from "./Footer";

describe("Footer - content", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("renders the official isologotype with alt text inside the brand area", () => {
    render(<Footer />);

    const logo = screen.getByRole("img", { name: "TuHospedaje" });

    expect(logo).toHaveAttribute(
      "src",
      expect.stringContaining("TuHospedaje_Isologotipo.png"),
    );
    expect(logo.closest(".footer-brand")).toContainElement(logo);
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

  it("mounts the single always-visible WhatsApp control inside the footer", () => {
    render(<Footer />);

    const footer = screen.getByRole("contentinfo");
    const whatsappButton = screen.getByRole("button", { name: "Contactar por WhatsApp" });

    expect(footer).toContainElement(whatsappButton);
    expect(screen.getAllByRole("button", { name: "Contactar por WhatsApp" })).toHaveLength(1);
  });
});
