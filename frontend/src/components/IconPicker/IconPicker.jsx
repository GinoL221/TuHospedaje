import { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
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
  const triggerRef = useRef(null);
  const popoverRef = useRef(null);
  const [coords, setCoords] = useState({ top: 0, left: 0, width: 0 });

  const iconKeys = useMemo(() => Object.keys(ICON_MAP), []);
  const filteredKeys = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return iconKeys;
    return iconKeys.filter((key) => key.toLowerCase().includes(q));
  }, [iconKeys, query]);

  useEffect(() => {
    if (!isOpen) return;

    const updatePosition = () => {
      if (triggerRef.current) {
        const rect = triggerRef.current.getBoundingClientRect();
        setCoords({
          top: rect.bottom,
          left: rect.left,
          width: rect.width,
        });
      }
    };

    updatePosition();

    const handleScrollResize = () => {
      setIsOpen(false);
    };

    const handleKeyDown = (event) => {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    };

    const handleOutsideClick = (event) => {
      if (
        triggerRef.current &&
        !triggerRef.current.contains(event.target) &&
        popoverRef.current &&
        !popoverRef.current.contains(event.target)
      ) {
        setIsOpen(false);
      }
    };

    window.addEventListener("scroll", handleScrollResize, true);
    window.addEventListener("resize", handleScrollResize);
    document.addEventListener("keydown", handleKeyDown);
    document.addEventListener("mousedown", handleOutsideClick);

    return () => {
      window.removeEventListener("scroll", handleScrollResize, true);
      window.removeEventListener("resize", handleScrollResize);
      document.removeEventListener("keydown", handleKeyDown);
      document.removeEventListener("mousedown", handleOutsideClick);
    };
  }, [isOpen]);

  return (
    <div className="icon-picker">
      <button
        ref={triggerRef}
        type="button"
        className="icon-picker-trigger"
        data-testid="icon-picker-trigger"
        onClick={() => setIsOpen((prev) => !prev)}
        aria-expanded={isOpen}
      >
        <span className="icon-picker-preview">
          <Icon name={value} size={18} />
          <code>{value || "Seleccionar"}</code>
        </span>
        <span className="icon-picker-caret">▾</span>
      </button>

      {isOpen &&
        createPortal(
          <div
            ref={popoverRef}
            className="icon-picker-popover"
            style={{
              position: "fixed",
              top: coords.top + 6,
              left: coords.left,
              width: coords.width,
              zIndex: 3000,
              boxSizing: "border-box",
            }}
          >
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="icon-picker-search"
              data-testid="icon-picker-search"
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
                    data-testid={`icon-picker-item-${key}`}
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
          </div>,
          document.body
        )}
    </div>
  );
}
