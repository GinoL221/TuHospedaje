import { useState } from "react";
import { Link } from "react-router-dom";
import { post, del } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import "./ProductCard.css";

export default function ProductCard({ lodging }) {
  const { user } = useAuth();
  const [isFavorite, setIsFavorite] = useState(false);
  const imageUrl = lodging.imageUrls?.[0] || "https://placehold.co/400x300";

  async function toggleFavorite(e) {
    e.preventDefault();
    try {
      if (isFavorite) {
        await del(`/favorites/${lodging.id}`);
        setIsFavorite(false);
      } else {
        await post(`/favorites/${lodging.id}`);
        setIsFavorite(true);
      }
    } catch (err) {
      console.error(err);
    }
  }

  return (
    <Link to={`/lodgings/${lodging.id}`} className="hotel-card-link">
      <article className="hotel-card">
        <div className="hotel-card-img-wrapper">
          <img src={imageUrl} alt={lodging.name} />
          {user && (
            <button
              className={`fav-btn ${isFavorite ? "fav-active" : ""}`}
              onClick={toggleFavorite}
              aria-label={
                isFavorite ? "Quitar de favoritos" : "Agregar a favoritos"
              }
            >
              ♥
            </button>
          )}
        </div>
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
