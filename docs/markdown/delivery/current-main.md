# Entrega académica en main

`main` es la entrega. El commit de referencia de este índice es `a430c73938663df2d7c72eb23b9e730d25c3a48a`. No rearmar las ramas de sprint. Una historia no está aprobada hasta que una corrida quede atada a este commit, o a uno posterior que la nombre.

## Camino corto

1. Partí de `main` en el commit de arriba.
2. Usá los documentos de la tabla. El Markdown es la fuente editable. El PDF es la copia publicada de ese corte.
3. Corré la prueba que el plan del sprint nombra para la historia que vas a corregir.
4. Si el resultado no corresponde a este commit, no lo uses como prueba de la entrega actual.

## Documentos

| Documento | Dónde está | Qué cubre |
| --- | --- | --- |
| Definición del producto | `product.md` | Recorridos de la demostración y qué queda fuera |
| Definición documental | `docs/markdown/project-definition.md` y `docs/entregables/project-definition.pdf` | Decisiones documentales |
| Plan y reporte del Sprint 1 | `docs/markdown/sprint-1/` y `docs/entregables/sprint-1-*.pdf` | Historias y pruebas históricas del sprint |
| Plan y reporte del Sprint 2 | `docs/markdown/sprint-2/` y `docs/entregables/sprint-2-*.pdf` | Historias y pruebas históricas del sprint |
| Plan y reporte del Sprint 3 | `docs/markdown/sprint-3/` y `docs/entregables/sprint-3-*.pdf` | Historias y pruebas históricas del sprint |
| Plan, reporte y guía de demo del Sprint 4 | `docs/markdown/sprint-4/` y `docs/entregables/sprint-4-*.pdf` | Historias, pruebas históricas y recorrido de demostración |
| Matriz de trazabilidad | `docs/markdown/compliance/close-user-story-gaps-traceability.md` | Cruce de historia, prueba y límite de evidencia |

La matriz pide, para cada historia, componente de UI, endpoint y criterio de aceptación. Los reportes de sprint ya tienen esas tablas para su corte histórico. Este índice no las reemplaza.

## Cómo reproducir una corrección

- Backend: `cd backend && ./mvnw -B verify`
- Frontend: `cd frontend && npm ci && npm run lint && npm run format:check && npm run coverage`
- E2E público, sin escribir en la base local preservada: `node e2e/scripts/public-runner.cjs list`, luego `smoke` o `public` según `README.md`

Una prueba que pasa en otro commit no corrige la entrega actual. La corrección entra en `main` con el test que falla antes del cambio y pasa después.

## Evidencia de registro, login y roles

Estas corridas no aprueban US #13, #14 ni #16. Cubren las pruebas automáticas de pantalla con autenticación simulada y las pruebas de API contra MariaDB desechable.

| Corrida | Comando | Resultado |
| --- | --- | --- |
| Pantalla | `npx vitest run src/pages/RegisterPage.test.jsx src/pages/LoginPage.test.jsx src/components/Header/Header.test.jsx src/pages/Admin/AdminUsers.test.jsx` | 4 archivos, 40 pruebas, 0 fallas |
| API | `cd backend && ./mvnw -B -Dtest=AuthControllerIntegrationTest,UserControllerIntegrationTest test` | 28 + 11 pruebas, 0 fallas, `BUILD SUCCESS` |

La API usó `mariadb:10.11` en `localhost:32769/test` y `localhost:32770/test`. No usó la base local preservada.

El recorrido en el stack local ya levantado, con un usuario nuevo, mostró el formulario de registro, la lista de contraseña, la vuelta a Home con el nombre y sin enlace de administración, el cierre de sesión, el login correcto y el rechazo de `/admin` hacia `/unauthorized`. El login con contraseña incorrecta mostró primero `Invalid credentials.`. Después de que el cliente pidiera `Accept-Language: es`, el mismo recorrido mostró `Credenciales inválidas.`.

El panel de Usuarios se abrió después en `http://127.0.0.1:5173/administración`, con la sesión admin ya levantada. La tabla mostró ID, nombre, email, rol y acciones. La fila de `admin@tuhospedaje.com` tenía rol `ADMIN` y el botón «Quitar admin». Las otras tres filas tenían rol `USER` y el botón «Hacer admin». En ese momento no se cambió ningún rol. El usuario confirmó que el puntero se bloquea sobre «Quitar admin» de su propia fila, y la captura muestra el cursor prohibido sobre ese botón. La captura no muestra el texto «No podés cambiarte tu propio rol».

Después, solo `walkthrough-auth-1792878045008@example.com` pasó a `ADMIN`. La tabla mostró la etiqueta `ADMIN` y el botón «Quitar admin». Esa fila volvió a `USER`, y el usuario confirmó que el botón muestra otra vez «Hacer admin». No se inició sesión con esa cuenta promovida, así que el acceso de admin de ese usuario no quedó comprobado.

## Evidencia de exploración y búsqueda

Estas corridas no aprueban US #2, #4 ni #22. No se recorrió el navegador y no se escribió en la base local.

| Corrida | Comando | Resultado |
| --- | --- | --- |
| Pantalla | `cd frontend && npx vitest run src/pages/Home/Home.test.jsx src/hooks/useHomeSearchResults.test.js src/hooks/useCityAutocomplete.test.js src/hooks/useHomeRecommendations.test.js` | 4 archivos, 63 pruebas, 0 fallas |
| API | `cd backend && ./mvnw -B -Dtest=LodgingControllerIntegrationTest,RecommendationServiceContractTest,RecommendationControllerContractTest test` | 43 + 4 + 2 pruebas, 0 fallas, `BUILD SUCCESS` |

La API de alojamientos usó `mariadb:10.11` en `localhost:32772/test`. No usó la base local preservada. `RecommendationServiceContractTest` usa mocks, no Testcontainers.

Estas corridas no establecen el color ni el alto mínimo del Home, la grilla de 4 columnas, ni que ciudad y país coincidan con la base. Tampoco establecen el máximo de 10 y el orden aleatorio repetido de `GET /api/lodgings/random`, ni todos los filtros de TC-22.

## Limitaciones conocidas

- Los PDF originales `Sprint 1.pdf` a `Sprint 4.pdf` no están en el repositorio. La autoridad usada por la matriz es el texto que esa matriz dice haber extraído.
- La matriz apunta al `main` publicado `a430c73938663df2d7c72eb23b9e730d25c3a48a`. Este índice está en `docs/main-academic-delivery` y no promueve ninguna historia a verificada.
- El mail de confirmación y el envío real de WhatsApp no quedaron comprobados en el recorrido local del `2026-10-24`.
- No hay despliegue de producción. La CI exitosa de `a430c73` no reemplaza esa comprobación.
