import { screen, userEvent, fireEvent } from "../../test/test-utils";
import { render } from "@testing-library/react";
import GalleryModal from "./GalleryModal";

const images = ["https://example.com/one.jpg", "https://example.com/two.jpg"];

function renderGalleryModal(overrides = {}) {
  const onClose = vi.fn();
  const onNavigate = vi.fn();
  render(
    <GalleryModal
      images={images}
      currentIndex={0}
      onClose={onClose}
      onNavigate={onNavigate}
      {...overrides}
    />,
  );
  return { onClose, onNavigate };
}

describe("GalleryModal", () => {
  it("keeps named controls while rendering decorative 24px Lucide icons", () => {
    renderGalleryModal();

    const closeIcon = screen
      .getByRole("button", { name: "Cerrar galería" })
      .querySelector("svg");
    const previousIcon = screen
      .getByRole("button", { name: "Imagen anterior" })
      .querySelector("svg");
    const nextIcon = screen
      .getByRole("button", { name: "Imagen siguiente" })
      .querySelector("svg");

    expect(screen.getByRole("dialog", { name: "Galería de imágenes" })).toHaveAttribute(
      "aria-modal",
      "true",
    );
    expect(closeIcon).toHaveClass("lucide-x");
    expect(previousIcon).toHaveClass("lucide-chevron-left");
    expect(nextIcon).toHaveClass("lucide-chevron-right");
    [closeIcon, previousIcon, nextIcon].forEach((icon) => {
      expect(icon).toHaveAttribute("width", "24");
      expect(icon).toHaveAttribute("aria-hidden", "true");
      expect(icon).toHaveAttribute("focusable", "false");
    });
  });

  it("preserves button and keyboard navigation plus Escape dismissal", async () => {
    const { onClose, onNavigate } = renderGalleryModal();
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: "Imagen siguiente" }));
    await user.click(screen.getByRole("button", { name: "Imagen anterior" }));
    fireEvent.keyDown(document, { key: "ArrowRight" });
    fireEvent.keyDown(document, { key: "ArrowLeft" });
    fireEvent.keyDown(document, { key: "Escape" });

    expect(onNavigate).toHaveBeenNthCalledWith(1, 1);
    expect(onNavigate).toHaveBeenNthCalledWith(2, 1);
    expect(onNavigate).toHaveBeenNthCalledWith(3, 1);
    expect(onNavigate).toHaveBeenNthCalledWith(4, 1);
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
