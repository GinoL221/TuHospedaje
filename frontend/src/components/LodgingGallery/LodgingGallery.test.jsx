import { render } from "@testing-library/react";
import { screen, userEvent } from "../../test/test-utils";
import LodgingGallery from "./LodgingGallery";

const images = Array.from(
  { length: 6 },
  (_, index) => `https://example.com/image-${index + 1}.jpg`,
);

describe("LodgingGallery", () => {
  it("shows a five-image preview and opens all images from Ver más", async () => {
    render(<LodgingGallery images={images} name="Cabaña del Lago" />);
    const user = userEvent.setup();

    expect(screen.getAllByRole("img", { name: /Cabaña del Lago - \d/ })).toHaveLength(5);
    await user.click(screen.getByRole("button", { name: "Ver más" }));

    expect(screen.getByRole("dialog", { name: "Galería de imágenes" })).toBeInTheDocument();
    expect(screen.getByText("1 / 6")).toBeInTheDocument();
  });

  it("renders only existing preview images when fewer than five are provided", () => {
    render(<LodgingGallery images={images.slice(0, 3)} name="Cabaña del Lago" />);

    expect(screen.getAllByRole("img", { name: /Cabaña del Lago - \d/ })).toHaveLength(3);
    expect(screen.getByRole("button", { name: "Ver más" })).toBeInTheDocument();
  });
});
