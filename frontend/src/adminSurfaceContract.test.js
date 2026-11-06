import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const readCss = (path) => readFileSync(resolve(process.cwd(), path), "utf8");

function rule(css, selector) {
	const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
	return css.match(new RegExp(`${escaped}\\s*\\{([^}]*)\\}`))?.[1];
}

function luminance(hex) {
	const channels = hex.match(/[a-f\d]{2}/gi).map((channel) => {
		const value = Number.parseInt(channel, 16) / 255;
		return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
	});
	return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
}

function contrast(foreground, background) {
	const lighter = Math.max(luminance(foreground), luminance(background));
	const darker = Math.min(luminance(foreground), luminance(background));
	return (lighter + 0.05) / (darker + 0.05);
}

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
});
