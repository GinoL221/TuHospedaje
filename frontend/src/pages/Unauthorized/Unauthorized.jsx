import { Link } from "react-router-dom";
import "../../App.css";

export default function Unauthorized() {
  return (
    <main className="page-container">
      <div className="empty-state">
        <p>No tenés permisos para acceder a esta página.</p>
        <Link to="/">Volver al inicio</Link>
      </div>
    </main>
  );
}
