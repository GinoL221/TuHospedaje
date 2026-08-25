import { useEffect, useRef, useState } from "react";
import { ChevronDown, ChevronLeft, ChevronRight, ChevronUp } from "lucide-react";
import GalleryModal from "../GalleryModal/GalleryModal";
import "./LodgingGallery.css";

const FALLBACK_IMAGE = "https://placehold.co/800x600?text=Sin+imagen";
const FALLBACK_THUMB = "https://placehold.co/400x300?text=Sin+imagen";

export default function LodgingGallery({ images = [], name }) {
	const [currentIndex, setCurrentIndex] = useState(0);
	const [isModalOpen, setIsModalOpen] = useState(false);
	const thumbnailStripRef = useRef(null);
	const thumbnailRefs = useRef([]);
	const previewImages = images.slice(0, 5);
	const previewIndex = Math.min(currentIndex, Math.max(previewImages.length - 1, 0));

	useEffect(() => {
		if (!window.matchMedia?.("(max-width: 768px)").matches) return;
		const thumbnail = thumbnailRefs.current[previewIndex];
		if (!thumbnail || !thumbnailStripRef.current?.contains(thumbnail)) return;
		thumbnail.scrollIntoView({
			inline: "center",
			block: "nearest",
			behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches
				? "auto"
				: "smooth",
		});
	}, [previewIndex]);

	if (images.length === 0) return null;

	return (
		<>
			<div className="gallery-wrapper">
				<div className="gallery-main">
					<button
						className="gallery-main-trigger"
						onClick={() => setIsModalOpen(true)}
						aria-label="Abrir galería"
					>
						<img
							src={previewImages[previewIndex]}
							alt={`${name} - ${previewIndex + 1}`}
							loading="lazy"
							onError={(event) => {
								event.currentTarget.src = FALLBACK_IMAGE;
							}}
						/>
					</button>
					{images.length > 1 && (
						<div className="gallery-mobile-controls">
							<button
								className="gallery-mobile-arrow gallery-mobile-arrow--prev"
								onClick={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
								disabled={currentIndex === 0}
								aria-label="Imagen anterior en galería"
							>
								<ChevronLeft size={24} aria-hidden="true" focusable="false" />
							</button>
							<button
								className="gallery-mobile-arrow gallery-mobile-arrow--next"
								onClick={() =>
									setCurrentIndex((prev) =>
										Math.min(previewImages.length - 1, prev + 1),
									)
								}
								disabled={previewIndex === previewImages.length - 1}
								aria-label="Imagen siguiente en galería"
							>
								<ChevronRight size={24} aria-hidden="true" focusable="false" />
							</button>
						</div>
					)}
				</div>
				{images.length > 1 && (
					<div className="gallery-thumbs-col">
						<button
							className="gallery-thumbs-arrow gallery-desktop-arrow"
							onClick={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
							disabled={currentIndex === 0}
							aria-label="Imagen anterior"
						>
							<ChevronUp size={20} aria-hidden="true" focusable="false" />
						</button>
						<div className="gallery-thumbs" ref={thumbnailStripRef}>
							{previewImages.slice(1).map((url, index) => (
								<button
									key={url}
									ref={(node) => {
										thumbnailRefs.current[index + 1] = node;
									}}
									className={`gallery-thumb ${previewIndex === index + 1 ? "gallery-thumb--active" : ""}`}
									onClick={() => {
										setCurrentIndex(index + 1);
										setIsModalOpen(true);
									}}
									aria-label={`Ver imagen ${index + 2}`}
									aria-current={previewIndex === index + 1 ? "true" : undefined}
								>
									<img
										src={url}
										alt={`${name} - ${index + 2}`}
										loading="lazy"
										onError={(event) => {
											event.currentTarget.src = FALLBACK_THUMB;
										}}
									/>
								</button>
							))}
						</div>
						<button
							className="gallery-thumbs-arrow gallery-desktop-arrow"
							onClick={() =>
								setCurrentIndex((prev) =>
									Math.min(previewImages.length - 1, prev + 1),
								)
							}
							disabled={previewIndex === previewImages.length - 1}
							aria-label="Imagen siguiente"
						>
							<ChevronDown size={20} aria-hidden="true" focusable="false" />
						</button>
						{images.length > 5 && (
							<button
								className="gallery-more"
								onClick={() => {
									setCurrentIndex(0);
									setIsModalOpen(true);
								}}
							>
								Ver más
							</button>
						)}
					</div>
				)}
			</div>
			{isModalOpen && (
				<GalleryModal
					images={images}
					currentIndex={currentIndex}
					onClose={() => setIsModalOpen(false)}
					onNavigate={setCurrentIndex}
				/>
			)}
		</>
	);
}
