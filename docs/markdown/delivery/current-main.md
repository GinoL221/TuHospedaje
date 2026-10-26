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

Estas corridas no aprueban US #2, #4 ni #22. Esas dos corridas no recorrieron el navegador y no escribieron en la base local.

| Corrida | Comando | Resultado |
| --- | --- | --- |
| Pantalla | `cd frontend && npx vitest run src/pages/Home/Home.test.jsx src/hooks/useHomeSearchResults.test.js src/hooks/useCityAutocomplete.test.js src/hooks/useHomeRecommendations.test.js` | 4 archivos, 63 pruebas, 0 fallas |
| API | `cd backend && ./mvnw -B -Dtest=LodgingControllerIntegrationTest,RecommendationServiceContractTest,RecommendationControllerContractTest test` | 43 + 4 + 2 pruebas, 0 fallas, `BUILD SUCCESS` |

La API de alojamientos usó `mariadb:10.11` en `localhost:32772/test`. No usó la base local preservada. `RecommendationServiceContractTest` usa mocks, no Testcontainers.

Estas corridas no establecen el color ni el alto mínimo del Home, la grilla de 4 columnas, ni que ciudad y país coincidan con la base. Tampoco establecen el máximo de 10 y el orden aleatorio repetido de `GET /api/lodgings/random`, ni todos los filtros de TC-22.

Un recorrido posterior de la Home ya levantada, en `http://127.0.0.1:5173/` a 1440×900, mostró los títulos «Buscar hospedaje», «Categorías» y «Recomendaciones», el campo Ciudad, Check-in, Check-out y el botón Buscar. Las categorías visibles fueron Cabañas, Departamentos, Glamping, Hostels, Hoteles y Resorts. Había 8 recomendaciones, no 10. Las cards mostraron nombre, ciudad y país. La grilla medida fue de 4 columnas de 320 px. El fondo del buscador fue `rgb(38, 70, 83)`, que es `--secondary`, no `--primary`. El fondo del main fue `rgb(244, 244, 249)`. El `min-height` calculado fue `auto`, no `100vh`; el alto del contenido superó la ventana. Buscar «Buenos» dejó la URL `/?city=Buenos`, agregó «Resultados de búsqueda» y mantuvo categorías y recomendaciones. No se creó ninguna reserva y no se comparó cada card contra una fila de la base. No es un despliegue nuevo del `main` publicado.

## Evidencia de paginación y filtros

Estas observaciones no aprueban US #8 ni #20. No se creó ninguna reserva y no se recorrió la tabla admin.

| Corrida | Comando | Resultado |
| --- | --- | --- |
| Chips | `cd frontend && npx vitest run src/pages/Home/CategoryCard.test.jsx` | 1 archivo, 11 pruebas, 0 fallas |

En la Home ya levantada, `http://127.0.0.1:5173/` mostró «Página 1 de 5». Inicio y Anterior estaban deshabilitados. Siguiente y Última no. Siguiente pasó el texto a «Página 2 de 5» y habilitó Inicio y Anterior. La URL no cambió y no se comprobó que las cards fueran distintas. Seguían viéndose 8 recomendaciones.

Clic en Hoteles dejó `/?categories=1`, mostró «Resultados de búsqueda» con 9 cards y el botón «Mostrar todos». «Mostrar todos» volvió a `/` y quitó esos resultados. El título «Recomendaciones» seguía en el documento durante el filtro; no se comprobó que la sección estuviera oculta. No se midieron conteos de coincidencias, selección múltiple ni otros anchos.

## Evidencia de reservas

Estas corridas automatizadas, por sí solas, no verifican US #23, #30, #31 ni #32. No incluyeron un recorrido del navegador ni usaron la base local; tampoco comprobaron el envío de mail.

| Corrida | Comando | Resultado |
| --- | --- | --- |
| Reserva en el frontend | `cd frontend && npx vitest run src/pages/Booking/BookingPage.test.jsx src/pages/Booking/BookingConfirmation.test.jsx src/hooks/useAvailability.test.js src/components/RequireAuth.test.jsx` | 4 archivos, 55 pruebas, 0 fallas |
| Reserva en la API | `cd backend && ./mvnw -B -Dtest=ReservationControllerIntegrationTest,ReservationConcurrencyTest test` | `ReservationConcurrencyTest` 3 y `ReservationControllerIntegrationTest` 33. Total 36, 0 fallas. BUILD SUCCESS |

Maven usó `jdbc:mariadb://localhost:32774/test` y `jdbc:mariadb://localhost:32775/test`, MariaDB 10.11. Testcontainers no tocó la base preservada. El texto de mail en la confirmación no prueba que el mensaje haya llegado.

### Recorrido local de BookingPage (sin confirmar)

En Playwright se hicieron dos recorridos autenticados locales a 1280×900. En el primero, el calendario de Check-in llegó a diciembre de 2026: los días 15, 16, 20 y 21 aparecieron deshabilitados; 17 y 22, habilitados. No se seleccionaron fechas; teléfono, check-in y check-out quedaron vacíos y «Confirmar reserva» siguió deshabilitado. En el segundo se eligió 17/12/2026–19/12/2026 y la página mostró `2 noches — $300`; las tres respuestas observadas de disponibilidad devolvieron HTTP 200. El teléfono siguió vacío y «Confirmar reserva» quedó habilitado, pero no se pulsó. Ningún recorrido envió un POST a `/api/reservations`; el segundo tampoco intentó otra escritura de reserva y no se creó una reserva. El login puede persistir una sesión de refresh local. Es evidencia del stack local en la rama documental, no de un despliegue limpio del `main` fijado. Estos recorridos de BookingPage no cubren el calendario de dos meses de ProductDetail, el envío de una reserva, la validación del teléfono, errores/reintentos ni la entrega de mail.

### Recorrido local autenticado adicional (pre-envío, 2026-10-25)

En una tercera corrida Playwright del stack local, `/booking/1` llevó al formulario de login; el único POST de login devolvió HTTP 200 y luego se alcanzó BookingPage. Los campos de nombre, apellido y email se reportaron completos, sin registrar sus valores. El resumen del alojamiento reportó título, ubicación, precio y descripción; había cinco elementos de imagen en el DOM. Se seleccionó 17/12/2026–20/12/2026: Check-in 17 y Check-out 20 estaban habilitados, el 21 deshabilitado y el total mostró `3 noches — $450`. Las tres consultas GET de disponibilidad devolvieron HTTP 200. «Confirmar reserva» quedó habilitado, pero no se pulsó: `confirmClicked=false`, `reservationWritesBlocked=0` y `otherMutationsBlocked=[]`. El navegador bloqueó 338 solicitudes externas, así que no se verificó la carga de imágenes; tampoco se comprobaron features, validación del teléfono, persistencia de una reserva ni entrega de mail. El JSON no registró el SHA del frontend o backend, por lo que esta evidencia local no se atribuye a `main` ni a una rama específica. El login puede dejar una sesión refresh local. US #30 y #31 siguen sin verificarse.

### Recorrido local desde ProductDetail hasta BookingPage (sin confirmar, 2026-10-25)

En una corrida autenticada adicional del stack local, `/lodgings/1` cargó con HTTP 200 y mostró el enlace de login. El único POST de login devolvió HTTP 200; la navegación volvió a la ficha. Se seleccionó 17/12/2026–20/12/2026: el 21/12 quedó deshabilitado y ProductDetail mostró `Total estimado: $450 (3 noches)`. Un clic en «Reservar» abrió `/booking/1` y los campos de BookingPage conservaron ambas fechas. Se observaron seis GET de alojamiento y siete GET de disponibilidad, todos con HTTP 200; no hubo errores del proxy.

`reservationWritesBlocked=0`, `reservationWritesForwarded=0`, `otherMutationsBlocked=0` y `confirmClicked=false`: no se envió ni confirmó una reserva. Se bloquearon 332 solicitudes externas, así que no se verificó la carga de imágenes. El frontend/backend SHA no quedó registrado; esta evidencia local no se atribuye a `main` ni a un despliegue. El login puede dejar una sesión refresh local. US #30–31 siguen sin verificarse.

### Búsqueda local por fechas desde Home (sin login ni reserva, 2026-10-25)

En una corrida Playwright del stack local a 1280×900, Home cargó con HTTP 200. Sin login, se seleccionó 17/12/2026–20/12/2026 y la búsqueda navegó a `/?checkIn=2026-12-17&checkOut=2026-12-20`. El único GET de búsqueda devolvió HTTP 200 con esos parámetros. La página mostró «38 resultados de 38 alojamientos» y nueve cards en el DOM; no mostró error. Una inspección estática posterior encontró que el endpoint de búsqueda usa `size=9` por defecto mientras Home muestra el total filtrado; eso explica la observación 38/9. El commit local `8f4b6fd` agrega paginación. La corrida original no probó esos controles. La paginación real se confirmó después con la API local en BE-US30-06; BE-US30-07 recorrió las cinco páginas y contrastó los 38 IDs con el endpoint de disponibilidad para ese intervalo.

`loginPostsForwarded=0`, `credentialsRead=false`, `mutationsBlocked=0`, `reservationWritesBlocked=0` y `apiProxyErrors=0`. Se bloquearon cinco solicitudes externas. El frontend/backend SHA no quedó registrado; esta observación local no se atribuye a `main` ni a un despliegue. US #30 sigue sin verificarse.

### Paginación local de resultados de búsqueda (commit local `8f4b6fd`, 2026-10-25)

Se agregaron controles de paginación a los resultados de búsqueda de Home. El Playwright con API GET simulada (BE-US30-05) recorrió 0→1→0, conservó ciudad/fechas y confirmó que cambiar la categoría reinicia la página. Luego, un Playwright GET-only contra la API local real (BE-US30-06) consultó 17–20/12/2026 sin ciudad: recibió HTTP 200, 38 resultados en 5 páginas de 9, y recorrió 0→1→0 con las fechas preservadas. La consulta previa con Bariloche y las mismas fechas devolvió 5 resultados/1 página, insuficientes para navegar. Esa corrida no recorrió las cinco páginas ni verificó disponibilidad individual.

En la corrida posterior BE-US30-07 (job-139, exit 0), Playwright recorrió las páginas 0→1→2→3→4 con las mismas fechas y sin ciudad. Las respuestas HTTP 200 entregaron 9/9/9/9/2 entradas: 38 IDs únicos (1–38), sin duplicados y coincidentes con el total informado. Las fechas y los indicadores de página se conservaron; «Siguiente» quedó deshabilitado al final. Los 38 GETs de disponibilidad para ese intervalo devolvieron HTTP 200 y `available=true`. Se comprobaron IDs del payload, no la identidad de cada card en el DOM ni un caso de exclusión de alojamientos ocupados. El HEAD local era `8f4b6fd`, Home estaba sin cambios y el módulo servido tenía el marcador de paginación; no se establecieron identidad exacta de los bytes servidos ni SHA del backend. Se bloquearon dos solicitudes externas; no hubo login, lectura de secretos, métodos distintos de GET ni reservas. Es evidencia local para ese intervalo, sin atribución a `main` o producción.

La comprobación posterior BE-US30-08 (job-140, exit 0) usó solo la API local, sin navegador, para 17–21/12/2026. El alojamiento 1 devolvió HTTP 200 y `available=false`; las cinco páginas de búsqueda devolvieron HTTP 200 y 9/9/9/9/1 entradas: 37 IDs únicos (2–38), sin duplicados, coincidentes con `totalItems=37` y sin el ID 1. Se comprobó la exclusión de ese alojamiento ocupado para ese intervalo; no se consultó la disponibilidad individual de los otros 37 ni se verificó la UI. Solo hubo GETs, sin login, lectura de secretos ni reservas. El SHA del backend no quedó registrado; esta evidencia no se atribuye a `main` ni a producción.

La suite frontend pasó 66 archivos/697 pruebas y el test enfocado de Home pasó 38/38 antes del commit; el hook de commit ejecutó Prettier después, sin repetir las pruebas. La revisión independiente no encontró problemas. El commit está solo en `docs/main-academic-delivery`: no fue publicado ni probado en `main`; US #30 sigue sin verificarse.

### Corrección del límite de check-out incorporada localmente (`1244af6`)

Las consultas GET de solo lectura a `/api/lodgings/1/availability` devolvieron HTTP 200 y `available: true` para 17–20/12/2026, y `available: false` para 17–21/12/2026, con una reserva ocupando 20–22/12. Antes del cambio, BookingPage deshabilitaba el 20 como check-out. Una prueba de regresión reprodujo el fallo; el filtro ahora compara el solapamiento del rango usando días locales y permite ese límite exclusivo, mientras sigue bloqueando el solapamiento. `cd frontend && npx vitest run src/pages/Booking/BookingPage.test.jsx -t "allows checkout on an occupied start date when the stay ends at that boundary"` pasó; el archivo completo pasó 34 pruebas y `cd frontend && npx vitest run` pasó 66 archivos y 695 pruebas. Esos resultados corresponden a la primera corrida de la corrección. No hubo envío de reserva ni escritura de reserva en la base local.

La versión final quedó guardada en `1244af68ba2d1aa2718a9071f8a7e5b8ed11622b` (`fix: allow exclusive checkout boundaries in Booking`), creado en `fix/booking-checkout-exclusive` sobre `8f4b6fd`. Incluye solo `BookingPage.jsx`, `BookingDateFields.jsx` y `BookingPage.test.jsx`. En `.worktrees/booking-checkout-exclusive/frontend`, `./node_modules/.bin/vitest run src/pages/Booking/BookingPage.test.jsx` pasó 35/35 y `./node_modules/.bin/vitest run` pasó 66 archivos/697 pruebas (job-141, exit 0). El verificador independiente pasó 35/35 y no encontró bloqueos; formato y `git diff --check` pasaron. La evaluación nativa del candidato aislado fue `medium`, `reviewDue=false`; no se inició una revisión nativa adicional. Los hooks del commit pasaron y los tres archivos finales coincidieron byte por byte con los probados. No se repitieron build, lint ni navegador en esta preparación (BE-US30-09).

Con autorización humana, `docs/main-academic-delivery` avanzó por fast-forward de `8f4b6fd` a `1244af6`. El primer intento fue rechazado por las copias locales pendientes; después de comprobar que coincidían con el commit, se autorizó restaurar solo esos tres archivos y repetir el fast-forward. Booking quedó sin cambios pendientes y los cuatro documentos conservaron sus bytes durante la incorporación. No se creó un merge commit ni se cambió `main`, se hizo push o se comprobó producción. No se repitieron pruebas tras el fast-forward; US #30 sigue sin verificarse en el `main` fijado.

### Cobertura local simulada del retorno de login a Booking (sin credenciales reales)

`frontend/src/pages/LoginPage.test.jsx` incorpora un recorrido automatizado con `AuthProvider`, `RequireAuth` y `LoginPage` reales, y `authService` simulado: una visita anónima a `/booking/1` muestra el mensaje de acceso y el enlace `/register`; un login sintético vuelve a una ruta centinela de Booking. Otra prueba sigue el enlace de registro hasta una ruta centinela y confirma que no se llamó a login ni a register. No se leyeron secretos ni se envió una solicitud real de autenticación.

En job-143, Prettier pasó; `vitest run src/pages/LoginPage.test.jsx src/components/RequireAuth.test.jsx` pasó 2 archivos/13 pruebas, y `vitest run` pasó 66 archivos/699 pruebas. La verificación independiente repitió una vez el comando enfocado y pasó 13/13. `git diff --check` pasó. La cobertura quedó guardada en el commit local `18aacfb07be84d88d7d0bc624015b4d8d379016c` (`test(auth): cover Booking login return and registration link`). Gitleaks y el hook de formato pasaron; el blob del archivo se mantuvo igual al revisado antes del commit. No se repitieron pruebas tras el commit ni se publicó. Este commit solo agrega cobertura; la verificación de comportamiento BE-US30-12 sigue ligada a `1244af6`.

Es cobertura automatizada local con auth simulada, no un recorrido de navegador, backend, `BookingPage` o `RegisterPage` reales. No prueba el comportamiento de `main` ni producción y no cambia el estado: US #30 sigue sin verificarse en `main`.

### Recorrido local de login a Booking con Playwright (APIs simuladas; sin reserva)

En job-144, la primera corrida se detuvo antes de llenar el formulario: el selector global «Crear cuenta» encontró dos enlaces, el del encabezado y el del formulario. No fue un fallo de la aplicación ni se envió login. La corrida corregida job-145 pasó con el selector limitado a `main.login-container`.

A 1280×720, el recorrido abrió `http://127.0.0.1:5173/booking/1`, comprobó el mensaje de autenticación y el enlace `/register`, envió un login sintético interceptado y volvió a cargar la interfaz real de `BookingPage`, donde se mostró «Confirmar reserva» y el alojamiento de fixture. El script standalone simuló todas las respuestas de auth, alojamiento, disponibilidad y reservas; no cargó la configuración E2E que lee dotenv. `loginPosts=1`, `unexpectedApi=0`, `pageErrors=0`, `blockedExternalRequests=129`, `backendRequests=0` y `reservationSubmitted=false`. Ningún POST distinto del login sintético pudo continuar; no se intentó reservar.

La corrida es evidencia local de interfaz con datos/API simulados. El SHA del frontend servido no quedó capturado, no se probaron fechas ni disponibilidad real y el script inline no se conservó para repetirla. No verifica `main` ni producción; US #30 sigue sin verificarse en el `main` fijado.

### US #30 verificada localmente en candidato `1244af6` (BE-US30-12)

Con aprobación del usuario, US #30 queda **verificada localmente en `1244af68ba2d1aa2718a9071f8a7e5b8ed11622b`** y **pendiente en `main@a430c739`**. Se contrastaron los diez criterios con el PDF original `Sprint 4.pdf`, páginas 4–5, recuperado de OneDrive; su ausencia en el repositorio no significaba que la fuente faltara.

Job-147 terminó con exit 0 en Chromium a 1280×900. Vite sirvió el frontend del worktree limpio `.worktrees/booking-checkout-exclusive`, con HEAD comprobado antes y después. La API real fue `127.0.0.1:8080`; su montaje `/workspace` apunta al backend del repositorio y sus fuentes coinciden con el candidato. Esto no es una certificación criptográfica del binario en ejecución.

- El acceso anónimo desde el detalle y el acceso directo a Booking mostraron login y los mensajes de autenticación/registro; el enlace de registro apuntó a `/register`.
- El único login real autorizado devolvió HTTP 200 y retornó a `/booking/1`. Luego «Reservar» desde el detalle autenticado llevó a Booking con 17–20/12/2026 visibles en los campos. Las fechas se ingresaron por teclado en los controles; no se hicieron clics en días habilitados para seleccionarlas.
- El calendario bloqueó check-in20 y checkout21; los clics sobre esos días deshabilitados no cambiaron los campos. Checkout20 siguió habilitado como límite exclusivo. La API rechazó el intervalo 17–21 como no disponible.
- Home buscó desde el formulario y recorrió las cinco páginas de ambos intervalos: 17–20 devolvió 38 IDs únicos (1–38), 9/9/9/9/2; 17–21 devolvió 37 (2–38), 9/9/9/9/1, excluyendo el alojamiento1. Cada card del DOM coincidió con el payload y cada alojamiento devuelto tuvo disponibilidad real `true`; se conservaron fechas y «Siguiente» terminó deshabilitado.

Job-146 había fallado con login403: el proxy reenviaba Origin5187, rechazado por CORS. Tras diagnóstico GET-only y autorización nueva, se normalizó solo ese origen del proxy a Origin5173. No se cambió la seguridad del backend. Job-147 usó un único login, no intentó escrituras de reserva ni pulsó confirmar; el login puede dejar una sesión refresh. Las 1187 solicitudes externas bloqueadas dejan las imágenes fuera de esta verificación; no hubo errores de página. No se afirman producción, envío de mail ni otros navegadores/anchos.

El runner `e2e/.run-local-persistent/us30-1244af6.cjs` y el resultado `us30-1244af6-result.json` se conservan localmente, ignorados por Git; hashes y correspondencia por criterio están en el índice BE-US30-12. No son artefactos publicados ni backups independientes. Repetir la corrida requiere permiso nuevo para leer las credenciales en memoria y hacer login. No se modificó `main`, no hubo commit, push ni despliegue.

### Recorrido público de ProductDetail (sin reservar)

Sin login, Playwright a 1280×900 mostró dos meses en `/lodgings/1` (`October 2026` y `November 2026`; al avanzar, `November 2026` y `December 2026`). Check-in 17/12/2026 apareció habilitado; tras elegirlo, check-out 20/12 quedó habilitado y 21/12 deshabilitado. Se seleccionó 17–20 y la página mostró `Total estimado: $450 (3 noches)`. El GET de disponibilidad para ese rango devolvió HTTP 200 y `available: true`; las tres respuestas de disponibilidad observadas devolvieron 200. No se hizo login, no se pulsó «Reservar» y no se envió POST a `/api/reservations`; no se creó una reserva. Es evidencia del stack local, no de un despliegue limpio del `main` fijado. La transición al envío sigue sin probarse.

### Error y reintento de disponibilidad (503 simulado)

En otra visita pública, se respondieron HTTP 503 a los primeros dos GET de disponibilidad. ProductDetail mostró `No pudimos obtener la disponibilidad de este alojamiento.` y «Reintentar», con las fechas deshabilitadas. Tras un único clic en «Reintentar», el GET real al backend devolvió HTTP 200; la alerta desapareció y los campos de fecha quedaron habilitados. Es una simulación controlada del error, no evidencia de una caída real del servicio. No hubo login ni solicitud de reserva.

### Verificación local del SHA fijado de `main` (sin reservar)

En el worktree aislado `/home/ginopc/Desarrollo/hoteleria-worktrees/us23-main-verification-20261025`, fijado en `a430c73938663df2d7c72eb23b9e730d25c3a48a`, `cd /home/ginopc/Desarrollo/hoteleria-worktrees/us23-main-verification-20261025/frontend && ./node_modules/.bin/vitest run src/pages/ProductDetail/ProductDetail.test.jsx` pasó 63 pruebas. El recorrido Playwright usó el frontend de ese SHA y un proxy de solo GET hacia el backend local existente; su árbol de fuentes coincide con el SHA fijado. Con check-in 17/12, el 20 quedó habilitado como check-out exclusivo y el 21 deshabilitado; el rango 17–20 dio HTTP 200, `available: true` y `$450 (3 noches)`. Como check-in, el 20 ocupado quedó deshabilitado y el 17 disponible.

La inspección visual final a 1280×900 confirmó dos meses visibles. Con diciembre abierto, el 20/12 ocupado tenía `aria-disabled=true`, texto gris, tachado y cursor por defecto; el 17/12 disponible tenía `aria-disabled=false`, texto negro, sin tachado y cursor de selección. La prueba de error interceptó la carga inicial con HTTP 503: mostró `No pudimos obtener la disponibilidad de este alojamiento.` y deshabilitó los campos. Un clic en «Reintentar» obtuvo HTTP 200 del backend, quitó la alerta y habilitó los campos. La falla fue inyectada; no fue una caída real. No hubo login, solicitudes API de escritura, POST a `/api/reservations` ni clic en «Reservar». Con autorización del usuario, US #23 se marca **Verified** en el SHA fijado. Esto no afirma un despliegue de producción.

## Limitaciones conocidas

- Los PDF originales `Sprint 1.pdf` a `Sprint 4.pdf` no están en el repositorio. La autoridad usada por la matriz es el texto que esa matriz dice haber extraído.
- La matriz apunta al `main` publicado `a430c73938663df2d7c72eb23b9e730d25c3a48a`. Este índice está en `docs/main-academic-delivery`; la verificación registrada para US #23 es local y no afirma un despliegue de producción.
- El mail de confirmación y el envío real de WhatsApp no quedaron comprobados en el recorrido local del `2026-10-24`.
- No hay despliegue de producción. La CI exitosa de `a430c73` no reemplaza esa comprobación.
