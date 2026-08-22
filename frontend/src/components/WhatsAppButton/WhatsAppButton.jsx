import { useState } from "react";
import "./WhatsAppButton.css";

const PHONE_PATTERN = /^[1-9]\d{7,14}$/;

const FIXED_MESSAGE =
  "Hola, quiero hacer una consulta sobre un alojamiento de TuHospedaje.";

const FEEDBACK = {
  handoff_requested: {
    role: "status",
    ariaLive: "polite",
    text: "Se abrió el acceso a WhatsApp; completá el envío allí.",
  },
  invalid_configuration: {
    role: "alert",
    ariaLive: "assertive",
    text: "El contacto por WhatsApp no está disponible en este momento.",
  },
  popup_blocked: {
    role: "alert",
    ariaLive: "assertive",
    text: "No pudimos abrir WhatsApp. Habilitá las ventanas emergentes e intentá de nuevo.",
  },
};

function readConfiguredNumber() {
  const rawNumber = import.meta.env.VITE_WHATSAPP_NUMBER;
  return typeof rawNumber === "string" ? rawNumber.trim() : "";
}

export default function WhatsAppButton() {
  const [status, setStatus] = useState(null);

  const handleClick = () => {
    const digits = readConfiguredNumber();

    if (!PHONE_PATTERN.test(digits)) {
      setStatus("invalid_configuration");
      return;
    }

    const handoffWindow = window.open("", "_blank");
    if (!handoffWindow) {
      setStatus("popup_blocked");
      return;
    }

    handoffWindow.opener = null;

    try {
      const encodedMessage = encodeURIComponent(FIXED_MESSAGE);
      handoffWindow.location = `https://wa.me/${digits}?text=${encodedMessage}`;
      setStatus("handoff_requested");
    } catch {
      handoffWindow.close?.();
      setStatus("popup_blocked");
    }
  };

  const feedback = status ? FEEDBACK[status] : null;

  return (
    <div className="whatsapp-button-wrapper">
      <button
        type="button"
        onClick={handleClick}
        aria-label="Contactar por WhatsApp"
        className="whatsapp-button"
      >
        <svg
          viewBox="0 0 24 24"
          width="28"
          height="28"
          fill="#fff"
          aria-hidden="true"
          className="whatsapp-button-icon"
        >
          <path d="M.057 24l1.687-6.163c-1.041-1.804-1.588-3.849-1.587-5.946.003-6.556 5.338-11.891 11.893-11.891 3.181.001 6.167 1.24 8.413 3.488 2.245 2.248 3.481 5.236 3.48 8.414-.003 6.557-5.338 11.892-11.893 11.892-1.99-.001-3.951-.5-5.688-1.448l-6.305 1.654zm6.597-3.807c1.676.995 3.276 1.591 5.392 1.592 5.448 0 9.886-4.434 9.889-9.885.002-5.462-4.415-9.89-9.881-9.892-5.452 0-9.887 4.434-9.889 9.884-.001 2.225.651 3.891 1.746 5.634l-.999 3.648 3.742-.981zm11.387-5.464c-.074-.124-.272-.198-.57-.347-.297-.149-1.758-.868-2.031-.967-.272-.099-.47-.149-.669.149-.198.297-.768.967-.941 1.165-.173.198-.347.223-.644.074-.297-.149-1.255-.462-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.372-.025-.521-.075-.148-.669-1.611-.916-2.206-.242-.579-.487-.501-.669-.51l-.57-.01c-.198 0-.52.074-.792.372s-1.04 1.016-1.04 2.479 1.065 2.876 1.213 3.074c.149.198 2.095 3.2 5.076 4.487.709.306 1.263.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.695.248-1.29.173-1.414z" />
        </svg>
      </button>
      {feedback && (
        <p
          role={feedback.role}
          aria-live={feedback.ariaLive}
          className="whatsapp-feedback"
        >
          {feedback.text}
        </p>
      )}
    </div>
  );
}
