import { post } from "./api";

// Module-scoped single-flight promise: as long as a refresh is in progress,
// every concurrent 401 coalesces onto this SAME promise instead of firing
// its own POST /auth/refresh (satisfies "no request storms").
let inFlightRefresh = null;

// Wraps POST /auth/refresh in a single-flight guard. `/auth/refresh` is
// itself listed in api.js's AUTH_BOOTSTRAP_ENDPOINTS, so a 401 from the
// refresh call terminates here without recursing back into this coordinator.
// On failure, auth:unauthorized is dispatched exactly once (from this single
// shared promise chain, not once per waiting caller) before the rejection
// propagates to every awaiter.
export function ensureRefreshed() {
  if (!inFlightRefresh) {
    inFlightRefresh = post("/auth/refresh")
      .catch((error) => {
        window.dispatchEvent(new CustomEvent("auth:unauthorized"));
        throw error;
      })
      .finally(() => {
        inFlightRefresh = null;
      });
  }
  return inFlightRefresh;
}
