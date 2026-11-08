import { readFileSync } from "node:fs";
import { resolve } from "node:path";

export const readCss = (path) =>
	readFileSync(resolve(process.cwd(), path), "utf8");
export const readSource = readCss;

export function rule(css, selector) {
	if (selector instanceof RegExp) {
		return css.match(new RegExp(`${selector.source}\\s*\\{([^}]*)\\}`))?.[1];
	}
	const stripped = css.replace(/\/\*[\s\S]*?\*\//g, "");
	const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
	return stripped.match(
		new RegExp(`(?:^|})\\s*${escaped}\\s*\\{([^{}]*)\\}`),
	)?.[1];
}

export function luminance(hex) {
	const channels = hex.match(/[a-f\d]{2}/gi).map((channel) => {
		const value = Number.parseInt(channel, 16) / 255;
		return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
	});
	return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
}

export function contrast(foreground, background) {
	const lighter = Math.max(luminance(foreground), luminance(background));
	const darker = Math.min(luminance(foreground), luminance(background));
	return (lighter + 0.05) / (darker + 0.05);
}
