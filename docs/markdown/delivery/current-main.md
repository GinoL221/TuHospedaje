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

El recorrido en el stack local ya levantado, con un usuario nuevo, mostró el formulario de registro, la lista de contraseña, la vuelta a Home con el nombre y sin enlace de administración, el cierre de sesión, el login correcto y el rechazo de `/admin` hacia `/unauthorized`. El login con contraseña incorrecta mostró `Invalid credentials.`, no `Credenciales inválidas`. No se recorrió el panel de roles: no hay contraseña de admin disponible y no se leyó ningún secreto.

## Limitaciones conocidas

- Los PDF originales `Sprint 1.pdf` a `Sprint 4.pdf` no están en el repositorio. La autoridad usada por la matriz es el texto que esa matriz dice haber extraído.
- La matriz está clavada en `1572f121e91a29257aa618618873d6b560df863a`, no en el commit de este índice. No promueve ninguna historia a verificada.
- El mail de confirmación y el envío real de WhatsApp no quedaron comprobados en el recorrido local del `2026-10-24`.
- No hay despliegue de producción. La CI exitosa de `a430c73` no reemplaza esa comprobación.
