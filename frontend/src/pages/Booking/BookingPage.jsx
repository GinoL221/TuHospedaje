import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { get, post } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";

import DatePicker from "react-datepicker";
import Icon from "../../components/Icons/Icon";
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
  const [occupiedDates, setOccupiedDates] = useState([]);
  const [guestPhone, setGuestPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    get("/reservations/my")
      .then((data) => {
        if (Array.isArray(data) && data.length > 0) {
          const last = data[data.length - 1];
          if (last.guestPhone) setGuestPhone(last.guestPhone);
        }
      })
      .catch(() => {});
  }, []);

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
          <p className="booking-location">
            {lodging.city}, {lodging.country}
          </p>
          <p className="booking-price">
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

          <label htmlFor="booking-phone">Teléfono</label>
          <input
            id="booking-phone"
            value={guestPhone}
            onChange={(e) => setGuestPhone(e.target.value)}
            placeholder="Ingresá tu teléfono"
            required
          />

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
          />

          {nights > 0 && (
            <p className="booking-total">
              {nights} {nights === 1 ? "noche" : "noches"} —{" "}
              <strong>${total.toLocaleString()}</strong>
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
