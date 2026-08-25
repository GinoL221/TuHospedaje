import { useState, useEffect, useRef } from "react";
import { get, post, put, del } from "../../services/api";
import ConfirmDialog from "../../components/ConfirmDialog";
import useConfirmCancel from "../../hooks/useConfirmCancel";
import Icon from "../../components/Icons/Icon";
import IconPicker from "../../components/IconPicker/IconPicker";
import useTableData from "../../hooks/useTableData";
import SortableTh from "../../components/SortableTh/SortableTh";
import Pagination from "../../components/Pagination/Pagination";

// Mirrors the backend's @HttpsImageUrl rule (absolute https URL, non-blank
// host) as a client-side convenience check. The backend remains the final
// validation authority; no network request is made here.
function isValidHttpsImageUrl(value) {
  const trimmed = (value || "").trim();
  if (!trimmed) return false;
  try {
    const url = new URL(trimmed);
    return url.protocol === "https:" && url.hostname.length > 0;
  } catch {
    return false;
  }
}

export default function AdminCategories() {
  const [catList, setCatList] = useState([]);
  const { pageItems, sortKey, sortDir, requestSort, page, totalPages, setPage } = useTableData(catList);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({ name: "", description: "", icon: "", imageUrl: "" });
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [deleteConfirm, setDeleteConfirm] = useState(null);
  const [deleteError, setDeleteError] = useState("");
  const nameInputRef = useRef(null);
  const imageUrlInputRef = useRef(null);

  const resetForm = () => setForm({ name: "", description: "", icon: "", imageUrl: "" });
  const cancel = useConfirmCancel(form.name || form.description || form.icon || form.imageUrl, () => { setFieldErrors({}); resetForm(); setShowModal(false); });

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

  useEffect(() => {
    if (fieldErrors.name) {
      nameInputRef.current?.focus();
    } else if (fieldErrors.imageUrl) {
      imageUrlInputRef.current?.focus();
    }
  }, [fieldErrors]);

  const openModal = (cat = null) => {
    if (cat) {
      setForm({ name: cat.name, description: cat.description || "", icon: cat.icon || "", imageUrl: cat.imageUrl || "" });
      setEditing(cat);
    } else {
      setForm({ name: "", description: "", icon: "", imageUrl: "" });
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
    const trimmedImageUrl = form.imageUrl.trim();
    if (!trimmedImageUrl) {
      // Creation requires media; a legacy edit may omit it to preserve the
      // previously stored image (see CategoryServiceImpl.update).
      if (!editing) errs.imageUrl = "La imagen representativa es obligatoria";
    } else if (!isValidHttpsImageUrl(trimmedImageUrl)) {
      errs.imageUrl = "La imagen debe ser una URL https válida";
    }
    return errs;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setError("");

    const errs = validate();
    setFieldErrors(errs);
    if (Object.keys(errs).length > 0) {
      return;
    }

    const trimmedImageUrl = form.imageUrl.trim();
    const body = {
      name: form.name,
      description: form.description,
      icon: form.icon || null,
      imageUrl: trimmedImageUrl || null,
    };
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
    setDeleteError("");
    setDeleteConfirm({ id, name });
  };

  const confirmDelete = async () => {
    if (!deleteConfirm) return;
    try {
      await del(`/categories/${deleteConfirm.id}`);
      setDeleteConfirm(null);
      refresh();
    } catch (err) {
      setDeleteError(err.message);
      setDeleteConfirm(null);
    }
  };

  return (
    <>
      <button className="btn-fab" data-testid="admin-add-btn" onClick={() => openModal(null)}>
        + Agregar categoría
      </button>
      {catList.length === 0 ? (
        <p className="empty-state">
          No hay categorías cargadas todavía. ¡Creá la primera!
        </p>
      ) : (
        <>
          <table>
            <thead>
              <tr>
                <SortableTh columnKey="id" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>ID</SortableTh>
                <SortableTh columnKey="name" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>Nombre</SortableTh>
                <SortableTh columnKey="description" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>Descripción</SortableTh>
                <SortableTh columnKey="icon" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>Ícono</SortableTh>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {pageItems.map((c) => (
                <tr key={c.id} data-testid={`row-${c.id}`}>
                  <td>{c.id}</td>
                  <td>{c.name}</td>
                  <td>{c.description || "—"}</td>
                  <td>
                    <Icon name={c.icon} /> <code>{c.icon}</code>
                  </td>
                  <td>
                    <button className="btn-edit" data-testid="row-edit-btn" onClick={() => openModal(c)}>
                      Editar
                    </button>
                    <button
                      className="btn-delete"
                      data-testid="row-delete-btn"
                      onClick={() => handleDelete(c.id, c.name)}
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={cancel.handleCancel}>
          <div className="modal" data-testid="admin-modal" onClick={(e) => e.stopPropagation()}>
            <h2>{editing ? "Editar categoría" : "Nueva categoría"}</h2>
            <form onSubmit={handleSubmit} noValidate>
              <label className="required-dot">
                Nombre
                <input ref={nameInputRef} data-testid="field-name" value={form.name} className={fieldErrors.name ? "input-error" : ""} onChange={(e) => { setForm({ ...form, name: e.target.value }); if (fieldErrors.name) setFieldErrors({ ...fieldErrors, name: "" }); }} />
                {fieldErrors.name && <span className="field-error" data-testid="error-name">{fieldErrors.name}</span>}
              </label>
              <label>
                Descripción
                <textarea
                  data-testid="field-description"
                  value={form.description}
                  onChange={(e) =>
                    setForm({ ...form, description: e.target.value })
                  }
                />
              </label>
              <label>
                Ícono
                <IconPicker value={form.icon} onChange={(val) => setForm({ ...form, icon: val })} placeholder="Buscar ícono" />
              </label>
              <label className={editing ? undefined : "required-dot"}>
                Imagen representativa (URL)
                <input
                  ref={imageUrlInputRef}
                  type="url"
                  data-testid="field-image-url"
                  value={form.imageUrl}
                  required={!editing}
                  className={fieldErrors.imageUrl ? "input-error" : ""}
                  aria-invalid={fieldErrors.imageUrl ? "true" : "false"}
                  aria-describedby={fieldErrors.imageUrl ? "error-image-url" : undefined}
                  onChange={(e) => {
                    setForm({ ...form, imageUrl: e.target.value });
                    if (fieldErrors.imageUrl) setFieldErrors({ ...fieldErrors, imageUrl: "" });
                  }}
                />
                {fieldErrors.imageUrl && (
                  <span className="field-error" id="error-image-url" data-testid="error-image-url">
                    {fieldErrors.imageUrl}
                  </span>
                )}
              </label>
              {isValidHttpsImageUrl(form.imageUrl) && (
                <img
                  src={form.imageUrl.trim()}
                  alt="Vista previa de la imagen representativa"
                  className="admin-image-preview"
                  data-testid="image-url-preview"
                />
              )}
              {error && <p className="form-error" data-testid="admin-form-error">{error}</p>}
              <p className="required-note">* Campos obligatorios</p>
              <div className="modal-actions">
                <button type="submit" className="btn-save" data-testid="admin-save-btn">
                  {editing ? "Guardar cambios" : "Crear"}
                </button>
                <button
                  type="button"
                  className="btn-cancel"
                  data-testid="admin-cancel-btn"
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
        testId="confirm-cancel"
      />

      <ConfirmDialog
        show={deleteConfirm !== null}
        message={deleteConfirm ? `¿Eliminar la categoría "${deleteConfirm.name}"? Solo se puede eliminar si no tiene alojamientos asociados.` : ""}
        onConfirm={confirmDelete}
        onCancel={() => setDeleteConfirm(null)}
        testId="confirm-delete"
      />
      {deleteError && <p role="alert" className="form-error">{deleteError}</p>}
    </>
  );
}
