package com.tuhospedaje.dto.admin;

/**
 * Row counts behind the admin dashboard's stat cards.
 *
 * <p>Counts are {@code long} because they answer "how many rows exist", which is not
 * bounded by any page size — the dashboard previously derived them from listing payloads
 * and inherited those listings' caps.
 */
public record AdminStatsResponse(
        long lodgings,
        long categories,
        long features,
        long users,
        long reservations
) {
}
