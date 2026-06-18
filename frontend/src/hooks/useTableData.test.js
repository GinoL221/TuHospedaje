import { renderHook, act } from "@testing-library/react";
import useTableData from "./useTableData";

describe("useTableData - default sort", () => {
  it("sorts pageItems ascending by initialSortKey on initialization", () => {
    const items = [
      { id: 3, name: "Charlie" },
      { id: 1, name: "Alpha" },
      { id: 2, name: "Bravo" },
    ];

    const { result } = renderHook(() => useTableData(items, { initialSortKey: "id" }));

    expect(result.current.pageItems.map((i) => i.id)).toEqual([1, 2, 3]);
    expect(result.current.sortKey).toBe("id");
    expect(result.current.sortDir).toBe("asc");
  });
});

describe("useTableData - toggle sort direction", () => {
  it("flips sortDir and resets page to 0 when requestSort is called again on the same key", () => {
    const items = [
      { id: 1, name: "Alpha" },
      { id: 2, name: "Bravo" },
    ];

    const { result } = renderHook(() => useTableData(items, { initialSortKey: "id" }));

    act(() => {
      result.current.setPage(0);
    });

    act(() => {
      result.current.requestSort("id");
    });

    expect(result.current.sortDir).toBe("desc");
    expect(result.current.page).toBe(0);
    expect(result.current.pageItems.map((i) => i.id)).toEqual([2, 1]);
  });
});

describe("useTableData - null/empty values sort last", () => {
  it("places null and empty values after all populated items when sorted ascending", () => {
    const items = [
      { id: 1, name: "Bravo" },
      { id: 2, name: null },
      { id: 3, name: "Alpha" },
      { id: 4, name: "" },
    ];

    const { result } = renderHook(() => useTableData(items, { initialSortKey: "name" }));

    expect(result.current.pageItems.map((i) => i.id)).toEqual([3, 1, 2, 4]);
  });
});

describe("useTableData - page clamps on shrink", () => {
  it("clamps safePage to totalPages - 1 when items shrink below the current page", () => {
    const manyItems = Array.from({ length: 25 }, (_, i) => ({ id: i + 1 }));

    const { result, rerender } = renderHook(
      ({ items }) => useTableData(items, { initialSortKey: "id", pageSize: 10 }),
      { initialProps: { items: manyItems } }
    );

    act(() => {
      result.current.setPage(2);
    });
    rerender({ items: manyItems });
    expect(result.current.page).toBe(2);

    const fewItems = manyItems.slice(0, 5);
    rerender({ items: fewItems });

    expect(result.current.totalPages).toBe(1);
    expect(result.current.page).toBe(0);
  });
});
