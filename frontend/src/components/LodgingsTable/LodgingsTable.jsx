import SortableTh from "../SortableTh/SortableTh";
import Pagination from "../Pagination/Pagination";

export default function LodgingsTable({
  lodgings,
  onDelete,
  onEdit,
  sortKey,
  sortDir,
  onSort,
  page,
  totalPages,
  onPageChange,
}) {
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
            <SortableTh
              columnKey="id"
              sortKey={sortKey}
              sortDir={sortDir}
              onSort={onSort}
            >
              ID
            </SortableTh>
            <SortableTh
              columnKey="name"
              sortKey={sortKey}
              sortDir={sortDir}
              onSort={onSort}
            >
              Nombre
            </SortableTh>
            <SortableTh
              columnKey="description"
              sortKey={sortKey}
              sortDir={sortDir}
              onSort={onSort}
            >
              Descripción
            </SortableTh>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {lodgings.map((l) => (
            <tr key={l.id} data-testid={`row-${l.id}`}>
              <td>{l.id}</td>
              <td>{l.name}</td>
              <td>{l.description}</td>
              <td>
                <button
                  className="btn-edit"
                  data-testid="row-edit-btn"
                  onClick={() => onEdit(l)}
                >
                  Editar
                </button>
                <button
                  className="btn-delete"
                  data-testid="row-delete-btn"
                  onClick={() => onDelete(l.id, l.name)}
                >
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <Pagination
        page={page}
        totalPages={totalPages}
        onPageChange={onPageChange}
      />
    </>
  );
}
