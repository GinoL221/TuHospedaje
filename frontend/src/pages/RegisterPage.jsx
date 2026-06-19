import { useState } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
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
  const [pwChecks, setPwChecks] = useState({
    minLength: false,
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
      navigate("/");
    } catch (err) {
      if (err.message.includes("email ya está registrado")) {
        setFieldErrors((prev) => ({ ...prev, email: err.message }));
      } else {
        setError(err.message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="register-box">
        <h2>Crear cuenta</h2>
        {error && <p className="error">{error}</p>}
        <form onSubmit={handleSubmit} noValidate>
          <label>Nombre</label>
          <input
            name="firstName"
            value={form.firstName}
            onChange={handleChange}
            className={fieldErrors.firstName ? "input-error" : ""}
          />
          {fieldErrors.firstName && (
            <p className="field-error">{fieldErrors.firstName}</p>
          )}

          <label>Apellido</label>
          <input
            name="lastName"
            value={form.lastName}
            onChange={handleChange}
            className={fieldErrors.lastName ? "input-error" : ""}
          />
          {fieldErrors.lastName && (
            <p className="field-error">{fieldErrors.lastName}</p>
          )}

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
            className={
              fieldErrors.password
                ? "input-error"
                : form.password && pwChecks.minLength && pwChecks.hasUpper && pwChecks.hasNumber
                  ? "input-valid"
                  : ""
            }
          />
          {form.password && (
            <ul className="pw-checks">
              <li className={pwChecks.minLength ? "pw-check-ok" : "pw-check-fail"}>
                {pwChecks.minLength ? "✔" : "✘"} Al menos 6 caracteres
              </li>
              <li className={pwChecks.hasUpper ? "pw-check-ok" : "pw-check-fail"}>
                {pwChecks.hasUpper ? "✔" : "✘"} Una letra mayúscula
              </li>
              <li className={pwChecks.hasNumber ? "pw-check-ok" : "pw-check-fail"}>
                {pwChecks.hasNumber ? "✔" : "✘"} Un número
              </li>
            </ul>
          )}
          {fieldErrors.password && (
            <p className="field-error">{fieldErrors.password}</p>
          )}

          <label>Confirmar contraseña</label>
          <input
            name="confirmPassword"
            type="password"
            value={form.confirmPassword}
            onChange={handleChange}
            className={fieldErrors.confirmPassword ? "input-error" : ""}
          />
          {fieldErrors.confirmPassword && (
            <p className="field-error">{fieldErrors.confirmPassword}</p>
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
    </div>
  );
}
