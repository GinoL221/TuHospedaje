import { useState } from "react";
import { postMultipart } from "../../services/api";

const GENERIC_UPLOAD_ERROR = "No se pudo subir la imagen. Intentá de nuevo.";

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
      const data = await postMultipart("/upload", formData);
      onUrlsChange([...urls, data.url]);
    } catch (err) {
      // Input rejections have a localized backend message. Timeout, abort, network,
      // and provider failures stay recoverable and generic rather than exposing raw text.
      setError(
        err?.status >= 400 && err.status < 500 && err.hasServerMessage
          ? err.message
          : GENERIC_UPLOAD_ERROR,
      );
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
