import Icon from "../../components/Icons/Icon";
import LodgingGallery from "../../components/LodgingGallery/LodgingGallery";

export default function BookingSummary({ lodging }) {
  return (
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
          {lodging.features.map((feature) => (
            <span key={feature.id} className="booking-feature-item">
              <Icon name={feature.icon} size={14} />
              {feature.name}
            </span>
          ))}
        </div>
      )}
    </section>
  );
}
