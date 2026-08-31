import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { get, post } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import useAvailability from "../../hooks/useAvailability";

import DatePicker from "react-datepicker";
import Icon from "../../components/Icons/Icon";
import LodgingGallery from "../../components/LodgingGallery/LodgingGallery";
import { minCheckoutDate } from "../../utils/dateRange";

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
    get("/reservations/my")
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
    get(`/lodgings/${lodgingId}`)
      .then(setLodging)
      .catch(() => {
        setError("No se pudo cargar el alojamiento.");
      });
  }, [lodgingId]);

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

  async function handleSubmit(e) {
    e.preventDefault();
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
      const reservation = await post("/reservations", {
        lodgingId: Number(lodgingId),
        checkIn: formatDate(checkIn),
        checkOut: formatDate(checkOut),
        guestName: `${user.firstName} ${user.lastName}`,
        guestEmail: user.email,
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

  if (!lodging) {
    return (
      <main
        className="page-container booking-page booking-page--loading"
        aria-busy="true"
      >
        <div className="booking-page-state" role="status" aria-live="polite">
          <span className="booking-state-indicator" aria-hidden="true" />
          <p>Cargando...</p>
        </div>
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
        <section
          className="booking-summary"
          aria-labelledby="booking-summary-title"
        >
          <div className="booking-summary-header">
            <p className="booking-section-kicker">Tu alojamiento</p>
            <h2 id="booking-summary-title">{lodging.name}</h2>
          </div>
          <p className="booking-location">
            {lodging.city}, {lodging.country}
          </p>
          <p className="booking-price">
            <strong>${lodging.pricePerNight?.toLocaleString()}</strong> / noche
          </p>
          <LodgingGallery images={lodging.imageUrls} name={lodging.name} />
          {lodging.description && (
            <p className="booking-description">{lodging.description}</p>
          )}
          {lodging.features && lodging.features.length > 0 && (
            <div className="booking-features">
              {lodging.features.map((f) => (
                <span key={f.id} className="booking-feature-item">
                  <Icon name={f.icon} size={14} />
                  {f.name}
                </span>
              ))}
            </div>
          )}
        </section>

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

          <fieldset className="booking-fieldset booking-identity-fieldset">
            <legend>Tus datos</legend>
            <div className="booking-field-grid booking-field-grid--identity">
              <div className="booking-field">
                <label htmlFor="booking-first-name">Nombre</label>
                <input
                  id="booking-first-name"
                  value={user.firstName}
                  readOnly
                  autoComplete="given-name"
                />
              </div>

              <div className="booking-field">
                <label htmlFor="booking-last-name">Apellido</label>
                <input
                  id="booking-last-name"
                  value={user.lastName}
                  readOnly
                  autoComplete="family-name"
                />
              </div>

              <div className="booking-field booking-field--full">
                <label htmlFor="booking-email">Email</label>
                <input
                  id="booking-email"
                  type="email"
                  value={user.email}
                  readOnly
                  autoComplete="email"
                />
              </div>
            </div>
          </fieldset>

          <fieldset className="booking-fieldset booking-guest-fieldset">
            <legend>Detalles del huésped</legend>
            <button
              type="button"
              className="guest-details-toggle"
              aria-expanded={guestDetailsExpanded}
              aria-controls="guest-details"
              aria-describedby={phoneError ? "booking-phone-error" : undefined}
              onClick={() => {
                if (guestDetailsExpanded && !guestPhone.trim()) {
                  setPhoneError("Ingresá un teléfono válido.");
                  return;
                }
                setGuestDetailsExpanded((expanded) => !expanded);
              }}
            >
              {guestDetailsExpanded
                ? "Ocultar detalles del huésped"
                : "Mostrar detalles del huésped"}
            </button>

            {guestDetailsExpanded && (
              <section id="guest-details" aria-label="Detalles del huésped">
                {user.imageUrl && (
                  <img
                    src={user.imageUrl}
                    alt={`Perfil de ${user.firstName} ${user.lastName}`}
                    className="guest-profile-image"
                  />
                )}
                <div className="booking-phone-field">
                  <label htmlFor="booking-phone">Teléfono</label>
                  <input
                    id="booking-phone"
                    ref={phoneInputRef}
                    type="tel"
                    inputMode="tel"
                    autoComplete="tel"
                    value={guestPhone}
                    onChange={(event) => {
                      setGuestPhone(event.target.value);
                      setPhoneError("");
                    }}
                    placeholder="Ingresá tu teléfono"
                    aria-required="true"
                    aria-invalid={phoneError ? "true" : undefined}
                    aria-describedby={
                      phoneError ? "booking-phone-error" : undefined
                    }
                  />
                  {phoneError && (
                    <p
                      id="booking-phone-error"
                      className="error"
                      role="alert"
                      aria-live="assertive"
                    >
                      {phoneError}
                    </p>
                  )}
                </div>
              </section>
            )}
          </fieldset>

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
                  onChange={(date) => setCheckIn(date)}
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
                  onChange={(date) => setCheckOut(date)}
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
