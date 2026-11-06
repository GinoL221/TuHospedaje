/**
 * Computes the minimum selectable check-out date for a given check-in date.
 *
 * A booking requires at least one night, so check-out must be strictly
 * after check-in (never the same day). When checkIn is not yet selected,
 * falls back to today's date.
 *
 * Uses setDate(getDate() + 1) on a copy of the Date instead of adding
 * 24h in milliseconds, since DST transitions can make a fixed millisecond
 * offset land on the wrong calendar day.
 *
 * @param {Date | null | undefined} checkIn
 * @returns {Date}
 */
export function minCheckoutDate(checkIn) {
	if (!checkIn) {
		return new Date();
	}

	const nextDay = new Date(checkIn);
	nextDay.setDate(nextDay.getDate() + 1);
	return nextDay;
}

/**
 * Formats a Date object to YYYY-MM-DD in the user's local timezone.
 * Unlike date.toISOString(), this does not shift days in negative UTC offsets.
 *
 * @param {Date | null | undefined} date
 * @returns {string | null}
 */
export function formatLocalDate(date) {
	if (!date || Number.isNaN(date.getTime())) return null;
	const year = date.getFullYear();
	const month = String(date.getMonth() + 1).padStart(2, "0");
	const day = String(date.getDate()).padStart(2, "0");
	return `${year}-${month}-${day}`;
}

/**
 * Parses a Date, YYYY-MM-DD, or ISO date string into a local Date object.
 *
 * @param {Date | string | null | undefined} val
 * @returns {Date | null}
 */
export function parseLocalDate(val) {
	if (!val) return null;
	if (val instanceof Date) return Number.isNaN(val.getTime()) ? null : val;
	if (typeof val === "string") {
		const match = val.match(/^(\d{4})-(\d{2})-(\d{2})/);
		if (match) {
			const [, y, m, d] = match;
			return new Date(Number(y), Number(m) - 1, Number(d));
		}
		const parsed = new Date(val);
		return Number.isNaN(parsed.getTime()) ? null : parsed;
	}
	return null;
}
