import { fireEvent, render, screen } from "@testing-library/react";
import WhatsAppButton from "./WhatsAppButton";

const HANDOFF_URL =
	"https://wa.me/5491122334455?text=Hola%2C%20quiero%20hacer%20una%20consulta%20sobre%20un%20alojamiento%20de%20TuHospedaje.";

function getHandoffLink() {
	return screen.getByRole("link", { name: "Contactar por WhatsApp" });
}

describe("WhatsAppButton - universal visibility", () => {
	afterEach(() => {
		vi.unstubAllEnvs();
	});

	it("renders no WhatsApp control without configuration", () => {
		vi.stubEnv("VITE_WHATSAPP_NUMBER", "");
		const { container } = render(<WhatsAppButton />);

		expect(container).toBeEmptyDOMElement();
		expect(
			screen.queryByRole("link", { name: "Contactar por WhatsApp" }),
		).toBeNull();
		expect(screen.queryByRole("button")).toBeNull();
		expect(screen.queryByRole("alert")).toBeNull();
	});

	it("renders the same accessible control regardless of authentication state", () => {
		vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");
		render(<WhatsAppButton />);

		expect(getHandoffLink()).toBeInTheDocument();
	});
});

describe("WhatsAppButton - valid configuration handoff", () => {
	afterEach(() => {
		vi.unstubAllEnvs();
	});

	it("uses a validated wa.me link with secure new-window attributes", () => {
		vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");
		render(<WhatsAppButton />);

		const link = getHandoffLink();
		expect(link).toHaveAttribute("href", HANDOFF_URL);
		expect(link).toHaveAttribute("target", "_blank");
		expect(link).toHaveAttribute("rel", "noopener noreferrer");
	});

	it("reports only that the handoff was initiated, never that a message was sent or delivered", () => {
		vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");
		render(<WhatsAppButton />);

		fireEvent.click(getHandoffLink());

		const feedback = screen.getByRole("status");
		expect(feedback).toHaveTextContent(/abrió/i);
		expect(feedback.textContent).not.toMatch(/enviad|entregad|leíd/i);
	});

	it("keeps reporting handoff initiation when the link is activated again", () => {
		vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");
		render(<WhatsAppButton />);

		const link = getHandoffLink();
		fireEvent.click(link);
		fireEvent.click(link);

		expect(screen.getByRole("status")).toHaveTextContent(/abrió/i);
	});
});

describe("WhatsAppButton - invalid or missing configuration", () => {
	afterEach(() => {
		vi.unstubAllEnvs();
	});

	it.each([
		["missing", ""],
		["too short", "1234567"],
		["too long", "1234567890123456"],
		["leading zero", "0491122334455"],
		["non-digit characters", "54911abc34455"],
	])("renders no WhatsApp control for %s configuration", (_label, value) => {
		vi.stubEnv("VITE_WHATSAPP_NUMBER", value);
		const { container } = render(<WhatsAppButton />);

		expect(container).toBeEmptyDOMElement();
		expect(
			screen.queryByRole("link", { name: "Contactar por WhatsApp" }),
		).toBeNull();
		expect(screen.queryByRole("button")).toBeNull();
		expect(screen.queryByRole("alert")).toBeNull();
	});
});

describe("WhatsAppButton - fixed lower-right placement", () => {
	afterEach(() => {
		vi.unstubAllEnvs();
	});

	it("keeps the semantic control inside the fixed-position wrapper", () => {
		vi.stubEnv("VITE_WHATSAPP_NUMBER", "5491122334455");
		render(<WhatsAppButton />);

		const link = getHandoffLink();
		expect(link.closest(".whatsapp-button-wrapper")).not.toBeNull();
		expect(link).toHaveClass("whatsapp-button");
	});
});
