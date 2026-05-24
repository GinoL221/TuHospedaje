# Plan de Pruebas — Sprint 2

**Proyecto:** TuHospedaje
**Sprint:** 2 — Seguridad y Organización
**Alcance:** 10 User Stories (#12 a #21)
**Tipos de prueba:** API (Postman), UI Manual, Automatizadas (JUnit 5 + MockMvc)

---

## TC-12: CRUD de Categorías (#12, #21)

| Campo | Detalle |
|-------|---------|
| **User Story** | #12 — Categorizar productos / #21 — Agregar categoría |
| **Precondición** | Backend corriendo, token de admin válido |
| **Tipo** | API + UI Manual + Automatizado |

| # | Paso | Resultado Esperado |
|---|------|-------------------|
| 1 | POST `/api/categories` con token admin y body válido | HTTP 201, JSON con `id`, `name`, `description` |
| 2 | POST `/api/categories` con nombre duplicado | HTTP 400 |
| 3 | GET `/api/categories` | HTTP 200, array JSON |
| 4 | GET `/api/categories/{id}` con ID existente | HTTP 200, objeto JSON |
| 5 | GET `/api/categories/{id}` con ID inexistente | HTTP 404 |
| 6 | PUT `/api/categories/{id}` con token admin | HTTP 200, datos actualizados |
| 7 | DELETE `/api/categories/{id}` sin lodgings vinculados | HTTP 204 |
| 8 | DELETE `/api/categories/{id}` con lodgings vinculados | HTTP 400, mensaje de error |
| 9 | POST sin token | HTTP 403 |
| 10 | UI: Admin → Categorías → Crear | Modal funciona, categoría aparece en tabla |
| 11 | UI: Admin → Categorías → Editar | Modal precarga datos, guarda cambios |
| 12 | UI: Admin → Categorías → Eliminar | Confirmación + tabla se actualiza |

---

## TC-13: Registro de Usuario (#13)

| Campo | Detalle |
|-------|---------|
| **User Story** | #13 — Registrar usuario |
| **Precondición** | Backend corriendo |
| **Tipo** | API + UI Manual + Automatizado |

| # | Paso | Resultado Esperado |
|---|------|-------------------|
| 1 | POST `/api/auth/register` con datos válidos | HTTP 201, JWT token |
| 2 | POST con email duplicado | HTTP 400, "El email ya está registrado" |
| 3 | POST sin firstName | HTTP 400, error de validación |
| 4 | POST con password < 6 caracteres | HTTP 400, error de validación |
| 5 | UI: Navegar a `/register` | Formulario visible |
| 6 | UI: Llenar formulario y enviar | Redirige a Home, muestra avatar + nombre |
| 7 | UI: Feedback de contraseña | ✘/✔ cambia en tiempo real al escribir |

---

## TC-14: Login + JWT (#14)

| Campo | Detalle |
|-------|---------|
| **User Story** | #14 — Identificar usuario |
| **Precondición** | Usuario registrado |
| **Tipo** | API + UI Manual + Automatizado |

| # | Paso | Resultado Esperado |
|---|------|-------------------|
| 1 | POST `/api/auth/login` con credenciales válidas | HTTP 200, JWT token con claims |
| 2 | POST con email inexistente | HTTP 401, "Credenciales inválidas" |
| 3 | POST con password incorrecta | HTTP 401 |
| 4 | UI: Login con admin | Header muestra avatar + nombre "Admin" |
| 5 | UI: Clic en avatar (admin) | Redirige a `/admin` |
| 6 | UI: Login con usuario normal | Header muestra avatar, sin acceso a admin |

---

## TC-15: Cerrar sesión (#15)

| Campo | Detalle |
|-------|---------|
| **Tipo** | UI Manual |

| # | Paso | Resultado Esperado |
|---|------|-------------------|
| 1 | Estando logueado, clic en "Cerrar sesión" | Token eliminado, header muestra botones de login/register |
| 2 | Recargar página | Sigue en modo anónimo |
| 3 | Navegar a `/admin` sin estar logueado | No puede acceder (si está protegido) |

---

## TC-16: Administración de Usuarios (#16)

| Campo | Detalle |
|-------|---------|
| **User Story** | #16 — Identificar administrador |
| **Precondición** | Backend corriendo, token de admin |
| **Tipo** | API + UI Manual |

| # | Paso | Resultado Esperado |
|---|------|-------------------|
| 1 | GET `/api/users` con token admin | HTTP 200, lista de usuarios |
| 2 | GET `/api/users` sin token | HTTP 403 |
| 3 | PUT `/api/users/{id}/role` con body `{"role":"ADMIN"}` | HTTP 200, rol actualizado |
| 4 | PUT `/api/users/{id}/role` al propio usuario | Botón deshabilitado en UI |
| 5 | UI: Admin → Usuarios | Tabla con ID, Nombre, Email, Rol, Acciones |
| 6 | UI: Hacer admin a un usuario | Rol cambia a ADMIN |
| 7 | UI: Quitar admin a un usuario | Rol cambia a USER |

---

## TC-17: CRUD de Características (#17)

| Campo | Detalle |
|-------|---------|
| **User Story** | #17 — Administrar características |
| **Precondición** | Backend corriendo, token admin |
| **Tipo** | API + UI Manual |

| # | Paso | Resultado Esperado |
|---|------|-------------------|
| 1 | POST `/api/features` con token admin | HTTP 201 |
| 2 | GET `/api/features` | HTTP 200, array con ícono |
| 3 | PUT `/api/features/{id}` | HTTP 200 |
| 4 | DELETE `/api/features/{id}` | HTTP 204 |
| 5 | POST sin token | HTTP 403 |
| 6 | GET sin token | HTTP 200 (público) |
| 7 | UI: Admin → Características → Crear | Modal funciona |
| 8 | UI: Modal lodging → checkboxes | Características aparecen como checkboxes |

---

## TC-18: Visualizar Características (#18)

| Campo | Detalle |
|-------|---------|
| **User Story** | #18 — Visualizar características |
| **Precondición** | Lodging con características asignadas |
| **Tipo** | UI Manual |

| # | Paso | Resultado Esperado |
|---|------|-------------------|
| 1 | GET `/api/lodgings/{id}` | JSON incluye `features` con `id`, `name`, `icon` |
| 2 | UI: Detalle de alojamiento con features | Bloque "Qué ofrece este lugar?" visible |
| 3 | Verificar íconos y nombres | Cada feature muestra su ícono + nombre |
| 4 | Responsive: reducir ventana | Las features se reordenan en grilla |

---

## TC-19: Email de Confirmación (#19)

| Campo | Detalle |
|-------|---------|
| **User Story** | #19 — Email de confirmación |
| **Precondición** | Backend con Mailtrap configurado |
| **Tipo** | Automatizado + Verificación externa |

| # | Paso | Resultado Esperado |
|---|------|-------------------|
| 1 | Registrar nuevo usuario | Log "Email de bienvenida enviado a ..." |
| 2 | Verificar Mailtrap inbox | Email recibido con nombre, email y link de login |
| 3 | Mail con SMTP fallido | Log de error, registro no se ve afectado |

---

## TC-20: Sección de Categorías en Home (#20)

| Campo | Detalle |
|-------|---------|
| **User Story** | #20 — Crear sección de categorías |
| **Precondición** | Al menos 1 categoría en BD |
| **Tipo** | UI Manual + API |

| # | Paso | Resultado Esperado |
|---|------|-------------------|
| 1 | GET `/api/lodgings?category={id}` | HTTP 200, solo alojamientos de esa categoría |
| 2 | GET `/api/lodgings?category={id}` sin alojamientos | HTTP 200, array vacío |
| 3 | UI: Cargar Home | Categorías visibles como tags |
| 4 | UI: Clic en categoría | Alojamientos se filtran, título cambia, botón "Mostrar todos" aparece |
| 5 | UI: Clic en "Mostrar todos" | Vuelven las recomendaciones aleatorias |

---

## Resumen de Ejecución

| TC | User Story | Tipo | Estado |
|----|-----------|------|--------|
| TC-12 | #12/#21 — CRUD Categorías | API + UI + Automatizado | ✅ Pasa (9 tests unitarios) |
| TC-13 | #13 — Registro | API + UI + Automatizado | ✅ Pasa (5 tests) |
| TC-14 | #14 — Login JWT | API + UI + Automatizado | ✅ Pasa |
| TC-15 | #15 — Cerrar sesión | UI Manual | ✅ Verificado |
| TC-16 | #16 — Admin usuarios | API + UI | ✅ Verificado |
| TC-17 | #17 — CRUD Características | API + UI | ✅ Verificado |
| TC-18 | #18 — Visualizar características | UI Manual | ✅ Verificado |
| TC-19 | #19 — Email confirmación | Automatizado + Mailtrap | ✅ Verificado |
| TC-20 | #20 — Filtrar por categoría | API + UI | ✅ Verificado |

**Total pruebas automatizadas:** 45 tests JUnit (0 fallos)
**Total pruebas manuales:** Verificadas funcionalmente

**Leyenda:** ✅ Pasa | ⚠️ Aprobado con observaciones | ❌ Fallido | 🔲 Pendiente
