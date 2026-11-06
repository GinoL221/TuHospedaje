import { minCheckoutDate, formatLocalDate, parseLocalDate } from "./dateRange";

describe("minCheckoutDate", () => {
	afterEach(() => {
		vi.useRealTimers();
	});

	it("returns the day after the given checkIn date", () => {
		const checkIn = new Date("2026-06-30T00:00:00");

		const result = minCheckoutDate(checkIn);

		expect(result.getFullYear()).toBe(2026);
		expect(result.getMonth()).toBe(6); // July is month index 6
		expect(result.getDate()).toBe(1); // July 1st, the day after June 30th
	});

	it("does not mutate the checkIn date passed in", () => {
		const checkIn = new Date("2026-06-30T00:00:00");
		const originalTime = checkIn.getTime();

		minCheckoutDate(checkIn);

		expect(checkIn.getTime()).toBe(originalTime);
	});

	it("rolls over correctly across a month/year boundary", () => {
		const checkIn = new Date("2026-12-31T00:00:00");

		const result = minCheckoutDate(checkIn);

		expect(result.getFullYear()).toBe(2027);
		expect(result.getMonth()).toBe(0); // January
		expect(result.getDate()).toBe(1);
	});

	it("returns the current date when checkIn is null", () => {
		vi.useFakeTimers();
		vi.setSystemTime(new Date("2026-07-15T10:00:00"));

		const result = minCheckoutDate(null);

		expect(result.getTime()).toBe(new Date("2026-07-15T10:00:00").getTime());
	});

	it("returns the current date when checkIn is undefined", () => {
		vi.useFakeTimers();
		vi.setSystemTime(new Date("2026-07-15T10:00:00"));

		const result = minCheckoutDate(undefined);

		expect(result.getTime()).toBe(new Date("2026-07-15T10:00:00").getTime());
	});
});

describe("formatLocalDate and parseLocalDate", () => {
	it("formats and parses local dates without timezone shifting", () => {
		const date = new Date(2026, 9, 15);
		const formatted = formatLocalDate(date);
		expect(formatted).toBe("2026-10-15");

		const parsed = parseLocalDate(formatted);
		expect(parsed.getFullYear()).toBe(2026);
		expect(parsed.getMonth()).toBe(9);
		expect(parsed.getDate()).toBe(15);
	});

	it("handles null, undefined and invalid inputs safely", () => {
		expect(formatLocalDate(null)).toBeNull();
		expect(formatLocalDate(undefined)).toBeNull();
		expect(formatLocalDate(new Date("invalid"))).toBeNull();

		expect(parseLocalDate(null)).toBeNull();
		expect(parseLocalDate(undefined)).toBeNull();
		expect(parseLocalDate("invalid-date")).toBeNull();
	});
});
