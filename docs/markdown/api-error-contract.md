# Contrato de errores de la API

Las respuestas de error que produce la aplicación son JSON con esta forma (no se verificaron los errores que genera el contenedor fuera de la aplicación, ni el cuerpo del estado de salud de actuator):

```json
{ "error": "Mensaje legible", "status": 403 }
```

| Campo | Tipo | Cuándo aparece |
| --- | --- | --- |
| `error` | string | Siempre. Mensaje en el idioma de la petición. |
| `status` | número | Siempre. Repite el código HTTP de la respuesta. |
| `code` | string | Solo en dos casos: `duplicate_email` (registro con email ya usado, respuesta 409) y `last_enabled_admin` (intento de dejar el sistema sin administrador habilitado, respuesta 409). |
| `fields` | objeto | Solo en errores de validación 400 de un cuerpo de petición: campo → mensaje. |

## Idioma

El mensaje sale de `messages_es.properties` o `messages.properties` según el header `Accept-Language`. Con `es` responde en español; con cualquier otro idioma responde en inglés (el paquete por defecto). Sin el header, el idioma lo decide la configuración regional del servidor.

## Quién produce cada respuesta

- `GlobalExceptionHandler` responde los errores que ocurren dentro de un controlador o de la validación: 400, 404, 405, 409, 413, 415, 429 y 500, y el 401 de credenciales inválidas en el login.
- `SecurityErrorHandler` responde los 401 y 403 que decide la cadena de filtros de seguridad, antes de que corra cualquier controlador. Usa el mismo formato.
- Tres productores propios usan también `{error, status}`: `AuthController.handleRefreshRejected` (401 al rechazar el refresco de sesión), `AuthRateLimitFilter` y `RefreshRateLimitFilter` (429 por límite de peticiones).

## 409 por duplicados

Crear o renombrar una categoría, característica o política con un nombre que ya existe responde 409 Conflict, no 400. El mensaje sale de `messages_es.properties` o `messages.properties` según `Accept-Language` e incluye el nombre rechazado. El registro con un email ya usado también responde 409, con `code: "duplicate_email"` en el cuerpo.

Dos reglas de negocio siguen en 400 y solo cambiaron a mensajes localizados: eliminar una categoría que tiene alojamientos y puntuar un alojamiento sin una estadía finalizada.

## 401 y 403 de seguridad

El código de estado depende de la ruta y no cambió al agregar el cuerpo.

| Situación | Estado |
| --- | --- |
| Petición sin autenticar a `/api/reservations/**`, `/api/auth/me`, `/api/auth/welcome-email/resend` o `/api/auth/csrf`, con token CSRF válido si el método modifica datos | 401 |
| Petición sin autenticar a cualquier otra ruta protegida | 403 |
| Usuario autenticado sin el rol requerido (`@PreAuthorize`) | 403 |
| Petición que modifica datos (POST, PUT, PATCH, DELETE) sin token CSRF válido, incluso a una ruta de 401 | 403, con un mensaje propio (`Tu token de seguridad falta o venció…`) |

La comprobación de CSRF corre antes que la autorización. Por eso un POST anónimo a `/api/reservations` sin token responde 403 y, con token válido, 401.

En las rutas protegidas por método (`@PreAuthorize`), la autorización de rol se declara únicamente en el método del controlador (sin duplicar reglas de rol por URL en `SecurityConfig`). Como Spring MVC resuelve y valida los argumentos del controlador (`@Valid @RequestBody`) antes de ejecutar el interceptor de `@PreAuthorize`, un usuario autenticado sin el rol requerido que envíe un cuerpo inválido recibe 400 en lugar de 403.

## Cómo lo consume el frontend

`frontend/src/services/api.js`:

- Ante un 401 en una ruta que no es de arranque de sesión, renueva la sesión una vez y reintenta. No lee el cuerpo.
- Ante cualquier otra respuesta no exitosa, arma el mensaje con `fields` (si hay), luego `error` y, si falta, con `Error <status>`. Expone además `code` y `status` en el error lanzado.

## Dónde se documenta en OpenAPI

Las respuestas 401 y 403 de los controladores referencian el esquema `ApiError` (`dto/common/ApiError.java`). Esa clase solo documenta la forma; las respuestas no se construyen con ella.
