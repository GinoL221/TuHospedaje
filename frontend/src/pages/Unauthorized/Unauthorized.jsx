import { Link } from "react-router-dom";
import { useDocumentTitle } from "../../hooks/useDocumentTitle";
import "../../App.css";

export default function Unauthorized() {
	useDocumentTitle("Acceso no autorizado");
	return (
		<main className="page-container">
			<div className="empty-state">
				<p>No tenés permisos para acceder a esta página.</p>
				<Link to="/">Volver al inicio</Link>
			</div>
		</main>
	);
}
