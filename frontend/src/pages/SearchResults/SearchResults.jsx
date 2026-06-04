import { useState, useEffect, useTransition } from "react";
import { useSearchParams } from "react-router-dom";
import { get } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import ProductCard from "../../components/ProductCard/ProductCard";
import "../../App.css";
import "./SearchResults.css";

export default function SearchResults() {
  const [searchParams] = useSearchParams();
  const { user } = useAuth();
  const [results, setResults] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [favoriteIds, setFavoriteIds] = useState(new Set());

  const city = searchParams.get("city") || "";
  const checkIn = searchParams.get("checkIn") || "";
  const checkOut = searchParams.get("checkOut") || "";

  const [filterCategory, setFilterCategory] = useState("");
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [, startTransition] = useTransition();

  function searchLodgings(params) {
    startTransition(() => {
      setLoading(true);
      setError("");
    });
    get(`/lodgings/search?${params.toString()}`)
      .then((data) => {
        setResults(Array.isArray(data) ? data : []);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  }

  useEffect(() => {
    get("/categories")
      .then((data) => setCategories(Array.isArray(data) ? data : []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!user) {
      setFavoriteIds(new Set());
      return;
    }
    get("/favorites")
      .then((data) => {
        if (Array.isArray(data))
          setFavoriteIds(new Set(data.map((l) => l.id)));
      })
      .catch(() => {});
  }, [user]);

  useEffect(() => {
    const params = new URLSearchParams();
    if (city) params.set("city", city);
    if (checkIn) params.set("checkIn", checkIn);
    if (checkOut) params.set("checkOut", checkOut);

    searchLodgings(params);
  }, [city, checkIn, checkOut]);

  function handleFilter() {
    const params = new URLSearchParams();
    if (city) params.set("city", city);
    if (checkIn) params.set("checkIn", checkIn);
    if (checkOut) params.set("checkOut", checkOut);
    if (filterCategory) params.set("category", filterCategory);
    if (minPrice) params.set("minPrice", minPrice);
    if (maxPrice) params.set("maxPrice", maxPrice);

    searchLodgings(params);
  }

  return (
    <main className="search-results page-container">
      <aside className="search-filters">
        <h3>Filtros</h3>

        <label>Categoría</label>
        <select value={filterCategory} onChange={(e) => setFilterCategory(e.target.value)}>
          <option value="">Todas</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>

        <label>Precio mínimo</label>
        <input type="number" value={minPrice} onChange={(e) => setMinPrice(e.target.value)} placeholder="$" />

        <label>Precio máximo</label>
        <input type="number" value={maxPrice} onChange={(e) => setMaxPrice(e.target.value)} placeholder="$" />

        <button onClick={handleFilter} className="btn-filter">Aplicar filtros</button>
      </aside>

      <section className="search-results-list">
        <h2>
          {city ? `Resultados para "${city}"` : "Todos los alojamientos"}
        </h2>
        {checkIn && checkOut && (
          <p className="search-dates">{checkIn} al {checkOut}</p>
        )}

        {loading ? (
          <p className="empty-state">Buscando...</p>
        ) : error ? (
          <p className="empty-state error">{error}</p>
        ) : results.length === 0 ? (
          <p className="empty-state">No se encontraron resultados para tu búsqueda.</p>
        ) : (
          <div className="hotel-list">
            {results.map((lodging) => (
              <ProductCard key={lodging.id} lodging={lodging} defaultFavorite={favoriteIds.has(lodging.id)} />
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
