import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getLodging } from "../../services/lodgingService";
import {
	createReservation,
	getMyReservations,
} from "../../services/reservationService";
import { useAuth } from "../../hooks/useAuth";
import useAvailability from "../../hooks/useAvailability";
import BookingDateFields from "./BookingDateFields";
import BookingSummary from "./BookingSummary";
import GuestDetails from "./GuestDetails";

import "./BookingPage.css";

export default function BookingPage() {
	const { lodgingId } = useParams();
	const navigate = useNavigate();
	const location = useLocation();
	const { user } = useAuth();

	const initialCheckIn = location.state?.checkIn
		? new Date(location.state.checkIn)
		: null;
	const initialCheckOut = location.state?.checkOut
		? new Date(location.state.checkOut)
		: null;

	const [lodging, setLodging] = useState(null);
	const [lodgingStatus, setLodgingStatus] = useState("loading");
	const [resolvedLodgingId, setResolvedLodgingId] = useState(null);
	const [lodgingRequestAttempt, setLodgingRequestAttempt] = useState(0);
	const lodgingRetryRef = useRef(null);
	const restoreRetryFocusRef = useRef(false);
	const [checkIn, setCheckIn] = useState(initialCheckIn);
	const [checkOut, setCheckOut] = useState(initialCheckOut);
	const [guestPhone, setGuestPhone] = useState("");
	const [notes, setNotes] = useState("");
	const [guestDetailsExpanded, setGuestDetailsExpanded] = useState(true);
	const [phoneError, setPhoneError] = useState("");
	const phoneInputRef = useRef(null);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState("");
	const {
		status: availabilityStatus,
		occupiedRanges,
		load: loadAvailability,
		retry: retryAvailability,
	} = useAvailability(lodgingId);

	useEffect(() => {
		getMyReservations()
			.then((data) => {
				if (Array.isArray(data) && data.length > 0) {
					// data is sorted checkIn DESC — first element is the most recent reservation
					const latest = data[0];
					if (latest.guestPhone?.trim()) {
						setGuestPhone(latest.guestPhone);
						setGuestDetailsExpanded(false);
					}
				}
			})
			.catch(() => {});
	}, []);

	useEffect(() => {
		if (phoneError && guestDetailsExpanded) phoneInputRef.current?.focus();
	}, [guestDetailsExpanded, phoneError]);

	function formatDate(date) {
		return date.toISOString().split("T")[0];
	}

	useEffect(() => {
		let active = true;

		getLodging(lodgingId)
			.then((data) => {
				if (!active) return;
				setLodging(data);
				setResolvedLodgingId(lodgingId);
				setLodgingStatus("ready");
				restoreRetryFocusRef.current = false;
			})
			.catch(() => {
				if (!active) return;
				setResolvedLodgingId(lodgingId);
				setLodgingStatus("error");
			});

		return () => {
			active = false;
		};
	}, [lodgingId, lodgingRequestAttempt]);

	useEffect(() => {
		if (lodgingStatus === "error" && restoreRetryFocusRef.current) {
			lodgingRetryRef.current?.focus();
			restoreRetryFocusRef.current = false;
		}
	}, [lodgingStatus]);

	function retryLodging() {
		restoreRetryFocusRef.current = true;
		setLodgingStatus("loading");
		setLodgingRequestAttempt((attempt) => attempt + 1);
	}

	// Replaces the two duplicated availability fetches (see ProductDetail):
	// dateless load on mount/reset, dated reload once both dates are picked.
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

	async function handleSubmit(event) {
		event.preventDefault();
		setError("");

		if (!guestPhone.trim()) {
			setGuestDetailsExpanded(true);
			setPhoneError("Ingresá un teléfono válido.");
			return;
		}

		if (!checkIn || !checkOut) {
			setError("Seleccioná un rango de fechas.");
			return;
		}

		// No attempt starts until the hook reports a current ready result —
		// stale/failed availability never proves a free range (US-23.2).
		if (availabilityStatus !== "ready") {
			setError(
				"Estamos verificando la disponibilidad. Probá de nuevo en un instante.",
			);
			return;
		}

		try {
			setLoading(true);

			// Client-side preflight is a UX improvement only; it never replaces
			// the backend's locked overlap check below.
			const preflight = await loadAvailability({ checkIn, checkOut });
			if (preflight?.available === false) {
				setError(
					"Las fechas seleccionadas ya no están disponibles. Elegí otro rango.",
				);
				return;
			}
			if (!preflight || preflight.available !== true) {
				setError(
					"No pudimos verificar la disponibilidad. Reintentá antes de confirmar la reserva.",
				);
				return;
			}

			const normalizedNotes = notes.trim();
			const reservation = await createReservation({
				lodgingId: Number(lodgingId),
				checkIn: formatDate(checkIn),
				checkOut: formatDate(checkOut),
				guestPhone,
				...(normalizedNotes ? { notes: normalizedNotes } : {}),
			});

			navigate("/booking/confirmation", {
				state: {
					reservation,
					lodging,
				},
			});
		} catch (err) {
			// Backend lock rejected an overlap the preflight missed (a race);
			// refresh availability so the user can recover.
			setError(err.message);
			retryAvailability();
		} finally {
			setLoading(false);
		}
	}

	function handleGuestDetailsToggle() {
		if (guestDetailsExpanded && !guestPhone.trim()) {
			setPhoneError("Ingresá un teléfono válido.");
			return;
		}
		setGuestDetailsExpanded((expanded) => !expanded);
	}

	function handleGuestPhoneChange(event) {
		setGuestPhone(event.target.value);
		setPhoneError("");
	}

	const rootLodgingStatus =
		resolvedLodgingId === lodgingId ? lodgingStatus : "loading";

	if (rootLodgingStatus !== "ready" || !lodging) {
		const isLoadingLodging = rootLodgingStatus === "loading";

		return (
			<main
				className="page-container booking-page booking-page--loading"
				aria-busy={isLoadingLodging ? "true" : "false"}
			>
				{isLoadingLodging ? (
					<div className="booking-page-state" role="status" aria-live="polite">
						<span className="booking-state-indicator" aria-hidden="true" />
						<p>Cargando...</p>
					</div>
				) : (
					<div
						className="booking-page-state booking-page-state--error"
						role="alert"
						aria-live="assertive"
					>
						<p>No se pudo cargar el alojamiento.</p>
						<button
							ref={lodgingRetryRef}
							type="button"
							aria-label="Reintentar alojamiento"
							onClick={retryLodging}
						>
							Reintentar
						</button>
					</div>
				)}
			</main>
		);
	}

	const nights = calcNights();
	const total = lodging.pricePerNight ? nights * lodging.pricePerNight : 0;
	const hasAvailabilityMessage =
		availabilityStatus === "loading" ||
		availabilityStatus === "error" ||
		availabilityStatus === "stale" ||
		(availabilityStatus === "ready" && occupiedRanges.length === 0);
	const availabilityMessageId = hasAvailabilityMessage
		? "booking-availability-message"
		: undefined;
	const dateFieldsetDescribedBy =
		[availabilityMessageId, error ? "booking-form-error" : undefined]
			.filter(Boolean)
			.join(" ") || undefined;

	return (
		<main className="page-container booking-page">
			<header className="booking-page-header">
				<h1>Confirmar reserva</h1>
			</header>

			<div className="booking-layout">
				<BookingSummary lodging={lodging} />

				<form
					className="booking-form"
					onSubmit={handleSubmit}
					aria-labelledby="booking-form-title"
					aria-describedby={error ? "booking-form-error" : undefined}
					aria-busy={loading}
				>
					<header className="booking-form-header">
						<h2 id="booking-form-title">Datos de la reserva</h2>
					</header>

					<GuestDetails
						user={user}
						guestPhone={guestPhone}
						guestDetailsExpanded={guestDetailsExpanded}
						phoneError={phoneError}
						phoneInputRef={phoneInputRef}
						onGuestPhoneChange={handleGuestPhoneChange}
						onToggle={handleGuestDetailsToggle}
					/>

					<BookingDateFields
						availabilityStatus={availabilityStatus}
						occupiedRanges={occupiedRanges}
						retryAvailability={retryAvailability}
						availabilityMessageId={availabilityMessageId}
						dateFieldsetDescribedBy={dateFieldsetDescribedBy}
						checkIn={checkIn}
						checkOut={checkOut}
						onCheckInChange={setCheckIn}
						onCheckOutChange={setCheckOut}
						isDateOccupied={isDateOccupied}
					/>

					<fieldset className="booking-fieldset booking-notes-fieldset">
						<legend>
							Notas adicionales <span>(opcional)</span>
						</legend>
						<div className="booking-field">
							<label htmlFor="booking-notes">Notas</label>
							<textarea
								id="booking-notes"
								value={notes}
								onChange={(event) => setNotes(event.target.value)}
								placeholder="Indicaciones adicionales para tu reserva"
								rows={4}
							/>
						</div>
					</fieldset>

					{nights > 0 && (
						<div className="booking-total" aria-live="polite">
							<span>
								{nights} {nights === 1 ? "noche" : "noches"} —{" "}
							</span>
							<strong>${total.toLocaleString()}</strong>
						</div>
					)}

					{error && (
						<p
							id="booking-form-error"
							className="error booking-form-error"
							role="alert"
							aria-live="assertive"
							aria-atomic="true"
						>
							{error}
						</p>
					)}

					<div className="booking-form-actions">
						<button
							type="submit"
							disabled={
								loading ||
								!checkIn ||
								!checkOut ||
								availabilityStatus !== "ready"
							}
							aria-busy={loading}
						>
							{loading ? "Confirmando..." : "Confirmar reserva"}
						</button>
					</div>
				</form>
			</div>
		</main>
	);
}
