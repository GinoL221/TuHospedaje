import { useState, useEffect } from "react";
import { useParams, useNavigate, useLocation, Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { get } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";

import DatePicker from "react-datepicker";
import ReviewsSection from "../../components/ReviewsSection/ReviewsSection";
import ShareModal from "../../components/ShareModal/ShareModal";
import GalleryModal from "../../components/GalleryModal/GalleryModal";
import Icon from "../../components/Icons/Icon";
import { minCheckoutDate } from "../../utils/dateRange";

import "./ProductDetail.css";

export default function ProductDetail() {
	const { id } = useParams();
	const navigate = useNavigate();
	const location = useLocation();
	const { user } = useAuth();
	const [lodging, setLodging] = useState(null);
	const [galleryIndex, setGalleryIndex] = useState(0);
	const [showGallery, setShowGallery] = useState(false);
	const [checkIn, setCheckIn] = useState(null);
	const [checkOut, setCheckOut] = useState(null);
	const [occupiedDates, setOccupiedDates] = useState([]);
	const [showShare, setShowShare] = useState(false);

	function formatDate(date) {
		return date.toISOString().split("T")[0];
	}

	useEffect(() => {
		get(`/lodgings/${id}`).then(setLodging).catch(console.error);
	}, [id]);

	useEffect(() => {
		get(`/lodgings/${id}/availability`)
			.then((data) => {
				if (data?.occupiedRanges) {
					setOccupiedDates(data.occupiedRanges);
				}
			})
			.catch(() => {});
	}, [id]);

	useEffect(() => {
		if (!checkIn || !checkOut) return;
		get(
			`/lodgings/${id}/availability?checkIn=${formatDate(checkIn)}&checkOut=${formatDate(checkOut)}`,
		)
			.then((data) => {
				if (data && data.occupiedRanges) {
					setOccupiedDates(data.occupiedRanges);
				}
			})
			.catch(() => {});
	}, [id, checkIn, checkOut]);

	function isDateOccupied(date) {
		return occupiedDates.some(
			(range) =>
				date >= new Date(range.checkIn) && date < new Date(range.checkOut),
		);
	}

	function calcNights() {
		if (!checkIn || !checkOut) return 0;
		return Math.round((checkOut - checkIn) / (1000 * 60 * 60 * 24));
	}

	if (!lodging)
		return (
			<main className="page-container">
				<p>Cargando...</p>
			</main>
		);

	const images = lodging.imageUrls || [];
	const nights = calcNights();
	const total = lodging.pricePerNight ? nights * lodging.pricePerNight : 0;

	return (
		<main className="page-container product-detail">
			<div className="detail-header">
				<button
					className="back-arrow"
					onClick={() => navigate(-1)}
					aria-label="Volver"
				>
					<ArrowLeft size={22} />
				</button>
				<div className="detail-title-group">
					<h1>{lodging.name}</h1>
					<span className="detail-location">
						{lodging.city}, {lodging.country}
					</span>
				</div>
				<button className="btn-share" onClick={() => setShowShare(true)}>
					Compartir
				</button>
			</div>

			{images.length > 0 && (
				<div className="gallery-wrapper">
					<div className="gallery-main">
						<button
							className="gallery-main-trigger"
							onClick={() => setShowGallery(true)}
							aria-label="Abrir galería"
						>
							<img
								src={images[galleryIndex]}
								alt={`${lodging.name} - ${galleryIndex + 1}`}
								loading="lazy"
								onError={(e) => {
									e.target.src = "https://placehold.co/800x600?text=Sin+imagen";
								}}
							/>
						</button>
					</div>
					{images.length > 1 && (
						<div className="gallery-thumbs-col">
							<button
								className="gallery-thumbs-arrow"
								onClick={() => setGalleryIndex((prev) => Math.max(0, prev - 1))}
								disabled={galleryIndex === 0}
								aria-label="Imagen anterior"
							>
								▲
							</button>
							<div className="gallery-thumbs">
								{images.map((url, i) => (
									<button
										key={i}
										className={`gallery-thumb ${galleryIndex === i ? "gallery-thumb--active" : ""}`}
										onClick={() => {
											setGalleryIndex(i);
											setShowGallery(true);
										}}
										aria-label={`Ver imagen ${i + 1}`}
									>
										<img
											src={url}
											alt={`${lodging.name} - ${i + 1}`}
											loading="lazy"
											onError={(e) => {
												e.target.src =
													"https://placehold.co/400x300?text=Sin+imagen";
											}}
										/>
									</button>
								))}
							</div>
							<button
								className="gallery-thumbs-arrow"
								onClick={() =>
									setGalleryIndex((prev) =>
										Math.min(images.length - 1, prev + 1),
									)
								}
								disabled={galleryIndex === images.length - 1}
								aria-label="Imagen siguiente"
							>
								▼
							</button>
						</div>
					)}
				</div>
			)}

			{lodging.pricePerNight && (
				<section className="booking-section">
					<div className="price-display">
						<strong>${lodging.pricePerNight.toLocaleString()}</strong> / noche
					</div>

					<div className="date-pickers">
						<div>
							<label htmlFor="product-check-in">Check-in</label>
							<DatePicker
								id="product-check-in"
								selected={checkIn}
								onChange={(date) => setCheckIn(date)}
								selectsStart
								startDate={checkIn}
								endDate={checkOut}
								minDate={new Date()}
								filterDate={(date) => !isDateOccupied(date)}
								placeholderText="Check-in"
								dateFormat="dd/MM/yyyy"
							/>
						</div>
						<div>
							<label htmlFor="product-check-out">Check-out</label>
							<DatePicker
								id="product-check-out"
								selected={checkOut}
								onChange={(date) => setCheckOut(date)}
								selectsEnd
								startDate={checkIn}
								endDate={checkOut}
								minDate={minCheckoutDate(checkIn)}
								filterDate={(date) => !isDateOccupied(date)}
								placeholderText="Check-out"
								dateFormat="dd/MM/yyyy"
							/>
						</div>
					</div>

					{nights > 0 && (
						<p className="total-estimate">
							Total estimado: <strong>${total.toLocaleString()}</strong> (
							{nights} noches)
						</p>
					)}

					{user ? (
						<button
							className="btn-reserve"
							onClick={() =>
								navigate(`/booking/${id}`, {
									state: {
										checkIn,
										checkOut,
									},
								})
							}
							disabled={!checkIn || !checkOut}
						>
							Reservar
						</button>
					) : (
						<p className="login-prompt">
							<Link to="/login" state={{ from: location }}>
								Iniciá sesión
							</Link>{" "}
							para reservar
						</p>
					)}
				</section>
			)}

			<section className="description">
				<h2>Descripción</h2>
				<p>{lodging.description}</p>
			</section>

			{lodging.features && lodging.features.length > 0 && (
				<section className="features-section">
					<h2>Qué ofrece este lugar?</h2>
					<div className="features-grid">
						{lodging.features.map((f) => (
							<div key={f.id} className="feature-item">
								<Icon name={f.icon} size={20} />
								<span className="feature-name">{f.name}</span>
							</div>
						))}
					</div>
				</section>
			)}

			{lodging.policies && lodging.policies.length > 0 && (
				<section className="policies-section">
					<h2 className="policies-title">Políticas</h2>
					<div className="policies-grid">
						{lodging.policies.map((p) => (
							<div key={p.id} className="policy-item">
								<h3>
									<Icon name={p.icon} size={18} /> {p.name}
								</h3>
								<p>{p.description}</p>
							</div>
						))}
					</div>
				</section>
			)}

			<ReviewsSection lodgingId={id} user={user} />

			{showShare && (
				<ShareModal lodging={lodging} onClose={() => setShowShare(false)} />
			)}

			{showGallery && (
				<GalleryModal
					images={images}
					currentIndex={galleryIndex}
					onClose={() => setShowGallery(false)}
					onNavigate={setGalleryIndex}
				/>
			)}
		</main>
	);
}
