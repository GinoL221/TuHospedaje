import { Link } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import logo from "../../assets/images/TuHospedaje_Isologotipo.png";

export default function Header() {
  const { user, logout } = useAuth();

  return (
    <header>
      <nav className="page-container">
        <div className="logo-container">
          <Link to="/">
            <img src={logo} alt="TuHospedaje" className="logo" />
          </Link>
          <p className="tagline">Encuentra tu lugar ideal al mejor precio</p>
        </div>
        <div className="nav-links">
          {user ? (
            <>
              <img
                src={user.imageUrl}
                alt={user.firstName}
                className="avatar"
              />
              <span>{user.firstName}</span>
              <button onClick={logout} className="btn-logout">
                Cerrar sesión
              </button>
            </>
          ) : (
            <>
              <Link to="/login">Iniciar sesión</Link>
              <Link to="/register" className="btn-secondary">
                Crear cuenta
              </Link>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}
