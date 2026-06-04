import { useEffect, useCallback } from "react";
import "./GalleryModal.css";

export default function GalleryModal({ images, currentIndex, onClose, onNavigate }) {
  const handleKeyDown = useCallback(
    (e) => {
      if (e.key === "Escape") {
        onClose();
      } else if (e.key === "ArrowLeft") {
        onNavigate((currentIndex - 1 + images.length) % images.length);
      } else if (e.key === "ArrowRight") {
        onNavigate((currentIndex + 1) % images.length);
      }
    },
    [currentIndex, images.length, onClose, onNavigate],
  );

  useEffect(() => {
    document.addEventListener("keydown", handleKeyDown);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "";
    };
  }, [handleKeyDown]);

  function handleOverlayClick(e) {
    if (e.target === e.currentTarget) {
      onClose();
    }
  }

  function prev() {
    onNavigate((currentIndex - 1 + images.length) % images.length);
  }

  function next() {
    onNavigate((currentIndex + 1) % images.length);
  }

  const fallback = "https://placehold.co/800x600?text=Sin+imagen";

  return (
    <div
      className="gallery-modal-overlay"
      onClick={handleOverlayClick}
      role="dialog"
      aria-label="Galería de imágenes"
      aria-modal="true"
    >
      <button
        className="gallery-modal-close"
        onClick={onClose}
        aria-label="Cerrar galería"
      >
        ×
      </button>

      <button
        className="gallery-nav gallery-nav--prev"
        onClick={prev}
        aria-label="Imagen anterior"
      >
        ‹
      </button>

      <div className="gallery-modal-image">
        <img
          src={images[currentIndex]}
          alt={`${currentIndex + 1} de ${images.length}`}
          onError={(e) => {
            e.target.src = fallback;
          }}
        />
      </div>

      <button
        className="gallery-nav gallery-nav--next"
        onClick={next}
        aria-label="Imagen siguiente"
      >
        ›
      </button>

      <div className="gallery-modal-counter" aria-live="polite">
        {currentIndex + 1} / {images.length}
      </div>
    </div>
  );
}
