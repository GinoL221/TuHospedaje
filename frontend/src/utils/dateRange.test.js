import { minCheckoutDate } from "./dateRange";

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
