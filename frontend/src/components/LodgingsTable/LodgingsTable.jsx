export default function LodgingsTable({
  lodgings,
  page,
  totalPages,
  onDelete,
  onEdit,
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
            <th>ID</th>
            <th>Nombre</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {lodgings.map((l) => (
            <tr key={l.id}>
              <td>{l.id}</td>
              <td>{l.name}</td>
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
      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => onPageChange(0)}>
            Inicio
          </button>
          <button disabled={page === 0} onClick={() => onPageChange(page - 1)}>
            Anterior
          </button>
          <span>
            Página {page + 1} de {totalPages}
          </span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => onPageChange(page + 1)}
          >
            Siguiente
          </button>
        </div>
      )}
    </>
  );
}
