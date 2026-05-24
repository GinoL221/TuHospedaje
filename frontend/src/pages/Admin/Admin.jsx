import { useState, useEffect } from "react";
import AdminLodgings from "./AdminLodgings";
import AdminCategories from "./AdminCategories";
import AdminFeatures from "./AdminFeatures";
import AdminUsers from "./AdminUsers";

import "./Admin.css";

export default function Admin() {
  const [tab, setTab] = useState("lodgings");
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    const check = () => {
      const isTouch = "ontouchstart" in window || navigator.maxTouchPoints > 0;
      setIsMobile(isTouch && window.innerWidth <= 1024);
    };
    check();
    window.addEventListener("resize", check);
    return () => window.removeEventListener("resize", check);
  }, []);

  if (isMobile) {
    return (
      <main className="page-container admin-page">
        <div className="mobile-block">
          <h2>Funcionalidad no disponible para dispositivos móviles</h2>
          <p>Por favor, accedé desde una computadora.</p>
        </div>
      </main>
    );
  }

  return (
    <main className="page-container admin-page">
      <h1>Panel de Administración</h1>

      <nav className="admin-menu">
        <button
          className={"menu-btn" + (tab === "lodgings" ? " active" : "")}
          onClick={() => setTab("lodgings")}
        >
          Alojamientos
        </button>
        <button
          className={"menu-btn" + (tab === "categories" ? " active" : "")}
          onClick={() => setTab("categories")}
        >
          Categorías
        </button>
        <button
          className={"menu-btn" + (tab === "features" ? " active" : "")}
          onClick={() => setTab("features")}
        >
          Características
        </button>
        <button
          className={"menu-btn" + (tab === "users" ? " active" : "")}
          onClick={() => setTab("users")}
        >
          Usuarios
        </button>
      </nav>

      {tab === "lodgings" && <AdminLodgings />}
      {tab === "categories" && <AdminCategories />}
      {tab === "features" && <AdminFeatures />}
      {tab === "users" && <AdminUsers />}
    </main>
  );
}
