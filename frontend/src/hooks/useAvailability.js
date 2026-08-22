import { useCallback, useEffect, useRef, useState } from "react";
import { get } from "../services/api";

function formatDate(date) {
	const year = date.getFullYear();
	const month = String(date.getMonth() + 1).padStart(2, "0");
	const day = String(date.getDate()).padStart(2, "0");
	return `${year}-${month}-${day}`;
}

function buildQuery({ checkIn, checkOut } = {}) {
	const params = [];
	if (checkIn) params.push(`checkIn=${formatDate(checkIn)}`);
	if (checkOut) params.push(`checkOut=${formatDate(checkOut)}`);
	return params.length ? `?${params.join("&")}` : "";
}

function rangesOverlap(checkIn, checkOut, range) {
	const rangeStart = new Date(range.checkIn);
	const rangeEnd = new Date(range.checkOut);
	return checkIn < rangeEnd && checkOut > rangeStart;
}

/**
 * Reusable availability state machine shared by ProductDetail and Booking.
 *
 * status: idle | loading | ready | error | stale
 * - idle -> loading -> ready | error on the first load().
 * - A later load() from ready -> loading -> ready, or -> stale on failure
 *   (never error again, since ranges from a previous ready result remain
 *   for warning/display but stop authorizing a booking).
 *
 * A generation ref discards a response for a request that is no longer the
 * latest one (out-of-order) or that resolves after the component unmounted.
 */
export default function useAvailability(lodgingId) {
	const [status, setStatus] = useState("idle");
	const [occupiedRanges, setOccupiedRanges] = useState([]);
	const [error, setError] = useState(null);
	const generationRef = useRef(0);
	const lastParamsRef = useRef({});
	const hasReadyRef = useRef(false);
	const mountedRef = useRef(true);

	useEffect(() => {
		mountedRef.current = true;
		return () => {
			mountedRef.current = false;
		};
	}, []);

	const load = useCallback(
		(params = {}) => {
			const generation = ++generationRef.current;
			lastParamsRef.current = params;
			setStatus("loading");

			return get(`/lodgings/${lodgingId}/availability${buildQuery(params)}`)
				.then((data) => {
					if (!mountedRef.current || generation !== generationRef.current) {
						return null;
					}
					hasReadyRef.current = true;
					setOccupiedRanges(data?.occupiedRanges || []);
					setError(null);
					setStatus("ready");
					return data ?? null;
				})
				.catch((err) => {
					if (!mountedRef.current || generation !== generationRef.current) {
						return null;
					}
					setStatus(hasReadyRef.current ? "stale" : "error");
					setError(err?.message || "No pudimos obtener la disponibilidad.");
					return null;
				});
		},
		[lodgingId],
	);

	const retry = useCallback(() => load(lastParamsRef.current), [load]);

	function isRangeAvailable(checkIn, checkOut) {
		if (status !== "ready" || !checkIn || !checkOut) return false;
		return !occupiedRanges.some((range) =>
			rangesOverlap(checkIn, checkOut, range),
		);
	}

	return { status, occupiedRanges, error, load, retry, isRangeAvailable };
}
