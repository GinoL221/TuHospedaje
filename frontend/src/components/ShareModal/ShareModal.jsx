import "./ShareModal.css";

export default function ShareModal({ lodging, onClose }) {
  const url = window.location.href;
  const text = `Mirá este alojamiento en TuHospedaje: ${lodging.name}`;

  const shareLinks = [
    {
      name: "Facebook",
      href: `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`,
      icon: "f",
    },
    {
      name: "Twitter",
      href: `https://twitter.com/intent/tweet?text=${encodeURIComponent(text)}&url=${encodeURIComponent(url)}`,
      icon: "𝕏",
    },
    {
      name: "WhatsApp",
      href: `https://wa.me/?text=${encodeURIComponent(text + " " + url)}`,
      icon: "W",
    },
  ];

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-content share-modal"
        onClick={(e) => e.stopPropagation()}
      >
        <h2>Compartir</h2>
        <button className="modal-close" onClick={onClose}>
          ×
        </button>

        {lodging.imageUrls?.[0] && (
          <img
            src={lodging.imageUrls[0]}
            alt={lodging.name}
            className="share-image"
          />
        )}
        <p className="share-description">
          {lodging.name} - {lodging.city}
        </p>
        <p className="share-link">{url}</p>

        <div className="share-options">
          {shareLinks.map((s) => (
            <a
              key={s.name}
              href={s.href}
              target="_blank"
              rel="noopener noreferrer"
              className="share-btn"
              onClick={onClose}
            >
              {s.icon} {s.name}
            </a>
          ))}
        </div>
      </div>
    </div>
  );
}
