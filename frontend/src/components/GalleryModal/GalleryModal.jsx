import { useEffect, useRef } from "react";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import ModalDialog from "../ModalDialog/ModalDialog";
import "./GalleryModal.css";

export default function GalleryModal({
	images,
	currentIndex,
	onClose,
	onNavigate,
}) {
	const closeButtonRef = useRef(null);

	useEffect(() => {
		const handleKeyDown = (event) => {
			if (images.length > 1 && event.key === "ArrowLeft") {
				event.preventDefault();
				onNavigate((currentIndex - 1 + images.length) % images.length);
				return;
			}

			if (images.length > 1 && event.key === "ArrowRight") {
				event.preventDefault();
				onNavigate((currentIndex + 1) % images.length);
			}
		};

		document.addEventListener("keydown", handleKeyDown);
		return () => document.removeEventListener("keydown", handleKeyDown);
	}, [currentIndex, images.length, onNavigate]);

	function handleBackdropClick(e) {
		if (
			e.target === e.currentTarget &&
			e.currentTarget.getBoundingClientRect().width > 0
		) {
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
		<ModalDialog
			className="gallery-modal"
			ariaLabel="Galería de imágenes"
			closedBy="any"
			initialFocusRef={closeButtonRef}
			onClose={onClose}
			onClick={handleBackdropClick}
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
		</ModalDialog>
	);
}
