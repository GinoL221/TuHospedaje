import { useState } from "react";
import { post, put } from "../../services/api";
import useConfirmCancel from "../../hooks/useConfirmCancel";
import ConfirmDialog from "../../components/ConfirmDialog";
import ImageUpload from "../../components/ImageUpload/ImageUpload";
import Icon from "../Icons/Icon";

export default function LodgingFormModal({
  lodging,
  categories,
  features,
  policies,
  onSaved,
  onClose,
}) {
  const isEdit = Boolean(lodging?.id);

  const [form, setForm] = useState({
    name: lodging?.name ?? "",
    description: lodging?.description ?? "",
    address: lodging?.address ?? "",
    city: lodging?.city ?? "",
    country: lodging?.country ?? "",
    phoneNumber: lodging?.phoneNumber ?? "",
    email: lodging?.email ?? "",
    categoryId: lodging?.categoryId ?? "",
    featureIds: lodging?.features?.map((f) => f.id) ?? [],
    policyIds: lodging?.policies?.map((p) => p.id) ?? [],
    imageUrls: lodging?.imageUrls ?? [],
  });
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});

  const hasChanges = isEdit
    ? form.name !== (lodging.name ?? "") ||
      form.description !== (lodging.description ?? "") ||
      form.address !== (lodging.address ?? "") ||
      form.city !== (lodging.city ?? "") ||
      form.country !== (lodging.country ?? "") ||
      form.phoneNumber !== (lodging.phoneNumber ?? "") ||
      form.email !== (lodging.email ?? "") ||
      String(form.categoryId) !== String(lodging.categoryId ?? "")
    : Boolean(
        form.name ||
        form.description ||
        form.address ||
        form.city ||
        form.country ||
        form.phoneNumber ||
        form.email ||
        form.categoryId ||
        form.featureIds.length > 0 ||
        form.policyIds.length > 0 ||
        form.imageUrls.length > 0,
      );

  const cancel = useConfirmCancel(hasChanges, () => {
    setFieldErrors({});
    onClose();
  });

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
    }
    const payload = {
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

    const request = isEdit
      ? put(`/lodgings/${lodging.id}`, payload)
      : post("/lodgings", payload);

    request
      .then(() => {
        onSaved();
        onClose();
      })
      .catch((err) => setError(err.message));
  };

  function inputField(name, label, type, field) {
    const value = form[name];
    const error = fieldErrors[name];
    return (
      <label className={label ? "required-dot" : ""}>
        {field || name.charAt(0).toUpperCase() + name.slice(1)}
        <input
          type={type || "text"}
          value={value}
          className={error ? "input-error" : ""}
          onChange={(e) => {
            setForm({ ...form, [name]: e.target.value });
            if (error) setFieldErrors({ ...fieldErrors, [name]: "" });
          }}
        />
        {error && <span className="field-error">{error}</span>}
      </label>
    );
  }

  return (
    <>
      <div className="modal-overlay" onClick={cancel.handleCancel}>
        <div className="modal modal-lg" onClick={(e) => e.stopPropagation()}>
          <h2>{isEdit ? "Editar alojamiento" : "Nuevo alojamiento"}</h2>
          <form onSubmit={handleSubmit} noValidate>
            <div className="modal-form-grid">
              {inputField("name", true, "text", "Nombre del alojamiento")}
              {inputField("email", true, "email", "Correo electrónico")}
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
                  <span className="field-error">{fieldErrors.description}</span>
                )}
              </label>
              {inputField("address", true, "text", "Dirección")}
              {inputField("city", true, "text", "Ciudad")}
              {inputField("country", true, "text", "País")}
              {inputField("phoneNumber", true, "tel", "Teléfono")}
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
                      <Icon name={f.icon} /> {f.name}
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
                              : (form.policyIds || []).filter(
                                  (pid) => pid !== id,
                                ),
                          });
                        }}
                      />
                      <Icon name={p.icon} /> {p.name}
                    </label>
                  ))}
                </div>
              </div>
              <ImageUpload
                urls={form.imageUrls}
                onUrlsChange={(urls) => setForm({ ...form, imageUrls: urls })}
              />
              {error && <p className="form-error full-width">{error}</p>}
              <p className="required-note full-width">* Campos obligatorios</p>
              <div className="modal-actions full-width">
                <button type="submit" className="btn-save">
                  {isEdit ? "Guardar cambios" : "Guardar"}
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
