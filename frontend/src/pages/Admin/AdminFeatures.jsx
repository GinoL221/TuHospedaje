import { useState, useEffect } from "react";
import { get, post, put, del } from "../../services/api";
import ConfirmDialog from "../../components/ConfirmDialog";
import useConfirmCancel from "../../hooks/useConfirmCancel";
import Icon from "../../components/Icons/Icon";
import IconPicker from "../../components/IconPicker/IconPicker";
import useTableData from "../../hooks/useTableData";
import SortableTh from "../../components/SortableTh/SortableTh";
import Pagination from "../../components/Pagination/Pagination";

export default function AdminFeatures() {
  const [featureList, setFeatureList] = useState([]);
  const { pageItems, sortKey, sortDir, requestSort, page, totalPages, setPage } = useTableData(featureList);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({ name: "", icon: "" });
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});

  const resetForm = () => setForm({ name: "", icon: "" });
  const cancel = useConfirmCancel(form.name || form.icon, () => { setFieldErrors({}); resetForm(); setShowModal(false); });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await get("/features");
        if (!cancelled) setFeatureList(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error(err);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const openModal = (feat = null) => {
    if (feat) {
      setForm({ name: feat.name, icon: feat.icon });
      setEditing(feat);
    } else {
      setForm({ name: "", icon: "" });
      setEditing(null);
    }
    setError("");
    setFieldErrors({});
    setShowModal(true);
  };

  const refresh = () => {
    get("/features")
      .then((data) => setFeatureList(Array.isArray(data) ? data : []))
      .catch(() => {});
  };

  const validate = () => {
    const errs = {};
    if (!form.name.trim()) errs.name = "El nombre es obligatorio";
    if (!form.icon.trim()) errs.icon = "El ícono es obligatorio";
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

    const body = { name: form.name, icon: form.icon };
    const request = editing
      ? put(`/features/${editing.id}`, body)
      : post("/features", body);
    request
      .then(() => {
        setShowModal(false);
        refresh();
      })
      .catch((err) => setError(err.message));
  };

  const handleDelete = async (id, name) => {
    if (window.confirm(`¿Eliminar característica "${name}"?`)) {
      try {
        await del(`/features/${id}`);
        refresh();
      } catch (err) {
        alert(err.message);
      }
    }
  };

  return (
    <>
      <button className="btn-fab" data-testid="admin-add-btn" onClick={() => openModal(null)}>
        + Agregar característica
      </button>
      {featureList.length === 0 ? (
        <p className="empty-state">
          No hay características cargadas todavía. ¡Creá la primera!
        </p>
      ) : (
        <>
          <table>
            <thead>
              <tr>
                <SortableTh columnKey="id" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>ID</SortableTh>
                <SortableTh columnKey="name" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>Nombre</SortableTh>
                <SortableTh columnKey="icon" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>Ícono</SortableTh>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {pageItems.map((f) => (
                <tr key={f.id} data-testid={`row-${f.id}`}>
                  <td>{f.id}</td>
                  <td>{f.name}</td>
                  <td>
                    <Icon name={f.icon} /> <code>{f.icon}</code>
                  </td>
                  <td>
                    <button className="btn-edit" data-testid="row-edit-btn" onClick={() => openModal(f)}>
                      Editar
                    </button>
                    <button
                      className="btn-delete"
                      data-testid="row-delete-btn"
                      onClick={() => handleDelete(f.id, f.name)}
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
            <h2>
              {editing ? "Editar característica" : "Nueva característica"}
            </h2>
            <form onSubmit={handleSubmit} noValidate>
              <label className="required-dot">
                Nombre
                <input data-testid="field-name" value={form.name} className={fieldErrors.name ? "input-error" : ""} onChange={(e) => { setForm({ ...form, name: e.target.value }); if (fieldErrors.name) setFieldErrors({ ...fieldErrors, name: "" }); }} />
                {fieldErrors.name && <span className="field-error" data-testid="error-name">{fieldErrors.name}</span>}
              </label>
              <label className="required-dot">
                Ícono
                <IconPicker value={form.icon} onChange={(val) => { setForm({ ...form, icon: val }); if (fieldErrors.icon) setFieldErrors({ ...fieldErrors, icon: "" }); }} placeholder="fa-solid fa-wifi" />
                {fieldErrors.icon && <span className="field-error" data-testid="error-icon">{fieldErrors.icon}</span>}
              </label>
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
    </>
  );
}
