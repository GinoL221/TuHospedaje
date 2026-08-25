import { useState, useEffect } from "react";
import { useParams, useNavigate, useLocation, Link } from "react-router-dom";
import {
	ArrowLeft,
} from "lucide-react";
import { get } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import useAvailability from "../../hooks/useAvailability";

import DatePicker from "react-datepicker";
import ReviewsSection from "../../components/ReviewsSection/ReviewsSection";
import ShareModal from "../../components/ShareModal/ShareModal";
import LodgingGallery from "../../components/LodgingGallery/LodgingGallery";
import Icon from "../../components/Icons/Icon";
import { minCheckoutDate } from "../../utils/dateRange";

import "./ProductDetail.css";

export default function ProductDetail() {
	const { id } = useParams();
	const navigate = useNavigate();
	const location = useLocation();
	const { user } = useAuth();
	const [lodging, setLodging] = useState(null);
	const [checkIn, setCheckIn] = useState(null);
	const [checkOut, setCheckOut] = useState(null);
	const [selectionConflict, setSelectionConflict] = useState({ lodgingId: id, visible: false });
	if (selectionConflict.lodgingId !== id) {
		setSelectionConflict({ lodgingId: id, visible: false });
	}
	const [showShare, setShowShare] = useState(false);
	const {
		status: availabilityStatus,
		occupiedRanges,
		load: loadAvailability,
		retry: retryAvailability,
		isRangeAvailable,
	} = useAvailability(id);

	useEffect(() => {
		get(`/lodgings/${id}`).then(setLodging).catch(console.error);
	}, [id]);

	// A single effect replaces the two duplicated availability fetches: it
	// runs the dateless (full occupied-ranges) load on mount/reset, then
	// re-runs with the selected range only once both dates are picked (a
	// partial selection must not trigger a premature refetch).
	useEffect(() => {
		if ((checkIn && !checkOut) || (!checkIn && checkOut)) return;
		loadAvailability({ checkIn, checkOut });
	}, [checkIn, checkOut, loadAvailability]);

	function isDateOccupied(date) {
		return occupiedRanges.some(
			(range) =>
				date >= new Date(range.checkIn) && date < new Date(range.checkOut),
		);
	}

	function calcNights() {
		if (!checkIn || !checkOut) return 0;
		return Math.round((checkOut - checkIn) / (1000 * 60 * 60 * 24));
	}

	// A prior selection can become invalid if a refreshed response reveals
	// it now overlaps a confirmed reservation (e.g. a concurrent booking).
	// No stale/failed result may authorize a booking, so the selection is
	// adjusted directly during render (React's documented "adjusting state
	// when a prop changes" pattern, not an effect) and the user gets an
	// explicit conflict message instead. Clearing checkIn/checkOut makes
	// this condition false on the immediate re-render, so it cannot loop.
	if (
		availabilityStatus === "ready" &&
		checkIn &&
		checkOut &&
		!isRangeAvailable(checkIn, checkOut)
	) {
		setCheckIn(null);
		setCheckOut(null);
		setSelectionConflict({ lodgingId: id, visible: true });
	}

	if (!lodging)
		return (
			<main className="page-container">
				<p>Cargando...</p>
			</main>
		);

	const nights = calcNights();
	const total = lodging.pricePerNight ? nights * lodging.pricePerNight : 0;

	return (
		<main className="page-container product-detail">
			<div className="detail-header">
				<div className="detail-title-group">
					<h1>{lodging.name}</h1>
					<span className="detail-location">
						{lodging.city}, {lodging.country}
					</span>
				</div>
				<button className="btn-share" onClick={() => setShowShare(true)}>
					Compartir
				</button>
				<button
					className="back-arrow"
					onClick={() => navigate(-1)}
					aria-label="Volver"
				>
					<ArrowLeft size={22} />
				</button>
			</div>

				<LodgingGallery images={lodging.imageUrls} name={lodging.name} />

			{lodging.pricePerNight && (
				<section className="booking-section">
					<div className="price-display">
						<strong>${lodging.pricePerNight.toLocaleString()}</strong> / noche
					</div>

					{availabilityStatus === "loading" && (
						<p className="availability-status" role="status">
							Comprobando disponibilidad...
						</p>
					)}
					{(availabilityStatus === "error" || availabilityStatus === "stale") && (
						<div className="availability-alert" role="alert">
							<p>
								{availabilityStatus === "stale"
									? "No pudimos actualizar la disponibilidad. Los datos mostrados pueden estar desactualizados."
									: "No pudimos obtener la disponibilidad de este alojamiento."}
							</p>
							<button type="button" onClick={retryAvailability}>
								Reintentar
							</button>
						</div>
					)}
					{availabilityStatus === "ready" && occupiedRanges.length === 0 && (
						<p className="availability-status" role="status">
							Todas las fechas están disponibles.
						</p>
					)}
					{selectionConflict.visible && (
						<p className="availability-alert" role="alert">
							Las fechas seleccionadas ya no están disponibles. Elegí otro
							rango.
						</p>
					)}

					<div className="date-pickers">
						<div>
							<label htmlFor="product-check-in">Check-in</label>
							<DatePicker
								id="product-check-in"
								selected={checkIn}
								onChange={(date) => {
									setSelectionConflict({ lodgingId: id, visible: false });
									setCheckIn(date);
								}}
								selectsStart
								startDate={checkIn}
								endDate={checkOut}
								minDate={new Date()}
								filterDate={(date) => !isDateOccupied(date)}
								placeholderText="Check-in"
								dateFormat="dd/MM/yyyy"
								disabled={availabilityStatus !== "ready"}
							/>
						</div>
						<div>
							<label htmlFor="product-check-out">Check-out</label>
							<DatePicker
								id="product-check-out"
								selected={checkOut}
								onChange={(date) => {
									setSelectionConflict({ lodgingId: id, visible: false });
									setCheckOut(date);
								}}
								selectsEnd
								startDate={checkIn}
								endDate={checkOut}
								minDate={minCheckoutDate(checkIn)}
								filterDate={(date) => !isDateOccupied(date)}
								placeholderText="Check-out"
								dateFormat="dd/MM/yyyy"
								disabled={availabilityStatus !== "ready"}
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
							disabled={!checkIn || !checkOut || availabilityStatus !== "ready"}
						>
							Reservar
						</button>
					) : (
						<p className="login-prompt">
							<Link
								to="/login"
								state={{
									from: location,
									message:
										"Para reservar necesitás iniciar sesión. Si no tenés cuenta, registrate.",
								}}
							>
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
					<h2>Características</h2>
					<div className="features-grid">
						{lodging.features.map((f) => (
							<div key={f.id} className="feature-item">
								<span role="img" aria-label={`Ícono de ${f.name}`}>
									<Icon name={f.icon} size={20} />
								</span>
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

		</main>
	);
}
