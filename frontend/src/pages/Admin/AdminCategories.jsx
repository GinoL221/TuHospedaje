import { useState, useEffect } from "react";
import { get, post, put, del } from "../../services/api";
import ConfirmDialog from "../../components/ConfirmDialog";
import useConfirmCancel from "../../hooks/useConfirmCancel";

export default function AdminCategories() {
  const [catList, setCatList] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({ name: "", description: "", imageUrl: "" });
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [deleteConfirm, setDeleteConfirm] = useState(null);

  const resetForm = () => setForm({ name: "", description: "", imageUrl: "" });
  const cancel = useConfirmCancel(form.name || form.description || form.imageUrl, () => { setFieldErrors({}); resetForm(); setShowModal(false); });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await get("/categories");
        if (!cancelled) setCatList(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error(err);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const openModal = (cat = null) => {
    if (cat) {
      setForm({ name: cat.name, description: cat.description || "", imageUrl: cat.imageUrl || "" });
      setEditing(cat);
    } else {
      setForm({ name: "", description: "", imageUrl: "" });
      setEditing(null);
    }
    setError("");
    setFieldErrors({});
    setShowModal(true);
  };

  const refresh = () => {
    get("/categories")
      .then((data) => setCatList(Array.isArray(data) ? data : []))
      .catch(() => {});
  };

  const validate = () => {
    const errs = {};
    if (!form.name.trim()) errs.name = "El nombre es obligatorio";
    return errs;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setError("");

    const errs = validate();
    setFieldErrors(errs);
    if (Object.keys(errs).length > 0) {
      setTimeout(() => document.querySelector(".input-error")?.focus(), 100);
      return;
    }

    const body = { name: form.name, description: form.description, imageUrl: form.imageUrl || null };
    const request = editing
      ? put(`/categories/${editing.id}`, body)
      : post("/categories", body);
    request
      .then(() => {
        setShowModal(false);
        refresh();
      })
      .catch((err) => setError(err.message));
  };

  const handleDelete = async (id, name) => {
    setDeleteConfirm({ id, name });
  };

  const confirmDelete = async () => {
    if (!deleteConfirm) return;
    try {
      await del(`/categories/${deleteConfirm.id}`);
      setDeleteConfirm(null);
      refresh();
    } catch (err) {
      alert(err.message);
      setDeleteConfirm(null);
    }
  };

  return (
    <>
      <button className="btn-fab" onClick={() => openModal(null)}>
        + Agregar categoría
      </button>
      {catList.length === 0 ? (
        <p className="empty-state">
          No hay categorías cargadas todavía. ¡Creá la primera!
        </p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Imagen</th>
              <th>Nombre</th>
              <th>Descripción</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {catList.map((c) => (
              <tr key={c.id}>
                <td>{c.id}</td>
                <td>
                  {c.imageUrl
                    ? <img src={c.imageUrl} alt={c.name} style={{ width: 32, height: 32, objectFit: "cover", borderRadius: 4 }} />
                    : "—"}
                </td>
                <td>{c.name}</td>
                <td>{c.description || "—"}</td>
                <td>
                  <button className="btn-edit" onClick={() => openModal(c)}>
                    Editar
                  </button>
                  <button
                    className="btn-delete"
                    onClick={() => handleDelete(c.id, c.name)}
                  >
                    Eliminar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={cancel.handleCancel}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>{editing ? "Editar categoría" : "Nueva categoría"}</h2>
            <form onSubmit={handleSubmit} noValidate>
              <label className="required-dot">
                Nombre
                <input value={form.name} className={fieldErrors.name ? "input-error" : ""} onChange={(e) => { setForm({ ...form, name: e.target.value }); if (fieldErrors.name) setFieldErrors({ ...fieldErrors, name: "" }); }} />
                {fieldErrors.name && <span className="field-error">{fieldErrors.name}</span>}
              </label>
              <label>
                Descripción
                <textarea
                  value={form.description}
                  onChange={(e) =>
                    setForm({ ...form, description: e.target.value })
                  }
                />
              </label>
              <label>
                URL de imagen
                <input
                  value={form.imageUrl}
                  onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
                  placeholder="https://..."
                />
              </label>
              {error && <p className="form-error">{error}</p>}
              <p className="required-note">* Campos obligatorios</p>
              <div className="modal-actions">
                <button type="submit" className="btn-save">
                  {editing ? "Guardar cambios" : "Crear"}
                </button>
                <button
                  type="button"
                  className="btn-cancel"
                  onClick={cancel.handleCancel}
                >
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDialog
        show={cancel.showConfirm}
        message="Hay cambios sin guardar. ¿Cancelar de todas formas?"
        onConfirm={() => { cancel.confirmCancel(); }}
        onCancel={cancel.dismissConfirm}
      />

      <ConfirmDialog
        show={deleteConfirm !== null}
        message={deleteConfirm ? `¿Eliminar la categoría "${deleteConfirm.name}"? Los alojamientos asociados quedarán sin categoría.` : ""}
        onConfirm={confirmDelete}
        onCancel={() => setDeleteConfirm(null)}
      />
    </>
  );
}
