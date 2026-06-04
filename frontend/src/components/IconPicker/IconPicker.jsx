import { useEffect, useMemo, useRef, useState } from "react";
import Icon from "../Icons/Icon";
import { ICON_MAP } from "../../utils/iconMap";
import "./IconPicker.css";

export default function IconPicker({
  value,
  onChange,
  placeholder = "Buscar ícono",
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState("");
  const rootRef = useRef(null);

  const iconKeys = useMemo(() => Object.keys(ICON_MAP), []);
  const filteredKeys = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return iconKeys;
    return iconKeys.filter((key) => key.toLowerCase().includes(q));
  }, [iconKeys, query]);

  useEffect(() => {
    function handleOutsideClick(event) {
      if (!rootRef.current?.contains(event.target)) {
        setIsOpen(false);
      }
    }

    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  return (
    <div className="icon-picker" ref={rootRef}>
      <button
        type="button"
        className="icon-picker-trigger"
        onClick={() => setIsOpen((prev) => !prev)}
        aria-expanded={isOpen}
      >
        <span className="icon-picker-preview">
          <Icon name={value} size={18} />
          <code>{value || "Seleccionar"}</code>
        </span>
        <span className="icon-picker-caret">▾</span>
      </button>

      {isOpen && (
        <div className="icon-picker-popover">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="icon-picker-search"
            placeholder={placeholder}
          />

          <div className="icon-picker-grid" role="listbox">
            {filteredKeys.map((key) => {
              const isSelected = value === key;
              return (
                <button
                  key={key}
                  type="button"
                  className={`icon-picker-item ${isSelected ? "selected" : ""}`}
                  onClick={() => {
                    onChange(key);
                    setIsOpen(false);
                  }}
                >
                  <Icon name={key} size={18} />
                  <span>{key}</span>
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
