import { useCallback, useEffect, useRef, useState } from "react";
import DatePicker, { registerLocale } from "react-datepicker";
import { es } from "date-fns/locale/es";
import { minCheckoutDate } from "../../utils/dateRange";

registerLocale("es", es);

export default function BookingDateFields({
	availabilityStatus,
	occupiedRanges,
	retryAvailability,
	availabilityMessageId,
	dateFieldsetDescribedBy,
	checkIn,
	checkOut,
	onCheckInChange,
	onCheckOutChange,
	isDateOccupied,
	isCheckOutDateAvailable,
}) {
	const escapeCloseIdRef = useRef(null);
	const restoreFramesRef = useRef({ outer: null, inner: null });
	const [suppressOpenForId, setSuppressOpenForId] = useState(null);

	const cancelPendingFocusRestore = useCallback(() => {
		if (restoreFramesRef.current.outer !== null) {
			cancelAnimationFrame(restoreFramesRef.current.outer);
			restoreFramesRef.current.outer = null;
		}
		if (restoreFramesRef.current.inner !== null) {
			cancelAnimationFrame(restoreFramesRef.current.inner);
			restoreFramesRef.current.inner = null;
		}
	}, []);

	useEffect(() => cancelPendingFocusRestore, [cancelPendingFocusRestore]);

	const handleDatePickerKeyDown = (id, event) => {
		if (event.key === "Escape") {
			escapeCloseIdRef.current = id;
		}
	};

	const handleCalendarOpen = (id) => {
		if (escapeCloseIdRef.current === id) {
			escapeCloseIdRef.current = null;
		}
		cancelPendingFocusRestore();
		setSuppressOpenForId((current) => (current === id ? null : current));
	};

	const handleCalendarClose = (id) => {
		if (escapeCloseIdRef.current !== id) return;
		escapeCloseIdRef.current = null;
		cancelPendingFocusRestore();
		setSuppressOpenForId(id);

		// Wait two animation frames so focus is restored strictly after
		// react-datepicker's single-rAF deferBlur() finishes (WebKit runs
		// setTimeout(0) before rAF, letting deferBlur() blur back to body).
		restoreFramesRef.current.outer = requestAnimationFrame(() => {
			restoreFramesRef.current.outer = null;
			restoreFramesRef.current.inner = requestAnimationFrame(() => {
				restoreFramesRef.current.inner = null;
				const input = document.getElementById(id);
				if (input && document.activeElement !== input) {
					input.focus({ preventScroll: true });
				}
				setSuppressOpenForId((current) => (current === id ? null : current));
			});
		});
	};

	return (
		<fieldset
			className="booking-fieldset booking-date-fieldset"
			aria-describedby={dateFieldsetDescribedBy}
		>
			<legend>Fechas de la estadía</legend>

			<div className="booking-availability">
				{availabilityStatus === "loading" && (
					<p
						id={availabilityMessageId}
						className="availability-status availability-status--loading"
						role="status"
						aria-live="polite"
					>
						Comprobando disponibilidad...
					</p>
				)}
				{(availabilityStatus === "error" || availabilityStatus === "stale") && (
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
						className="availability-status availability-status--ready"
						role="status"
						aria-live="polite"
					>
						Todas las fechas están disponibles.
					</p>
				)}
			</div>

			<div className="booking-field-grid booking-field-grid--dates">
				<div className="booking-field">
					<label htmlFor="booking-check-in">Check-in</label>
					<DatePicker
						id="booking-check-in"
						name="checkIn"
						locale="es"
						selected={checkIn}
						onChange={onCheckInChange}
						onKeyDown={(event) =>
							handleDatePickerKeyDown("booking-check-in", event)
						}
						onCalendarOpen={() => handleCalendarOpen("booking-check-in")}
						onCalendarClose={() => handleCalendarClose("booking-check-in")}
						preventOpenOnFocus={suppressOpenForId === "booking-check-in"}
						selectsStart
						startDate={checkIn}
						endDate={checkOut}
						minDate={new Date()}
						filterDate={(date) => !isDateOccupied(date)}
						dateFormat="dd/MM/yyyy"
						placeholderText="Check-in"
						aria-required="true"
						aria-invalid="false"
						aria-describedby={availabilityMessageId}
						customInput={<input enterKeyHint="next" />}
						disabled={availabilityStatus === "loading"}
					/>
				</div>

				<div className="booking-field">
					<label htmlFor="booking-check-out">Check-out</label>
					<DatePicker
						id="booking-check-out"
						name="checkOut"
						locale="es"
						selected={checkOut}
						onChange={onCheckOutChange}
						onKeyDown={(event) =>
							handleDatePickerKeyDown("booking-check-out", event)
						}
						onCalendarOpen={() => handleCalendarOpen("booking-check-out")}
						onCalendarClose={() => handleCalendarClose("booking-check-out")}
						preventOpenOnFocus={suppressOpenForId === "booking-check-out"}
						selectsEnd
						startDate={checkIn}
						endDate={checkOut}
						minDate={minCheckoutDate(checkIn)}
						filterDate={isCheckOutDateAvailable}
						dateFormat="dd/MM/yyyy"
						placeholderText="Check-out"
						aria-required="true"
						aria-invalid="false"
						aria-describedby={availabilityMessageId}
						customInput={<input enterKeyHint="next" />}
						disabled={availabilityStatus === "loading"}
					/>
				</div>
			</div>
		</fieldset>
	);
}
