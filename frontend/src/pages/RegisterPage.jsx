import { useState } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import { Eye, EyeOff } from "lucide-react";
import { useAuth } from "../hooks/useAuth";
import "../App.css";
import "../assets/css/auth.css";

export default function RegisterPage() {
	const { register } = useAuth();
	const navigate = useNavigate();
	const location = useLocation();

	const [form, setForm] = useState({
		firstName: "",
		lastName: "",
		email: "",
		password: "",
		confirmPassword: "",
	});
	const [fieldErrors, setFieldErrors] = useState({});
	const [error, setError] = useState("");
	const [loading, setLoading] = useState(false);
	const [showPassword, setShowPassword] = useState(false);
	const [showConfirmPassword, setShowConfirmPassword] = useState(false);
	const [pwChecks, setPwChecks] = useState({
		minLength: false,
		maxLength: false,
		hasUpper: false,
		hasNumber: false,
	});

	const handleChange = (e) => {
		const { name, value } = e.target;
		setForm({ ...form, [name]: value });
		if (fieldErrors[name]) {
			setFieldErrors({ ...fieldErrors, [name]: "" });
		}
		if (name === "password") {
			setPwChecks({
				minLength: value.length >= 6,
				maxLength: value.length > 0 && value.length <= 20,
				hasUpper: /[A-Z]/.test(value),
				hasNumber: /[0-9]/.test(value),
			});
		}
	};

	const validate = () => {
		const errors = {};
		if (!form.firstName.trim()) errors.firstName = "El nombre es obligatorio";
		if (!form.lastName.trim()) errors.lastName = "El apellido es obligatorio";
		if (!form.email.trim()) {
			errors.email = "El email es obligatorio";
		} else if (!/\S+@\S+\.\S+/.test(form.email)) {
			errors.email = "El email no es válido";
		}
		if (!form.password) {
			errors.password = "La contraseña es obligatoria";
		} else if (form.password.length < 6) {
			errors.password = "Debe tener al menos 6 caracteres";
		} else if (form.password.length > 20) {
			errors.password = "Debe tener como máximo 20 caracteres";
		} else if (!/[A-Z]/.test(form.password)) {
			errors.password = "Debe contener una mayúscula";
		} else if (!/[0-9]/.test(form.password)) {
			errors.password = "Debe contener un número";
		}
		if (!form.confirmPassword) {
			errors.confirmPassword = "Confirmá tu contraseña";
		} else if (form.password !== form.confirmPassword) {
			errors.confirmPassword = "Las contraseñas no coinciden";
		}
		return errors;
	};

	const handleSubmit = async (e) => {
		e.preventDefault();
		setError("");

		const errors = validate();
		setFieldErrors(errors);
		if (Object.keys(errors).length > 0) return;

		try {
			setLoading(true);
			await register(form.firstName, form.lastName, form.email, form.password);
			const from = location.state?.from;
			if (from && typeof from === "object") {
				const { state: fromState, ...target } = from;
				navigate(target, {
					replace: true,
					state: { ...(fromState ?? {}), registered: true },
				});
			} else if (from) {
				navigate(from, { replace: true, state: { registered: true } });
			} else {
				navigate("/", { state: { registered: true } });
			}
		} catch (err) {
			if (err.code === "duplicate_email") {
				setFieldErrors((prev) => ({ ...prev, email: err.message }));
			} else {
				setError(err.message);
			}
		} finally {
			setLoading(false);
		}
	};

	return (
		<main className="login-container">
			<div className="register-box">
				<h2>Crear cuenta</h2>
				{error && (
					<p className="error" role="alert">
						{error}
					</p>
				)}
				<form onSubmit={handleSubmit} noValidate>
					<label htmlFor="register-first-name">Nombre</label>
					<input
						id="register-first-name"
						name="firstName"
						value={form.firstName}
						onChange={handleChange}
						className={fieldErrors.firstName ? "input-error" : ""}
						aria-invalid={fieldErrors.firstName ? "true" : undefined}
						aria-describedby={
							fieldErrors.firstName ? "register-firstName-error" : undefined
						}
					/>
					{fieldErrors.firstName && (
						<p
							id="register-firstName-error"
							className="field-error"
							role="alert"
						>
							{fieldErrors.firstName}
						</p>
					)}

					<label htmlFor="register-last-name">Apellido</label>
					<input
						id="register-last-name"
						name="lastName"
						value={form.lastName}
						onChange={handleChange}
						className={fieldErrors.lastName ? "input-error" : ""}
						aria-invalid={fieldErrors.lastName ? "true" : undefined}
						aria-describedby={
							fieldErrors.lastName ? "register-lastName-error" : undefined
						}
					/>
					{fieldErrors.lastName && (
						<p
							id="register-lastName-error"
							className="field-error"
							role="alert"
						>
							{fieldErrors.lastName}
						</p>
					)}

					<label htmlFor="register-email">Email</label>
					<input
						id="register-email"
						name="email"
						type="email"
						value={form.email}
						onChange={handleChange}
						className={fieldErrors.email ? "input-error" : ""}
						aria-invalid={fieldErrors.email ? "true" : undefined}
						aria-describedby={
							fieldErrors.email ? "register-email-error" : undefined
						}
					/>
					{fieldErrors.email && (
						<p id="register-email-error" className="field-error" role="alert">
							{fieldErrors.email}
						</p>
					)}

					<label htmlFor="register-password">Contraseña</label>
					<div className="input-wrap">
						<input
							id="register-password"
							name="password"
							type={showPassword ? "text" : "password"}
							value={form.password}
							onChange={handleChange}
							className={
								fieldErrors.password
									? "input-error"
									: form.password &&
										  pwChecks.minLength &&
										  pwChecks.maxLength &&
										  pwChecks.hasUpper &&
										  pwChecks.hasNumber
										? "input-valid"
										: ""
							}
							aria-invalid={fieldErrors.password ? "true" : undefined}
							aria-describedby={
								fieldErrors.password ? "register-password-error" : undefined
							}
						/>
						<button
							type="button"
							className="pw-toggle"
							aria-controls="register-password"
							aria-pressed={showPassword}
							aria-label={
								showPassword ? "Ocultar contraseña" : "Mostrar contraseña"
							}
							onClick={() => setShowPassword((visible) => !visible)}
						>
							{showPassword ? (
								<EyeOff size={18} aria-hidden="true" focusable="false" />
							) : (
								<Eye size={18} aria-hidden="true" focusable="false" />
							)}
						</button>
					</div>
					{form.password && (
						<ul className="pw-checks">
							<li
								className={pwChecks.minLength ? "pw-check-ok" : "pw-check-fail"}
							>
								{pwChecks.minLength ? "✔" : "✘"} Al menos 6 caracteres
							</li>
							<li
								className={pwChecks.maxLength ? "pw-check-ok" : "pw-check-fail"}
							>
								{pwChecks.maxLength ? "✔" : "✘"} Como máximo 20 caracteres
							</li>
							<li
								className={pwChecks.hasUpper ? "pw-check-ok" : "pw-check-fail"}
							>
								{pwChecks.hasUpper ? "✔" : "✘"} Una letra mayúscula
							</li>
							<li
								className={pwChecks.hasNumber ? "pw-check-ok" : "pw-check-fail"}
							>
								{pwChecks.hasNumber ? "✔" : "✘"} Un número
							</li>
						</ul>
					)}
					{fieldErrors.password && (
						<p
							id="register-password-error"
							className="field-error"
							role="alert"
						>
							{fieldErrors.password}
						</p>
					)}

					<label htmlFor="register-confirm-password">
						Confirmar contraseña
					</label>
					<div className="input-wrap">
						<input
							id="register-confirm-password"
							name="confirmPassword"
							type={showConfirmPassword ? "text" : "password"}
							value={form.confirmPassword}
							onChange={handleChange}
							className={fieldErrors.confirmPassword ? "input-error" : ""}
							aria-invalid={fieldErrors.confirmPassword ? "true" : undefined}
							aria-describedby={
								fieldErrors.confirmPassword
									? "register-confirmPassword-error"
									: undefined
							}
						/>
						<button
							type="button"
							className="pw-toggle"
							aria-controls="register-confirm-password"
							aria-pressed={showConfirmPassword}
							aria-label={
								showConfirmPassword
									? "Ocultar confirmación de contraseña"
									: "Mostrar confirmación de contraseña"
							}
							onClick={() => setShowConfirmPassword((visible) => !visible)}
						>
							{showConfirmPassword ? (
								<EyeOff size={18} aria-hidden="true" focusable="false" />
							) : (
								<Eye size={18} aria-hidden="true" focusable="false" />
							)}
						</button>
					</div>
					{fieldErrors.confirmPassword && (
						<p
							id="register-confirmPassword-error"
							className="field-error"
							role="alert"
						>
							{fieldErrors.confirmPassword}
						</p>
					)}

					<button type="submit" disabled={loading}>
						{loading ? "Creando cuenta..." : "Crear cuenta"}
					</button>
				</form>
				<p>
					¿Ya tenés cuenta?{" "}
					<Link to="/login" state={{ from: location.state?.from }}>
						Iniciá sesión
					</Link>
				</p>
			</div>
		</main>
	);
}
