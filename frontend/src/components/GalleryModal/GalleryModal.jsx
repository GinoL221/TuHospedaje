import { useEffect, useRef } from "react";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import "./GalleryModal.css";

const FOCUSABLE_SELECTOR = [
	"button:not([disabled])",
	"[href]",
	"input:not([disabled])",
	"select:not([disabled])",
	"textarea:not([disabled])",
	'[tabindex]:not([tabindex="-1"])',
].join(",");

export default function GalleryModal({ images, currentIndex, onClose, onNavigate }) {
	const dialogRef = useRef(null);
	const closeButtonRef = useRef(null);
	const previousFocusRef = useRef(null);

	useEffect(() => {
		previousFocusRef.current = document.activeElement;
		closeButtonRef.current?.focus();

		return () => {
			const previousFocus = previousFocusRef.current;
			if (
				previousFocus?.isConnected &&
				previousFocus.matches(FOCUSABLE_SELECTOR)
			) {
				previousFocus.focus();
			}
		};
	}, []);

	useEffect(() => {
		const handleKeyDown = (event) => {
			if (event.key === "Escape") {
				event.preventDefault();
				onClose();
				return;
			}

			if (images.length > 1 && event.key === "ArrowLeft") {
				event.preventDefault();
				onNavigate((currentIndex - 1 + images.length) % images.length);
				return;
			}

			if (images.length > 1 && event.key === "ArrowRight") {
				event.preventDefault();
				onNavigate((currentIndex + 1) % images.length);
				return;
			}

			if (event.key !== "Tab") return;

			const focusableElements = Array.from(
				dialogRef.current?.querySelectorAll(FOCUSABLE_SELECTOR) ?? [],
			);
			if (focusableElements.length === 0) {
				event.preventDefault();
				return;
			}

			const firstElement = focusableElements[0];
			const lastElement = focusableElements.at(-1);
			if (!dialogRef.current?.contains(document.activeElement)) {
				event.preventDefault();
				firstElement.focus();
			} else if (event.shiftKey && document.activeElement === firstElement) {
				event.preventDefault();
				lastElement.focus();
			} else if (!event.shiftKey && document.activeElement === lastElement) {
				event.preventDefault();
				firstElement.focus();
			}
		};

		document.addEventListener("keydown", handleKeyDown);
		return () => document.removeEventListener("keydown", handleKeyDown);
	}, [currentIndex, images.length, onClose, onNavigate]);

	useEffect(() => {
		const previousOverflow = document.body.style.overflow;
		document.body.style.overflow = "hidden";

		return () => {
			document.body.style.overflow = previousOverflow;
		};
	}, []);

  function handleOverlayClick(e) {
    if (e.target === e.currentTarget) {
      onClose();
    }
  }

	function prev() {
    if (images.length <= 1) return;
    onNavigate((currentIndex - 1 + images.length) % images.length);
  }

  function next() {
    if (images.length <= 1) return;
    onNavigate((currentIndex + 1) % images.length);
  }

  const fallback = "https://placehold.co/800x600?text=Sin+imagen";

  return (
    <div
      ref={dialogRef}
      className="gallery-modal-overlay"
      onClick={handleOverlayClick}
      role="dialog"
      aria-label="Galería de imágenes"
      aria-modal="true"
    >
      <button
        type="button"
        ref={closeButtonRef}
        className="gallery-modal-close"
        onClick={onClose}
        aria-label="Cerrar galería"
      >
        <X size={24} aria-hidden="true" focusable="false" />
      </button>

      <button
        type="button"
        className="gallery-nav gallery-nav--prev"
        onClick={prev}
        disabled={images.length <= 1}
        aria-label="Imagen anterior"
      >
        <ChevronLeft size={24} aria-hidden="true" focusable="false" />
      </button>

      <div className="gallery-modal-image">
        <img
          src={images[currentIndex]}
          alt={`${currentIndex + 1} de ${images.length}`}
          onError={(e) => {
            e.currentTarget.src = fallback;
          }}
        />
      </div>

      <button
        type="button"
        className="gallery-nav gallery-nav--next"
        onClick={next}
        disabled={images.length <= 1}
        aria-label="Imagen siguiente"
      >
        <ChevronRight size={24} aria-hidden="true" focusable="false" />
      </button>

      <div className="gallery-modal-counter" aria-live="polite">
        {currentIndex + 1} / {images.length}
      </div>
    </div>
  );
}
