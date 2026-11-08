import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const readCss = (path) => readFileSync(resolve(process.cwd(), path), "utf8");

function rule(css, selector) {
	if (selector instanceof RegExp) {
		return css.match(new RegExp(`${selector.source}\\s*\\{([^}]*)\\}`))?.[1];
	}
	const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
	return css.match(new RegExp(`${escaped}\\s*\\{([^}]*)\\}`))?.[1];
}

describe("Design tokens consistency contract (Manual de Identidad v2.1)", () => {
	it("UserProfileModal uses secondary, accent and feedback tokens instead of hardcoded hex", () => {
		const css = readCss("src/components/UserProfileModal/UserProfileModal.css");
		const avatar = rule(css, ".profile-modal-avatar");
		const headerTitle = rule(css, ".profile-modal-header h2");
		const resendBtn = rule(css, ".btn-profile-resend");
		const scheduledFeedback = rule(css, ".profile-modal-feedback--scheduled");

		expect(avatar).toBeDefined();
		expect(avatar).toMatch(/border:\s*3px solid var\(--secondary\)/);
		expect(avatar).not.toContain("#264653");

		expect(headerTitle).toBeDefined();
		expect(headerTitle).toMatch(/color:\s*var\(--secondary\)/);
		expect(headerTitle).not.toContain("#264653");

		expect(resendBtn).toBeDefined();
		expect(resendBtn).toMatch(/background:\s*var\(--accent\)/);
		expect(resendBtn).not.toContain("#2a9d8f");

		expect(scheduledFeedback).toBeDefined();
		expect(scheduledFeedback).toMatch(
			/background:\s*var\(--feedback-success-bg\)/,
		);
		expect(scheduledFeedback).toMatch(
			/color:\s*var\(--feedback-success-text\)/,
		);
		expect(scheduledFeedback).toMatch(
			/border:\s*1px solid var\(--feedback-success-border\)/,
		);
	});

	it("Admin form errors adopt feedback-danger tokens instead of hardcoded #d32f2f", () => {
		const css = readCss("src/pages/Admin/Admin.css");
		const fieldError = rule(css, ".field-error");
		const inputError = rule(css, /input\.input-error,\s*textarea\.input-error/);

		expect(fieldError).toBeDefined();
		expect(fieldError).toMatch(/color:\s*var\(--feedback-danger-text\)/);
		expect(fieldError).not.toContain("#d32f2f");

		expect(inputError).toBeDefined();
		expect(inputError).toMatch(
			/border-color:\s*var\(--feedback-danger-border\)/,
		);
		expect(inputError).not.toContain("#d32f2f");
	});

	it("MyReservationsPage reservation status colors map to approved feedback tokens", () => {
		const css = readCss("src/pages/MyReservations/MyReservationsPage.css");
		const pageContainer = rule(css, ".my-reservations");

		expect(pageContainer).toBeDefined();
		expect(pageContainer).toMatch(
			/--reservation-success:\s*var\(--feedback-success-text\)/,
		);
		expect(pageContainer).toMatch(
			/--reservation-warning:\s*var\(--feedback-warning-text\)/,
		);
		expect(pageContainer).toMatch(
			/--reservation-danger:\s*var\(--feedback-danger-text\)/,
		);
	});
});
