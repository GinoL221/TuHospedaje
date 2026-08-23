import { useState } from "react";
import { ImageOff } from "lucide-react";

// A category's representative image (US-21.2) is deliberately distinct from
// the lodging feature icons rendered by <Icon>/ICON_MAP elsewhere in the
// app: reusing a feature icon here would conflate category media with
// amenity representation, which is the exact gap this component closes.
export default function CategoryCard({ category, isActive, onClick }) {
  const [imageFailed, setImageFailed] = useState(false);
  const hasImage = Boolean(category.imageUrl) && !imageFailed;

  return (
    <button
      type="button"
      className={"category-tag" + (isActive ? " active" : "")}
      onClick={onClick}
      aria-pressed={isActive}
    >
      {hasImage ? (
        <img
          src={category.imageUrl}
          alt={`Imagen representativa de ${category.name}`}
          className="category-image"
          onError={() => setImageFailed(true)}
        />
      ) : (
        <span
          className="category-image category-image-fallback"
          role="img"
          aria-label={`Imagen no disponible para ${category.name}`}
        >
          <ImageOff size={24} aria-hidden="true" />
        </span>
      )}
      <span className="category-name">{category.name}</span>
      {category.description && (
        <span className="category-description">{category.description}</span>
      )}
    </button>
  );
}
