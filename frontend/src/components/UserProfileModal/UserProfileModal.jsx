import { useState } from "react";
import {
	Mail,
	Shield,
	CheckCircle2,
	AlertCircle,
	Clock,
	X,
} from "lucide-react";
import { resendWelcomeEmail } from "../../services/authService";
import "./UserProfileModal.css";

export default function UserProfileModal({ user, onClose }) {
	const [resendStatus, setResendStatus] = useState("idle"); // idle | sending | scheduled | pending | cooldown | error
	const [feedbackMessage, setFeedbackMessage] = useState("");

	if (!user) return null;

	const handleResend = async () => {
		try {
			setResendStatus("sending");
			setFeedbackMessage("");
			const res = await resendWelcomeEmail();
			if (res && res.status === "pending") {
				setResendStatus("pending");
				setFeedbackMessage(
					"Tu correo de bienvenida ya está en camino. Revisá tu casilla y la carpeta de spam.",
				);
			} else {
				setResendStatus("scheduled");
				setFeedbackMessage(
					"¡Listo! Te reenviamos el correo de bienvenida. Revisá tu casilla en unos instantes.",
				);
			}
		} catch (err) {
			if (err.status === 429) {
				setResendStatus("cooldown");
				setFeedbackMessage(
					"Ya solicitaste un reenvío recientemente. Por favor esperá unos minutos antes de volver a intentar.",
				);
			} else {
				setResendStatus("error");
				setFeedbackMessage(
					err.message ||
						"Ocurrió un error al intentar reenviar el correo. Probá más tarde.",
				);
			}
		}
	};

	return (
		<div
			className="profile-modal-backdrop"
			onClick={onClose}
			role="presentation"
		>
			<div
				className="profile-modal"
				role="dialog"
				aria-modal="true"
				aria-labelledby="profile-modal-title"
				onClick={(e) => e.stopPropagation()}
			>
				<button
					type="button"
					className="profile-modal-close"
					onClick={onClose}
					aria-label="Cerrar ventana de perfil"
				>
					<X size={20} />
				</button>

				<div className="profile-modal-header">
					<img
						src={
							user.imageUrl ||
							`https://ui-avatars.com/api/?name=${encodeURIComponent(user.firstName)}&background=264653&color=fff&size=64`
						}
						alt={user.firstName}
						className="profile-modal-avatar"
					/>
					<h2 id="profile-modal-title">
						{user.firstName} {user.lastName}
					</h2>
					<span className="profile-modal-role">
						<Shield size={14} />
						{user.role === "ADMIN" ? "Administrador" : "Usuario"}
					</span>
				</div>

				<div className="profile-modal-details">
					<div className="profile-detail-row">
						<Mail size={16} />
						<span>{user.email}</span>
					</div>
				</div>

				<div className="profile-modal-section">
					<h3>Correo de bienvenida</h3>
					<p className="profile-modal-text">
						¿No encontrás el correo con tus datos de registro y confirmación?
						Podés solicitar que te lo enviemos de nuevo.
					</p>

					{feedbackMessage && (
						<div
							className={`profile-modal-feedback profile-modal-feedback--${resendStatus}`}
							role="status"
						>
							{resendStatus === "scheduled" && <CheckCircle2 size={16} />}
							{resendStatus === "pending" && <Clock size={16} />}
							{resendStatus === "cooldown" && <Clock size={16} />}
							{resendStatus === "error" && <AlertCircle size={16} />}
							<span>{feedbackMessage}</span>
						</div>
					)}

					<button
						type="button"
						className="btn-profile-resend"
						onClick={handleResend}
						disabled={
							resendStatus === "sending" ||
							resendStatus === "scheduled" ||
							resendStatus === "cooldown"
						}
					>
						{resendStatus === "sending"
							? "Reenviando..."
							: "Reenviar correo de bienvenida"}
					</button>
				</div>
			</div>
		</div>
	);
}
