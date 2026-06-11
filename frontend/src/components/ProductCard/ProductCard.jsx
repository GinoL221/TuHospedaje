import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { post, del } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import { Heart } from "lucide-react";
import "./ProductCard.css";

export default function ProductCard({ lodging, defaultFavorite = false, onFavoriteToggle, showFavoriteButton = true }) {
  const { user } = useAuth();
  const [isFavorite, setIsFavorite] = useState(defaultFavorite);
  const [imgError, setImgError] = useState(false);

  useEffect(() => {
    setIsFavorite(defaultFavorite);
  }, [defaultFavorite]);
  const imageUrl = imgError
    ? "https://placehold.co/400x300?text=Sin+imagen"
    : lodging.imageUrls?.[0] || "https://placehold.co/400x300?text=Sin+imagen";

  async function toggleFavorite(e) {
    e.preventDefault();
    e.stopPropagation();
    const next = !isFavorite;
    setIsFavorite(next);
    onFavoriteToggle?.(lodging.id, next);
    try {
      if (next) {
        await post(`/favorites/${lodging.id}`);
      } else {
        await del(`/favorites/${lodging.id}`);
      }
    } catch (err) {
      console.error(err);
      setIsFavorite(!next);
      onFavoriteToggle?.(lodging.id, !next);
    }
  }

  return (
    <Link to={`/lodgings/${lodging.id}`} className="hotel-card-link">
      <article className="hotel-card">
        <div className="hotel-card-img-wrapper">
          <img
            src={imageUrl}
            alt={lodging.name}
            width="400"
            height="300"
            loading="lazy"
            onError={() => setImgError(true)}
          />
          {user && showFavoriteButton && (
            <button
              className={`fav-btn ${isFavorite ? "fav-active" : ""}`}
              onClick={toggleFavorite}
              aria-label={
                isFavorite ? "Quitar de favoritos" : "Agregar a favoritos"
              }
            >
              <Heart
                size={20}
                fill={isFavorite ? "var(--primary)" : "none"}
                stroke={isFavorite ? "var(--primary)" : "var(--secondary)"}
              />
            </button>
          )}
          {lodging.averageRating > 0 && (
            <div className="card-rating-overlay">
              {Array.from({ length: 5 }, (_, i) => {
                const fill = lodging.averageRating - i;
                const cls = fill >= 1 ? "ov-star-full" : fill >= 0.25 ? "ov-star-half" : "ov-star-empty";
                return <span key={i} className={cls}>★</span>;
              })}
            </div>
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
