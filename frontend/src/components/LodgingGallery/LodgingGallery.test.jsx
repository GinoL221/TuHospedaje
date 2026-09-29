import { render } from "@testing-library/react";
import { screen, userEvent } from "../../test/test-utils";
import LodgingGallery from "./LodgingGallery";

const images = Array.from(
	{ length: 7 },
	(_, index) => `https://example.com/image-${index + 1}.jpg`,
);

describe("LodgingGallery", () => {
	it("shows a five-image preview and opens all images from Ver más", async () => {
		render(<LodgingGallery images={images} name="Cabaña del Lago" />);
		const user = userEvent.setup();

		expect(
			screen.getAllByRole("img", { name: /Cabaña del Lago - \d/ }),
		).toHaveLength(5);
		await user.click(screen.getByRole("button", { name: "Ver más" }));

		expect(
			screen.getByRole("dialog", { name: "Galería de imágenes" }),
		).toBeInTheDocument();
		expect(screen.getByText("1 / 7")).toBeInTheDocument();
	});

	it.each([0, 1, 2, 3, 4, 5, 7])(
		"shows a valid preview for %i images",
		(imageCount) => {
			render(
				<LodgingGallery
					images={images.slice(0, imageCount)}
					name="Cabaña del Lago"
				/>,
			);

			if (imageCount === 0) {
				expect(
					screen.queryByRole("button", { name: "Abrir galería" }),
				).not.toBeInTheDocument();
				return;
			}

			expect(
				screen.getAllByRole("img", { name: /Cabaña del Lago - \d/ }),
			).toHaveLength(Math.min(imageCount, 5));
			if (imageCount === 1) {
				expect(
					screen.queryByRole("button", { name: "Ver más" }),
				).not.toBeInTheDocument();
			} else {
				expect(
					screen.getByRole("button", { name: "Ver más" }),
				).toBeInTheDocument();
			}
		},
	);

	it.each([1, 2, 4, 5, 7])(
		"opens the full %i-image list from the main image",
		async (imageCount) => {
			render(
				<LodgingGallery
					images={images.slice(0, imageCount)}
					name="Cabaña del Lago"
				/>,
			);
			await userEvent
				.setup()
				.click(screen.getByRole("button", { name: "Abrir galería" }));

			expect(
				screen.getByRole("dialog", { name: "Galería de imágenes" }),
			).toBeInTheDocument();
			expect(screen.getByText(`1 / ${imageCount}`)).toBeInTheDocument();
		},
	);
});
