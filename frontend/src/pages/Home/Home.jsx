import { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import DatePicker from "react-datepicker";
import { get } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import ProductCard from "../../components/ProductCard/ProductCard";
import CategoryCard from "./CategoryCard";
import "../../App.css";
import "./Home.css";

export default function Home() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [lodgings, setLodgings] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [city, setCity] = useState("");
  const [checkIn, setCheckIn] = useState(null);
  const [checkOut, setCheckOut] = useState(null);
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [loadingCities, setLoadingCities] = useState(false);
  const [searchError, setSearchError] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [favoriteIds, setFavoriteIds] = useState(new Set());
  const debounceRef = useRef();

  const fetchLodgings = useCallback(() => {
    if (selectedCategory) {
      get(`/lodgings?category=${selectedCategory}`)
        .then((data) => {
          setLodgings(Array.isArray(data) ? data : []);
          setTotalPages(1);
        })
        .catch(console.error);
    } else {
      get(`/lodgings?page=${page}&size=8`)
        .then((data) => {
          setLodgings(data.lodgings || []);
          setTotalPages(data.totalPages || 1);
        })
        .catch(console.error);
    }
  }, [selectedCategory, page]);

  useEffect(() => {
    fetchLodgings();
  }, [fetchLodgings]);

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

  function formatDate(date) {
    return date ? date.toISOString().split("T")[0] : "";
  }

  function handleSearch(e) {
    e.preventDefault();
    setSearchError("");

    if (checkIn && checkOut && checkIn >= checkOut) {
      setSearchError("La fecha de check-out debe ser posterior al check-in");
      return;
    }

    const params = new URLSearchParams();
    if (city) params.set("city", city);
    if (checkIn) params.set("checkIn", formatDate(checkIn));
    if (checkOut) params.set("checkOut", formatDate(checkOut));

    navigate(`/search?${params.toString()}`);
  }

  function handleFavoriteToggle(id, add) {
    setFavoriteIds((prev) => {
      const next = new Set(prev);
      if (add) next.add(id);
      else next.delete(id);
      return next;
    });
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
            <div>
              <DatePicker
                selected={checkIn}
                onChange={(date) => setCheckIn(date)}
                selectsStart
                startDate={checkIn}
                endDate={checkOut}
                minDate={new Date()}
                placeholderText="Check-in"
                dateFormat="dd/MM/yyyy"
              />
            </div>
            <div>
              <DatePicker
                selected={checkOut}
                onChange={(date) => setCheckOut(date)}
                selectsEnd
                startDate={checkIn}
                endDate={checkOut}
                minDate={checkIn || new Date()}
                placeholderText="Check-out"
                dateFormat="dd/MM/yyyy"
              />
            </div>
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
              <CategoryCard
                key={c.id}
                category={c}
                isActive={selectedCategory === c.id}
                onClick={() => {
                  setSelectedCategory(selectedCategory === c.id ? null : c.id);
                  setPage(0);
                }}
              />
            ))}
          </div>
        )}
      </section>
      <section className="recommendations">
        <div className="section-header">
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
        </div>
        {lodgings.length === 0 ? (
          <p className="empty-state">
            No hay alojamientos cargados todavía. Volvé más tarde.
          </p>
        ) : (
          <div className="hotel-list">
            {lodgings.map((lodging) => (
              <ProductCard key={lodging.id} lodging={lodging} defaultFavorite={favoriteIds.has(lodging.id)} onFavoriteToggle={handleFavoriteToggle} />
            ))}
          </div>
        )}
        {!selectedCategory && totalPages > 1 && (
          <div className="home-pagination">
            <button disabled={page === 0} onClick={() => setPage(0)}>
              Inicio
            </button>
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Anterior
            </button>
            <span>Página {page + 1} de {totalPages}</span>
            <button disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
              Siguiente
            </button>
          </div>
        )}
      </section>
    </main>
  );
}
