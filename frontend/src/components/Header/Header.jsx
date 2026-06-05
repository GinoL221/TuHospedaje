import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Menu, X } from "lucide-react";
import { useAuth } from "../../hooks/useAuth";

import logo from "../../assets/images/TuHospedaje_Isologotipo.png";

export default function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const goToAdmin = () => {
    if (user?.role === "ADMIN") navigate("/admin");
  };

  return (
    <header>
      <nav className="page-container">
        <div className="logo-container">
          <Link to="/">
            <img src={logo} alt="TuHospedaje" className="logo" />
          </Link>
          <p className="tagline">Encuentra tu lugar ideal al mejor precio</p>
        </div>
        <button
          className="hamburger-btn"
          onClick={() => setMenuOpen((prev) => !prev)}
          aria-label={menuOpen ? "Cerrar menú" : "Abrir menú"}
        >
          {menuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
        <div className={`nav-links${menuOpen ? " nav-links--open" : ""}`}>
          {user ? (
            <>
              <img
                src={
                  user.imageUrl ||
                  `https://ui-avatars.com/api/?name=${encodeURIComponent(user.firstName)}&background=264653&color=fff&size=36`
                }
                alt={user.firstName}
                className="avatar"
                onClick={goToAdmin}
                style={{
                  cursor: user?.role === "ADMIN" ? "pointer" : "default",
                }}
                title={
                  user?.role === "ADMIN" ? "Ir al panel de administración" : ""
                }
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(user.firstName)}&background=264653&color=fff&size=36`;
                }}
              />
              <span
                onClick={goToAdmin}
                style={{
                  cursor: user?.role === "ADMIN" ? "pointer" : "default",
                }}
                title={
                  user?.role === "ADMIN" ? "Ir al panel de administración" : ""
                }
              >
                {user.firstName}
              </span>
              <Link
                to="/favorites"
                className="nav-link"
                onClick={() => setMenuOpen(false)}
              >
                Favoritos
              </Link>
              <Link
                to="/my-reservations"
                className="nav-link"
                onClick={() => setMenuOpen(false)}
              >
                Mis reservas
              </Link>
              <button
                onClick={() => {
                  logout();
                  setMenuOpen(false);
                }}
                className="btn-logout"
              >
                Cerrar sesión
              </button>
            </>
          ) : (
            <>
              <Link to="/login" onClick={() => setMenuOpen(false)}>
                Iniciar sesión
              </Link>
              <Link
                to="/register"
                className="btn-secondary"
                onClick={() => setMenuOpen(false)}
              >
                Crear cuenta
              </Link>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}
