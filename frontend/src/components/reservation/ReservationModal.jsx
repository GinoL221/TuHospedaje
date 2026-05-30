import { useState } from "react";
import { post } from "../../services/api";
import "./ReservationModal.css";

export default function ReservationModal({
  lodging,
  checkIn,
  checkOut,
  nights,
  total,
  onClose,
  onSuccess,
}) {
  const [guestName, setGuestName] = useState("");
  const [guestEmail, setGuestEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  function formatDate(date) {
    return date.toISOString().split("T")[0];
  }

  async function handleConfirm(e) {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      await post("/reservations", {
        lodgingId: lodging.id,
        checkIn: formatDate(checkIn),
        checkOut: formatDate(checkOut),
        guestName,
        guestEmail,
      });
      onSuccess();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <h2>Confirmar reserva</h2>
        <button className="modal-close" onClick={onClose}>
          ×
        </button>

        <div className="modal-detail">
          <p>
            <strong>{lodging.name}</strong>
          </p>
          <p>
            {lodging.city}, {lodging.country}
          </p>
          <p>
            {formatDate(checkIn)} → {formatDate(checkOut)}
          </p>
          <p>
            {nights} noches × ${lodging.pricePerNight} ={" "}
            <strong>${total.toLocaleString()}</strong>
          </p>
        </div>

        <form onSubmit={handleConfirm}>
          <input
            type="text"
            placeholder="Nombre del huésped"
            value={guestName}
            onChange={(e) => setGuestName(e.target.value)}
            required
          />
          <input
            type="email"
            placeholder="Email del huésped"
            value={guestEmail}
            onChange={(e) => setGuestEmail(e.target.value)}
            required
          />
          {error && <p className="modal-error">{error}</p>}
          <button type="submit" className="btn-confirm" disabled={loading}>
            {loading ? "Reservando..." : "Confirmar reserva"}
          </button>
        </form>
      </div>
    </div>
  );
}
