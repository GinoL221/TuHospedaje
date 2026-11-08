import { useState, useEffect, useRef, useCallback } from "react";
import {
	getFavorites,
	removeFavorite as removeFavoriteRequest,
} from "../../services/favoriteService";
import ProductCard from "../../components/ProductCard/ProductCard";
import { useDocumentTitle } from "../../hooks/useDocumentTitle";
import "../../App.css";
import "./FavoritesPage.css";

export default function FavoritesPage() {
	useDocumentTitle("Favoritos");
	const [favorites, setFavorites] = useState([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState("");
	const [removalErrors, setRemovalErrors] = useState({});
	const [pendingRemovals, setPendingRemovals] = useState(() => new Set());
	const pendingRemovalsRef = useRef(new Set());

	const loadFavorites = useCallback((isMountedRef) => {
		getFavorites()
			.then((data) => {
				if (isMountedRef && !isMountedRef.current) return;
				setFavorites(Array.isArray(data) ? data : []);
				setError("");
				setLoading(false);
			})
			.catch((err) => {
				if (isMountedRef && !isMountedRef.current) return;
				setError(err.message || "No se pudieron cargar los favoritos.");
				setLoading(false);
			});
	}, []);

	useEffect(() => {
		const isMountedRef = { current: true };
		loadFavorites(isMountedRef);

		const handleVisibilityChange = () => {
			if (document.visibilityState === "visible") {
				loadFavorites(isMountedRef);
			}
		};
		const handleFocus = () => {
			loadFavorites(isMountedRef);
		};

		window.addEventListener("focus", handleFocus);
		document.addEventListener("visibilitychange", handleVisibilityChange);

		return () => {
			isMountedRef.current = false;
			window.removeEventListener("focus", handleFocus);
			document.removeEventListener("visibilitychange", handleVisibilityChange);
		};
	}, [loadFavorites]);

	async function removeFavorite(id) {
		if (pendingRemovalsRef.current.has(id)) return;

		pendingRemovalsRef.current.add(id);
		setPendingRemovals(new Set(pendingRemovalsRef.current));
		setRemovalErrors((current) => ({ ...current, [id]: "" }));
		try {
			await removeFavoriteRequest(id);
			setFavorites((prev) => prev.filter((l) => l.id !== id));
		} catch (err) {
			setRemovalErrors((current) => ({
				...current,
				[id]:
					err.message || "No se pudo quitar de favoritos. Intentá nuevamente.",
			}));
		} finally {
			pendingRemovalsRef.current.delete(id);
			setPendingRemovals(new Set(pendingRemovalsRef.current));
		}
	}

	if (loading)
		return (
			<main className="page-container favorites-page">
				<p className="empty-state" role="status">
					Cargando...
				</p>
			</main>
		);

	return (
		<main className="page-container favorites-page">
			<h1 className="favorites-title">Mis favoritos</h1>
			{error ? (
				<p className="empty-state error" role="alert">
					{error}
				</p>
			) : favorites.length === 0 ? (
				<p className="empty-state">No tenés favoritos guardados.</p>
			) : (
				<div className="favorites-grid">
					{favorites.map((lodging) => (
						<div key={lodging.id} className="favorite-item">
							<ProductCard lodging={lodging} showFavoriteButton={false} />
							<button
								className="btn-remove-fav"
								onClick={() => removeFavorite(lodging.id)}
								disabled={pendingRemovals.has(lodging.id)}
							>
								{pendingRemovals.has(lodging.id)
									? "Quitando de favoritos"
									: "Quitar de favoritos"}
							</button>
							{removalErrors[lodging.id] && (
								<p className="favorite-remove-error" role="alert">
									{removalErrors[lodging.id]}
								</p>
							)}
						</div>
					))}
				</div>
			)}
		</main>
	);
}
