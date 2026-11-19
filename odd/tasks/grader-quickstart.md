# Grader quickstart

Make a fresh clone runnable and navigable for the course grader without changing product behavior.
Branch: `feat/grader-quickstart` from `main@2113c00`. Engram mirror: `odd/grader-quickstart/tasks`.

## Specs

- S1. One generated, valid `deploy/dev.env`: a script creates it with random Base64 secrets (≥32 bytes), a `DEV_DB_NAME` with a standalone `dev` segment, and bcrypt hashes of documented demo passwords. No secret or hash is committed; `test_env_example_files_do_not_contain_plaintext_secrets` stays unchanged. User choice: "Script que lo genera (Recomendado)" — "scripts/dev-setup.sh crea deploy/dev.env con secretos aleatorios y el hash de una contraseña demo documentada (bcrypt vía Docker, sin Python). El test de higiene queda igual."
- S2. The script never overwrites an existing `deploy/dev.env` unless forced, and fails with a clear message when it cannot hash.
- S3. Demo guest user (role `USER`) in the dev seed through a new `db/dev/V1_9001` migration, with its own bcrypt hash placeholder. `V1_9000` stays byte-identical. User choice: "En el seed (V1_9001) (Recomendado)" — "Nueva migración db/dev con un USER y su propio hash. Toca seed-entrypoint.sh, el placeholder de Flyway, el test de migraciones y el manifest. Queda disponible apenas arranca."
- S4. Every consumer of `db/dev` keeps working: seed entrypoint (dev and local), CI e2e backend start, `DatabaseMigrationIntegrationTest`, `deploy/local-admin.env.example`.
- S5. README opens with a grader quickstart (dev + seed, script, `http://localhost:5173`, demo credentials); `compose.local.yaml` becomes an optional section that states it needs external images; README links `docs/README.md`, `docs/markdown/api-reference.md` and Swagger UI.
- S6. `deploy/dev.env.example` documents the format of each required value.
- S7. `docs/markdown/sprint-4/academic-demo-guide.md` no longer mentions the removed `ConsoleEmailServiceImpl`, and uses the seeded guest.

## Tasks

- T1 [S3, S4] seed guest user + tests + CI — inline — commit: `e5f268d` (slice 1, Risk: item 5 — CI)
- T2 [S1, S2] `scripts/dev-setup.sh` + `tools/tests/test_dev_setup.py` — inline — commit: `2036387` (slice 2; ~400-line boundary noted after 568 cumulative lines across slices 1–2)
- T3 [S5, S6, S7] README, `dev.env.example`, demo guide — inline — commit: `8924b9d`, follow-up `docs(readme): note port conflicts and stale dev DB volume` commit (README troubleshooting from the smoke test; cumulative 767 lines across 4 commits, delivered as one PR)

## Log

- L1 (user, verbatim): "podes hacer un analisis general del proyecto antes de la entrega? ya es facilmente ejecutable y navegable para corregir?"
- L2 (user, verbatim, answering whether example files and the seed may change, not only docs): "si permite"
- L3 (decision): hygiene test forbids >20-char values in `deploy/*.env.example`; user chose a generator script over committing fixed values, and a seeded guest via `V1_9001`.
- L4 (evidence): native review blocked by `managed_assets_outdated` even after `gentle-ai sync --agent pi` (binary skew, see gentle-ai #4379/#4271); user chose to skip it and rely on independent verify + real CI.
- L5 (evidence): smoke test of the README quickstart on a fresh volume: db/backend/frontend healthy, `http://localhost:5173` 200, login 200 for `admin@tuhospedaje.com` (ADMIN) and `huesped@tuhospedaje.com` (USER), CORS preflight from `localhost:5173` OK. First attempt failed with `Access denied for user 'tuhospedaje_dev'` because a stale `tuhospedaje-dev-seeded-db` volume kept an old password, and port 5173 was held by `tuhospedaje-local`; README now documents both.
