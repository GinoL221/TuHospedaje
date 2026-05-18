import { useState, useEffect, useCallback } from "react";
import { get, post, del } from "../../services/api";
import "./Admin.css";

export default function Admin() {
  const [lodgings, setLodgings] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({
    name: "",
    description: "",
    imageUrls: "",
  });
  const [error, setError] = useState("");
  const [isMobile, setIsMobile] = useState(false);
  const size = 10;
  // Detectar mobile
  useEffect(() => {
    const check = () => {
      const isTouch = "ontouchstart" in window || navigator.maxTouchPoints > 0;
      setIsMobile(isTouch && window.innerWidth <= 1024);
    };
    check();
    window.addEventListener("resize", check);
    return () => window.removeEventListener("resize", check);
  }, []);
  const fetchLodgings = useCallback(() => {
    get(`/lodgings?page=${page}&size=${size}`)
      .then((data) => {
        if (Array.isArray(data)) {
          setLodgings(data);
          setTotalPages(1);
        } else {
          setLodgings(data.lodgings || []);
          setTotalPages(data.totalPages || 0);
        }
      })
      .catch(console.error);
  }, [page, size]);
  useEffect(() => {
    fetchLodgings();
  }, [fetchLodgings]);
  // Bloqueo mobile
  if (isMobile) {
    return (
      <main className="page-container admin-page">
        <div className="mobile-block">
          <h2>Funcionalidad no disponible para dispositivos móviles</h2>
          <p>Por favor, accedé desde una computadora.</p>
        </div>
      </main>
    );
  }
  const handleDelete = (id, name) => {
    if (window.confirm(`¿Eliminar "${name}"?`)) {
      del(`/lodgings/${id}`)
        .then(() => fetchLodgings())
        .catch(console.error);
    }
  };
  const handleSubmit = (e) => {
    e.preventDefault();
    setError("");
    const body = {
      name: form.name,
      description: form.description,
      imageUrls: form.imageUrls
        ? form.imageUrls
            .split(",")
            .map((u) => u.trim())
            .filter(Boolean)
        : [],
    };
    post("/lodgings", body)
      .then(() => {
        setShowModal(false);
        setForm({ name: "", description: "", imageUrls: "" });
        setPage(0);
        fetchLodgings();
      })
      .catch((err) => setError(err.message));
  };
  return (
    <main className="page-container admin-page">
      <h1>Panel de Administración</h1>
      <nav className="admin-menu">
        <button className="menu-btn active">Lista de productos</button>
      </nav>
      <div className="admin-toolbar">
        <button className="btn-add" onClick={() => setShowModal(true)}>
          + Agregar producto
        </button>
      </div>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Nombre</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {lodgings.map((l) => (
            <tr key={l.id}>
              <td>{l.id}</td>
              <td>{l.name}</td>
              <td>
                <button
                  className="btn-delete"
                  onClick={() => handleDelete(l.id, l.name)}
                >
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(0)}>
            Inicio
          </button>
          <button disabled={page === 0} onClick={() => setPage(page - 1)}>
            Anterior
          </button>
          <span>
            Página {page + 1} de {totalPages}
          </span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => setPage(page + 1)}
          >
            Siguiente
          </button>
        </div>
      )}
      {/* MODAL */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>Nuevo producto</h2>
            <form onSubmit={handleSubmit}>
              <label>
                Nombre *
                <input
                  required
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                />
              </label>
              <label>
                Descripción *
                <textarea
                  required
                  value={form.description}
                  onChange={(e) =>
                    setForm({ ...form, description: e.target.value })
                  }
                />
              </label>
              <label>
                URLs de imágenes (separadas por coma)
                <textarea
                  value={form.imageUrls}
                  onChange={(e) =>
                    setForm({ ...form, imageUrls: e.target.value })
                  }
                  placeholder="https://ejemplo.com/img1.jpg, https://ejemplo.com/img2.jpg"
                />
              </label>
              {error && <p className="form-error">{error}</p>}
              <div className="modal-actions">
                <button type="submit" className="btn-save">
                  Guardar
                </button>
                <button
                  type="button"
                  className="btn-cancel"
                  onClick={() => setShowModal(false)}
                >
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </main>
  );
}
