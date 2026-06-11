export default function SortableTh({ columnKey, sortKey, sortDir, onSort, children }) {
  const isActive = sortKey === columnKey;
  const ariaSort = isActive
    ? sortDir === "asc"
      ? "ascending"
      : "descending"
    : "none";

  const icon = isActive ? (sortDir === "asc" ? "▲" : "▼") : "↕";

  return (
    <th
      className="sortable"
      aria-sort={ariaSort}
      tabIndex={0}
      onClick={() => onSort(columnKey)}
      onKeyDown={(e) => e.key === "Enter" && onSort(columnKey)}
    >
      {children}
      <span className="sort-icon">{icon}</span>
    </th>
  );
}
