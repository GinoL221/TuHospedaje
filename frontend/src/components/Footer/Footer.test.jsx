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

  it("mounts the single WhatsApp link inside the footer when configured", () => {
    vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");
    render(<Footer />);

    const footer = screen.getByRole("contentinfo");
    const whatsappLink = screen.getByRole("link", { name: "Contactar por WhatsApp" });

    expect(footer).toContainElement(whatsappLink);
    expect(screen.getAllByRole("link", { name: "Contactar por WhatsApp" })).toHaveLength(1);
  });
});
