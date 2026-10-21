import { act, render } from "@testing-library/react";
import { screen, userEvent } from "../../test/test-utils";
import { afterEach, vi } from "vitest";
import LodgingGallery from "./LodgingGallery";

const MOBILE_MEDIA_QUERY = "(max-width: 768px)";
const DESKTOP_MEDIA_QUERY = "(min-width: 901px)";

afterEach(() => {
	vi.unstubAllGlobals();
});

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

	it("shows an empty state when no images are provided", () => {
		render(<LodgingGallery images={[]} name="Cabaña del Lago" />);

		expect(
			screen.getByText("Este alojamiento todavía no tiene fotos.", {
				exact: true,
			}),
		).toBeInTheDocument();
		expect(screen.queryAllByRole("img")).toHaveLength(0);
		expect(screen.queryAllByRole("button")).toHaveLength(0);
		expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
		expect(
			screen.queryByText("Ver más", { exact: true }),
		).not.toBeInTheDocument();
	});

	it.each([1, 2, 3, 4, 5, 7])(
		"shows a valid preview for %i images",
		(imageCount) => {
			render(
				<LodgingGallery
					images={images.slice(0, imageCount)}
					name="Cabaña del Lago"
				/>,
			);

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

	it("keeps bounded desktop navigation and Ver más in the desktop action row", async () => {
		const { container } = render(
			<LodgingGallery images={images} name="Cabaña del Lago" />,
		);
		const user = userEvent.setup();
		const actions = container.querySelector(".gallery-desktop-actions");
		const previous = screen.getByRole("button", { name: "Imagen anterior" });
		const next = screen.getByRole("button", { name: "Imagen siguiente" });
		const more = screen.getByRole("button", { name: "Ver más" });

		expect(actions).not.toBeNull();
		expect(actions).toContainElement(previous);
		expect(actions).toContainElement(next);
		expect(actions).toContainElement(more);
		expect(previous).toBeDisabled();
		expect(next).toBeEnabled();
		await user.click(next);
		expect(
			screen
				.getByRole("button", { name: "Abrir galería" })
				.querySelector("img"),
		).toHaveAccessibleName("Cabaña del Lago - 2");
		expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

		await user.click(previous);
		expect(previous).toBeDisabled();
		for (let index = 0; index < 4; index += 1) await user.click(next);
		expect(
			screen
				.getByRole("button", { name: "Abrir galería" })
				.querySelector("img"),
		).toHaveAccessibleName("Cabaña del Lago - 5");
		expect(next).toBeDisabled();
		expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

		await user.click(more);
		expect(
			screen.getByRole("dialog", { name: "Galería de imágenes" }),
		).toBeInTheDocument();
		expect(screen.getByText("1 / 7")).toBeInTheDocument();
	});

	it("keeps tablet keyboard order and focus aligned across breakpoint changes", async () => {
		let viewportWidth = 768.5;
		const listeners = new Map();
		const matchesMedia = (query) => {
			if (query === MOBILE_MEDIA_QUERY) return viewportWidth <= 768;
			if (query === DESKTOP_MEDIA_QUERY) return viewportWidth >= 901;
			return false;
		};
		vi.stubGlobal("matchMedia", (query) => ({
			get matches() {
				return matchesMedia(query);
			},
			addEventListener: (_event, listener) => {
				const queryListeners = listeners.get(query) ?? new Set();
				queryListeners.add(listener);
				listeners.set(query, queryListeners);
			},
			removeEventListener: (_event, listener) =>
				listeners.get(query)?.delete(listener),
		}));
		const { container } = render(
			<LodgingGallery images={images} name="Cabaña del Lago" />,
		);
		const user = userEvent.setup();
		const next = screen.getByRole("button", { name: "Imagen siguiente" });
		const more = screen.getByRole("button", { name: "Ver más" });

		await user.click(next);

		const previous = screen.getByRole("button", { name: "Imagen anterior" });
		const thumbnailButtons = Array.from(
			container.querySelectorAll(".gallery-thumb"),
		);
		const tabletOrder = [previous, ...thumbnailButtons, next, more];
		expect(
			Array.from(
				container.querySelectorAll(
					".gallery-desktop-arrow, .gallery-thumb, .gallery-more",
				),
			),
		).toEqual(tabletOrder);
		expect(next).toHaveFocus();

		const setViewportWidth = (width) => {
			viewportWidth = width;
			act(() => {
				listeners.forEach((queryListeners, query) => {
					const event = { matches: matchesMedia(query) };
					queryListeners.forEach((listener) => listener(event));
				});
			});
		};

		setViewportWidth(900.5);
		expect(
			Array.from(
				container.querySelectorAll(
					".gallery-desktop-arrow, .gallery-thumb, .gallery-more",
				),
			),
		).toEqual(tabletOrder);
		expect(next).toHaveFocus();
		setViewportWidth(901);
		const desktopNext = screen.getByRole("button", {
			name: "Imagen siguiente",
		});
		expect(desktopNext).toHaveFocus();
		const desktopActions = container.querySelector(".gallery-desktop-actions");
		expect(desktopActions).toContainElement(
			screen.getByRole("button", { name: "Imagen anterior" }),
		);
		expect(desktopActions).toContainElement(desktopNext);

		setViewportWidth(900.5);
		expect(
			screen.getByRole("button", { name: "Imagen siguiente" }),
		).toHaveFocus();
	});
});
