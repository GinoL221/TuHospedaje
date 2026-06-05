import { Link, useLocation, useNavigate } from "react-router-dom";
import { useEffect } from "react";

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
    <main className="page-container">
      <h1>¡Reserva confirmada!</h1>

      <section>
        <h2>{lodging.name}</h2>
        <p>
          {lodging.city}, {lodging.country}
        </p>
        <p>
          {reservation.checkIn} → {reservation.checkOut}
        </p>
        <p>
          Huésped: <strong>{reservation.guestName}</strong>
        </p>
        <p>
          Total: <strong>${reservation.totalPrice?.toLocaleString()}</strong>
        </p>
        <p>Te enviamos un email de confirmación.</p>
      </section>

      <div>
        <Link to="/my-reservations">Ver mis reservas</Link>
        {" | "}
        <Link to="/">Volver al inicio</Link>
      </div>
    </main>
  );
}
