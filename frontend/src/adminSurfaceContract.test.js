import { contrast, readCss, rule } from "./test/cssContractUtils";

describe("admin shell contract", () => {
	it("keeps the admin top bar on the product's light surface, not a brand-orange panel", () => {
		const css = readCss("src/pages/Admin/Admin.css");
		const topbar = rule(css, ".admin-topbar");

		expect(topbar).toBeDefined();
		expect(topbar).toMatch(/background:\s*var\(--surface-raised\)/);
		expect(topbar).toMatch(/border-bottom:\s*1px solid var\(--border-subtle\)/);
		expect(topbar).not.toMatch(/background:\s*var\(--primary\)/);
	});

	it("keeps the top bar text and buttons on petrol, which clears AA on the light surface", () => {
		const css = readCss("src/pages/Admin/Admin.css");

		expect(rule(css, ".admin-topbar-title")).toMatch(
			/color:\s*var\(--secondary\)/,
		);
		expect(rule(css, ".admin-topbar-name")).toMatch(
			/color:\s*var\(--secondary\)/,
		);
		expect(rule(css, ".admin-btn-logout")).toMatch(
			/color:\s*var\(--secondary\)/,
		);
		expect(contrast("#264653", "#ffffff")).toBeGreaterThanOrEqual(4.5);
		// The pair this replaced: white on brand orange.
		expect(contrast("#ffffff", "#FF6B35")).toBeLessThan(3);
	});

	it("allows horizontal scrolling in admin content area on narrow windows", () => {
		const css = readCss("src/pages/Admin/Admin.css");
		const contentRule = rule(css, ".admin-content");

		expect(contentRule).toBeDefined();
		expect(contentRule).toMatch(/overflow-x:\s*auto/);
		expect(contentRule).not.toMatch(/overflow-x:\s*hidden/);
	});

	it("preserves table column readability and collapses sidebar on narrow viewports", () => {
		const css = readCss("src/pages/Admin/Admin.css");
		const tableRule = rule(css, "table");

		expect(tableRule).toMatch(/min-width:\s*580px/);
		expect(css).toMatch(/@media\s*\(max-width:\s*900px\)/);
	});

	it("defines Admin semantic tokens in App.css without introducing --text-muted", () => {
		const appCss = readCss("src/App.css");

		expect(appCss).toMatch(/--content-muted:\s*#595959;/);
		expect(appCss).toMatch(/--accent-on-dark:\s*#7fd8cc;/);
		expect(appCss).toMatch(/--surface-on-dark-text:\s*#ffffff;/);
		expect(appCss).not.toMatch(/--text-muted\s*:/);
	});

	it("replaces hardcoded Admin.css colors with semantic tokens and color-mix", () => {
		const css = readCss("src/pages/Admin/Admin.css");

		expect(css).not.toMatch(/#7fd8cc/i);
		expect(css).not.toMatch(/#595959/i);
		expect(css).not.toMatch(/#d32f2f/i);
		expect(css).not.toMatch(/#1a4f6e/i);
		expect(css).not.toMatch(/#fff\b/i);
		expect(css).not.toContain("rgba(38, 70, 83, 0.03)");

		expect(rule(css, ".admin-sidebar-brand-sub")).toMatch(
			/color:\s*var\(--accent-on-dark\)/,
		);
		expect(rule(css, ".stat-label")).toMatch(/color:\s*var\(--content-muted\)/);
		expect(rule(css, ".required-dot::before")).toMatch(
			/color:\s*var\(--feedback-danger-text\)/,
		);
		expect(rule(css, ".required-note")).toMatch(
			/color:\s*var\(--feedback-danger-text\)/,
		);
		expect(rule(css, "tbody tr:hover")).toMatch(
			/background:\s*color-mix\(in srgb,\s*var\(--secondary\)\s*3%,\s*transparent\)/,
		);
		expect(rule(css, "th.sortable:hover")).toMatch(
			/background:\s*color-mix\(in srgb,\s*var\(--secondary\)\s*85%,\s*black\)/,
		);
	});

	it("styles scrollable admin table wrappers with focus-visible and sticky thead", () => {
		const css = readCss("src/pages/Admin/Admin.css");
		const wrapperRule = rule(css, ".admin-table-wrapper");
		const wrapperFocusRule = rule(css, ".admin-table-wrapper:focus-visible");
		const theadRule = rule(css, "table thead");

		expect(wrapperRule).toBeDefined();
		expect(wrapperRule).toMatch(/overflow:\s*auto/);
		expect(wrapperFocusRule).toBeDefined();
		expect(wrapperFocusRule).toMatch(/outline:/);
		expect(theadRule).toBeDefined();
		expect(theadRule).toMatch(/position:\s*sticky/);
		expect(theadRule).toMatch(/inset-block-start:\s*0/);
		expect(theadRule).toMatch(/z-index:\s*1/);
	});

	it("reserves bottom clearance in admin-content so the fixed FAB never covers the last table row", () => {
		const css = readCss("src/pages/Admin/Admin.css");
		const contentRule = rule(css, ".admin-content");
		const mobileBlock = css.match(
			/@media\s*\(max-width:\s*900px\)\s*\{([\s\S]*)\}\s*$/,
		)?.[1];

		expect(contentRule).toBeDefined();
		expect(contentRule).toMatch(/padding:\s*28px 32px 96px/);
		expect(mobileBlock).toBeDefined();
		expect(mobileBlock).toMatch(
			/\.admin-content\s*\{[^}]*padding:\s*20px 16px 96px/,
		);
	});
});
