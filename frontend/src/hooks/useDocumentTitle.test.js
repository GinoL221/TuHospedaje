import { renderHook } from "@testing-library/react";
import { useDocumentTitle } from "./useDocumentTitle";

describe("useDocumentTitle", () => {
	const initialTitle = "TuHospedaje";

	beforeEach(() => {
		document.title = initialTitle;
	});

	it("sets document title with product suffix", () => {
		renderHook(() => useDocumentTitle("Iniciar sesión"));
		expect(document.title).toBe("Iniciar sesión | TuHospedaje");
	});

	it("restores original title when unmounted", () => {
		const { unmount } = renderHook(() => useDocumentTitle("Mis reservas"));
		expect(document.title).toBe("Mis reservas | TuHospedaje");
		unmount();
		expect(document.title).toBe(initialTitle);
	});

	it("falls back to default brand title when title is empty", () => {
		renderHook(() => useDocumentTitle(""));
		expect(document.title).toBe("TuHospedaje");
	});
});
