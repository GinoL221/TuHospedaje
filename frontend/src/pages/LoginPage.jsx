import { useState } from "react";
import { useNavigate, Link, useLocation } from "react-router-dom";
import { Eye, EyeOff } from "lucide-react";
import { useAuth } from "../hooks/useAuth";

import "../App.css";
import "../assets/css/auth.css";

export default function LoginPage() {
	const { login } = useAuth();
	const navigate = useNavigate();

	const [form, setForm] = useState({
		email: "",
		password: "",
	});
	const [fieldErrors, setFieldErrors] = useState({});
	const [error, setError] = useState("");
	const [loading, setLoading] = useState(false);
	const [showPassword, setShowPassword] = useState(false);
	const location = useLocation();
	const message = location.state?.message;
	const destination = location.state?.from;
	const from =
		destination && typeof destination === "object"
			? {
					pathname: destination.pathname || "/",
					search: destination.search ?? "",
				}
			: destination || "/";
	const fromState = destination?.state;
	const registerState = destination ? { from: destination } : undefined;

	const handleChange = (e) => {
		setForm({ ...form, [e.target.name]: e.target.value });
		if (fieldErrors[e.target.name]) {
			setFieldErrors({ ...fieldErrors, [e.target.name]: "" });
		}
	};

	const validate = () => {
		const errors = {};
		if (!form.email.trim()) {
			errors.email = "El email es obligatorio";
		} else if (!/\S+@\S+\.\S+/.test(form.email)) {
			errors.email = "El email no es válido";
		}
		if (!form.password) {
			errors.password = "La contraseña es obligatoria";
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
			await login(form.email, form.password);
			navigate(
				from,
				fromState ? { replace: true, state: fromState } : { replace: true },
			);
		} catch (err) {
			setError(err.message);
		} finally {
			setLoading(false);
		}
	};

	return (
		<main className="login-container">
			<div className="login-box">
				<h2>Iniciar sesión</h2>
				{message && (
					<p className="error" role="status">
						{message}
					</p>
				)}
				{error && (
					<p className="error" role="alert">
						{error}
					</p>
				)}
				<form onSubmit={handleSubmit} noValidate>
					<label htmlFor="login-email">Email</label>
					<input
						id="login-email"
						name="email"
						type="email"
						value={form.email}
						onChange={handleChange}
						className={fieldErrors.email ? "input-error" : ""}
						aria-invalid={fieldErrors.email ? "true" : undefined}
						aria-describedby={
							fieldErrors.email ? "login-email-error" : undefined
						}
					/>
					{fieldErrors.email && (
						<p id="login-email-error" className="field-error" role="alert">
							{fieldErrors.email}
						</p>
					)}

					<label htmlFor="login-password">Contraseña</label>
					<div className="input-wrap">
						<input
							id="login-password"
							name="password"
							type={showPassword ? "text" : "password"}
							value={form.password}
							onChange={handleChange}
							className={fieldErrors.password ? "input-error" : ""}
							aria-invalid={fieldErrors.password ? "true" : undefined}
							aria-describedby={
								fieldErrors.password ? "login-password-error" : undefined
							}
						/>
						<button
							type="button"
							className="pw-toggle"
							aria-controls="login-password"
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
					{fieldErrors.password && (
						<p id="login-password-error" className="field-error" role="alert">
							{fieldErrors.password}
						</p>
					)}

					<button type="submit" disabled={loading}>
						{loading ? "Iniciando sesión..." : "Iniciar sesión"}
					</button>
				</form>
				<p>
					¿No tenés cuenta?{" "}
					<Link to="/register" state={registerState}>
						Crear cuenta
					</Link>
				</p>
			</div>
		</main>
	);
}
