import { ICON_MAP } from "../../utils/iconMap";

export default function Icon({ name, size = 20, className = "" }) {
  const Component = ICON_MAP[name];
  if (!Component) return <span className={className}>?</span>;
  return <Component size={size} className={className} />;
}
