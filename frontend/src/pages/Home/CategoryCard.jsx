import {
  Tag,
  Hotel,
  TreePine,
  Building2,
  Tent,
  BedDouble,
  Building,
  House,
  Landmark,
  Waves,
  Bike,
  Mountain,
} from "lucide-react";

const ICON_MAP = {
  Hotel,
  Hoteles: Hotel,
  "Hotel Boutique": Hotel,
  Cabaña: TreePine,
  Cabañas: TreePine,
  Departamento: Building2,
  Departamentos: Building2,
  Glamping: Tent,
  Hostel: BedDouble,
  Hostels: BedDouble,
  "Apart-Hotel": Building,
  "Apart Hotel": Building,
  Casa: House,
  Casas: House,
  Villa: Landmark,
  Villas: Landmark,
  Resort: Waves,
  Resorts: Waves,
  Bicicleta: Bike,
  Montaña: Mountain,
};

export default function CategoryCard({ category, isActive, onClick }) {
  const Icon = ICON_MAP[category.name] ?? Tag;
  return (
    <div
      className={"category-tag" + (isActive ? " active" : "")}
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === "Enter" && onClick()}
    >
      <Icon size={28} className="category-icon" />
      <span className="category-name">{category.name}</span>
      {category.description && (
        <span className="category-description">{category.description}</span>
      )}
    </div>
  );
}
