import logo from "../../assets/images/TuHospedaje_Isologotipo.png";
import WhatsAppButton from "../WhatsAppButton/WhatsAppButton";

export default function Footer() {
  return (
    <footer>
      <div className="page-container">
        <section className="footer-left">
          <span className="footer-brand">
            <img src={logo} alt="TuHospedaje" className="footer-logo" />
          </span>
          <p>© 2026 TuHospedaje. Todos los derechos reservados.</p>
        </section>
        <section className="footer-right">
          <a href="https://facebook.com" target="_blank" rel="noreferrer">
            <img src="https://img.icons8.com/ios-filled/50/ffffff/facebook.png" alt="Facebook" />
          </a>
          <a href="https://instagram.com" target="_blank" rel="noreferrer">
            <img src="https://img.icons8.com/ios-filled/50/ffffff/instagram-new.png" alt="Instagram" />
          </a>
        </section>
        <WhatsAppButton />
      </div>
    </footer>
  );
}
