import { Link } from "react-router-dom";
import { useDocumentTitle } from "../../hooks/useDocumentTitle";
import "../../App.css";

export default function NotFound() {
	useDocumentTitle("Página no encontrada");
	return (
		<main className="page-container">
			<div className="empty-state">
				<p>La página que buscás no existe.</p>
				<Link to="/">Volver al inicio</Link>
			</div>
		</main>
	);
}
