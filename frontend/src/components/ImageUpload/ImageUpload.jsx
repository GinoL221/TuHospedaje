import { useState } from "react";

export default function ImageUpload({ urls, onUrlsChange }) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);

  async function handleUpload(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append("file", file);
      const res = await fetch(`${import.meta.env.VITE_API_URL}/upload`, {
        method: "POST",
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
        body: formData,
      });
      if (!res.ok) {
        throw new Error("No se pudo subir la imagen. Intentá de nuevo.");
      }
      const data = await res.json();
      onUrlsChange([...urls, data.url]);
    } catch (err) {
      console.error(err);
      setError("No se pudo subir la imagen. Intentá de nuevo.");
    } finally {
      setUploading(false);
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
          disabled={uploading}
        />
        <button
          type="button"
          className="btn-upload"
          onClick={() => document.getElementById("imageUpload").click()}
          disabled={uploading}
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
