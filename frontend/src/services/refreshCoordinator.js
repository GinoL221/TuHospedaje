import { post } from "./api";

// Module-scoped single-flight promise: as long as a refresh is in progress,
// every concurrent 401 coalesces onto this SAME promise instead of firing
// its own POST /auth/refresh (satisfies "no request storms").
let inFlightRefresh = null;
let shouldNotifyOnFailure = false;

// Wraps POST /auth/refresh in a single-flight guard. `/auth/refresh` is
// itself listed in api.js's AUTH_BOOTSTRAP_ENDPOINTS, so a 401 from the
// refresh call terminates here without recursing back into this coordinator.
// On failure, auth:unauthorized is dispatched exactly once (from this single
// shared promise chain, not once per waiting caller) if at least one caller
// in the flight opted into failure notification (protected requests do; silent
// `/auth/me` session bootstrap on mount does not).
export function ensureRefreshed({ notifyOnFailure = true } = {}) {
	if (notifyOnFailure) {
		shouldNotifyOnFailure = true;
	}
	if (!inFlightRefresh) {
		inFlightRefresh = post("/auth/refresh")
			.catch((error) => {
				if (shouldNotifyOnFailure) {
					window.dispatchEvent(new CustomEvent("auth:unauthorized"));
				}
				throw error;
			})
			.finally(() => {
				inFlightRefresh = null;
				shouldNotifyOnFailure = false;
			});
	}
	return inFlightRefresh;
}
