import { useState } from "react";
import { getCsrfToken } from "../../services/api";

const GENERIC_UPLOAD_ERROR = "No se pudo subir la imagen. Intentá de nuevo.";

/**
 * A 4xx from /upload is the admin's own input — an unsupported format, an empty
 * part, a file over the size ceiling — and the backend already localized that
 * reason. Showing "Intentá de nuevo" instead tells them to repeat the exact
 * action that just failed.
 *
 * A 5xx is a server fault they cannot act on, and its body carries the
 * deliberately non-disclosing "Internal server error.", so the generic message
 * stays. Same for an unparseable body: there is no reason to relay.
 */
async function clientErrorMessage(res) {
  if (res.status < 400 || res.status >= 500) return GENERIC_UPLOAD_ERROR;
  try {
    const body = await res.json();
    return body?.error || GENERIC_UPLOAD_ERROR;
  } catch {
    return GENERIC_UPLOAD_ERROR;
  }
}

export default function ImageUpload({
  urls,
  onUrlsChange,
  disabled = false,
  onUploadingChange,
}) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);

  async function handleUpload(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    onUploadingChange?.(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append("file", file);
      const res = await fetch(`${import.meta.env.VITE_API_URL}/upload`, {
        method: "POST",
        credentials: "include",
        headers: { "X-XSRF-TOKEN": getCsrfToken() },
        body: formData,
      });
      if (!res.ok) {
        setError(await clientErrorMessage(res));
        return;
      }
      const data = await res.json();
      onUrlsChange([...urls, data.url]);
    } catch (err) {
      // fetch itself rejected (network/CORS): err.message is raw browser text,
      // never fit to show a user.
      console.error(err);
      setError(GENERIC_UPLOAD_ERROR);
    } finally {
      setUploading(false);
      onUploadingChange?.(false);
      e.target.value = "";
    }
  }

  return (
    <label className="full-width">
      URLs de imágenes (separadas por coma)
      <div className="image-upload-row">
        <input
          type="file"
          accept="image/*"
          id="imageUpload"
          style={{ display: "none" }}
          onChange={handleUpload}
          disabled={disabled || uploading}
          tabIndex={-1}
        />
        <button
          type="button"
          className="btn-upload"
          onClick={() => document.getElementById("imageUpload").click()}
          disabled={disabled || uploading}
        >
          {uploading ? "Subiendo..." : "Subir imagen"}
        </button>
      </div>
      {error && <p className="field-error">{error}</p>}
      {urls.length > 0 && (
        <div className="image-preview-list">
          {urls.map((url, i) => (
            <div key={i} className="image-preview-item">
              <img src={url} alt={`Imagen ${i + 1}`} />
              <button
                type="button"
                className="image-remove"
                onClick={() => onUrlsChange(urls.filter((_, j) => j !== i))}
                disabled={disabled || uploading}
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}
    </label>
  );
}
