import { useState, useEffect } from "react";
import { get, del } from "../../services/api";
import ProductCard from "../../components/ProductCard/ProductCard";
import "../../App.css";
import "./FavoritesPage.css";

export default function FavoritesPage() {
  const [favorites, setFavorites] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    get("/favorites")
      .then((data) => {
        setFavorites(Array.isArray(data) ? data : []);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message || "No se pudieron cargar los favoritos.");
        setLoading(false);
      });
  }, []);

  async function removeFavorite(id) {
    try {
      await del(`/favorites/${id}`);
      setFavorites((prev) => prev.filter((l) => l.id !== id));
    } catch (err) {
      console.error(err);
    }
  }

  if (loading)
    return (
      <main className="page-container favorites-page">
        <p className="empty-state">Cargando...</p>
      </main>
    );

  return (
    <main className="page-container favorites-page">
      <h1 className="favorites-title">Mis favoritos</h1>
      {error ? (
        <p className="empty-state error">{error}</p>
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
              >
                Quitar de favoritos
              </button>
            </div>
          ))}
        </div>
      )}
    </main>
  );
}
