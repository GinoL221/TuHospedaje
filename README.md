# TuHospedaje

TuHospedaje es una plataforma web para descubrir y reservar alojamientos turísticos. Permite explorar por ciudad y fechas, consultar disponibilidad, guardar favoritos, reservar y administrar el catálogo.

Es el proyecto final integrador de Digital House. Los PDF originales de los Sprints 1–4 definen las historias #1–#35 y sus criterios; [`product.md`](product.md) resume ese alcance y la [matriz de trazabilidad](docs/markdown/compliance/close-user-story-gaps-traceability.md) registra fuentes y evidencia. Una descripción funcional no certifica implementación ni aceptación.

Pasarelas de pago, chat interno y mapas en vivo quedan fuera de esta entrega. **Solo #19 (correo de registro) es explícitamente opcional/no evaluada.** Compartir, puntuaciones, WhatsApp y correo de reserva (#27, #28, #34 y #35) siguen en el alcance oficial aunque su prioridad sea baja. La exclusión de chat interno no excluye WhatsApp. Cancelación es una mejora posterior, no un criterio original ni un reemplazo del historial #33.

La web del huésped debe adaptarse a dispositivos según los criterios oficiales; no se exige una app móvil nativa. **Admin es la excepción de #9:** no debe ser responsive y debe mostrar un mensaje de no disponibilidad en móvil. Los breakpoints concretos son decisiones técnicas. Las diferencias documentadas de calendario doble y ubicación móvil de WhatsApp están [pendientes de reconciliación](DESIGN.md#official-requirements-reconciliation), sin aprobación implícita ni cambios de interfaz en este ajuste.

## Para corregir: inicio rápido

Requisitos para este flujo: **Docker Compose v2** e internet en el primer build (Java y Node solo hacen falta para correr fuera de contenedores).

1. Generá `deploy/dev.env` con secretos aleatorios y los hashes demo:

   ```bash
   ./scripts/dev-setup.sh
   ```

2. Levantá MariaDB, backend con datos demo y frontend:

   ```bash
   docker compose --env-file deploy/dev.env \
     -f compose.yaml -f compose.dev.yaml -f compose.dev-seed.yaml up --build
   ```

   El primer build descarga dependencias de Maven y npm y tarda varios minutos. Sin `compose.dev-assets.yaml`, los alojamientos muestran imágenes placeholder. Los puertos `5173`, `8080` y `3307` tienen que estar libres: si otro stack de TuHospedaje (por ejemplo `compose.local.yaml`) está corriendo, detenelo antes.

3. Abrí `http://localhost:5173` (usá `localhost`, **no** `127.0.0.1`, por la política CORS de desarrollo). El panel de administración muestra por diseño un mensaje de no disponibilidad en móvil (#9). Swagger UI queda en `http://localhost:8080/swagger-ui/index.html`.

   | Rol | Email | Contraseña |
   | --- | --- | --- |
   | `ADMIN` | `admin@tuhospedaje.com` | `Admin1234` |
   | `USER` (huésped) | `huesped@tuhospedaje.com` | `Huesped1234` |

4. Para detener sin borrar datos, usá `down`; para reiniciar la base demo, usá `down -v` con los mismos tres archivos. Hacé lo mismo si una migración de seed falla, o si el backend no arranca con `Access denied for user 'tuhospedaje_dev'`: MariaDB solo aplica la contraseña de `deploy/dev.env` al crear el volumen, así que un volumen de una corrida anterior conserva la contraseña vieja.

   ```bash
   docker compose --env-file deploy/dev.env \
     -f compose.yaml -f compose.dev.yaml -f compose.dev-seed.yaml down -v
   ```

**Dónde mirar:** [índice de documentación](docs/README.md) · [alcance (`product.md`)](product.md) · [matriz de trazabilidad](docs/markdown/compliance/close-user-story-gaps-traceability.md) · [guía de demo académica](docs/markdown/sprint-4/academic-demo-guide.md) · [referencia de la API](docs/markdown/api-reference.md) · [arquitectura del backend](docs/markdown/backend-architecture.md).

## Stack

- **Backend:** Java 17, Spring Boot 4.0, Spring Security/JWT, Spring Data JPA, Flyway y MariaDB 10.11.
- **Frontend:** React 19, Vite 8, React Router y Lucide React.
- **E2E:** Playwright para Chromium, Firefox y mobile Chromium.
- **Almacenamiento de imágenes:** Almacenamiento local en el backend mediante `LocalImageStorageService`, con endpoints públicos seguros en `/uploads/**` e imágenes canónicas de seed en `/canonical-lodging-images/**`. Validación estricta con `ImageContentValidator` (magic bytes, JPEG, PNG, WebP, GIF hasta 5 MB y sin SVG).
- **Correo electrónico:** Outbox transaccional en base de datos (`email_outbox`) con despacho asíncrono vía SMTP (configurable con las variables `MAILTRAP_*`; en producción, el servidor SMTP del operador). Configurar SMTP no prueba la entrega ni la llegada al buzón.

## Requisitos

- Java 17 o superior.
- Node.js `>=22.12.0` (recomendado Node 24 LTS, alineado con el CI y el build contenedorizado).
- Docker Compose v2 para el flujo contenedorizado.
- Python 3 únicamente para la alternativa manual de hashes bcrypt en el seed demo (no hace falta con `scripts/dev-setup.sh`).

## Inicio rápido

### Desarrollo completo con Docker Compose

Este es el flujo recomendado para desarrollo. Levanta MariaDB, backend y frontend con el código montado desde el working tree. El stack usa puertos de loopback: MariaDB `127.0.0.1:3307`, backend `127.0.0.1:8080` y Vite `127.0.0.1:5173`.

1. Copiá la plantilla local y completá sus valores. No commitees `deploy/dev.env`:

   ```bash
   cp deploy/dev.env.example deploy/dev.env
   ```

2. Validá la composición y levantá los servicios:

   ```bash
   docker compose --env-file deploy/dev.env \
     -f compose.yaml -f compose.dev.yaml config --quiet
   docker compose --env-file deploy/dev.env \
     -f compose.yaml -f compose.dev.yaml up --build
   ```

   Este flujo aplica sólo las migraciones de esquema. No carga filas demo.

   El frontend de desarrollo usa `VITE_WHATSAPP_NUMBER=12025550100`, un número ficticio del rango reservado norteamericano 202-555-0100–0199. No es un canal real de contacto y no demuestra entrega de mensajes. Las pruebas verifican el enlace sin abrir WhatsApp ni enviar mensajes. Para ejecución manual, configurá la misma variable en `frontend/.env`. Un despliegue real requiere un número de contacto autorizado; este valor queda limitado a desarrollo.

   Para verificar esta configuración sin levantar servicios ni leer archivos privados, ejecutá `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools/tests -p test_dev_whatsapp_config.py -v` desde la raíz. La prueba resuelve los archivos Compose públicos con valores ficticios y `--env-file /dev/null`; no descarga imágenes ni modifica bases.

3. Para detenerlo sin borrar la base:

   ```bash
   docker compose --env-file deploy/dev.env \
     -f compose.yaml -f compose.dev.yaml down
   ```

   Para eliminar la base descartable de desarrollo, agregá `-v`. Nunca uses `down -v` sobre producción.

### Datos demo opcionales

El seed usa un proyecto y volumen separados (`tuhospedaje-dev-seeded`). Requiere `DEV_ADMIN_PASSWORD_HASH` y `DEV_GUEST_PASSWORD_HASH` en `deploy/dev.env`. Compose no debe interpolar esos valores: un hash bcrypt contiene `$` y Compose lo corta. El entrypoint del seed lee el archivo y los exporta sin pasarlos por la interpolación.

La forma recomendada es generar `deploy/dev.env` con el script del repositorio (usa `htpasswd` del host o la imagen `httpd:2.4-alpine` en Docker y escribe ambos hashes entre comillas simples):

```bash
./scripts/dev-setup.sh
```

Si preferís generar los hashes manualmente con Python (`bcrypt`, costo 10–14) para tus propias contraseñas de desarrollo, agregá en `deploy/dev.env` las líneas `DEV_ADMIN_PASSWORD_HASH='...'` y `DEV_GUEST_PASSWORD_HASH='...'` (las comillas simples son opcionales, pero evitan que Compose intente interpolar `$`).

El entrypoint lee `DEV_ADMIN_PASSWORD_HASH` y `DEV_GUEST_PASSWORD_HASH`, acepta un par opcional de comillas simples envolventes, y valida 60 caracteres, prefijos `$2a$`, `$2b$` o `$2y$`, costo de `04` a `31` y el alfabeto bcrypt (`./A-Za-z0-9`). Un valor ausente o malformado detiene el arranque sin imprimir el hash. Esta validación comprueba el formato; no verifica una contraseña ni calcula hashes. El desarrollo sin el overlay de seed mantiene su entrypoint habitual.

Para verificar el entrypoint sin bases ni archivos privados, usá una imagen **ya instalada** con `/bin/sh`, `awk`, `cut` y `grep`:

```bash
PYTHONDONTWRITEBYTECODE=1 SEED_TEST_IMAGE=ID_DE_IMAGEN_LOCAL \
  python3 -m unittest discover -s backend/docker -p test_seed_entrypoint.py -v
shellcheck backend/docker/seed-entrypoint.sh
```

Las pruebas usan contenedores sin red y sin descargas, raíz de sólo lectura, capacidades eliminadas, fixtures ficticios y un proceso testigo en lugar del backend. Sólo escriben en temporales propios, que se eliminan al terminar.

Después, levantá el overlay:

```bash
docker compose --env-file deploy/dev.env \
  -f compose.yaml -f compose.dev.yaml -f compose.dev-seed.yaml up --build
```

Las cuentas demo cargadas por `scripts/dev-setup.sh` son `admin@tuhospedaje.com` / `Admin1234` (rol `ADMIN`) y `huesped@tuhospedaje.com` / `Huesped1234` (rol `USER`), o las contraseñas cuyos hashes hayas cargado manualmente. Si una migración demo falla a mitad de camino, recreá únicamente la base del proyecto seeded antes de reintentar:

```bash
docker compose --env-file deploy/dev.env \
  -f compose.yaml -f compose.dev.yaml -f compose.dev-seed.yaml down -v
```

### Imágenes canónicas opcionales

Los JPEG maestros no forman parte del repositorio. Para montarlos de forma local, `CANONICAL_ASSETS_HOST_DIR` debe ser una ruta absoluta existente en tu máquina:

```bash
CANONICAL_ASSETS_HOST_DIR=/ruta/absoluta/a/canonical-lodging-images \
  docker compose --env-file deploy/dev.env \
  -f compose.yaml -f compose.dev.yaml -f compose.dev-assets.yaml up --build
```

Podés combinar `compose.dev-seed.yaml` y `compose.dev-assets.yaml` en el mismo comando cuando necesites ambos overlays.

### Entorno local persistente con imágenes aprobadas

Este modo requiere los directorios externos de JPEG canónicos (38 carpetas `masters` bajo `CANONICAL_ASSETS_HOST_DIR`) y falla al iniciar si faltan; para evaluar el proyecto usá el inicio rápido de arriba.

Usá `compose.local.yaml` como archivo independiente (sin los overlays dev/prod).
Crea el proyecto `tuhospedaje-local` y el volumen `tuhospedaje-local-db`, sin usar
las bases existentes. MariaDB no publica puertos; frontend y backend escuchan
solo en loopback, en `5173` y `8080`. Detené cualquier demo que use esos puertos
antes del arranque, sin borrar sus datos.

1. Prepará `deploy/local.env` siguiendo `deploy/local.env.example`, con claves
   aleatorias propias. Conservá esas claves entre arranques. Prepará
   **`deploy/local-admin.env`** a partir de `deploy/local-admin.env.example` con
   los hashes bcrypt (costo 10–14) de `DEV_ADMIN_PASSWORD_HASH` y
   `DEV_GUEST_PASSWORD_HASH`, separado del archivo que Compose interpola. Ambos
   archivos están ignorados por Git:

   ```bash
   chmod 600 deploy/local.env deploy/local-admin.env
   docker compose --env-file deploy/local.env -f compose.local.yaml config --quiet
   docker compose --env-file deploy/local.env -f compose.local.yaml up -d --build
   ```

2. Abrí `http://127.0.0.1:5173`. El primer arranque en el volumen vacío aplica
   las migraciones y el seed aprobado: 38 alojamientos y 190 imágenes. Los
   arranques siguientes conservan los datos y Flyway no repite el seed. El admin
   es `admin@tuhospedaje.com`; usá la contraseña cuyo hash preparaste. Cambiar el
   archivo de hash después del primer arranque **no cambia** la contraseña en DB.

   El backend monta solo los 38 directorios `masters` en lectura, con los nombres
   exactos del seed. No copia imágenes al repositorio ni publica notas, prompts o
   evidencias. La ruta raíz la define `CANONICAL_ASSETS_HOST_DIR`;
   un directorio ausente impide el arranque en vez de crear uno vacío.

3. Para detener y volver a arrancar conservando la base:

   ```bash
   docker compose --env-file deploy/local.env -f compose.local.yaml down
   docker compose --env-file deploy/local.env -f compose.local.yaml up -d
   ```

   **No uses `down -v`, `docker volume rm` ni `docker volume prune`: borran datos.**
   El volumen conserva datos entre reinicios, pero no reemplaza un backup.
   Los contenedores usan `restart: unless-stopped`; si los detenés explícitamente,
   volvé a iniciarlos con `up -d`. Este entorno sigue el checkout local y no
   equivale a desplegar las imágenes publicadas de producción.

Desde otra notebook, conservá el túnel SSH de `5173` y `8080` y abrí la misma URL.
Para verificar la configuración pública sin secretos ni servicios:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover \
  -s tools/tests -p test_local_persistent_config.py -v
```

### Ejecución manual en el host

Si preferís ejecutar backend y frontend fuera de contenedores, levantá sólo la base con los mismos archivos de desarrollo; no uses `docker compose up -d db` sin el overlay de desarrollo:

```bash
docker compose --env-file deploy/dev.env \
  -f compose.yaml -f compose.dev.yaml up -d db
```

Configurá los archivos locales a partir de [`backend/.env.example`](backend/.env.example) y [`frontend/.env.example`](frontend/.env.example). Luego iniciá cada proceso en su propio terminal:

```bash
cd backend
./mvnw spring-boot:run
```

```bash
cd frontend
npm ci
npm run dev
```

El backend queda disponible en `http://localhost:8080` y Vite en `http://localhost:5173`. Para scripts y estructura del paquete frontend, consultá [`frontend/package.json`](frontend/package.json).

## Producción con Docker Compose

El Compose de producción ejecuta imágenes ya construidas y referenciadas por `BACKEND_IMAGE` y `FRONTEND_IMAGE`. `compose.prod.yaml` no contiene instrucciones `build`: publicá las imágenes desde tu pipeline y usá tags inmutables o digests.

El backend empaqueta `target/app.jar`, independiente de la versión del `pom.xml`; el Dockerfile conserva `/app/app.jar` como entrada.

WhatsApp se configura **al construir la imagen**, mediante la variable de repositorio de GitHub Actions `VITE_WHATSAPP_NUMBER` (dígitos internacionales, sin `+`). CI la pasa como argumento de build junto con `VITE_API_URL=/api`. Si está vacía, el control no aparece. Cambiarla exige construir una imagen nueva: agregarla a `deploy/prod.env` o al entorno de nginx no cambia el bundle. Para un build manual:

```bash
docker build -t tuhospedaje-frontend:local \
  --build-arg VITE_API_URL=/api \
  --build-arg VITE_WHATSAPP_NUMBER="${VITE_WHATSAPP_NUMBER:-}" frontend
```

Para enviar los correos encolados, configurá **ambas** opciones `TUHOSPEDAJE_EMAIL_OUTBOX_ENABLED=true` y `MAIL_SMTP_ENABLED=true` en `deploy/prod.env`, además de `MAILTRAP_HOST`, `MAILTRAP_PORT`, `MAILTRAP_USERNAME` y `MAILTRAP_PASSWORD` del transporte SMTP. Por defecto ambas están desactivadas; con cualquiera desactivada los correos quedan `PENDING`, sin envío. Habilitarlas requiere un servidor SMTP válido; la configuración por sí sola no prueba la entrega. El despachador conserva los reintentos y la limpieza existentes.

1. Copiá la plantilla del operador y completá sus valores fuera del repositorio:

   ```bash
   cp deploy/prod.env.example deploy/prod.env
   ```

2. Validá la configuración sin levantar servicios:

   ```bash
   docker compose --env-file deploy/prod.env \
     -f compose.yaml -f compose.prod.yaml config --quiet
   ```

3. Levantá la base, esperá el healthcheck y provisioná las cuentas de runtime y Flyway:

   ```bash
   docker compose --env-file deploy/prod.env \
     -f compose.yaml -f compose.prod.yaml up -d db
   docker compose --env-file deploy/prod.env \
     -f compose.yaml -f compose.prod.yaml ps
   docker compose --env-file deploy/prod.env \
     -f compose.yaml -f compose.prod.yaml --profile provision run --rm db-provision
   ```

4. Levantá backend y frontend con las imágenes configuradas:

   ```bash
   docker compose --env-file deploy/prod.env \
     -f compose.yaml -f compose.prod.yaml up -d backend frontend
   ```

   Sólo el frontend publica el puerto HTTP configurado (por defecto `127.0.0.1:8080`). El backend permanece dentro de las redes Compose. TLS, certificados y el reverse proxy HTTPS quedan fuera de Compose; el proxy externo debe conservar `Host` y enviar `X-Forwarded-Proto: https`.

### Configurado en el repositorio vs. comprobado en producción

Este repositorio documenta y configura estos controles. Que estén escritos aquí no prueba que se ejecutaron en un entorno productivo.

| Control | Configurado en el repositorio | Comprobado en producción |
| --- | --- | --- |
| TLS y reverse proxy | Fuera de Compose; se documenta el contrato (`Host`, `X-Forwarded-Proto: https`) | No comprobado: depende de infraestructura externa |
| Entrega real de correo | Outbox y SMTP configurables con `TUHOSPEDAJE_EMAIL_OUTBOX_ENABLED` y `MAIL_SMTP_ENABLED` | No comprobado: sin SMTP real no hay evidencia de recepción |
| Backups externos | Scripts y runbook en [ops/mariadb](ops/mariadb/README.md) y [ops/uploads](ops/uploads/README.md) | No comprobado: no hay ejecución productiva registrada |
| Imágenes canónicas | Montaje local opcional; los JPEG maestros no forman parte del repositorio | No comprobado: no hay publicación productiva de los JPEG canónicos |
| Imágenes de contenedor | Build, escaneo Trivy y publicación en GHCR desde CI | Comprobado en CI para v1.0.0; el despliegue en un host propio no se registra |

El volumen `tuhospedaje-prod-db` persiste entre recreaciones. Para una detención normal:

```bash
docker compose --env-file deploy/prod.env \
  -f compose.yaml -f compose.prod.yaml down
```

No uses `down -v` en producción. El backup, la restauración y la retención se operan con el [runbook de MariaDB](ops/mariadb/README.md); este README no declara que exista evidencia de una ejecución productiva.

## Desarrollo sin Compose: configuración y migraciones

- El backend usa [`backend/.env.example`](backend/.env.example) como plantilla de configuración local.
- El frontend usa [`frontend/.env.example`](frontend/.env.example); las variables públicas observadas incluyen `VITE_API_URL` y, opcionalmente, `VITE_WHATSAPP_NUMBER`.
- Flyway aplica `db/migration/V1__baseline_schema.sql` en una base nueva. Las migraciones aplicadas no se editan: agregá una migración versionada nueva.
- La política de adopción y rollback del esquema está en [`docs/markdown/migrations/migration-rollback-policy.md`](docs/markdown/migrations/migration-rollback-policy.md).
- Para producción, la cuenta de aplicación y la cuenta de migración son distintas; `compose.prod.yaml` exige ambas configuraciones.

## Pruebas

### Backend

```bash
cd backend
./mvnw -B verify
```

### Frontend

```bash
cd frontend
npm ci
npm run lint
npm run format:check
npm run coverage
```

Los scripts disponibles están declarados en [`frontend/package.json`](frontend/package.json). `npm run format` modifica archivos; `npm run format:check` sólo verifica el formato.

### E2E con Playwright

Antes de ejecutar cualquier modo, instalá las dependencias E2E desde la raíz del repositorio; esta instalación queda fuera del demo preservado:

```bash
cd e2e && npm ci && cd ..
```

#### Runner local nativo (`run-e2e.cjs`)

Para ejecutar la suite E2E localmente contra un stack desechable ya levantado (`tuhospedaje-dev-seeded` por defecto), usá el runner nativo desde la raíz o desde `e2e/`:

```bash
node e2e/scripts/run-e2e.cjs --dry-run
node e2e/scripts/run-e2e.cjs tests/smoke.spec.js --project=chromium --workers=1
cd e2e && npm run test:local -- --project=chromium
```

El runner verifica que los contenedores `db`, `backend` y `frontend` del proyecto Compose desechable estén en estado `running`, comprueba readiness HTTP en frontend (`BASE_URL/`) y backend (`API_BASE_URL/actuator/health/readiness`), y luego invoca Playwright con los argumentos recibidos. No levanta servidores, no descarga imágenes ni borra volúmenes automáticamente.

Variables y opciones admitidas:

- `E2E_COMPOSE_PROJECT` (por defecto `tuhospedaje-dev-seeded`): proyecto Compose desechable; rechaza nombres sin un segmento `dev` o `test` (como `tuhospedaje-local` o `prod`) para proteger el stack preservado.
- `E2E_FRONTEND_PORT` (por defecto `5173`) y `E2E_BACKEND_PORT` (por defecto `8080`): puertos enteros en `1..65535`.
- `BASE_URL` (por defecto `http://localhost:<E2E_FRONTEND_PORT>`) y `API_BASE_URL` (por defecto `http://localhost:<E2E_BACKEND_PORT>`).
- `--dry-run`: ejecuta todas las verificaciones e imprime el comando de Playwright sin lanzarlo.
- `--help` / `-h`: muestra la ayuda sin tocar Docker ni HTTP.

Las corridas locales reutilizan `tuhospedaje-dev-seeded`. Si el estado acumulado en la base de desarrollo provoca una falla local, recreá manualmente el stack desechable (nunca se automatiza en el runner para evitar borrados accidentales de volúmenes).

- **`mobile-webkit` (CI y opt-in local):** el proyecto `mobile-webkit` (`iPhone 13`, `smoke.spec.js` y `mobile-*.spec.js`) corre automáticamente en GitHub Actions CI. En entorno local es opt-in con `E2E_ENABLE_WEBKIT=1` si los binarios de WebKit están disponibles.
- **Assets canónicos:** las pruebas sobre imágenes canónicas siguen siendo opt-in mediante `CANONICAL_ASSETS_E2E=1`.
- **Aislamiento de datos:** las suites de autenticación, reservas, reseñas y administración escriben en la base; ejecutalas únicamente contra un stack desechable, nunca contra la base local preservada.

## Documentación y recorridos

- [`docs/README.md`](docs/README.md): índice general de documentación y jerarquía de fuentes.
- [`docs/markdown/api-reference.md`](docs/markdown/api-reference.md): referencia de la API HTTP (50 operaciones verificadas contra `/v3/api-docs`). En desarrollo, Swagger UI está en `http://localhost:8080/swagger-ui/index.html` y el esquema OpenAPI en `/v3/api-docs`; no hay colección Postman en el repositorio porque Swagger/OpenAPI la reemplaza.
- [`docs/markdown/backend-architecture.md`](docs/markdown/backend-architecture.md): arquitectura del backend.
- [`docs/markdown/bitacora-del-proyecto.md`](docs/markdown/bitacora-del-proyecto.md): bitácora del proyecto desde el Sprint 1 hasta v1.0.1.
- [`docs/markdown/delivery/current-main.md`](docs/markdown/delivery/current-main.md): registro histórico de la entrega académica del corte `18d9898` (anterior a v1.0.0). No describe el `main` actual; el historial y las verificaciones ligadas a ese SHA viven aquí.
- [`product.md`](product.md): resumen de alcance subordinado a los PDF originales; recorridos de demostración, excepción móvil de Admin e incrementos posteriores.
- [`DESIGN.md`](DESIGN.md): traducción técnica de identidad, convenciones registradas y diferencias pendientes frente a los requisitos oficiales; no sustituye su aceptación.
- [`docs/diseno/manual-identidad.md`](docs/diseno/manual-identidad.md): autoridad de identidad visual.
- [`docs/markdown/sprint-4/academic-demo-guide.md`](docs/markdown/sprint-4/academic-demo-guide.md): guía de demostración académica.
- [`docs/markdown/sprint-4/sprint-4-test-plan.md`](docs/markdown/sprint-4/sprint-4-test-plan.md): plan de pruebas del Sprint 4.
- [`docs/markdown/sprint-4/sprint-4-report.md`](docs/markdown/sprint-4/sprint-4-report.md): reporte histórico del Sprint 4.
- [`docs/markdown/project-definition.md`](docs/markdown/project-definition.md): definición y decisiones documentales del proyecto.
- [`docs/markdown/backend-security-hardening/backend-security-hardening-report.md`](docs/markdown/backend-security-hardening/backend-security-hardening-report.md): decisiones y controles de seguridad documentados.
- [`ops/mariadb/README.md`](ops/mariadb/README.md): backup y verificación de restore en un target aislado.

La documentación fechada y los reportes de CI describen revisiones concretas; no deben interpretarse como evidencia de un entorno productivo actual sin una verificación nueva.

## Licencia

Uso educativo — Digital House.
