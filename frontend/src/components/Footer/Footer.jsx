import logo from "../../assets/images/TuHospedaje_Isologotipo.png";
import WhatsAppButton from "../WhatsAppButton/WhatsAppButton";

export default function Footer() {
	return (
		<footer className="site-footer">
			<div className="page-container">
				<section className="footer-left">
					<span className="footer-brand">
						<img
							src={logo}
							alt="TuHospedaje"
							className="footer-logo"
							width="451"
							height="253"
							loading="lazy"
						/>
					</span>
					<p>© 2026 TuHospedaje. Todos los derechos reservados.</p>
				</section>
				<nav className="footer-right" aria-label="Redes sociales">
					<a href="https://facebook.com" target="_blank" rel="noreferrer">
						<img
							src="https://img.icons8.com/ios-filled/50/ffffff/facebook.png"
							alt="Facebook"
							width="24"
							height="24"
							loading="lazy"
						/>
					</a>
					<a href="https://instagram.com" target="_blank" rel="noreferrer">
						<img
							src="https://img.icons8.com/ios-filled/50/ffffff/instagram-new.png"
							alt="Instagram"
							width="24"
							height="24"
							loading="lazy"
						/>
					</a>
				</nav>
				<WhatsAppButton />
			</div>
		</footer>
	);
}
