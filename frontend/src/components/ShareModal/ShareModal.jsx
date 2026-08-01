import { useEffect, useId, useRef, useState } from "react";
import { Copy, X } from "lucide-react";
import "./ShareModal.css";

const FOCUSABLE_SELECTOR = [
	"button:not([disabled])",
	"[href]",
	"input:not([disabled])",
	"select:not([disabled])",
	"textarea:not([disabled])",
	'[tabindex]:not([tabindex="-1"])',
].join(",");

export default function ShareModal({ lodging, onClose }) {
	const dialogRef = useRef(null);
	const closeButtonRef = useRef(null);
	const previousFocusRef = useRef(null);
	const titleId = useId();
	const descriptionId = useId();
	const [copyStatus, setCopyStatus] = useState("");
	const url = window.location.href;
	const text = `Mirá este alojamiento en TuHospedaje: ${lodging.name}`;

	useEffect(() => {
		previousFocusRef.current = document.activeElement;
		closeButtonRef.current?.focus();

		return () => {
			const previousFocus = previousFocusRef.current;
			if (
				previousFocus?.isConnected &&
				previousFocus.matches(FOCUSABLE_SELECTOR)
			) {
				previousFocus.focus();
			}
		};
	}, []);

	const handleKeyDown = (event) => {
		if (event.key === "Escape") {
			event.preventDefault();
			onClose();
			return;
		}

		if (event.key !== "Tab") return;

		const focusableElements = Array.from(
			dialogRef.current?.querySelectorAll(FOCUSABLE_SELECTOR) ?? [],
		);
		if (focusableElements.length === 0) {
			event.preventDefault();
			return;
		}

		const firstElement = focusableElements[0];
		const lastElement = focusableElements.at(-1);
		if (event.shiftKey && document.activeElement === firstElement) {
			event.preventDefault();
			lastElement.focus();
		} else if (!event.shiftKey && document.activeElement === lastElement) {
			event.preventDefault();
			firstElement.focus();
		}
	};

	const handleCopy = async () => {
		try {
			if (navigator.clipboard?.writeText) {
				await navigator.clipboard.writeText(url);
			} else {
				const textarea = document.createElement("textarea");
				textarea.value = url;
				textarea.setAttribute("readonly", "");
				textarea.className = "share-copy-fallback";
				document.body.appendChild(textarea);
				let copied = false;
				try {
					textarea.select();
					copied = document.execCommand?.("copy") ?? false;
				} finally {
					textarea.remove();
				}
				if (!copied) throw new Error("Copy command unavailable");
			}
			setCopyStatus("Enlace copiado");
		} catch {
			setCopyStatus("No se pudo copiar el enlace");
		}
	};

  const shareLinks = [
    {
      name: "Facebook",
      bg: "#1877F2",
      href: `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`,
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z" />
        </svg>
      ),
    },
    {
      name: "Twitter",
      bg: "#000",
      href: `https://twitter.com/intent/tweet?text=${encodeURIComponent(text)}&url=${encodeURIComponent(url)}`,
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
        </svg>
      ),
    },
    {
      name: "WhatsApp",
      bg: "#25D366",
      href: `https://wa.me/?text=${encodeURIComponent(text + " " + url)}`,
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 0 1-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 0 1-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 0 1 2.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0 0 12.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 0 0 5.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 0 0-3.48-8.413z" />
        </svg>
      ),
    },
    {
      name: "Instagram",
      bg: "#E1306C",
      href: "https://www.instagram.com/",
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <rect x="2" y="2" width="20" height="20" rx="5" ry="5" />
          <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z" />
          <line x1="17.5" y1="6.5" x2="17.51" y2="6.5" />
        </svg>
      ),
    },
  ];

  return (
			<div
					className="share-modal-overlay"
				onClick={onClose}
				onKeyDown={handleKeyDown}
			>
			<div
				ref={dialogRef}
				className="modal-content share-modal"
				role="dialog"
				aria-modal="true"
				aria-labelledby={titleId}
				aria-describedby={descriptionId}
				onClick={(e) => e.stopPropagation()}
			>
						<header className="share-header">
							<div>
								<h2 id={titleId}>Compartir</h2>
								<p id={descriptionId} className="share-sr-only">
									Elegí cómo enviar este alojamiento.
								</p>
							</div>
							<button
								ref={closeButtonRef}
								type="button"
								className="modal-close"
							onClick={onClose}
							aria-label="Cerrar"
						>
							<X size={20} aria-hidden="true" />
						</button>
					</header>

					<div className="share-lodging">
						{lodging.imageUrls?.[0] && (
							<img
								src={lodging.imageUrls[0]}
								alt={lodging.name}
								className="share-image"
							/>
						)}
							<p className="share-accessible-description">
							{lodging.name} - {lodging.city}
						</p>
						<div className="share-description" aria-hidden="true">
							<strong>{lodging.name}</strong>
							<span>{lodging.city}</span>
						</div>
					</div>

					<div className="share-link-row">
						<div className="share-link-summary" aria-hidden="true">
							<span>Enlace del alojamiento</span>
							<strong>{window.location.host}</strong>
						</div>
						<button type="button" className="share-copy-btn" onClick={handleCopy}>
							<Copy size={18} aria-hidden="true" />
							Copiar enlace
						</button>
					</div>
					<p className="share-copy-status" aria-live="polite" aria-atomic="true">
						{copyStatus}
					</p>

					<div className="share-options" aria-label="Redes sociales">
          {shareLinks.map((s) => (
            <a
              key={s.name}
              href={s.href}
              target="_blank"
              rel="noopener noreferrer"
              className="share-btn"
								style={{ "--share-brand": s.bg }}
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
