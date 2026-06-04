import { useState, useEffect } from "react";
import { get, del } from "../../services/api";
import ProductCard from "../../components/ProductCard/ProductCard";
import "../../App.css";
import "./FavoritesPage.css";

export default function FavoritesPage() {
  const [favorites, setFavorites] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    get("/favorites")
      .then((data) => {
        setFavorites(Array.isArray(data) ? data : []);
        setLoading(false);
      })
      .catch(() => setLoading(false));
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
      <main className="page-container">
        <p>Cargando...</p>
      </main>
    );

  return (
    <main className="page-container">
      <h2>Mis favoritos</h2>
      {favorites.length === 0 ? (
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
