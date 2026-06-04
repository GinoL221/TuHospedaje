import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { get } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import ProductCard from "../../components/ProductCard/ProductCard";
import "../../App.css";
import "./Home.css";

export default function Home() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [lodgings, setLodgings] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [city, setCity] = useState("");
  const [checkIn, setCheckIn] = useState("");
  const [checkOut, setCheckOut] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [loadingCities, setLoadingCities] = useState(false);
  const [searchError, setSearchError] = useState("");
  const [favoriteIds, setFavoriteIds] = useState(new Set());
  const debounceRef = useRef();

  useEffect(() => {
    if (selectedCategory) {
      get(`/lodgings?category=${selectedCategory}`)
        .then(setLodgings)
        .catch(console.error);
    } else {
      get("/lodgings/random").then(setLodgings).catch(console.error);
    }
  }, [selectedCategory]);

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
    if (city.length < 2) {
      setSuggestions([]);
      setShowSuggestions(false);
      setLoadingCities(false);
      return;
    }

    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setLoadingCities(true);
      setShowSuggestions(true);
      get(`/lodgings/cities?q=${encodeURIComponent(city)}`)
        .then((data) => {
          setSuggestions(Array.isArray(data) ? data : []);
          setLoadingCities(false);
        })
        .catch(() => {
          setSuggestions([]);
          setLoadingCities(false);
        });
    }, 200);

    return () => clearTimeout(debounceRef.current);
  }, [city]);

  function handleSearch(e) {
    e.preventDefault();
    setSearchError("");

    if (checkIn && checkOut && checkIn >= checkOut) {
      setSearchError("La fecha de check-out debe ser posterior al check-in");
      return;
    }

    const params = new URLSearchParams();
    if (city) params.set("city", city);
    if (checkIn) params.set("checkIn", checkIn);
    if (checkOut) params.set("checkOut", checkOut);

    navigate(`/search?${params.toString()}`);
  }

  function handleCityChange(value) {
    setCity(value);
    if (value.length < 2) {
      setSuggestions([]);
      setShowSuggestions(false);
      setLoadingCities(false);
    }
  }

  return (
    <main className="home page-container">
      <section className="search">
        <div className="search-card">
          <h2>Buscar hospedaje</h2>
          <form onSubmit={handleSearch}>
            <div style={{ position: "relative", width: "100%" }}>
              <input
                type="text"
                placeholder="Ciudad"
                value={city}
                onChange={(e) => handleCityChange(e.target.value)}
                onFocus={() => setShowSuggestions(true)}
                onBlur={() => setTimeout(() => setShowSuggestions(false), 300)}
              />
              {showSuggestions && (
                <ul className="city-suggestions">
                  {loadingCities ? (
                    <li className="city-suggestions-loading">Buscando...</li>
                  ) : suggestions.length === 0 ? (
                    <li className="city-suggestions-empty">Sin resultados</li>
                  ) : (
                    suggestions.map((c) => (
                      <li key={c} onMouseDown={() => { setCity(c); setShowSuggestions(false); }}>
                        {c}
                      </li>
                    ))
                  )}
                </ul>
              )}
            </div>
            <input type="date" value={checkIn} onChange={(e) => setCheckIn(e.target.value)} />
            <input type="date" value={checkOut} onChange={(e) => setCheckOut(e.target.value)} />
            <button type="submit" className="btn-search">Buscar</button>
          </form>
          {searchError && <p className="search-error">{searchError}</p>}
        </div>
      </section>
      <section className="categories">
        <h2>Categorías</h2>
        {categories.length === 0 ? (
          <p className="empty-state">No hay categorías disponibles.</p>
        ) : (
          <div className="category-list">
            {categories.map((c) => (
              <span
                key={c.id}
                className={
                  "category-tag" + (selectedCategory === c.id ? " active" : "")
                }
                onClick={() =>
                  setSelectedCategory(selectedCategory === c.id ? null : c.id)
                }
              >
                {c.name}
              </span>
            ))}
          </div>
        )}
      </section>
      <section className="recommendations">
        <h2>
          {selectedCategory
            ? categories.find((c) => c.id === selectedCategory)?.name
            : "Recomendaciones"}
        </h2>
        {selectedCategory && (
          <button
            className="btn-clear-filter"
            onClick={() => setSelectedCategory(null)}
          >
            Mostrar todos
          </button>
        )}
        {lodgings.length === 0 ? (
          <p className="empty-state">
            No hay alojamientos cargados todavía. Volvé más tarde.
          </p>
        ) : (
          <div className="hotel-list">
            {lodgings.map((lodging) => (
              <ProductCard key={lodging.id} lodging={lodging} defaultFavorite={favoriteIds.has(lodging.id)} />
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
