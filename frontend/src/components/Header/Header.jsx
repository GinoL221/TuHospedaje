import { useState } from "react";
import { Link } from "react-router-dom";
import { Menu, X } from "lucide-react";
import { useAuth } from "../../hooks/useAuth";

import logo from "../../assets/images/TuHospedaje_Isologotipo.png";

export default function Header() {
	  const { user, logout, logoutError } = useAuth();
	const [menuOpen, setMenuOpen] = useState(false);

  return (
    <header className="site-header">
      <nav className="page-container">
        <div className="logo-container">
          <Link to="/" className="brand-link">
            <img src={logo} alt="TuHospedaje — Inicio" className="logo" />
          </Link>
          <p className="tagline">Encuentra tu lugar ideal al mejor precio</p>
        </div>
        <button
          type="button"
          className="hamburger-btn"
          onClick={() => setMenuOpen((prev) => !prev)}
          aria-label={menuOpen ? "Cerrar menú" : "Abrir menú"}
          aria-expanded={menuOpen}
          aria-controls="mobile-navigation"
        >
          {menuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
        <div
          id="mobile-navigation"
          className={`nav-links${menuOpen ? " nav-links--open" : ""}`}
        >
          {logoutError && <p role="alert">{logoutError}</p>}
          {user ? (
            <>
              <img
                src={
                  user.imageUrl ||
                  `https://ui-avatars.com/api/?name=${encodeURIComponent(user.firstName)}&background=264653&color=fff&size=36`
                }
					alt={user.firstName}
					className="avatar"
					onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(user.firstName)}&background=264653&color=fff&size=36`;
                }}
              />
				{user.role === "ADMIN" ? (
					<Link
						to="/admin"
						className="nav-link nav-username"
						onClick={() => setMenuOpen(false)}
					>
						{user.firstName}
					</Link>
				) : (
					<span className="nav-username">{user.firstName}</span>
				)}
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
                  logout().catch(() => {});
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
                className="btn-secondary header-register-cta"
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
