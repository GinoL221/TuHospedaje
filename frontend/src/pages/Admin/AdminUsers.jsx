import { useState, useEffect } from "react";
import { get, put } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import useTableData from "../../hooks/useTableData";
import SortableTh from "../../components/SortableTh/SortableTh";
import Pagination from "../../components/Pagination/Pagination";

export default function AdminUsers() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const { pageItems, sortKey, sortDir, requestSort, page, totalPages, setPage } = useTableData(users, {
    accessors: { name: (u) => (u.firstName || "") + " " + (u.lastName || "") },
  });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await get("/users");
        if (!cancelled) setUsers(Array.isArray(data) ? data : []);
      } catch (err) { console.error(err); }
    })();
    return () => { cancelled = true; };
  }, []);

  const toggleRole = async (u) => {
    const newRole = u.role === "ADMIN" ? "USER" : "ADMIN";
    const action = newRole === "ADMIN" ? "dar permisos de admin" : "quitar permisos de admin";
    if (!window.confirm(`¿${action} a "${u.firstName} ${u.lastName}"?`)) return;

    try {
      const updated = await put(`/users/${u.id}/role`, { role: newRole });
      setUsers(users.map((usr) => (usr.id === u.id ? updated : usr)));
    } catch (err) {
      alert(err.message);
    }
  };

  return (
    <>
      {users.length === 0 ? (
        <p className="empty-state">No hay usuarios registrados.</p>
      ) : (
        <>
          <table>
            <thead>
              <tr>
                <SortableTh columnKey="id" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>ID</SortableTh>
                <SortableTh columnKey="name" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>Nombre</SortableTh>
                <SortableTh columnKey="email" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>Email</SortableTh>
                <SortableTh columnKey="role" sortKey={sortKey} sortDir={sortDir} onSort={requestSort}>Rol</SortableTh>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {pageItems.map((u) => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td>{u.firstName} {u.lastName}</td>
                  <td>{u.email}</td>
                  <td><span className={"role-badge " + (u.role === "ADMIN" ? "role-admin" : "role-user")}>{u.role}</span></td>
                  <td>
                    <button
                      className={"btn-role " + (u.role === "ADMIN" ? "btn-demote" : "btn-promote")}
                      onClick={() => toggleRole(u)}
                      disabled={u.email === currentUser?.email}
                      title={u.email === currentUser?.email ? "No podés cambiarte tu propio rol" : ""}
                    >
                      {u.role === "ADMIN" ? "Quitar admin" : "Hacer admin"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      )}
    </>
  );
}
