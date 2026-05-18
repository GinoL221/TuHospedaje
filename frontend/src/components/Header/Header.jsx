import { Link } from "react-router-dom";
import logo from "../../assets/images/TuHospedaje_Isologotipo.png";

export default function Header() {
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
          <Link to="/login">Iniciar sesión</Link>
          <Link to="/register" className="btn-secondary">Crear cuenta</Link>
        </div>
      </nav>
    </header>
  );
}
