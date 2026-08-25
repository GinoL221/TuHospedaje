import { useState } from "react";
import { Link } from "react-router-dom";
import { post, del } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import { Heart } from "lucide-react";
import "./ProductCard.css";

export default function ProductCard({
	lodging,
	defaultFavorite = false,
	onFavoriteToggle,
	showFavoriteButton = true,
}) {
	const { user } = useAuth();
	const [optimisticFavorite, setOptimisticFavorite] = useState(null);
	const [imgError, setImgError] = useState(false);
	const [pending, setPending] = useState(false);
	const isFavorite = optimisticFavorite ?? defaultFavorite;

	const imageUrl = imgError
		? "https://placehold.co/400x300?text=Sin+imagen"
		: lodging.imageUrls?.[0] || "https://placehold.co/400x300?text=Sin+imagen";

	async function toggleFavorite(e) {
		e.preventDefault();
		e.stopPropagation();
		if (pending) return;
		const next = !isFavorite;
		setPending(true);
		setOptimisticFavorite(next);
		onFavoriteToggle?.(lodging.id, next);
		try {
			if (next) {
				await post(`/favorites/${lodging.id}`);
			} else {
				await del(`/favorites/${lodging.id}`);
			}
		} catch (err) {
			console.error(err);
			setOptimisticFavorite(null);
			onFavoriteToggle?.(lodging.id, !next);
		} finally {
			setPending(false);
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
							disabled={pending}
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
								const cls =
									fill >= 1
										? "ov-star-full"
										: fill >= 0.25
											? "ov-star-half"
											: "ov-star-empty";
								return (
									<span key={i} className={cls}>
										★
									</span>
								);
							})}
						</div>
					)}
				</div>
				<div className="hotel-card-body">
					<h3>{lodging.name}</h3>
					<p className="rating-summary">{Number(lodging.averageRating ?? 0).toFixed(1)} ({lodging.ratingCount ?? 0} opiniones)</p>
					<p className="location">
						{lodging.city}, {lodging.country}
					</p>
					<p className="description">{lodging.description}</p>
				</div>
			</article>
		</Link>
	);
}
