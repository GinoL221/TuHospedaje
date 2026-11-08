import { existsSync } from "node:fs";
import { resolve } from "node:path";
import { readCss, rule } from "./test/cssContractUtils";

describe("Touch targets accessibility contract (WCAG >= 44x44px)", () => {
	it("defines .btn-cancel once in App.css with 44x44px touch target and no duplicates in ConfirmDialog.css or Admin.css", () => {
		const appCss = readCss("src/App.css");
		const confirmCss = readCss("src/components/ConfirmDialog.css");
		const adminCss = readCss("src/pages/Admin/Admin.css");

		const cancelBtn = rule(appCss, ".btn-cancel");
		expect(cancelBtn).toBeDefined();
		expect(cancelBtn).toMatch(/min-height:\s*44px/);
		expect(cancelBtn).toMatch(/min-width:\s*44px/);
		expect(rule(appCss, ".btn-cancel:focus-visible")).toBeDefined();
		expect(rule(appCss, ".btn-cancel:disabled")).toBeDefined();

		expect(rule(confirmCss, ".btn-cancel")).toBeUndefined();
		expect(rule(adminCss, ".btn-cancel")).toBeUndefined();
		expect(rule(adminCss, ".confirm-dialog")).toBeUndefined();
		expect(rule(adminCss, ".confirm-message")).toBeUndefined();
	});

	it("ConfirmDialog has its own dedicated stylesheet with touch-accessible confirm action", () => {
		const cssPath = "src/components/ConfirmDialog.css";
		expect(existsSync(resolve(process.cwd(), cssPath))).toBe(true);

		const css = readCss(cssPath);
		const confirmBtn = rule(css, ".btn-confirm");

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
