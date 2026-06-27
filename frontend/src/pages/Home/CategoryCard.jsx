import { Tag } from "lucide-react";
import { ICON_MAP } from "../../utils/iconMap";
import Icon from "../../components/Icons/Icon";

export default function CategoryCard({ category, isActive, onClick }) {
  const hasIcon = category.icon && ICON_MAP[category.icon];
  return (
    <div
      className={"category-tag" + (isActive ? " active" : "")}
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === "Enter" && onClick()}
    >
      {hasIcon ? (
        <Icon name={category.icon} size={28} className="category-icon" />
      ) : (
        <Tag size={28} className="category-icon" />
      )}
      <span className="category-name">{category.name}</span>
      {category.description && (
        <span className="category-description">{category.description}</span>
      )}
    </div>
  );
}
