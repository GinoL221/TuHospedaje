import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const readCss = (path) => readFileSync(resolve(process.cwd(), path), "utf8");
const readSource = (path) => readFileSync(resolve(process.cwd(), path), "utf8");

function rule(css, selector) {
	const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
	return css.match(new RegExp(`${escaped}\\s*\\{([^}]*)\\}`))?.[1];
}

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

describe("approved visual integration contracts", () => {
	it("keeps the Home search action colored, readable, and visibly distinct by state", () => {
		const css = readCss("src/pages/Home/Home.css");

		expect(rule(css, ".btn-search")).toMatch(
			/background-color:\s*var\(--secondary\)/,
		);
		expect(rule(css, ".btn-search")).toMatch(/color:\s*var\(--bg\)/);
		expect(rule(css, ".btn-search:hover")).toMatch(
			/background-color:\s*var\(--bg\)/,
		);
		expect(rule(css, ".btn-search:hover")).toMatch(
			/border-color:\s*var\(--accent\)/,
		);
		expect(rule(css, ".btn-search:focus-visible")).toMatch(
			/outline:\s*3px solid var\(--accent\)/,
		);
		expect(css).not.toMatch(/\.btn-search:hover\s*\{[^}]*#333/i);
	});

	it("scopes the refreshed calendar states and practical targets to Home", () => {
		const css = readCss("src/pages/Home/Home.css");
		const source = readSource("src/pages/Home/Home.jsx");

		expect(source).toMatch(/registerLocale\("es", es\)/);
		expect(source.match(/locale="es"/g)).toHaveLength(2);
		expect(source.match(/popperClassName="home-datepicker-popper"/g)).toHaveLength(2);
		expect(rule(css, ".home .react-datepicker__navigation")).toMatch(
			/width:\s*44px/,
		);
		expect(css).toMatch(
			/\.home \.react-datepicker__day-name,\s*\.home \.react-datepicker__day\s*\{[^}]*width:\s*36px[^}]*height:\s*36px/s,
		);
		expect(css).toMatch(/\.home \.react-datepicker__day:focus-visible/);
		expect(css).toMatch(/\.home \.react-datepicker__day--keyboard-selected/);
		expect(css).toMatch(/\.home \.react-datepicker__day--disabled/);
	});

	it("optically aligns the Home calendar chevrons symmetrically", () => {
		const css = readCss("src/pages/Home/Home.css");
		const icon = rule(css, ".home .react-datepicker__navigation-icon");
		const chevron = rule(css, ".home .react-datepicker__navigation-icon::before");
		const next = rule(css, ".home .react-datepicker__navigation-icon--next::before");
		const previous = rule(
			css,
			".home .react-datepicker__navigation-icon--previous::before",
		);

		expect(icon).toMatch(/position:\s*absolute/);
		expect(icon).toMatch(/inset:\s*0/);
		// Rotated border glyphs carry more visual weight above their box center.
		expect(chevron).toMatch(/top:\s*calc\(50% \+ 4px\)/);
		expect(chevron).toMatch(/left:\s*50%/);
		expect(next).toMatch(
			/transform:\s*translate\(-50%,\s*-50%\) rotate\(45deg\)/,
		);
		expect(previous).toMatch(
			/transform:\s*translate\(-50%,\s*-50%\) rotate\(225deg\)/,
		);
	});

	it("renders Home weekday labels as petroleum text on the light weekday surface", () => {
		const css = readCss("src/pages/Home/Home.css");
		const weekdayRow = rule(css, ".home .react-datepicker__day-names");
		const weekday = rule(css, ".home .react-datepicker__day-name");
		const month = rule(css, ".home .react-datepicker__current-month");

		expect(weekdayRow).toMatch(/background-color:\s*var\(--bg\)/);
		expect(weekdayRow).toMatch(/border-top:\s*1px solid var\(--accent\)/);
		expect(weekday).toMatch(/color:\s*var\(--secondary\)/);
		expect(month).toMatch(/color:\s*var\(--bg\)/);
		expect(contrast("#264653", "#F4F4F9")).toBeGreaterThanOrEqual(4.5);
		expect(contrast("#F4F4F9", "#264653")).toBeGreaterThanOrEqual(4.5);
	});

	it("uses the approved month title and city suggestion visual contracts", () => {
		const css = readCss("src/pages/Home/Home.css");
		const source = readSource("src/pages/Home/Home.jsx");
		const month = rule(css, ".home .react-datepicker__current-month");
		const suggestions = rule(css, ".city-suggestions");
		const option = rule(css, ".city-suggestions li");

		expect(month).toMatch(/font-size:\s*1\.125rem/);
		expect(month).toMatch(/font-weight:\s*700/);
		expect(month).toMatch(/text-transform:\s*capitalize/);
		expect(month).toMatch(/min-height:\s*44px/);
		expect(suggestions).toMatch(/border:\s*2px solid var\(--accent\)/);
		expect(suggestions).toMatch(/max-height:\s*220px/);
		expect(suggestions).toMatch(/overflow-y:\s*auto/);
		expect(option).toMatch(/min-height:\s*44px/);
		expect(css).toMatch(/\.city-suggestions li\.is-active/);
		expect(source).toMatch(/role="combobox"/);
		expect(source).toMatch(/role="listbox"/);
		expect(source).toMatch(/role="option"/);
		expect(contrast("#2A9D8F", "#F4F4F9")).toBeGreaterThanOrEqual(3);
		expect(contrast("#F4F4F9", "#264653")).toBeGreaterThanOrEqual(4.5);
	});

	it("reserves the fixed Header and stable breathing room on both auth pages", () => {
		const css = readCss("src/assets/css/auth.css");
		const container = rule(css, ".login-container");

		expect(container).toMatch(/align-items:\s*flex-start/);
		expect(container).toMatch(
			/padding:\s*calc\(var\(--site-header-height\) \+ 48px\) 20px 56px/,
		);
		expect(container).not.toMatch(/align-items:\s*center/);
	});
});
