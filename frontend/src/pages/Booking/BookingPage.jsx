import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { get, post } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";

import DatePicker from "react-datepicker";
import Icon from "../../components/Icons/Icon";

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
  const [occupiedDates, setOccupiedDates] = useState([]);
  const [guestPhone, setGuestPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

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

  useEffect(() => {
    get(`/lodgings/${lodgingId}/availability`)
      .then((data) => {
        if (data?.occupiedRanges) {
          setOccupiedDates(data.occupiedRanges);
        }
      })
      .catch(() => {});
  }, [lodgingId]);

  useEffect(() => {
    if (!checkIn || !checkOut) return;

    get(
      `/lodgings/${lodgingId}/availability?checkIn=${formatDate(checkIn)}&checkOut=${formatDate(checkOut)}`,
    )
      .then((data) => {
        if (data?.occupiedRanges) {
          setOccupiedDates(data.occupiedRanges);
        }
      })
      .catch(() => {});
  }, [lodgingId, checkIn, checkOut]);

  function isDateOccupied(date) {
    return occupiedDates.some(
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

    if (!checkIn || !checkOut) {
      setError("Seleccioná un rango de fechas.");
      return;
    }

    try {
      setLoading(true);

      const reservation = await post("/reservations", {
        lodgingId: Number(lodgingId),
        checkIn: formatDate(checkIn),
        checkOut: formatDate(checkOut),
        guestName: `${user.firstName} ${user.lastName}`,
        guestEmail: user.email,
        guestPhone,
      });

      navigate("/booking/confirmation", {
        state: {
          reservation,
          lodging,
        },
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  if (!lodging) {
    return (
      <main className="page-container">
        <p>Cargando...</p>
      </main>
    );
  }

  const nights = calcNights();
  const total = lodging.pricePerNight ? nights * lodging.pricePerNight : 0;

  return (
    <main className="page-container booking-page">
      <h1>Confirmar reserva</h1>

      <div className="booking-layout">
        <section className="booking-summary">
          <h2>{lodging.name}</h2>
          <p>
            {lodging.city}, {lodging.country}
          </p>
          <p>
            <strong>${lodging.pricePerNight?.toLocaleString()}</strong> / noche
          </p>
          {lodging.imageUrls?.[0] && (
            <img
              src={lodging.imageUrls[0]}
              alt={lodging.name}
              className="booking-image"
            />
          )}
          {lodging.description && (
            <p className="booking-description">{lodging.description}</p>
          )}
          {lodging.features && lodging.features.length > 0 && (
            <div className="booking-features">
              {lodging.features.map((f) => (
                <span key={f.id} className="booking-feature-item">
                  <Icon name={f.icon} size={16} />
                  {f.name}
                </span>
              ))}
            </div>
          )}
        </section>

        <form className="booking-form" onSubmit={handleSubmit}>
          <h2>Datos de la reserva</h2>

          <label>Nombre</label>
          <input value={user.firstName} readOnly />

          <label>Apellido</label>
          <input value={user.lastName} readOnly />

          <label>Email</label>
          <input value={user.email} readOnly />

          <label>Teléfono</label>
          <input
            value={guestPhone}
            onChange={(e) => setGuestPhone(e.target.value)}
            placeholder="Ingresá tu teléfono"
            required
          />

          <label>Check-in</label>
          <DatePicker
            selected={checkIn}
            onChange={(date) => setCheckIn(date)}
            selectsStart
            startDate={checkIn}
            endDate={checkOut}
            minDate={new Date()}
            filterDate={(date) => !isDateOccupied(date)}
            dateFormat="dd/MM/yyyy"
            placeholderText="Check-in"
          />

          <label>Check-out</label>
          <DatePicker
            selected={checkOut}
            onChange={(date) => setCheckOut(date)}
            selectsEnd
            startDate={checkIn}
            endDate={checkOut}
            minDate={checkIn || new Date()}
            filterDate={(date) => !isDateOccupied(date)}
            dateFormat="dd/MM/yyyy"
            placeholderText="Check-out"
          />

          {nights > 0 && (
            <p>
              {nights} noches — <strong>${total.toLocaleString()}</strong>
            </p>
          )}

          {error && <p className="error">{error}</p>}

          <button type="submit" disabled={loading || !checkIn || !checkOut}>
            {loading ? "Confirmando..." : "Confirmar reserva"}
          </button>
        </form>
      </div>
    </main>
  );
}
