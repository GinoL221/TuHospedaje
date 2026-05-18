import { useState, useEffect } from "react";
import { get } from "../../services/api";
import ProductCard from "../../components/ProductCard/ProductCard";
import "../../App.css";
import "./Home.css";

export default function Home() {
  const [lodgings, setLodgings] = useState([]);
  useEffect(() => {
    get("/lodgings/random")
      .then(setLodgings)
      .catch(console.error);
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
            <button type="submit" className="btn-search">Buscar</button>
          </form>
        </div>
      </section>
      <section className="categories">
        <h2>Categorías</h2>
        <p className="placeholder">(Sprint 2 — sección de categorías)</p>
      </section>
      <section className="recommendations">
        <h2>Recomendaciones</h2>
        <div className="hotel-list">
          {lodgings.map((lodging) => (
            <ProductCard key={lodging.id} lodging={lodging} />
          ))}
        </div>
      </section>
    </main>
  );
}