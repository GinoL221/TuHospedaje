import { readFileSync, existsSync } from "node:fs";
import { resolve } from "node:path";

const readCss = (path) => readFileSync(resolve(process.cwd(), path), "utf8");

function rule(css, selector) {
	const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
	return css.match(new RegExp(`${escaped}\\s*\\{([^}]*)\\}`))?.[1];
}

describe("Touch targets accessibility contract (WCAG >= 44x44px)", () => {
	it("ConfirmDialog has its own dedicated stylesheet with touch-accessible actions", () => {
		const cssPath = "src/components/ConfirmDialog.css";
		expect(existsSync(resolve(process.cwd(), cssPath))).toBe(true);

		const css = readCss(cssPath);
		const cancelBtn = rule(css, ".btn-cancel");
		const confirmBtn = rule(css, ".btn-confirm");

		expect(cancelBtn).toBeDefined();
		expect(cancelBtn).toMatch(/min-height:\s*44px/);
		expect(cancelBtn).toMatch(/min-width:\s*44px/);

		expect(confirmBtn).toBeDefined();
		expect(confirmBtn).toMatch(/min-height:\s*44px/);
		expect(confirmBtn).toMatch(/min-width:\s*44px/);
		expect(confirmBtn).toMatch(/background:\s*var\(--primary\)/);
	});

	it("UserProfileModal close button meets the 44x44px touch target", () => {
		const css = readCss("src/components/UserProfileModal/UserProfileModal.css");
		const closeBtn = rule(css, ".profile-modal-close");

		expect(closeBtn).toBeDefined();
		expect(closeBtn).toMatch(/(width:\s*44px|min-width:\s*44px)/);
		expect(closeBtn).toMatch(/(height:\s*44px|min-height:\s*44px)/);
	});

	it("Home pagination buttons satisfy the 44x44px touch target", () => {
		const css = readCss("src/pages/Home/Home.css");
		const pageBtn = rule(css, ".home-pagination button");

		expect(pageBtn).toBeDefined();
		expect(pageBtn).toMatch(/min-height:\s*44px/);
		expect(pageBtn).toMatch(/min-width:\s*44px/);
	});

	it("Admin pagination buttons satisfy the 44x44px touch target", () => {
		const css = readCss("src/pages/Admin/Admin.css");
		const pageBtn = rule(css, ".pagination button");

		expect(pageBtn).toBeDefined();
		expect(pageBtn).toMatch(/min-height:\s*44px/);
		expect(pageBtn).toMatch(/min-width:\s*44px/);
	});
});
