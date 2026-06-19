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
