export default function WhatsAppButton() {
  const phoneNumber = import.meta.env.VITE_WHATSAPP_NUMBER;

  if (!phoneNumber) return null;

  const message = encodeURIComponent(
    "Hola, quiero hacer una consulta sobre un alojamiento de TuHospedaje.",
  );
  const url = `https://wa.me/${phoneNumber}?text=${message}`;

  return (
    <a
      href={url}
      target="_blank"
      rel="noreferrer"
      aria-label="Contactar por WhatsApp"
      style={{
        position: "fixed",
        right: "24px",
        bottom: "24px",
        zIndex: 1000,
        width: "56px",
        height: "56px",
        borderRadius: "50%",
        background: "#25D366",
        display: "grid",
        placeItems: "center",
        boxShadow: "0 8px 20px rgba(0, 0, 0, 0.2)",
      }}
    >
      <svg viewBox="0 0 32 32" width="28" height="28" aria-hidden="true">
        <path
          fill="#fff"
          d="M19.11 17.54c-.28-.14-1.65-.82-1.91-.92-.26-.1-.45-.14-.64.14-.19.28-.73.92-.9 1.11-.17.19-.33.21-.61.07-.28-.14-1.18-.44-2.24-1.41-.83-.74-1.39-1.66-1.56-1.94-.17-.28-.02-.43.13-.57.13-.13.28-.33.42-.49.14-.17.19-.28.28-.47.09-.19.05-.35-.02-.49-.07-.14-.64-1.54-.88-2.11-.23-.56-.47-.49-.64-.5l-.55-.01c-.19 0-.49.07-.74.35-.26.28-.96.94-.96 2.29s.98 2.66 1.12 2.85c.14.19 1.95 2.98 4.73 4.18.66.29 1.18.46 1.58.59.66.21 1.26.18 1.73.11.53-.08 1.65-.67 1.88-1.32.23-.65.23-1.2.16-1.32-.07-.12-.25-.19-.53-.33zM16.02 5.33c-5.9 0-10.7 4.8-10.7 10.7 0 1.88.49 3.72 1.42 5.34L5 27l5.8-1.52a10.65 10.65 0 0 0 5.22 1.33h.01c5.9 0 10.7-4.8 10.7-10.7 0-5.9-4.8-10.78-10.71-10.78zm0 19.46h-.01c-1.67 0-3.32-.45-4.77-1.3l-.34-.2-3.44.9.92-3.35-.22-.35a8.94 8.94 0 0 1-1.38-4.77c0-4.93 4.01-8.94 8.94-8.94 4.93 0 8.98 4.01 8.98 8.94 0 4.93-4.05 9.07-8.68 9.07z"
        />
      </svg>
    </a>
  );
}
