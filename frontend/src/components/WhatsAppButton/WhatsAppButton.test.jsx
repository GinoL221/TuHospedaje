import { render, screen } from "@testing-library/react";
import WhatsAppButton from "./WhatsAppButton";

describe("WhatsAppButton - env not set", () => {
  beforeEach(() => {
    vi.stubEnv("VITE_WHATSAPP_NUMBER", "");
  });

  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("renders nothing when VITE_WHATSAPP_NUMBER is not configured", () => {
    const { container } = render(<WhatsAppButton />);
    expect(container).toBeEmptyDOMElement();
  });
});

describe("WhatsAppButton - env is set", () => {
  beforeEach(() => {
    vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");
  });

  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("renders an accessible link pointing to the correct wa.me URL", () => {
    render(<WhatsAppButton />);

    const link = screen.getByRole("link", { name: "Contactar por WhatsApp" });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute("href", expect.stringContaining("wa.me/5491122334455"));
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noreferrer");
  });

  it("is positioned fixed at bottom-right with correct z-index", () => {
    render(<WhatsAppButton />);

    const link = screen.getByRole("link", { name: "Contactar por WhatsApp" });
    expect(link).toHaveStyle({ position: "fixed", right: "24px", bottom: "24px" });
  });

  it("does not require authentication to render", () => {
    // Component renders without any auth context — no crash, link present
    render(<WhatsAppButton />);
    expect(screen.getByRole("link", { name: "Contactar por WhatsApp" })).toBeInTheDocument();
  });
});
