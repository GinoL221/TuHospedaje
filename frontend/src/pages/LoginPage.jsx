import { useState } from "react";
import { useNavigate, Link, useLocation } from "react-router-dom";
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
  const location = useLocation();
  const message = location.state?.message;
  const from = location.state?.from?.pathname || "/";

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
    } else if (form.password.length < 6) {
      errors.password = "Debe tener al menos 6 caracteres";
    } else if (!/[A-Z]/.test(form.password)) {
      errors.password = "Debe contener una mayúscula";
    } else if (!/[0-9]/.test(form.password)) {
      errors.password = "Debe contener un número";
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
      navigate(from, { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>Iniciar sesión</h2>
        {message && <p className="error">{message}</p>}
        {error && <p className="error">{error}</p>}
        <form onSubmit={handleSubmit} noValidate>
          <label>Email</label>
          <input
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange}
            className={fieldErrors.email ? "input-error" : ""}
          />
          {fieldErrors.email && (
            <p className="field-error">{fieldErrors.email}</p>
          )}

          <label>Contraseña</label>
          <input
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            className={fieldErrors.password ? "input-error" : ""}
          />
          {fieldErrors.password && (
            <p className="field-error">{fieldErrors.password}</p>
          )}

          <button type="submit" disabled={loading}>
            {loading ? "Iniciando sesión..." : "Iniciar sesión"}
          </button>
        </form>
        <p>
          ¿No tenés cuenta? <Link to="/register">Crear cuenta</Link>
        </p>
      </div>
    </div>
  );
}
