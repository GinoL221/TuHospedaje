import { useState, useEffect } from "react";
import { get, post, put, del } from "../../services/api";
import ConfirmDialog from "../../components/ConfirmDialog";
import useConfirmCancel from "../../hooks/useConfirmCancel";
import Icon from "../../components/Icons/Icon";
import IconPicker from "../../components/IconPicker/IconPicker";
import useTableData from "../../hooks/useTableData";
import SortableTh from "../../components/SortableTh/SortableTh";
import Pagination from "../../components/Pagination/Pagination";

export default function AdminPolicies() {
  const [policyList, setPolicyList] = useState([]);
  const { pageItems, sortKey, sortDir, requestSort, page, totalPages, setPage } = useTableData(policyList);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({ name: "", description: "", icon: "" });
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [deleteConfirm, setDeleteConfirm] = useState(null);

  const resetForm = () => setForm({ name: "", description: "", icon: "" });
  const cancel = useConfirmCancel(form.name || form.description || form.icon, () => { setFieldErrors({}); resetForm(); setShowModal(false); });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await get("/policies");
        if (!cancelled) setPolicyList(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error(err);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const openModal = (policy = null) => {
    if (policy) {
      setForm({ name: policy.name, description: policy.description || "", icon: policy.icon });
      setEditing(policy);
    } else {
      setForm({ name: "", description: "", icon: "" });
      setEditing(null);
    }
    setError("");
    setFieldErrors({});
    setShowModal(true);
  };

  const refresh = () => {
    get("/policies")
      .then((data) => setPolicyList(Array.isArray(data) ? data : []))
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

    const body = { name: form.name, description: form.description, icon: form.icon };
    const request = editing
      ? put(`/policies/${editing.id}`, body)
      : post("/policies", body);
    request
      .then(() => {
        setShowModal(false);
        refresh();
      })
      .catch((err) => setError(err.message));
  };

  const handleDelete = (id, name) => {
    setDeleteConfirm({ id, name });
  };

  const confirmDelete = async () => {
    if (!deleteConfirm) return;
    try {
      await del(`/policies/${deleteConfirm.id}`);
      refresh();
    } catch (err) {
      alert(err.message);
    } finally {
      setDeleteConfirm(null);
    }
  };

  return (
    <>
      <button className="btn-fab" data-testid="admin-add-btn" onClick={() => openModal(null)}>
        + Agregar política
      </button>
      {policyList.length === 0 ? (
        <p className="empty-state">
          No hay políticas cargadas todavía. ¡Creá la primera!
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
              {pageItems.map((p) => (
                <tr key={p.id} data-testid={`row-${p.id}`}>
                  <td>{p.id}</td>
                  <td>{p.name}</td>
                  <td>{p.description || "—"}</td>
                  <td>
                    <Icon name={p.icon} /> <code>{p.icon}</code>
                  </td>
                  <td>
                    <button className="btn-edit" data-testid="row-edit-btn" onClick={() => openModal(p)}>
                      Editar
                    </button>
                    <button
                      className="btn-delete"
                      data-testid="row-delete-btn"
                      onClick={() => handleDelete(p.id, p.name)}
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
              {editing ? "Editar política" : "Nueva política"}
            </h2>
            <form onSubmit={handleSubmit} noValidate>
              <label className="required-dot">
                Nombre
                <input data-testid="field-name" value={form.name} className={fieldErrors.name ? "input-error" : ""} onChange={(e) => { setForm({ ...form, name: e.target.value }); if (fieldErrors.name) setFieldErrors({ ...fieldErrors, name: "" }); }} />
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
              <label className="required-dot">
                Ícono
                <IconPicker value={form.icon} onChange={(val) => { setForm({ ...form, icon: val }); if (fieldErrors.icon) setFieldErrors({ ...fieldErrors, icon: "" }); }} placeholder="fa-solid fa-clock" />
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

      <ConfirmDialog
        show={deleteConfirm !== null}
        message={deleteConfirm ? `¿Eliminar política "${deleteConfirm.name}"?` : ""}
        onConfirm={confirmDelete}
        onCancel={() => setDeleteConfirm(null)}
        testId="confirm-delete"
      />
    </>
  );
}
