import DatePicker from "react-datepicker";
import { minCheckoutDate } from "../../utils/dateRange";

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
}) {
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
            selected={checkIn}
            onChange={onCheckInChange}
            selectsStart
            startDate={checkIn}
            endDate={checkOut}
            minDate={new Date()}
            filterDate={(date) => !isDateOccupied(date)}
            dateFormat="dd/MM/yyyy"
            placeholderText="Check-in"
            aria-required="true"
            aria-describedby={availabilityMessageId}
            disabled={availabilityStatus === "loading"}
          />
        </div>

        <div className="booking-field">
          <label htmlFor="booking-check-out">Check-out</label>
          <DatePicker
            id="booking-check-out"
            selected={checkOut}
            onChange={onCheckOutChange}
            selectsEnd
            startDate={checkIn}
            endDate={checkOut}
            minDate={minCheckoutDate(checkIn)}
            filterDate={(date) => !isDateOccupied(date)}
            dateFormat="dd/MM/yyyy"
            placeholderText="Check-out"
            aria-required="true"
            aria-describedby={availabilityMessageId}
            disabled={availabilityStatus === "loading"}
          />
        </div>
      </div>
    </fieldset>
  );
}
