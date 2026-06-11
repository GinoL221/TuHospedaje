import useTableData from "../../hooks/useTableData";
import SortableTh from "../SortableTh/SortableTh";
import Pagination from "../Pagination/Pagination";

export default function LodgingsTable({ lodgings, onDelete, onEdit }) {
  const { pageItems, sortKey, sortDir, requestSort, page, totalPages, setPage } =
    useTableData(lodgings);

  if (lodgings.length === 0) {
    return (
      <p className="empty-state">
        No hay alojamientos cargados todavía. ¡Agregá el primero!
      </p>
    );
  }

  return (
    <>
      <table>
        <thead>
          <tr>
            <SortableTh columnKey="id" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>
              ID
            </SortableTh>
            <SortableTh columnKey="name" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>
              Nombre
            </SortableTh>
            <SortableTh columnKey="description" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>
              Descripción
            </SortableTh>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {pageItems.map((l) => (
            <tr key={l.id}>
              <td>{l.id}</td>
              <td>{l.name}</td>
              <td>{l.description}</td>
              <td>
                <button className="btn-edit" onClick={() => onEdit(l)}>
                  Editar
                </button>
                <button
                  className="btn-delete"
                  onClick={() => onDelete(l.id, l.name)}
                >
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  );
}
