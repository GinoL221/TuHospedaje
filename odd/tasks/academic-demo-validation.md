# Academic Demo Validation

## Goal

Demonstrate the product's core guest and administrator journeys in a repeatable local environment, while recording only evidence observed during this validation.

## Scope

- Catalog and search
- Registration and login
- Reservation creation, listing, and cancellation
- Administrator access and catalog management
- Evidence and explicit limitations

## Non-goals

- Production backup/restore readiness
- S3/KMS infrastructure
- New product features
- Canonical JPEG production
- SMTP delivery unless selected after the core demo passes

## Tasks

- [x] 1. Define the disposable demo environment and test data.
  - Use `compose.yaml`, `compose.dev.yaml`, and `compose.dev-seed.yaml` with a private temporary env file outside the repository.
  - Use project `tuhospedaje-dev-seeded`, database `tuhospedaje_dev_demo`, profile `dev`, and generated local-only credentials.
  - Generate the documented demo administrator bcrypt hash in a disposable Python virtual environment; never access or overwrite `backend/.env`.
  - Register a unique guest during the demo and use a future available date range for the reservation.
  - Evidence: merged Compose configuration passed; ports 3307, 8080, and 5173 were free. Python bcrypt is not installed globally, so hash generation remains part of environment startup.

- [x] 2. Validate the guest journey.
  - The disposable stack was healthy and 13 focused Chromium tests passed with no failures or skips across smoke, search, authentication, booking, and reservation listing.
  - A separate live browser journey registered a unique guest, verified that the guest was denied the administration route, created a future reservation, found it in My Reservations, cancelled it, and observed final status `CANCELLED` with the cancel action removed.
  - The first live attempt used `127.0.0.1` and failed CORS with `Failed to fetch`; repeating the same flow through the configured `http://localhost:5173` origin passed.

- [x] 3. Validate the administrator journey.
  - Guest access to `/administración` redirected to the unauthorized route.
  - Seven focused Chromium checks passed with no failures or skips across admin smoke and policy CRUD.
  - Each policy CRUD test tracks its created row by API response ID; `afterEach` removes tracked rows, while the explicit delete scenario removes its row and clears it from cleanup tracking.

- [x] 4. Record the academic evidence and limitations.
  - Added the observed local guest and administrator outcomes to `docs/markdown/sprint-4/academic-demo-guide.md`.
  - Recorded the local MariaDB restore rehearsal as technical-only evidence and listed the operational controls it did not validate.
  - Kept Mailtrap, canonical JPEGs, production backup/restore, restored-application health, and cutover outside the verified evidence.
  - `git diff --check` passed and independent read-only verification found no overclaim or contradiction.

## Work-unit evidence

Primary work-unit commit: `926c944` (`docs: record academic demo validation`).
