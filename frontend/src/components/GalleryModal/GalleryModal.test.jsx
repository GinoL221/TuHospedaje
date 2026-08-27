import { readFileSync } from "node:fs";
import { resolve } from "node:path";
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

  it("focuses close on open, contains Tab navigation, and restores opener focus", async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    const renderGallery = (show) => (
      <>
        <button type="button">Open gallery</button>
        {show && <GalleryModal images={images} currentIndex={0} onClose={onClose} onNavigate={vi.fn()} />}
      </>
    );
    const { rerender } = render(renderGallery(false));
    const trigger = screen.getByRole("button", { name: "Open gallery" });

    await user.click(trigger);
    rerender(renderGallery(true));

    const closeButton = screen.getByRole("button", { name: "Cerrar galería" });
    const previousButton = screen.getByRole("button", { name: "Imagen anterior" });
    const nextButton = screen.getByRole("button", { name: "Imagen siguiente" });
    expect(closeButton).toHaveFocus();

    for (const expectedButton of [previousButton, nextButton, closeButton]) {
      await user.tab();
      expect(expectedButton).toHaveFocus();
    }
    await user.tab({ shift: true });
    expect(nextButton).toHaveFocus();

    rerender(renderGallery(false));
    expect(trigger).toHaveFocus();
  });

  it("disables modal navigation when there is only one image", () => {
    renderGalleryModal({ images: [images[0]] });

    expect(screen.getByRole("button", { name: "Imagen anterior" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Imagen siguiente" })).toBeDisabled();
  });

  it("uses the approved action and focus tokens for modal control states", () => {
    const css = readFileSync(
      resolve(process.cwd(), "src/components/GalleryModal/GalleryModal.css"),
      "utf8",
    );

    expect(css).toMatch(/\.gallery-modal-close,\s*\.gallery-nav[\s\S]*var\(--action-primary-bg\)/);
    expect(css).toContain(".gallery-modal-close:hover:not(:disabled)");
    expect(css).toContain(".gallery-nav:disabled");
    expect(css).toContain("outline: 3px solid var(--action-primary-focus)");
    expect(css).not.toMatch(/\.gallery-modal-close\s*{[^}]*color:\s*#fff\b/);
    expect(css).toMatch(/@media \(max-width: 768px\)[\s\S]*width:\s*44px;[\s\S]*min-height:\s*44px;/);
  });
});
