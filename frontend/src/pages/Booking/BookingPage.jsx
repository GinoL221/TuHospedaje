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

        <form className="booking-form" onSubmit={handleSubmit}>
          <h2>Datos de la reserva</h2>

          <label htmlFor="booking-first-name">Nombre</label>
          <input id="booking-first-name" value={user.firstName} readOnly />

          <label htmlFor="booking-last-name">Apellido</label>
          <input id="booking-last-name" value={user.lastName} readOnly />

          <label htmlFor="booking-email">Email</label>
          <input id="booking-email" value={user.email} readOnly />

          <button
            type="button"
            className="guest-details-toggle"
            aria-expanded={guestDetailsExpanded}
            aria-controls="guest-details"
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
              <label htmlFor="booking-phone">Teléfono</label>
              <input
                id="booking-phone"
                ref={phoneInputRef}
                value={guestPhone}
                onChange={(event) => {
                  setGuestPhone(event.target.value);
                  setPhoneError("");
                }}
                placeholder="Ingresá tu teléfono"
                required
                aria-describedby={phoneError ? "booking-phone-error" : undefined}
              />
              {phoneError && (
                <p id="booking-phone-error" className="error" role="alert">
                  {phoneError}
                </p>
              )}
            </section>
          )}

          <label htmlFor="booking-notes">Notas</label>
          <textarea
            id="booking-notes"
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            placeholder="Indicaciones adicionales para tu reserva"
          />

          {availabilityStatus === "loading" && (
            <p className="availability-status" role="status">
              Comprobando disponibilidad...
            </p>
          )}
          {(availabilityStatus === "error" ||
            availabilityStatus === "stale") && (
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
            disabled={availabilityStatus === "loading"}
          />

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
            disabled={availabilityStatus === "loading"}
          />

          {nights > 0 && (
            <p className="booking-total">
              {nights} {nights === 1 ? "noche" : "noches"} —{" "}
              <strong>${total.toLocaleString()}</strong>
            </p>
          )}

          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={
              loading ||
              !checkIn ||
              !checkOut ||
              availabilityStatus !== "ready"
            }
          >
            {loading ? "Confirmando..." : "Confirmar reserva"}
          </button>
        </form>
      </div>
    </main>
  );
}
