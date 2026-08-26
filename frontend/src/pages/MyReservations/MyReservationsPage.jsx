import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Calendar, MapPin, User, Mail, Phone, ExternalLink } from "lucide-react";
import { get } from "../../services/api";
import { cancelReservation } from "../../services/reservationService";
import { hasReservationNotes, reservationCreatedAtLabel } from "../../utils/reservationPresentation";
import "./MyReservationsPage.css";

const BUSINESS_TIME_ZONE = "America/Argentina/Buenos_Aires";

function fmtDate(str) {
  return str ? str.split("-").reverse().join("/") : "-";
}

function calcNights(checkIn, checkOut) {
  if (!checkIn || !checkOut) return 0;
  return Math.round((new Date(checkOut) - new Date(checkIn)) / 86400000);
}

function businessDate() {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: BUSINESS_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
}

function canCancel(reservation) {
  return reservation.status === "CONFIRMED" && reservation.checkIn > businessDate();
}

export default function MyReservationsPage() {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [pendingIds, setPendingIds] = useState(new Set());
  const pendingIdsRef = useRef(new Set());
  const [cancellationErrors, setCancellationErrors] = useState({});

  useEffect(() => {
    get("/reservations/my")
      .then(setReservations)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  async function handleCancel(reservation) {
    if (
      pendingIdsRef.current.has(reservation.id) ||
      !window.confirm("¿Querés cancelar esta reserva?")
    ) return;

    pendingIdsRef.current.add(reservation.id);
    setPendingIds(new Set(pendingIdsRef.current));
    setCancellationErrors((current) => ({ ...current, [reservation.id]: "" }));
    try {
      const cancelled = await cancelReservation(reservation.id);
      setReservations((current) =>
        current.map((item) => (item.id === reservation.id ? cancelled : item)),
      );
    } catch (err) {
      setCancellationErrors((current) => ({
        ...current,
        [reservation.id]: err.message || "No se pudo cancelar. Intentá nuevamente.",
      }));
    } finally {
      pendingIdsRef.current.delete(reservation.id);
      setPendingIds(new Set(pendingIdsRef.current));
    }
  }

  if (loading) {
    return (
      <main className="page-container">
        <p>Cargando reservas...</p>
      </main>
    );
  }

  return (
    <main className="page-container my-reservations">
      <div className="reservations-header">
        <h1>Mis reservas</h1>
        {reservations.length > 0 && (
          <span className="reservations-count">
            {reservations.length} {reservations.length === 1 ? "reserva" : "reservas"}
          </span>
        )}
      </div>

      {error && <p className="error">{error}</p>}

      {!error && reservations.length === 0 ? (
        <div className="reservations-empty">
          <p>No tenés reservas todavía.</p>
          <Link to="/" className="reservations-back">Explorar alojamientos</Link>
        </div>
      ) : (
        <div className="reservations-list">
          {reservations.map((r) => {
            const nights = calcNights(r.checkIn, r.checkOut);
            const createdAtLabel = reservationCreatedAtLabel(r);
            return (
              <article key={r.id} className="reservation-card">
                <div className="reservation-card-header">
                  <div>
                    <h2>{r.lodgingName}</h2>
                    <p className="reservation-location">
                      <MapPin size={13} />
                      {r.city}
                    </p>
                  </div>
                  <span className={`reservation-status ${r.status?.toLowerCase()}`}>
                    {r.status}
                  </span>
                </div>

                <div className="reservation-card-body">
                  <div className="reservation-row">
                    <Calendar size={14} />
                    <span>{fmtDate(r.checkIn)} → {fmtDate(r.checkOut)}</span>
                    {nights > 0 && (
                      <span className="reservation-nights">
                        {nights} {nights === 1 ? "noche" : "noches"}
                      </span>
                    )}
                  </div>
                  <div className="reservation-row">
                    <User size={14} />
                    <span>{r.guestName}</span>
                  </div>
                  <div className="reservation-row">
                    <Mail size={14} />
                    <span>{r.guestEmail}</span>
                  </div>
                  <div className="reservation-row">
                    <Phone size={14} />
                    <span>{r.guestPhone}</span>
                  </div>
                  {createdAtLabel && (
                    <div className="reservation-row">
                      <Calendar size={14} />
                      <span>{createdAtLabel}</span>
                    </div>
                  )}
                  {hasReservationNotes(r.notes) && (
                    <div className="reservation-row">
                      <span>Notas: {r.notes.trim()}</span>
                    </div>
                  )}
                </div>

                <div className="reservation-card-footer">
                  <span className="reservation-total">
                    Total: <strong>${r.totalPrice?.toLocaleString()}</strong>
                  </span>
                  <Link to={`/lodgings/${r.lodgingId}`} className="reservation-link">
                    Ver alojamiento <ExternalLink size={13} />
                  </Link>
                </div>
                {canCancel(r) && (
                  <div className="reservation-actions">
                    <button
                      type="button"
                      className="reservation-cancel"
                      disabled={pendingIds.has(r.id)}
                      onClick={() => handleCancel(r)}
                    >
                      {pendingIds.has(r.id) ? "Cancelando..." : "Cancelar reserva"}
                    </button>
                    {cancellationErrors[r.id] && (
                      <p className="reservation-cancel-error" role="alert">
                        {cancellationErrors[r.id]}
                      </p>
                    )}
                  </div>
                )}
              </article>
            );
          })}
        </div>
      )}
    </main>
  );
}
