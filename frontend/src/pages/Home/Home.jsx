import { useState, useEffect } from "react";
import { get } from "../../services/api";
import ProductCard from "../../components/ProductCard/ProductCard";
import "../../App.css";
import "./Home.css";

export default function Home() {
  const [lodgings, setLodgings] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);

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
  
  return (
    <main className="home page-container">
      <section className="search">
        <div className="search-card">
          <h2>Buscar hospedaje</h2>
          <form onSubmit={(e) => e.preventDefault()}>
            <input type="text" placeholder="Destino" />
            <input type="date" placeholder="Check-in" />
            <input type="date" placeholder="Check-out" />
            <button type="submit" className="btn-search">
              Buscar
            </button>
          </form>
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
              <ProductCard key={lodging.id} lodging={lodging} />
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
