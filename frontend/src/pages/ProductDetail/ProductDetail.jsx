import { useRef, useState, useEffect } from "react";
import { useParams, useNavigate, useLocation, Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { getLodging } from "../../services/lodgingService";
import { useAuth } from "../../hooks/useAuth";
import useAvailability from "../../hooks/useAvailability";

import DatePicker from "react-datepicker";
import ReviewsSection from "../../components/ReviewsSection/ReviewsSection";
import ShareModal from "../../components/ShareModal/ShareModal";
import LodgingGallery from "../../components/LodgingGallery/LodgingGallery";
import Icon from "../../components/Icons/Icon";
import { minCheckoutDate } from "../../utils/dateRange";

import "./ProductDetail.css";

function toCalendarDay(value) {
	if (typeof value === "string") {
		const [year, month, day] = value.slice(0, 10).split("-").map(Number);
		if (year && month && day) return new Date(year, month - 1, day);
	}

	const date = new Date(value);
	return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function DateRangeInputs({
	checkIn,
	checkOut,
	activeField,
	onActivateField,
	drafts,
	errors,
	onEditDate,
	onCommitDate,
	inputRefs,
	calendarOpen,
	disabled,
	"aria-invalid": ariaInvalid,
	"aria-describedby": ariaDescribedBy,
}) {
	const formatDate = (date) =>
		date
			? new Intl.DateTimeFormat("es-AR", {
					day: "2-digit",
					month: "2-digit",
					year: "numeric",
				}).format(date)
			: "";
	return (
		<div className="date-range-inputs">
			{[
				["checkIn", "Check-in", "product-check-in", checkIn],
				["checkOut", "Check-out", "product-check-out", checkOut],
			].map(([field, label, inputId, date]) => (
				<div className="date-range-field" key={field}>
					<label htmlFor={inputId}>{label}</label>
					<input
						ref={(input) => {
							inputRefs.current[field] = input;
						}}
						id={inputId}
						aria-label={label}
						value={drafts[field] ?? formatDate(date)}
						placeholder={`Elegí el ${label.toLowerCase()}`}
						inputMode="numeric"
						disabled={disabled}
						required
						aria-required="true"
						aria-invalid={errors[field] ? "true" : ariaInvalid}
						aria-describedby={[
							ariaDescribedBy || "product-date-format",
							errors[field] && `product-${field}-error`,
						]
							.filter(Boolean)
							.join(" ")}
						aria-expanded={calendarOpen && activeField === field}
						aria-controls={calendarOpen ? "product-range-calendar" : undefined}
						aria-haspopup="dialog"
						onChange={(event) => onEditDate(field, event.target.value)}
						onBlur={() => onCommitDate(field)}
						onClick={() => onActivateField(field)}
						onFocus={() => onActivateField(field)}
						onKeyDown={(event) => {
							if (event.key === "Enter") {
								event.preventDefault();
								event.stopPropagation();
								onCommitDate(field);
							}
						}}
					/>
					{errors[field] && (
						<p
							id={`product-${field}-error`}
							className="availability-alert"
							role="alert"
						>
							{errors[field]}
						</p>
					)}
				</div>
			))}
		</div>
	);
}

export default function ProductDetail() {
	const { id } = useParams();
	const navigate = useNavigate();
	const location = useLocation();
	const { user } = useAuth();
	const [lodgingState, setLodgingState] = useState({
		id,
		lodging: null,
		error: false,
	});
	if (lodgingState.id !== id) {
		setLodgingState({ id, lodging: null, error: false });
	}
	const { lodging, error: lodgingError } = lodgingState;
	const [checkIn, setCheckIn] = useState(null);
	const [checkOut, setCheckOut] = useState(null);
	const [activeDateField, setActiveDateField] = useState("checkIn");
	const [calendarOpen, setCalendarOpen] = useState(false);
	const dateControlsRef = useRef(null);
	const dateInputRefs = useRef({});
	const ignoreFocusOpening = useRef(false);
	const [dateDrafts, setDateDrafts] = useState({
		checkIn: null,
		checkOut: null,
	});
	const [dateErrors, setDateErrors] = useState({});
	const hasDateDraft = Object.values(dateDrafts).some(
		(value) => value !== null,
	);
	const [selectionConflict, setSelectionConflict] = useState({
		lodgingId: id,
		visible: false,
	});
	if (selectionConflict.lodgingId !== id) {
		setSelectionConflict({ lodgingId: id, visible: false });
	}
	const [showShare, setShowShare] = useState(false);
	const {
		status: availabilityStatus,
		occupiedRanges,
		load: loadAvailability,
		retry: retryAvailability,
	} = useAvailability(id);

	useEffect(() => {
		let active = true;

		getLodging(id)
			.then((lodging) => {
				if (active) setLodgingState({ id, lodging, error: false });
			})
			.catch(() => {
				if (active) setLodgingState({ id, lodging: null, error: true });
			});

		return () => {
			active = false;
		};
	}, [id]);

	function retryLodging() {
		setLodgingState({ id, lodging: null, error: false });
		getLodging(id)
			.then((lodging) =>
				setLodgingState((current) =>
					current.id === id ? { id, lodging, error: false } : current,
				),
			)
			.catch(() =>
				setLodgingState((current) =>
					current.id === id ? { id, lodging: null, error: true } : current,
				),
			);
	}

	// A single effect replaces the two duplicated availability fetches: it
	// runs the dateless (full occupied-ranges) load on mount/reset, then
	// re-runs with the selected range only once both dates are picked (a
	// partial selection must not trigger a premature refetch).
	useEffect(() => {
		if ((checkIn && !checkOut) || (!checkIn && checkOut)) return;
		loadAvailability({ checkIn, checkOut });
	}, [checkIn, checkOut, loadAvailability]);

	function isDateOccupied(date) {
		const calendarDate = toCalendarDay(date);
		return occupiedRanges.some(
			(range) =>
				calendarDate >= toCalendarDay(range.checkIn) &&
				calendarDate < toCalendarDay(range.checkOut),
		);
	}

	function isRangeAvailable(start, end) {
		if (availabilityStatus !== "ready" || !start || !end) return false;
		const checkInDate = toCalendarDay(start);
		const checkOutDate = toCalendarDay(end);
		return !occupiedRanges.some(
			(range) =>
				checkInDate < toCalendarDay(range.checkOut) &&
				checkOutDate > toCalendarDay(range.checkIn),
		);
	}

	function activateDateField(field) {
		setActiveDateField(field === "checkOut" && !checkIn ? "checkIn" : field);
		if (ignoreFocusOpening.current) {
			ignoreFocusOpening.current = false;
			return;
		}
		setCalendarOpen(true);
	}

	function closeCalendar(restoreFocus = false) {
		setCalendarOpen(false);
		const input = dateInputRefs.current[activeDateField];
		if (restoreFocus && input && document.activeElement !== input) {
			ignoreFocusOpening.current = true;
			input.focus();
		}
	}

	function handleDateControlKeys(event) {
		if (event.key === "Escape") {
			event.preventDefault();
			event.stopPropagation();
			closeCalendar(true);
		} else if (
			event.key === "ArrowDown" &&
			Object.values(dateInputRefs.current).includes(event.target)
		) {
			event.preventDefault();
			setCalendarOpen(true);
			const day =
				dateControlsRef.current?.querySelector(
					".react-datepicker__day[tabindex='0'][aria-disabled='false']",
				) ||
				dateControlsRef.current?.querySelector(
					".react-datepicker__day[aria-disabled='false']",
				);
			day?.focus();
		}
	}

	function applyDate(date, field) {
		if (!date || availabilityStatus !== "ready") return false;
		if (field === "checkIn") {
			if (date < toCalendarDay(new Date()) || isDateOccupied(date))
				return false;
			setCheckIn(date);
			if (!checkOut || date >= checkOut || !isRangeAvailable(date, checkOut)) {
				setCheckOut(null);
			}
			setActiveDateField("checkOut");
		} else {
			if (!checkIn || date <= checkIn || !isRangeAvailable(checkIn, date))
				return false;
			setCheckOut(date);
		}
		setSelectionConflict({ lodgingId: id, visible: false });
		return true;
	}

	function selectCalendarDate(date) {
		if (applyDate(date, activeDateField)) {
			if (activeDateField === "checkOut") closeCalendar(true);
			setDateDrafts((current) => ({ ...current, [activeDateField]: null }));
			setDateErrors((current) => ({
				...current,
				[activeDateField]: undefined,
			}));
		}
	}

	function editTypedDate(field, value) {
		setDateDrafts((current) => ({ ...current, [field]: value }));
		setDateErrors((current) => ({ ...current, [field]: undefined }));
	}

	function commitTypedDate(field) {
		const draft = dateDrafts[field];
		if (draft === null) return;
		if (!draft.trim()) {
			if (field === "checkIn") {
				clearDates();
			} else {
				setCheckOut(null);
				setDateDrafts((current) => ({ ...current, checkOut: null }));
				setDateErrors((current) => ({ ...current, checkOut: undefined }));
			}
			return;
		}
		const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(draft.trim());
		const date = match
			? new Date(Number(match[3]), Number(match[2]) - 1, Number(match[1]))
			: null;
		if (
			!date ||
			date.getFullYear() !== Number(match[3]) ||
			date.getMonth() !== Number(match[2]) - 1 ||
			date.getDate() !== Number(match[1])
		) {
			setDateErrors((current) => ({
				...current,
				[field]: "Ingresá una fecha válida en formato dd/mm/aaaa.",
			}));
			return;
		}
		if (!applyDate(date, field)) {
			setDateErrors((current) => ({
				...current,
				[field]: "La fecha no está disponible o no forma un rango válido.",
			}));
			return;
		}
		setDateDrafts((current) => ({ ...current, [field]: null }));
		setDateErrors((current) => ({ ...current, [field]: undefined }));
		if (field === "checkOut") closeCalendar(true);
	}

	function clearDates() {
		setCalendarOpen(false);
		setDateDrafts({ checkIn: null, checkOut: null });
		setDateErrors({});
		setCheckIn(null);
		setCheckOut(null);
		setActiveDateField("checkIn");
		setSelectionConflict({ lodgingId: id, visible: false });
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
		setActiveDateField("checkIn");
		setDateDrafts({ checkIn: null, checkOut: null });
		setDateErrors({});
		setSelectionConflict({ lodgingId: id, visible: true });
	}

	if (!lodging)
		return (
			<main className="page-container">
				{lodgingError ? (
					<div
						className="availability-alert"
						role="alert"
						aria-live="assertive"
						aria-atomic="true"
					>
						<p>No pudimos cargar el alojamiento.</p>
						<button type="button" onClick={retryLodging}>
							Reintentar
						</button>
					</div>
				) : (
					<p role="status" aria-live="polite">
						Cargando...
					</p>
				)}
			</main>
		);

	const nights = calcNights();
	const total = lodging.pricePerNight ? nights * lodging.pricePerNight : 0;
	const availabilityMessageId =
		availabilityStatus === "loading" ||
		(availabilityStatus === "ready" && occupiedRanges.length === 0)
			? "product-availability-status"
			: availabilityStatus === "error" || availabilityStatus === "stale"
				? "product-availability-alert"
				: selectionConflict.visible
					? "product-selection-conflict"
					: undefined;

	return (
		<main
			className="page-container product-detail"
			aria-labelledby="product-detail-title"
		>
			<header className="detail-header">
				<div className="detail-title-group">
					<h1 id="product-detail-title">{lodging.name}</h1>
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
					<ArrowLeft size={22} aria-hidden="true" focusable="false" />
				</button>
			</header>

			<LodgingGallery images={lodging.imageUrls} name={lodging.name} />

			{lodging.pricePerNight && (
				<section
					className="booking-section"
					aria-label="Reservar este alojamiento"
				>
					<div className="price-display">
						<strong>${lodging.pricePerNight.toLocaleString()}</strong> / noche
					</div>

					{availabilityStatus === "loading" && (
						<p
							id={availabilityMessageId}
							className="availability-status"
							role="status"
							aria-live="polite"
						>
							Comprobando disponibilidad...
						</p>
					)}
					{(availabilityStatus === "error" ||
						availabilityStatus === "stale") && (
						<div
							id={availabilityMessageId}
							className="availability-alert"
							role="alert"
							aria-live="assertive"
							aria-atomic="true"
						>
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
						<p
							id={availabilityMessageId}
							className="availability-status"
							role="status"
							aria-live="polite"
						>
							Todas las fechas están disponibles.
						</p>
					)}
					{selectionConflict.visible && (
						<p
							id={availabilityMessageId}
							className="availability-alert"
							role="alert"
							aria-live="assertive"
							aria-atomic="true"
						>
							Las fechas seleccionadas ya no están disponibles. Elegí otro
							rango.
						</p>
					)}

					<div
						className="date-pickers"
						role="group"
						aria-label="Fechas de la estadía"
						aria-describedby={availabilityMessageId}
					>
						<div
							className="date-range-controls"
							ref={dateControlsRef}
							onKeyDown={handleDateControlKeys}
							onBlur={(event) => {
								if (!event.currentTarget.contains(event.relatedTarget))
									closeCalendar();
							}}
						>
							<DateRangeInputs
								checkIn={checkIn}
								checkOut={checkOut}
								activeField={activeDateField}
								onActivateField={activateDateField}
								drafts={dateDrafts}
								errors={dateErrors}
								onEditDate={editTypedDate}
								onCommitDate={commitTypedDate}
								inputRefs={dateInputRefs}
								calendarOpen={calendarOpen}
								disabled={availabilityStatus !== "ready"}
								aria-invalid={selectionConflict.visible ? "true" : undefined}
								aria-describedby={availabilityMessageId}
							/>
							{calendarOpen && (
								<div
									id="product-range-calendar"
									className="product-datepicker-popper"
									role="dialog"
									aria-label="Calendario de la estadía"
								>
									<DatePicker
										inline
										selected={
											activeDateField === "checkIn" ? checkIn : checkOut
										}
										onChange={selectCalendarDate}
										selectsStart={activeDateField === "checkIn"}
										selectsEnd={activeDateField === "checkOut"}
										allowSameDay
										startDate={checkIn}
										endDate={checkOut}
										monthsShown={2}
										filterDate={(date) =>
											activeDateField === "checkIn"
												? availabilityStatus === "ready" &&
													!isDateOccupied(date)
												: Boolean(
														checkIn &&
														date > checkIn &&
														isRangeAvailable(checkIn, date),
													)
										}
										minDate={
											activeDateField === "checkOut" && checkIn
												? minCheckoutDate(checkIn)
												: new Date()
										}
										onClickOutside={(event) => {
											if (!dateControlsRef.current?.contains(event.target))
												closeCalendar();
										}}
									/>
								</div>
							)}
						</div>
						<p id="product-date-format" className="availability-status">
							Escribí dd/mm/aaaa y presioná Enter, o elegí las fechas en el
							calendario.
						</p>
						<div className="date-range-actions">
							<p className="date-range-instruction" aria-live="polite">
								{activeDateField === "checkIn"
									? "Elegí la entrada"
									: "Elegí la salida"}
							</p>
							<button
								type="button"
								className="date-range-clear"
								onClick={clearDates}
								disabled={!checkIn && !checkOut && !hasDateDraft}
							>
								Limpiar fechas
							</button>
						</div>
					</div>

					{nights > 0 && !hasDateDraft && (
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
							disabled={
								!checkIn ||
								!checkOut ||
								hasDateDraft ||
								availabilityStatus !== "ready"
							}
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

			<section
				className="description"
				aria-labelledby="product-description-title"
			>
				<h2 id="product-description-title">Descripción</h2>
				<p>{lodging.description}</p>
			</section>

			{lodging.features && lodging.features.length > 0 && (
				<section
					className="features-section"
					aria-labelledby="product-features-title"
				>
					<h2 id="product-features-title">Características</h2>
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
				<section
					className="policies-section"
					aria-labelledby="product-policies-title"
				>
					<h2 className="policies-title" id="product-policies-title">
						Políticas
					</h2>
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
