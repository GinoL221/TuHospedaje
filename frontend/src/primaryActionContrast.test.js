import { contrast, readCss } from "./test/cssContractUtils";

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
		[
			"booking form",
			"src/pages/Booking/BookingPage.css",
			'.booking-form button[type="submit"]',
		],
		[
			"booking confirmation",
			"src/pages/Booking/BookingConfirmation.css",
			".confirmation-actions .btn-primary",
		],
		[
			"lodging reservation",
			"src/pages/ProductDetail/ProductDetail.css",
			".btn-reserve",
		],
		[
			"review submission",
			"src/components/ReviewsSection/ReviewsSection.css",
			".btn-submit-review",
		],
		["admin add", "src/pages/Admin/Admin.css", ".btn-fab"],
		["admin save", "src/pages/Admin/Admin.css", ".btn-save"],
	])("applies the semantic contract to the %s CTA", (_name, path, selector) => {
		const css = readCss(path);
		const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
		const rule = css.match(
			new RegExp(`${escapedSelector}\\s*\\{([^}]*)\\}`),
		)?.[1];

		expect(rule).toBeDefined();
		expect(rule).toMatch(/background(?:-color)?:\s*var\(--action-primary-bg\)/);
		expect(rule).toMatch(/color:\s*var\(--action-primary-fg\)/);
		expect(rule).not.toMatch(/background(?:-color)?:\s*var\(--primary\)/);
	});

	it("keeps the Header registration CTA distinct against the light header", () => {
		const css = readCss("src/layout.css");
		const rule = css.match(
			/\.site-header \.header-register-cta\s*\{([^}]*)\}/,
		)?.[1];

		expect(rule).toBeDefined();
		expect(rule).toMatch(/min-height:\s*44px/);
		expect(rule).toMatch(/background-color:\s*var\(--secondary\)/);
		expect(rule).toMatch(/color:\s*#ffffff/);
		expect(rule).toMatch(/border:\s*2px solid var\(--secondary\)/);
		expect(rule).not.toMatch(/background-color:\s*var\(--primary\)/);
		expect(contrast("#FFFFFF", "#264653")).toBeGreaterThanOrEqual(4.5);
		expect(contrast("#264653", "#FFFFFF")).toBeGreaterThanOrEqual(4.5);
	});

	it.each([
		[
			"lodging share",
			"src/pages/ProductDetail/ProductDetail.css",
			".btn-share",
		],
		[
			"empty reservations",
			"src/pages/MyReservations/MyReservationsPage.css",
			".reservations-back",
		],
	])(
		"keeps the %s secondary action readable and bounded",
		(_name, path, selector) => {
			const css = readCss(path);
			const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
			const rules = [
				...css.matchAll(new RegExp(`${escapedSelector}\\s*\\{([^}]*)\\}`, "g")),
			];
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
		},
	);

	it("keeps Header nav links above 4.5:1 contrast on hover against the light header", () => {
		const css = readCss("src/layout.css");
		const hoverRule = css.match(/\.nav-links a:hover\s*\{([^}]*)\}/)?.[1];

		expect(hoverRule).toBeDefined();
		expect(hoverRule).not.toMatch(/color:\s*var\(--primary\)/);
		expect(hoverRule).toMatch(/color:\s*var\(--secondary\)/);
	});

	it("keeps Admin table action buttons above 4.5:1 contrast in resting and hover states", () => {
		const css = readCss("src/pages/Admin/Admin.css");
		const editRule = css.match(/\.btn-edit\s*\{([^}]*)\}/)?.[1];
		const deleteRule = css.match(/\.btn-delete\s*\{([^}]*)\}/)?.[1];
		const promoteRule = css.match(/\.btn-promote\s*\{([^}]*)\}/)?.[1];
		const demoteRule = css.match(/\.btn-demote\s*\{([^}]*)\}/)?.[1];

		expect(editRule).not.toMatch(/color:\s*var\(--primary\)/);
		expect(editRule).toMatch(/color:\s*var\(--secondary\)/);

		expect(deleteRule).not.toMatch(/#e74c3c/);
		expect(deleteRule).toMatch(/var\(--feedback-danger-text\)/);

		expect(demoteRule).not.toMatch(/#e74c3c/);
		expect(demoteRule).toMatch(/var\(--feedback-danger-text\)/);

		expect(promoteRule).not.toMatch(/var\(--accent\)/);
		expect(promoteRule).toMatch(/var\(--feedback-success-text\)/);
	});

	it("keeps Home recommendation alert above 4.5:1 contrast and aligned with feedback tokens", () => {
		const css = readCss("src/pages/Home/Home.css");
		const alertRule = css.match(/\.recommendations-alert\s*\{([^}]*)\}/)?.[1];

		expect(alertRule).toBeDefined();
		expect(alertRule).not.toMatch(/color:\s*var\(--primary\)/);
		expect(alertRule).toMatch(/color:\s*var\(--feedback-danger-text\)/);
	});
});
