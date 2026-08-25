import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const readCss = (path) => readFileSync(resolve(process.cwd(), path), "utf8");

function luminance(hex) {
	const channels = hex.match(/[a-f\d]{2}/gi).map((channel) => {
		const value = Number.parseInt(channel, 16) / 255;
		return value <= 0.04045
			? value / 12.92
			: ((value + 0.055) / 1.055) ** 2.4;
	});

	return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
}

function contrast(foreground, background) {
	const lighter = Math.max(luminance(foreground), luminance(background));
	const darker = Math.min(luminance(foreground), luminance(background));
	return (lighter + 0.05) / (darker + 0.05);
}

describe("primary action contrast contract", () => {
	it("uses approved brand tokens with AA text and visible focus contrast", () => {
		const css = readCss("src/App.css");

		expect(css).toMatch(/--action-primary-bg:\s*var\(--secondary\)/);
		expect(css).toMatch(/--action-primary-fg:\s*var\(--bg\)/);
		expect(css).toMatch(/--action-primary-hover-bg:\s*var\(--text\)/);
		expect(css).toMatch(/--action-primary-focus:\s*var\(--accent\)/);
		expect(contrast("#F4F4F9", "#264653")).toBeGreaterThanOrEqual(4.5);
		expect(contrast("#F4F4F9", "#333333")).toBeGreaterThanOrEqual(4.5);
		expect(contrast("#2A9D8F", "#264653")).toBeGreaterThanOrEqual(3);
		expect(contrast("#2A9D8F", "#F4F4F9")).toBeGreaterThanOrEqual(3);
	});

	it.each([
		["authentication", "src/assets/css/auth.css", ".register-box button"],
		["booking form", "src/pages/Booking/BookingPage.css", '.booking-form button[type="submit"]'],
		["booking confirmation", "src/pages/Booking/BookingConfirmation.css", ".confirmation-actions .btn-primary"],
		["lodging reservation", "src/pages/ProductDetail/ProductDetail.css", ".btn-reserve"],
		["review submission", "src/components/ReviewsSection/ReviewsSection.css", ".btn-submit-review"],
		["admin add", "src/pages/Admin/Admin.css", ".btn-fab"],
		["admin save", "src/pages/Admin/Admin.css", ".btn-save"],
	])("applies the semantic contract to the %s CTA", (_name, path, selector) => {
		const css = readCss(path);
		const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
		const rule = css.match(new RegExp(`${escapedSelector}\\s*\\{([^}]*)\\}`))?.[1];

		expect(rule).toBeDefined();
		expect(rule).toMatch(/background(?:-color)?:\s*var\(--action-primary-bg\)/);
		expect(rule).toMatch(/color:\s*var\(--action-primary-fg\)/);
		expect(rule).not.toMatch(/background(?:-color)?:\s*var\(--primary\)/);
		});

	it("keeps the Header registration CTA distinct against the petroleum header", () => {
		const css = readCss("src/layout.css");
		const rule = css.match(
			/\.site-header \.header-register-cta\s*\{([^}]*)\}/,
		)?.[1];

		expect(rule).toBeDefined();
		expect(rule).toMatch(/min-height:\s*44px/);
		expect(rule).toMatch(/background-color:\s*var\(--bg\)/);
		expect(rule).toMatch(/color:\s*var\(--secondary\)/);
		expect(rule).toMatch(/border:\s*2px solid var\(--action-primary-accent\)/);
		expect(rule).not.toMatch(/background-color:\s*var\(--action-primary-bg\)/);
		expect(contrast("#264653", "#F4F4F9")).toBeGreaterThanOrEqual(4.5);
		expect(contrast("#FF6B35", "#264653")).toBeGreaterThanOrEqual(3);
		expect(contrast("#2A9D8F", "#264653")).toBeGreaterThanOrEqual(3);
		expect(contrast("#2A9D8F", "#F4F4F9")).toBeGreaterThanOrEqual(3);
	});

	it.each([
		["lodging share", "src/pages/ProductDetail/ProductDetail.css", ".btn-share"],
		["empty reservations", "src/pages/MyReservations/MyReservationsPage.css", ".reservations-back"],
	])("keeps the %s secondary action readable and bounded", (_name, path, selector) => {
		const css = readCss(path);
		const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
		const rules = [...css.matchAll(new RegExp(`${escapedSelector}\\s*\\{([^}]*)\\}`, "g"))];
		const rule = rules.find((match) =>
			/background(?:-color)?:\s*var\(--bg\)/.test(match[1]),
		)?.[1];

		expect(rule).toMatch(/background(?:-color)?:\s*var\(--bg\)/);
		expect(rule).toMatch(/color:\s*var\(--secondary\)/);
		expect(rule).toMatch(/border:\s*2px solid var\(--accent\)/);
		expect(css).toMatch(
			new RegExp(
				`${escapedSelector}:focus-visible\\s*\\{[^}]*outline:\\s*3px solid var\\(--accent\\)`,
			),
		);
		expect(contrast("#264653", "#F4F4F9")).toBeGreaterThanOrEqual(4.5);
		expect(contrast("#2A9D8F", "#F4F4F9")).toBeGreaterThanOrEqual(3);
	});
});
