import { Link, useLocation, useNavigate } from "react-router-dom";
import { useEffect } from "react";
import { CheckCircle, Mail } from "lucide-react";
import { hasReservationNotes } from "../../utils/reservationPresentation";
import "./BookingConfirmation.css";

function fmtDate(str) {
  return str ? str.split("-").reverse().join("/") : "-";
}

export default function BookingConfirmationPage() {
  const { state } = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    if (!state?.reservation || !state?.lodging) {
      navigate("/", { replace: true });
    }
  }, [state, navigate]);

  if (!state?.reservation || !state?.lodging) return null;

  const { reservation, lodging } = state;

  return (
    <main className="page-container confirmation-page">
      <div className="confirmation-header">
        <div className="confirmation-icon">
          <CheckCircle size={32} />
        </div>
        <h1>¡Reserva confirmada!</h1>
        <p>Tu reserva fue procesada exitosamente.</p>
      </div>

      <div className="confirmation-card">
        <h2>{lodging.name}</h2>
        <p className="confirmation-location">
          {lodging.city}, {lodging.country}
        </p>

        <div className="confirmation-rows">
          <div className="confirmation-row">
            <span className="confirmation-row-label">Reserva</span>
            <span className="confirmation-row-value">Reserva #{reservation.id}</span>
          </div>
          <div className="confirmation-row">
            <span className="confirmation-row-label">Fechas</span>
            <span className="confirmation-row-value">
              {fmtDate(reservation.checkIn)} → {fmtDate(reservation.checkOut)}
            </span>
          </div>
          <div className="confirmation-row">
            <span className="confirmation-row-label">Huésped</span>
            <span className="confirmation-row-value">{reservation.guestName}</span>
          </div>
          {reservation.guestEmail && (
            <div className="confirmation-row">
              <span className="confirmation-row-label">Email</span>
              <span className="confirmation-row-value">{reservation.guestEmail}</span>
            </div>
          )}
          {hasReservationNotes(reservation.notes) && (
            <div className="confirmation-row">
              <span className="confirmation-row-label">Notas</span>
              <span className="confirmation-row-value">{reservation.notes.trim()}</span>
            </div>
          )}
          <div className="confirmation-row confirmation-total">
            <span className="confirmation-row-label">Total</span>
            <span className="confirmation-row-value">
              ${reservation.totalPrice?.toLocaleString()}
            </span>
          </div>
        </div>

        <p className="confirmation-email-note">
          <Mail size={14} />
          Te enviamos un email de confirmación.
        </p>
      </div>

      <div className="confirmation-actions">
        <Link to="/my-reservations" className="btn-primary">
          Ver mis reservas
        </Link>
        <Link to="/" className="btn-ghost">
          Volver al inicio
        </Link>
      </div>
    </main>
  );
}
