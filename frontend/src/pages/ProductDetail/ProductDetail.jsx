import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { get } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import DatePicker from "react-datepicker";
import ReservationModal from "../../components/Reservation/ReservationModal";
import "react-datepicker/dist/react-datepicker.css";
import "./ProductDetail.css";

export default function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [lodging, setLodging] = useState(null);
  const [showAllImages, setShowAllImages] = useState(false);
  const [checkIn, setCheckIn] = useState(null);
  const [checkOut, setCheckOut] = useState(null);
  const [occupiedDates, setOccupiedDates] = useState([]);
  const [showModal, setShowModal] = useState(false);

  function formatDate(date) {
    return date.toISOString().split("T")[0];
  }

  useEffect(() => {
    get(`/lodgings/${id}`).then(setLodging).catch(console.error);
  }, [id]);

  useEffect(() => {
    if (!checkIn || !checkOut) return;
    get(`/lodgings/${id}/availability?checkIn=${formatDate(checkIn)}&checkOut=${formatDate(checkOut)}`)
      .then((data) => {
        if (data && data.occupiedRanges) {
          setOccupiedDates(data.occupiedRanges);
        }
      })
      .catch(() => {});
  }, [id, checkIn, checkOut]);

  function isDateOccupied(date) {
    return occupiedDates.some(
      (range) => date >= new Date(range.checkIn) && date < new Date(range.checkOut)
    );
  }

  function calcNights() {
    if (!checkIn || !checkOut) return 0;
    return Math.round((checkOut - checkIn) / (1000 * 60 * 60 * 24));
  }

  if (!lodging)
    return (
      <main className="page-container">
        <p>Cargando...</p>
      </main>
    );

  const images = lodging.imageUrls || [];
  const nights = calcNights();
  const total = lodging.pricePerNight ? nights * lodging.pricePerNight : 0;

  return (
    <main className="page-container product-detail">
      <div className="detail-header">
        <h1>{lodging.name}</h1>
        <button className="back-arrow" onClick={() => navigate(-1)}>←</button>
      </div>
      <p className="location">{lodging.city}, {lodging.country}</p>

      {images.length > 0 && (
        <div className="gallery-wrapper">
          <div className="gallery">
            <div className="gallery-main">
              <img src={images[0]} alt={`${lodging.name} - 1`} />
            </div>
            {images.length > 1 && (
              <div className="gallery-grid">
                {images.slice(1, 5).map((url, i) => (
                  <img key={i} src={url} alt={`${lodging.name} - ${i + 2}`} />
                ))}
              </div>
            )}
          </div>
          {images.length > 5 && !showAllImages && (
            <button className="btn-show-more" onClick={() => setShowAllImages(true)}>Ver más</button>
          )}
          {showAllImages && (
            <>
              <div className="gallery-all">
                {images.slice(5).map((url, i) => (
                  <img key={i + 5} src={url} alt={`${lodging.name} - ${i + 6}`} />
                ))}
              </div>
              <button className="btn-show-more" onClick={() => setShowAllImages(false)}>Ver menos</button>
            </>
          )}
        </div>
      )}

      {lodging.pricePerNight && (
        <section className="booking-section">
          <div className="price-display">
            <strong>${lodging.pricePerNight.toLocaleString()}</strong> / noche
          </div>

          <div className="date-pickers">
            <div>
              <label>Check-in</label>
              <DatePicker
                selected={checkIn}
                onChange={(date) => setCheckIn(date)}
                selectsStart
                startDate={checkIn}
                endDate={checkOut}
                minDate={new Date()}
                filterDate={(date) => !isDateOccupied(date)}
                placeholderText="Check-in"
                dateFormat="dd/MM/yyyy"
              />
            </div>
            <div>
              <label>Check-out</label>
              <DatePicker
                selected={checkOut}
                onChange={(date) => setCheckOut(date)}
                selectsEnd
                startDate={checkIn}
                endDate={checkOut}
                minDate={checkIn || new Date()}
                filterDate={(date) => !isDateOccupied(date)}
                placeholderText="Check-out"
                dateFormat="dd/MM/yyyy"
              />
            </div>
          </div>

          {nights > 0 && (
            <p className="total-estimate">
              Total estimado: <strong>${total.toLocaleString()}</strong> ({nights} noches)
            </p>
          )}

          {user ? (
            <button className="btn-reserve" onClick={() => setShowModal(true)}>
              Reservar
            </button>
          ) : (
            <p className="login-prompt">
              <a href="/login">Iniciá sesión</a> para reservar
            </p>
          )}
        </section>
      )}

      <section className="description">
        <h2>Descripción</h2>
        <p>{lodging.description}</p>
      </section>

      {lodging.features && lodging.features.length > 0 && (
        <section className="features-section">
          <h2>Qué ofrece este lugar?</h2>
          <div className="features-grid">
            {lodging.features.map((f) => (
              <div key={f.id} className="feature-item">
                <span className="feature-icon">{f.icon}</span>
                <span className="feature-name">{f.name}</span>
              </div>
            ))}
          </div>
        </section>
      )}

      {showModal && (
        <ReservationModal
          lodging={lodging}
          checkIn={checkIn}
          checkOut={checkOut}
          nights={nights}
          total={total}
          onClose={() => setShowModal(false)}
          onSuccess={() => {
            setShowModal(false);
            navigate("/");
          }}
        />
      )}
    </main>
  );
}
