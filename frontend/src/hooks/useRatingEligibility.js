import { useCallback, useEffect, useRef, useState } from "react";
import { get } from "../services/api";

/**
 * Fetches the US-28.1 rating eligibility for the current authenticated user
 * against one lodging. Mirrors useAvailability's minimal state-machine
 * shape so ReviewsSection can drive an effect/retry pair without calling
 * setState synchronously inside its own effect body.
 *
 * status: idle | loading | eligible | ineligible | error
 */
export default function useRatingEligibility(lodgingId) {
  const [status, setStatus] = useState("idle");
  const generationRef = useRef(0);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const load = useCallback(() => {
    const generation = ++generationRef.current;
    setStatus("loading");

    return get(`/ratings/lodging/${lodgingId}/eligibility`)
      .then((data) => {
        if (!mountedRef.current || generation !== generationRef.current) return;
        setStatus(data.eligible ? "eligible" : "ineligible");
      })
      .catch(() => {
        if (!mountedRef.current || generation !== generationRef.current) return;
        setStatus("error");
      });
  }, [lodgingId]);

  return { status, load };
}
