import { useEffect } from "react";

/**
 * Sets document.title on mount and updates it on change, restoring the previous title on unmount.
 *
 * @param {string} [title] - View title, e.g. "Iniciar sesión".
 * When omitted or empty, document.title falls back to the brand title "TuHospedaje".
 */
export function useDocumentTitle(title) {
	useEffect(() => {
		const previousTitle = document.title;
		document.title = title ? `${title} | TuHospedaje` : "TuHospedaje";
		return () => {
			document.title = previousTitle;
		};
	}, [title]);
}
