import { useEffect, useState } from "react";

const FEEDBACK_DELAY_MS = 150;

export default function RouteLoadingFallback() {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const timerId = window.setTimeout(() => setIsVisible(true), FEEDBACK_DELAY_MS);
    return () => window.clearTimeout(timerId);
  }, []);

  if (!isVisible) return null;

  return (
    <div className="route-loading" role="status">
      <span
        className="route-loading__spinner"
        data-testid="route-loading-spinner"
        aria-hidden="true"
      />
      <span>Cargando página…</span>
    </div>
  );
}
