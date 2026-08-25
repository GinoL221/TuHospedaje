import { useEffect, useRef, useState } from "react";
import { ChevronDown, ChevronLeft, ChevronRight, ChevronUp } from "lucide-react";
import GalleryModal from "../GalleryModal/GalleryModal";
import "./LodgingGallery.css";

const FALLBACK_IMAGE = "https://placehold.co/800x600?text=Sin+imagen";

export default function LodgingGallery({ images = [], name }) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const thumbnailStripRef = useRef(null);
  const thumbnailRefs = useRef([]);
  const previewImages = images.slice(0, 5);
  const previewIndex = Math.min(currentIndex, previewImages.length - 1);

  useEffect(() => {
    if (!window.matchMedia?.("(max-width: 768px)").matches) return;
    const thumbnail = thumbnailRefs.current[previewIndex];
    if (!thumbnail || !thumbnailStripRef.current?.contains(thumbnail)) return;
    thumbnail.scrollIntoView({
      inline: "center",
      block: "nearest",
      behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth",
    });
  }, [previewIndex]);

  if (images.length === 0) return null;

  function changeImage(index) {
    setCurrentIndex(index);
  }

  return (
    <>
      <div className="lodging-gallery">
        <div className="lodging-gallery-main">
          <button className="lodging-gallery-main-trigger" onClick={() => setIsModalOpen(true)} aria-label="Abrir galería">
            <img src={previewImages[previewIndex]} alt={`${name} - ${previewIndex + 1}`} loading="lazy" onError={(event) => { event.currentTarget.src = FALLBACK_IMAGE; }} />
          </button>
          {images.length > 1 && <div className="lodging-gallery-mobile-controls">
            <button onClick={() => changeImage(Math.max(0, currentIndex - 1))} disabled={currentIndex === 0} aria-label="Imagen anterior en galería"><ChevronLeft /></button>
            <button onClick={() => changeImage(Math.min(previewImages.length - 1, currentIndex + 1))} disabled={previewIndex === previewImages.length - 1} aria-label="Imagen siguiente en galería"><ChevronRight /></button>
          </div>}
        </div>
        {images.length > 1 && <div className="lodging-gallery-thumbnails-column">
          <button className="lodging-gallery-desktop-arrow" onClick={() => changeImage(Math.max(0, currentIndex - 1))} disabled={currentIndex === 0} aria-label="Imagen anterior"><ChevronUp /></button>
          <div className="lodging-gallery-thumbnails" ref={thumbnailStripRef}>
            {previewImages.slice(1).map((url, index) => <button key={url} ref={(node) => { thumbnailRefs.current[index + 1] = node; }} className={previewIndex === index + 1 ? "is-active" : ""} onClick={() => { changeImage(index + 1); setIsModalOpen(true); }} aria-label={`Ver imagen ${index + 2}`} aria-current={previewIndex === index + 1 ? "true" : undefined}><img src={url} alt={`${name} - ${index + 2}`} loading="lazy" onError={(event) => { event.currentTarget.src = FALLBACK_IMAGE; }} /></button>)}
          </div>
          <button className="lodging-gallery-desktop-arrow" onClick={() => changeImage(Math.min(previewImages.length - 1, currentIndex + 1))} disabled={previewIndex === previewImages.length - 1} aria-label="Imagen siguiente"><ChevronDown /></button>
          {images.length > 5 && <button className="lodging-gallery-more" onClick={() => { changeImage(0); setIsModalOpen(true); }}>Ver más</button>}
        </div>}
      </div>
      {isModalOpen && <GalleryModal images={images} currentIndex={currentIndex} onClose={() => setIsModalOpen(false)} onNavigate={changeImage} />}
    </>
  );
}
