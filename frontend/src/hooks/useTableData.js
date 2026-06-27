import { useState, useMemo } from "react";

function compareValues(a, b) {
  // null / empty sorts to the end
  const aEmpty = a === null || a === undefined || a === "";
  const bEmpty = b === null || b === undefined || b === "";
  if (aEmpty && bEmpty) return 0;
  if (aEmpty) return 1;
  if (bEmpty) return -1;

  if (typeof a === "number" && typeof b === "number") return a - b;
  return String(a).localeCompare(String(b), "es", {
    numeric: true,
    sensitivity: "base",
  });
}

export default function useTableData(
  items,
  { pageSize = 10, initialSortKey = "id", accessors = {} } = {}
) {
  const [sortKey, setSortKey] = useState(initialSortKey);
  const [sortDir, setSortDir] = useState("asc");
  const [page, setPage] = useState(0);

  const sorted = useMemo(() => {
    if (!Array.isArray(items) || items.length === 0) return [];
    const getValue = (item) =>
      accessors[sortKey] ? accessors[sortKey](item) : item[sortKey];
    return [...items].sort((a, b) => {
      const cmp = compareValues(getValue(a), getValue(b));
      return sortDir === "asc" ? cmp : -cmp;
    });
  }, [items, sortKey, sortDir, accessors]);

  const totalPages = Math.ceil(sorted.length / pageSize);

  // Clamp page to valid range without triggering an extra render cycle
  const safePage = totalPages > 0 ? Math.min(page, totalPages - 1) : 0;

  const pageItems = sorted.slice(safePage * pageSize, (safePage + 1) * pageSize);

  const requestSort = (key) => {
    if (key === sortKey) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortKey(key);
      setSortDir("asc");
    }
    setPage(0);
  };

  return {
    pageItems,
    sortKey,
    sortDir,
    requestSort,
    page: safePage,
    totalPages,
    setPage,
  };
}
