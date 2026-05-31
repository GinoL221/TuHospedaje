import { useState, useEffect } from "react";
import { get, post, del } from "../../services/api";
import ConfirmDialog from "../../components/ConfirmDialog";
import useConfirmCancel from "../../hooks/useConfirmCancel";

export default function AdminLodgings() {
  const [lodgings, setLodgings] = useState([]);
  const [categories, setCategories] = useState([]);
  const [features, setFeatures] = useState([]);
  const [policies, setPolicies] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({
    name: "",
    description: "",
    address: "",
    city: "",
    country: "",
    phoneNumber: "",
    email: "",
    categoryId: "",
    featureIds: [],
    policyIds: [],
    imageUrls: [],
  });
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [uploading, setUploading] = useState(false);
  const size = 10;

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await get(`/lodgings?page=${page}&size=${size}`);
        if (cancelled) return;
        if (Array.isArray(data)) {
          setLodgings(data);
          setTotalPages(1);
          return;
        }
        setLodgings(data.lodgings || []);
        setTotalPages(data.totalPages || 0);
      } catch (err) {
        console.error(err);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [page, size]);

  const resetForm = () =>
    setForm({
      name: "",
      description: "",
      address: "",
      city: "",
      country: "",
      phoneNumber: "",
      email: "",
      categoryId: "",
      featureIds: [],
      policyIds: [],
      imageUrls: [],
    });

  async function handleImageUpload(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      const formData = new FormData();
      formData.append("file", file);
      const res = await fetch(`${import.meta.env.VITE_API_URL}/upload`, {
        method: "POST",
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
        body: formData,
      });
      const data = await res.json();
      const newUrl = data.url;
      setForm((prev) => ({
        ...prev,
        imageUrls: [...prev.imageUrls, newUrl],
      }));
    } catch (err) {
      console.error(err);
    } finally {
      setUploading(false);
      e.target.value = "";
    }
  }

  const hasChanges =
    form.name ||
    form.description ||
    form.address ||
    form.city ||
    form.country ||
    form.phoneNumber ||
    form.email ||
    form.categoryId ||
    form.featureIds.length > 0 ||
    form.imageUrls.length > 0;

  const cancel = useConfirmCancel(hasChanges, () => {
    setFieldErrors({});
    resetForm();
    setShowModal(false);
  });

  useEffect(() => {
    get("/categories")
      .then((data) => setCategories(Array.isArray(data) ? data : []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    get("/features")
      .then((data) => setFeatures(Array.isArray(data) ? data : []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    get("/policies")
      .then((data) => setPolicies(Array.isArray(data) ? data : []))
      .catch(() => {});
  }, []);

  const handleDelete = async (id, name) => {
    if (window.confirm(`¿Eliminar "${name}"?`)) {
      try {
        await del(`/lodgings/${id}`);
        const data = await get(`/lodgings?page=${page}&size=${size}`);
        if (Array.isArray(data)) {
          setLodgings(data);
          setTotalPages(1);
        } else {
          setLodgings(data.lodgings || []);
          setTotalPages(data.totalPages || 0);
        }
        const items = Array.isArray(data) ? data : data.lodgings || [];
        if (items.length === 0 && page > 0) setPage((p) => p - 1);
      } catch (err) {
        console.error(err);
      }
    }
  };

  const validate = () => {
    const errs = {};
    if (!form.name.trim()) errs.name = "El nombre es obligatorio";
    if (!form.description.trim())
      errs.description = "La descripción es obligatoria";
    if (!form.address?.trim()) errs.address = "La dirección es obligatoria";
    if (!form.city?.trim()) errs.city = "La ciudad es obligatoria";
    if (!form.country?.trim()) errs.country = "El país es obligatorio";
    if (!form.phoneNumber?.trim())
      errs.phoneNumber = "El teléfono es obligatorio";
    if (!form.email?.trim()) errs.email = "El email es obligatorio";
    else if (!/\S+@\S+\.\S+/.test(form.email))
      errs.email = "El email no es válido";
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
    };

    const body = {
      name: form.name,
      description: form.description,
      address: form.address,
      city: form.city,
      country: form.country,
      phoneNumber: form.phoneNumber,
      email: form.email,
      categoryId: form.categoryId || null,
      featureIds: form.featureIds,
      imageUrls: form.imageUrls || [],
      policyIds: form.policyIds || [],
    };

    post("/lodgings", body)
      .then(() => {
        setShowModal(false);
        setForm({
          name: "",
          description: "",
          address: "",
          city: "",
          country: "",
          phoneNumber: "",
          email: "",
          categoryId: "",
          featureIds: [],
          imageUrls: [],
          policyIds: [],
        });
        setPage(0);
      })
      .catch((err) => setError(err.message));
  };

  return (
    <>
      <div className="admin-toolbar">
        <button
          className="btn-add"
          onClick={() => {
            setFieldErrors({});
            setShowModal(true);
          }}
        >
          + Agregar alojamiento
        </button>
      </div>
      {lodgings.length === 0 ? (
        <p className="empty-state">
          No hay alojamientos cargados todavía. ¡Agregá el primero!
        </p>
      ) : (
        <>
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
        </>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={cancel.handleCancel}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>Nuevo alojamiento</h2>
            <form onSubmit={handleSubmit} noValidate>
              <div className="modal-form-grid">
                <label className="required-dot">
                  Nombre
                  <input
                    value={form.name}
                    className={fieldErrors.name ? "input-error" : ""}
                    onChange={(e) => {
                      setForm({ ...form, name: e.target.value });
                      if (fieldErrors.name)
                        setFieldErrors({ ...fieldErrors, name: "" });
                    }}
                  />
                  {fieldErrors.name && (
                    <span className="field-error">{fieldErrors.name}</span>
                  )}
                </label>
                <label className="required-dot">
                  Email
                  <input
                    type="email"
                    value={form.email}
                    className={fieldErrors.email ? "input-error" : ""}
                    onChange={(e) => {
                      setForm({ ...form, email: e.target.value });
                      if (fieldErrors.email)
                        setFieldErrors({ ...fieldErrors, email: "" });
                    }}
                  />
                  {fieldErrors.email && (
                    <span className="field-error">{fieldErrors.email}</span>
                  )}
                </label>
                <label className="full-width required-dot">
                  Descripción
                  <textarea
                    value={form.description}
                    className={fieldErrors.description ? "input-error" : ""}
                    onChange={(e) => {
                      setForm({ ...form, description: e.target.value });
                      if (fieldErrors.description)
                        setFieldErrors({ ...fieldErrors, description: "" });
                    }}
                  />
                  {fieldErrors.description && (
                    <span className="field-error">
                      {fieldErrors.description}
                    </span>
                  )}
                </label>
                <label className="required-dot">
                  Dirección
                  <input
                    value={form.address}
                    className={fieldErrors.address ? "input-error" : ""}
                    onChange={(e) => {
                      setForm({ ...form, address: e.target.value });
                      if (fieldErrors.address)
                        setFieldErrors({ ...fieldErrors, address: "" });
                    }}
                  />
                  {fieldErrors.address && (
                    <span className="field-error">{fieldErrors.address}</span>
                  )}
                </label>
                <label className="required-dot">
                  Ciudad
                  <input
                    value={form.city}
                    className={fieldErrors.city ? "input-error" : ""}
                    onChange={(e) => {
                      setForm({ ...form, city: e.target.value });
                      if (fieldErrors.city)
                        setFieldErrors({ ...fieldErrors, city: "" });
                    }}
                  />
                  {fieldErrors.city && (
                    <span className="field-error">{fieldErrors.city}</span>
                  )}
                </label>
                <label className="required-dot">
                  País
                  <input
                    value={form.country}
                    className={fieldErrors.country ? "input-error" : ""}
                    onChange={(e) => {
                      setForm({ ...form, country: e.target.value });
                      if (fieldErrors.country)
                        setFieldErrors({ ...fieldErrors, country: "" });
                    }}
                  />
                  {fieldErrors.country && (
                    <span className="field-error">{fieldErrors.country}</span>
                  )}
                </label>
                <label className="required-dot">
                  Teléfono
                  <input
                    value={form.phoneNumber}
                    className={fieldErrors.phoneNumber ? "input-error" : ""}
                    onChange={(e) => {
                      setForm({ ...form, phoneNumber: e.target.value });
                      if (fieldErrors.phoneNumber)
                        setFieldErrors({ ...fieldErrors, phoneNumber: "" });
                    }}
                  />
                  {fieldErrors.phoneNumber && (
                    <span className="field-error">
                      {fieldErrors.phoneNumber}
                    </span>
                  )}
                </label>
                <label>
                  Categoría
                  <select
                    value={form.categoryId || ""}
                    onChange={(e) =>
                      setForm({ ...form, categoryId: e.target.value })
                    }
                  >
                    <option value="">Sin categoría</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </label>
                <div className="full-width field-group">
                  <span className="field-label">Características</span>
                  <div className="feature-checkboxes">
                    {features.map((f) => (
                      <label key={f.id} className="feature-checkbox">
                        <input
                          type="checkbox"
                          value={f.id}
                          checked={form.featureIds.includes(f.id)}
                          onChange={(e) => {
                            const id = Number(e.target.value);
                            setForm({
                              ...form,
                              featureIds: e.target.checked
                                ? [...form.featureIds, id]
                                : form.featureIds.filter((fid) => fid !== id),
                            });
                          }}
                        />
                        {f.icon} {f.name}
                      </label>
                    ))}
                  </div>
                </div>
                <div className="full-width field-group">
                  <span className="field-label">Políticas</span>
                  <div className="feature-checkboxes">
                    {policies.map((p) => (
                      <label key={p.id} className="feature-checkbox">
                        <input
                          type="checkbox"
                          value={p.id}
                          checked={form.policyIds?.includes(p.id)}
                          onChange={(e) => {
                            const id = Number(e.target.value);
                            setForm({
                              ...form,
                              policyIds: e.target.checked
                                ? [...(form.policyIds || []), id]
                                : (form.policyIds || []).filter((pid) => pid !== id),
                            });
                          }}
                        />
                        {p.icon} {p.name}
                      </label>
                    ))}
                  </div>
                </div>
                <label className="full-width">
                  URLs de imágenes (separadas por coma)
                  <div className="image-upload-row">
                    <input
                      type="file"
                      accept="image/*"
                      id="imageUpload"
                      style={{ display: "none" }}
                      onChange={handleImageUpload}
                      disabled={uploading}
                    />
                    <button
                      type="button"
                      className="btn-upload"
                      onClick={() =>
                        document.getElementById("imageUpload").click()
                      }
                      disabled={uploading}
                    >
                      {uploading ? "Subiendo..." : "Subir imagen"}
                    </button>
                  </div>
                  <textarea
                    value={form.imageUrls}
                    onChange={(e) =>
                      setForm({ ...form, imageUrls: e.target.value })
                    }
                    placeholder="https://ejemplo.com/img1.jpg, https://ejemplo.com/img2.jpg"
                  />
                </label>
                {error && <p className="form-error full-width">{error}</p>}
                <p className="required-note full-width">
                  * Campos obligatorios
                </p>
                <div className="modal-actions full-width">
                  <button type="submit" className="btn-save">
                    Guardar
                  </button>
                  <button
                    type="button"
                    className="btn-cancel"
                    onClick={cancel.handleCancel}
                  >
                    Cancelar
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDialog
        show={cancel.showConfirm}
        message="Hay cambios sin guardar. ¿Cancelar de todas formas?"
        onConfirm={() => {
          cancel.confirmCancel();
        }}
        onCancel={cancel.dismissConfirm}
      />
    </>
  );
}