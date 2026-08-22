import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import WhatsAppButton from "./WhatsAppButton";

function makeFakeWindow({ throwOnLocation = false } = {}) {
  const fakeWindow = { opener: "not-null" };
  Object.defineProperty(fakeWindow, "location", {
    set() {
      if (throwOnLocation) {
        throw new Error("blocked by browser policy");
      }
    },
    configurable: true,
  });
  return fakeWindow;
}

describe("WhatsAppButton - universal visibility", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.restoreAllMocks();
  });

  it("renders an accessible button for anonymous visitors even without configuration", () => {
    vi.stubEnv("VITE_WHATSAPP_NUMBER", "");
    render(<WhatsAppButton />);
    expect(screen.getByRole("button", { name: "Contactar por WhatsApp" })).toBeInTheDocument();
  });

  it("renders the same accessible control regardless of authentication state", () => {
    vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");
    render(<WhatsAppButton />);
    expect(screen.getByRole("button", { name: "Contactar por WhatsApp" })).toBeInTheDocument();
  });
});

describe("WhatsAppButton - valid configuration handoff", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.restoreAllMocks();
  });

  it("opens a blank window synchronously, isolates opener, and assigns the wa.me URL", async () => {
    const user = userEvent.setup();
    const fakeWindow = makeFakeWindow();
    const openSpy = vi.spyOn(window, "open").mockReturnValue(fakeWindow);
    vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");

    render(<WhatsAppButton />);
    await user.click(screen.getByRole("button", { name: "Contactar por WhatsApp" }));

    expect(openSpy).toHaveBeenCalledWith("", "_blank");
    expect(fakeWindow.opener).toBeNull();
  });

  it("reports only that the handoff was initiated, never that a message was sent or delivered", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "open").mockReturnValue(makeFakeWindow());
    vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");

    render(<WhatsAppButton />);
    await user.click(screen.getByRole("button", { name: "Contactar por WhatsApp" }));

    const feedback = await screen.findByRole("status");
    expect(feedback).toHaveTextContent(/abrió/i);
    expect(feedback.textContent).not.toMatch(/enviad|entregad|leíd/i);
  });
});

describe("WhatsAppButton - invalid or missing configuration", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.restoreAllMocks();
  });

  it.each([
    ["missing", ""],
    ["too short", "1234567"],
    ["too long", "1234567890123456"],
    ["leading zero", "0491122334455"],
    ["non-digit characters", "54911abc34455"],
  ])("never opens a URL for %s configuration", async (_label, value) => {
    const user = userEvent.setup();
    const openSpy = vi.spyOn(window, "open").mockReturnValue(makeFakeWindow());
    vi.stubEnv("VITE_WHATSAPP_NUMBER", value);

    render(<WhatsAppButton />);
    await user.click(screen.getByRole("button", { name: "Contactar por WhatsApp" }));

    expect(openSpy).not.toHaveBeenCalled();
    expect(await screen.findByRole("alert")).toHaveTextContent(/no está disponible/i);
  });
});

describe("WhatsAppButton - detectable handoff failures", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.restoreAllMocks();
  });

  it("handles window.open returning null without throwing", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "open").mockReturnValue(null);
    vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");

    render(<WhatsAppButton />);
    await expect(
      user.click(screen.getByRole("button", { name: "Contactar por WhatsApp" })),
    ).resolves.not.toThrow();

    expect(await screen.findByRole("alert")).toHaveTextContent(/no pudimos abrir whatsapp/i);
  });

  it("catches a URL-assignment exception without crashing", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "open").mockReturnValue(makeFakeWindow({ throwOnLocation: true }));
    vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");

    render(<WhatsAppButton />);
    await expect(
      user.click(screen.getByRole("button", { name: "Contactar por WhatsApp" })),
    ).resolves.not.toThrow();

    expect(await screen.findByRole("alert")).toHaveTextContent(/no pudimos abrir whatsapp/i);
  });
});

describe("WhatsAppButton - fixed lower-right placement", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("keeps the semantic control inside the fixed-position wrapper", () => {
    vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");
    render(<WhatsAppButton />);

    const button = screen.getByRole("button", { name: "Contactar por WhatsApp" });
    expect(button.closest(".whatsapp-button-wrapper")).not.toBeNull();
    expect(button).toHaveClass("whatsapp-button");
  });
});
