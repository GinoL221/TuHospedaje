import { useEffect, useLayoutEffect, useRef, useState } from "react";
import {
	ChevronDown,
	ChevronLeft,
	ChevronRight,
	ChevronUp,
	ImageOff,
} from "lucide-react";
import GalleryModal from "../GalleryModal/GalleryModal";
import "./LodgingGallery.css";

const FALLBACK_IMAGE = "https://placehold.co/800x600?text=Sin+imagen";
const FALLBACK_THUMB = "https://placehold.co/400x300?text=Sin+imagen";
const MOBILE_MEDIA_QUERY = "(max-width: 768px)";
const DESKTOP_MEDIA_QUERY = "(min-width: 901px)";

function matchesMobileViewport() {
	if (typeof window === "undefined") return false;
	if (typeof window.matchMedia === "function") {
		return window.matchMedia(MOBILE_MEDIA_QUERY).matches;
	}
	return window.innerWidth <= 768;
}

function matchesTabletViewport() {
	if (typeof window === "undefined") return false;
	if (typeof window.matchMedia === "function") {
		return (
			!window.matchMedia(MOBILE_MEDIA_QUERY).matches &&
			!window.matchMedia(DESKTOP_MEDIA_QUERY).matches
		);
	}
	return window.innerWidth > 768 && window.innerWidth < 901;
}

export default function LodgingGallery({ images = [], name }) {
	const [currentIndex, setCurrentIndex] = useState(0);
	const [isModalOpen, setIsModalOpen] = useState(false);
	const [isTabletContainer, setIsTabletContainer] = useState(
		matchesTabletViewport,
	);
	const [isMobileContainer, setIsMobileContainer] = useState(
		matchesMobileViewport,
	);
	const containerRef = useRef(null);
	const isTabletContainerRef = useRef(isTabletContainer);
	const isMobileContainerRef = useRef(isMobileContainer);
	const pendingArrowFocusRef = useRef(null);
	const previousButtonRef = useRef(null);
	const nextButtonRef = useRef(null);
	const thumbnailStripRef = useRef(null);
	const thumbnailRefs = useRef([]);
	const hasImages = images.length > 0;
	const previewImages = images.slice(0, 5);
	const previewIndex = Math.min(
		currentIndex,
		Math.max(previewImages.length - 1, 0),
	);

	useEffect(() => {
		const applyLayoutMode = (nextIsTablet, nextIsMobile) => {
			if (nextIsMobile !== isMobileContainerRef.current) {
				isMobileContainerRef.current = nextIsMobile;
				setIsMobileContainer(nextIsMobile);
			}

			if (nextIsTablet === isTabletContainerRef.current) return;

			pendingArrowFocusRef.current = null;
			if (!nextIsMobile) {
				if (document.activeElement === previousButtonRef.current) {
					pendingArrowFocusRef.current = "previous";
				} else if (document.activeElement === nextButtonRef.current) {
					pendingArrowFocusRef.current = "next";
				}
			}

			isTabletContainerRef.current = nextIsTablet;
			setIsTabletContainer(nextIsTablet);
		};

		const updateFromWidth = (width) => {
			if (typeof width === "number" && width > 0) {
				applyLayoutMode(width > 768 && width < 901, width <= 768);
				return true;
			}
			return false;
		};

		const updateForViewport = () => {
			const measuredWidth =
				containerRef.current?.getBoundingClientRect?.().width;
			if (updateFromWidth(measuredWidth)) return;
			applyLayoutMode(matchesTabletViewport(), matchesMobileViewport());
		};

		const initialWidth = containerRef.current?.getBoundingClientRect?.().width;
		updateFromWidth(initialWidth);

		let observer = null;
		if (typeof ResizeObserver !== "undefined" && containerRef.current) {
			observer = new ResizeObserver((entries) => {
				const entryWidth = entries[0]?.contentRect?.width;
				const width =
					typeof entryWidth === "number" && entryWidth > 0
						? entryWidth
						: containerRef.current?.getBoundingClientRect?.().width;
				if (!updateFromWidth(width)) {
					applyLayoutMode(matchesTabletViewport(), matchesMobileViewport());
				}
			});
			observer.observe(containerRef.current);
		}

		let cleanupViewportListener;
		if (typeof window.matchMedia === "function") {
			const mediaQueries = [
				window.matchMedia(MOBILE_MEDIA_QUERY),
				window.matchMedia(DESKTOP_MEDIA_QUERY),
			];
			if (mediaQueries.every((query) => query.addEventListener)) {
				mediaQueries.forEach((query) =>
					query.addEventListener("change", updateForViewport),
				);
				cleanupViewportListener = () =>
					mediaQueries.forEach((query) =>
						query.removeEventListener("change", updateForViewport),
					);
			}
		}

		if (!cleanupViewportListener) {
			window.addEventListener("resize", updateForViewport);
			cleanupViewportListener = () =>
				window.removeEventListener("resize", updateForViewport);
		}

		return () => {
			observer?.disconnect();
			cleanupViewportListener();
		};
	}, [hasImages]);

	useLayoutEffect(() => {
		const focusedArrow = pendingArrowFocusRef.current;
		if (!focusedArrow) return;

		pendingArrowFocusRef.current = null;
		const button =
			focusedArrow === "previous"
				? previousButtonRef.current
				: nextButtonRef.current;
		if (button && !button.disabled) button.focus();
	}, [isTabletContainer]);

	useEffect(() => {
		if (!isMobileContainer) return;
		const thumbnail = thumbnailRefs.current[previewIndex];
		if (!thumbnail || !thumbnailStripRef.current?.contains(thumbnail)) return;
		thumbnail.scrollIntoView?.({
			inline: "center",
			block: "nearest",
			behavior: window.matchMedia?.("(prefers-reduced-motion: reduce)").matches
				? "auto"
				: "smooth",
		});
	}, [isMobileContainer, previewIndex]);

	const previousButton = (
		<button
			ref={previousButtonRef}
			className="gallery-thumbs-arrow gallery-desktop-arrow gallery-desktop-arrow--prev"
			onClick={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
			disabled={currentIndex === 0}
			aria-label="Imagen anterior"
		>
			<ChevronUp size={20} aria-hidden="true" focusable="false" />
		</button>
	);
	const nextButton = (
		<button
			ref={nextButtonRef}
			className="gallery-thumbs-arrow gallery-desktop-arrow gallery-desktop-arrow--next"
			onClick={() =>
				setCurrentIndex((prev) => Math.min(previewImages.length - 1, prev + 1))
			}
			disabled={previewIndex === previewImages.length - 1}
			aria-label="Imagen siguiente"
		>
			<ChevronDown size={20} aria-hidden="true" focusable="false" />
		</button>
	);

	if (images.length === 0) {
		return (
			<section className="gallery-empty" aria-label="Galería de imágenes">
				<ImageOff size={24} aria-hidden="true" focusable="false" />
				<p>Este alojamiento todavía no tiene fotos.</p>
			</section>
		);
	}

	return (
		<>
			<div ref={containerRef} className="gallery-container">
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
								fetchPriority="high"
								width="800"
								height="600"
								onError={(event) => {
									event.currentTarget.src = FALLBACK_IMAGE;
								}}
							/>
						</button>
						{images.length > 1 && !isTabletContainer && (
							<div className="gallery-desktop-navigation">
								{previousButton}
								{nextButton}
							</div>
						)}
						{images.length > 1 && (
							<div className="gallery-mobile-controls">
								<button
									className="gallery-mobile-arrow gallery-mobile-arrow--prev"
									onClick={() =>
										setCurrentIndex((prev) => Math.max(0, prev - 1))
									}
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
									<ChevronRight
										size={24}
										aria-hidden="true"
										focusable="false"
									/>
								</button>
							</div>
						)}
					</div>
					{images.length > 1 && (
						<div className="gallery-thumbs-col">
							{isTabletContainer && previousButton}
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
										aria-current={
											previewIndex === index + 1 ? "true" : undefined
										}
									>
										<img
											src={url}
											alt={`${name} - ${index + 2}`}
											loading="lazy"
											fetchPriority="low"
											width="400"
											height="300"
											onError={(event) => {
												event.currentTarget.src = FALLBACK_THUMB;
											}}
										/>
									</button>
								))}
							</div>
							{isTabletContainer && nextButton}
							<div className="gallery-desktop-actions">
								<button
									className="gallery-more"
									onClick={() => {
										setCurrentIndex(0);
										setIsModalOpen(true);
									}}
								>
									Ver más
								</button>
							</div>
						</div>
					)}
				</div>
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
