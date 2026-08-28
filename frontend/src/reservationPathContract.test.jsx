import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const readSource = (path) =>
	readFileSync(resolve(process.cwd(), "src", path), "utf8");

describe("reservation path visual contract", () => {
	it("keeps gallery navigation, recovery controls, and reservation surfaces on semantic tokens", () => {
		const gallery = readSource("components/LodgingGallery/LodgingGallery.jsx");
		const modal = readSource("components/GalleryModal/GalleryModal.jsx");
		const productDetail = readSource("pages/ProductDetail/ProductDetail.jsx");
		const product = readSource("pages/ProductDetail/ProductDetail.css");
		const bookingPage = readSource("pages/Booking/BookingPage.jsx");
		const booking = readSource("pages/Booking/BookingPage.css");
		const confirmationPage = readSource("pages/Booking/BookingConfirmation.jsx");
		const confirmation = readSource("pages/Booking/BookingConfirmation.css");
		const reviews = readSource("components/ReviewsSection/ReviewsSection.css");

		expect(gallery).toContain('inline: "center"');
		expect(gallery).toContain('block: "nearest"');
		expect(modal).toContain("closeButtonRef.current?.focus()");
		expect(modal).toContain('event.key === "Escape"');
		expect(productDetail).toContain("retryAvailability");
		expect(bookingPage).toContain("preflight?.available === false");
		expect(confirmationPage).toContain('navigate("/", { replace: true })');
		expect(product).toContain("var(--surface-raised)");
		expect(booking).toContain("var(--surface-raised)");
		expect(confirmation).toContain("var(--surface-raised)");
		expect(reviews).toContain("var(--surface-raised)");
	});
});
