import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { get } from "../../services/api";
import "./MyReservationsPage.css";

export default function MyReservationsPage() {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    get("/reservations/my")
      .then(setReservations)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <main className="page-container">
        <p>Cargando reservas...</p>
      </main>
    );
  }

  return (
    <main className="page-container my-reservations">
      <h1>Mis reservas</h1>

      {error && <p className="error">{error}</p>}

      {!error && reservations.length === 0 ? (
        <div className="reservations-empty">
          <p>No tenés reservas todavía.</p>
        </div>
      ) : (
        <div className="reservations-list">
          {reservations.map((reservation) => (
            <article key={reservation.id} className="reservation-card">
              <h2>{reservation.lodgingName}</h2>
              <p>
                {reservation.city} — {reservation.checkIn} a{" "}
                {reservation.checkOut}
              </p>
              <p>Huésped: {reservation.guestName}</p>
              <p>Email: {reservation.guestEmail}</p>
              <p>Teléfono: {reservation.guestPhone}</p>
              <p>
                Total:{" "}
                <strong>${reservation.totalPrice?.toLocaleString()}</strong>
              </p>
              <span
                className={`reservation-status ${reservation.status?.toLowerCase()}`}
              >
                {reservation.status}
              </span>
            </article>
          ))}
        </div>
      )}

      <Link to="/" className="reservations-back">← Volver al inicio</Link>
    </main>
  );
}
