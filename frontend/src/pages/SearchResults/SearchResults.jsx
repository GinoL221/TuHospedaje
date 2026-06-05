import { useState, useEffect, useTransition } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { get } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import ProductCard from "../../components/ProductCard/ProductCard";
import CategoryCard from "../Home/CategoryCard";
import "../../App.css";
import "./SearchResults.css";

export default function SearchResults() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [results, setResults] = useState([]);
  const [categories, setCategories] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [favoriteIds, setFavoriteIds] = useState(new Set());

  const city = searchParams.get("city") || "";
  const checkIn = searchParams.get("checkIn") || "";
  const checkOut = searchParams.get("checkOut") || "";

  const [filterCategories, setFilterCategories] = useState(new Set());
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
    get("/lodgings/random")
      .then((data) => setRecommendations(Array.isArray(data) ? data : []))
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

  function toggleCategory(id) {
    setFilterCategories((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  function clearFilters() {
    setFilterCategories(new Set());
    setMinPrice("");
    setMaxPrice("");
    const params = new URLSearchParams();
    if (city) params.set("city", city);
    if (checkIn) params.set("checkIn", checkIn);
    if (checkOut) params.set("checkOut", checkOut);
    searchLodgings(params);
  }

  function handleFilter() {
    const params = new URLSearchParams();
    if (city) params.set("city", city);
    if (checkIn) params.set("checkIn", checkIn);
    if (checkOut) params.set("checkOut", checkOut);
    if (minPrice) params.set("minPrice", minPrice);
    if (maxPrice) params.set("maxPrice", maxPrice);

    if (filterCategories.size === 0) {
      searchLodgings(params);
    } else if (filterCategories.size === 1) {
      params.set("category", [...filterCategories][0]);
      searchLodgings(params);
    } else {
      // Multi-category: fetch all without category filter, then filter client-side
      get(`/lodgings/search?${params.toString()}`)
        .then((data) => {
          const all = Array.isArray(data) ? data : [];
          setResults(all.filter((l) => filterCategories.has(l.categoryId)));
          setLoading(false);
        })
        .catch((err) => {
          setError(err.message);
          setLoading(false);
        });
    }
  }

  return (
    <main className="search-results page-container">
      <aside className="search-filters">
        <h3>Filtros</h3>

        <label>Categorías</label>
        <div className="filter-checkboxes">
          {categories.map((c) => (
            <label key={c.id} className="filter-checkbox-label">
              <input
                type="checkbox"
                checked={filterCategories.has(c.id)}
                onChange={() => toggleCategory(c.id)}
              />
              {c.name}
            </label>
          ))}
        </div>

        <label>Precio mínimo</label>
        <input type="number" value={minPrice} onChange={(e) => setMinPrice(e.target.value)} placeholder="$" />

        <label>Precio máximo</label>
        <input type="number" value={maxPrice} onChange={(e) => setMaxPrice(e.target.value)} placeholder="$" />

        <button onClick={handleFilter} className="btn-filter">Aplicar filtros</button>
        <button onClick={clearFilters} className="btn-clear">Limpiar filtros</button>
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

        {categories.length > 0 && (
          <section className="categories" style={{ marginTop: "40px" }}>
            <h2>Categorías</h2>
            <div className="category-list">
              {categories.map((c) => (
                <CategoryCard
                  key={c.id}
                  category={c}
                  isActive={false}
                  onClick={() => navigate(`/search?city=${encodeURIComponent(city)}`)}
                />
              ))}
            </div>
          </section>
        )}

        {recommendations.length > 0 && (
          <section className="recommendations" style={{ marginTop: "40px" }}>
            <h2>Te puede interesar</h2>
            <div className="hotel-list">
              {recommendations.map((lodging) => (
                <ProductCard
                  key={lodging.id}
                  lodging={lodging}
                  defaultFavorite={favoriteIds.has(lodging.id)}
                />
              ))}
            </div>
          </section>
        )}
      </section>
    </main>
  );
}
