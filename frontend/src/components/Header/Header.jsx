import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import logo from "../../assets/images/TuHospedaje_Isologotipo.png";

export default function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

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
        <div className="nav-links">
          {user ? (
            <>
              <img
                src={user.imageUrl}
                alt={user.firstName}
                className="avatar"
                onClick={goToAdmin}
                style={{ cursor: user?.role === "ADMIN" ? "pointer" : "default" }}
                title={user?.role === "ADMIN" ? "Ir al panel de administración" : ""}
              />
              <span
                onClick={goToAdmin}
                style={{ cursor: user?.role === "ADMIN" ? "pointer" : "default" }}
                title={user?.role === "ADMIN" ? "Ir al panel de administración" : ""}
              >
                {user.firstName}
              </span>
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
