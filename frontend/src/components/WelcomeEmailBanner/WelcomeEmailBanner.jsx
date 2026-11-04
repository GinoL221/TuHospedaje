import { useState } from "react";
import { Mail, CheckCircle2, AlertCircle, Clock } from "lucide-react";
import { resendWelcomeEmail } from "../../services/authService";
import "./WelcomeEmailBanner.css";

export default function WelcomeEmailBanner({ user, onDismiss }) {
	const [status, setStatus] = useState("idle"); // idle | sending | scheduled | pending | cooldown | error
	const [message, setMessage] = useState("");

	if (!user) return null;

	const handleResend = async () => {
		try {
			setStatus("sending");
			setMessage("");
			const res = await resendWelcomeEmail();
			if (res && res.status === "pending") {
				setStatus("pending");
				setMessage(
					"Tu correo de bienvenida ya está en camino. Revisá tu casilla y la carpeta de spam.",
				);
			} else {
				setStatus("scheduled");
				setMessage(
					"¡Listo! Te reenviamos el correo de bienvenida. Revisá tu casilla en unos instantes.",
				);
			}
		} catch (err) {
			if (err.status === 429) {
				setStatus("cooldown");
				setMessage(
					"Ya solicitaste un reenvío recientemente. Por favor esperá unos minutos antes de volver a intentar.",
				);
			} else {
				setStatus("error");
				setMessage(
					err.message ||
						"Ocurrió un error al intentar reenviar el correo. Probá más tarde.",
				);
			}
		}
	};

	return (
		<div className="welcome-banner" role="status" aria-live="polite">
			<div className="welcome-banner-header">
				<div className="welcome-banner-title">
					<Mail className="welcome-banner-icon" size={24} />
					<div>
						<h3>¡Te damos la bienvenida a TuHospedaje, {user.firstName}!</h3>
						<p>
							Te enviamos un correo de bienvenida a{" "}
							<strong>{user.email}</strong> para confirmar tu registro.
						</p>
					</div>
				</div>
				{onDismiss && (
					<button
						type="button"
						className="welcome-banner-close"
						onClick={onDismiss}
						aria-label="Cerrar aviso de bienvenida"
					>
						×
					</button>
				)}
			</div>

			{message && (
				<div
					className={`welcome-banner-feedback welcome-banner-feedback--${status}`}
				>
					{status === "scheduled" && <CheckCircle2 size={18} />}
					{status === "pending" && <Clock size={18} />}
					{status === "cooldown" && <Clock size={18} />}
					{status === "error" && <AlertCircle size={18} />}
					<span>{message}</span>
				</div>
			)}

			<div className="welcome-banner-actions">
				<span>¿No lo recibiste?</span>
				<button
					type="button"
					className="btn-resend-welcome"
					onClick={handleResend}
					disabled={
						status === "sending" ||
						status === "scheduled" ||
						status === "cooldown"
					}
				>
					{status === "sending"
						? "Reenviando..."
						: "Reenviar correo de bienvenida"}
				</button>
			</div>
		</div>
	);
}
