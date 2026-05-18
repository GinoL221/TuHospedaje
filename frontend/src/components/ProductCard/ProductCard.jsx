import { Link } from "react-router-dom";
import "./ProductCard.css";

export default function ProductCard({ lodging }) {
  const imageUrl = lodging.imageUrls?.[0] || "https://placehold.co/400x300";
  return (
    <Link to={`/lodgings/${lodging.id}`} className="hotel-card-link">
      <article className="hotel-card">
        <img src={imageUrl} alt={lodging.name} />
        <div className="hotel-card-body">
          <h3>{lodging.name}</h3>
          <p className="location">
            {lodging.city}, {lodging.country}
          </p>
          <p className="description">{lodging.description}</p>
        </div>
      </article>
    </Link>
  );
}
