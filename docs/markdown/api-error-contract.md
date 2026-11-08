# Contrato de errores de la API

Toda respuesta de error de la API es JSON con esta forma:

```json
{ "error": "Mensaje legible", "status": 403 }
```

| Campo | Tipo | Cuándo aparece |
| --- | --- | --- |
| `error` | string | Siempre. Mensaje en el idioma de la petición. |
| `status` | número | Siempre. Repite el código HTTP de la respuesta. |
| `code` | string | Solo en dos casos: `duplicate_email` (registro con email ya usado) y `last_enabled_admin` (intento de dejar el sistema sin administrador habilitado). |
| `fields` | objeto | Solo en errores de validación 400 de un cuerpo de petición: campo → mensaje. |

## Idioma

El mensaje sale de `messages_es.properties` o `messages.properties` según el header `Accept-Language`. Con `es` responde en español; con cualquier otro idioma responde en inglés (el paquete por defecto).

## Quién produce cada respuesta

- `GlobalExceptionHandler` responde los errores que ocurren dentro de un controlador o de la validación: 400, 404, 405, 409, 413, 415, 429 y 500, y el 401 de credenciales inválidas en el login.
- `SecurityErrorHandler` responde los 401 y 403 que decide la cadena de filtros de seguridad, antes de que corra cualquier controlador. Usa el mismo formato.

## 401 y 403 de seguridad

El código de estado depende de la ruta y no cambió al agregar el cuerpo.

| Situación | Estado |
| --- | --- |
| Petición sin autenticar a `/api/reservations/**`, `/api/auth/me`, `/api/auth/welcome-email/resend` o `/api/auth/csrf` | 401 |
| Petición sin autenticar a cualquier otra ruta protegida | 403 |
| Usuario autenticado sin el rol requerido (`@PreAuthorize`) | 403 |
| Petición que modifica datos sin token CSRF válido | 403 |

## Cómo lo consume el frontend

`frontend/src/services/api.js`:

- Ante un 401 en una ruta que no es de arranque de sesión, renueva la sesión una vez y reintenta. No lee el cuerpo.
- Ante cualquier otra respuesta no exitosa, arma el mensaje con `fields` (si hay), luego `error` y, si falta, con `Error <status>`. Expone además `code` y `status` en el error lanzado.

## Dónde se documenta en OpenAPI

Las respuestas 401 y 403 de los controladores referencian el esquema `ApiError` (`dto/common/ApiError.java`). Esa clase solo documenta la forma; las respuestas no se construyen con ella.
