# TuHospedaje

TuHospedaje es una plataforma web para descubrir y reservar alojamientos turísticos. Permite explorar por ciudad y fechas, consultar disponibilidad, guardar favoritos, reservar y administrar el catálogo.

Es el proyecto final integrador de Digital House. El alcance académico corresponde a los Sprints 1–4; pasarelas de pago, chat interno y mapas en vivo quedan fuera de esta entrega. La definición funcional está en [`product.md`](product.md).

## Stack

- **Backend:** Java 17, Spring Boot 3.5, Spring Security/JWT, Spring Data JPA y MariaDB.
- **Frontend:** React 19, Vite 8, React Router y Lucide React.
- **E2E:** Playwright para Chromium, Firefox y mobile Chromium.
- **Servicios opcionales:** Cloudinary para imágenes y SMTP para email. La configuración SMTP no prueba por sí sola la entrega del proveedor o la llegada al buzón.

## Requisitos

- Java 17 o superior.
- Node.js `^20.19.0 || >=22.12.0` para Vite 8.
- Docker Compose v2 para el flujo contenedorizado.
- Python 3 únicamente si vas a cargar el seed demo y necesitás generar un hash bcrypt.

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

3. Para detenerlo sin borrar la base:

   ```bash
   docker compose --env-file deploy/dev.env \
     -f compose.yaml -f compose.dev.yaml down
   ```

   Para eliminar la base descartable de desarrollo, agregá `-v`. Nunca uses `down -v` sobre producción.

### Datos demo opcionales

El seed usa un proyecto y volumen separados (`tuhospedaje-dev-seeded`). Requiere `DEV_ADMIN_PASSWORD_HASH`; generá el hash con una contraseña de desarrollo que no uses en otro entorno:

```bash
python3 -m venv /tmp/bcrypt-venv
/tmp/bcrypt-venv/bin/pip install --quiet bcrypt
export DEV_ADMIN_PASSWORD_HASH=$(/tmp/bcrypt-venv/bin/python3 -c \
  "import bcrypt, getpass; print(bcrypt.hashpw(getpass.getpass().encode(), bcrypt.gensalt(10)).decode())")
```

Después, levantá el overlay:

```bash
docker compose --env-file deploy/dev.env \
  -f compose.yaml -f compose.dev.yaml -f compose.dev-seed.yaml up --build
```

El usuario administrador demo es `admin@tuhospedaje.com`; la contraseña es la que usaste para generar el hash. Si una migración demo falla a mitad de camino, recreá únicamente la base del proyecto seeded antes de reintentar:

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

Requiere backend y frontend corriendo, además de una base con los datos necesarios para el escenario:

```bash
cd e2e
npm ci
npx playwright install
npx playwright test
```

También podés ejecutar un proyecto específico con `npm run test:chrome`. La configuración usa `BASE_URL` y está en [`e2e/playwright.config.js`](e2e/playwright.config.js). Los reportes se generan en `e2e/playwright-report/`.

El escenario de imágenes canónicas requiere los masters externos y `CANONICAL_ASSETS_E2E=1`; el CI no los incluye porque esos JPEG permanecen fuera del repositorio.

## Documentación y recorridos

- [`product.md`](product.md): alcance funcional y recorridos académicos actuales.
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
