import Icon from "../../components/Icons/Icon";

export default function CategoryCard({ category, isActive, onClick }) {
  return (
    <button
      type="button"
      className={"category-tag" + (isActive ? " active" : "")}
      onClick={onClick}
      aria-pressed={isActive}
    >
      <span className="category-icon" aria-hidden="true">
        <Icon name={category.icon} size={24} />
      </span>
      <span className="category-name">{category.name}</span>
      {category.description && (
        <span className="category-description">{category.description}</span>
      )}
    </button>
  );
}
