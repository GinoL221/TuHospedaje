# US19 welcome email resend
Status: accepted for isolated observation; preserve the existing isolated stack and its queued messages.

## Specs
- S1: “hacemos la verificacion completa”
- S2: “lo hacemos”
- S3: “me parece correcto” — “mostrarlo en Home después del registro y también en una sección accesible para el usuario autenticado”
- S4: “lo hacemos asi” — “permitir que el usuario autenticado solicite otro correo aunque el registro anterior ya no esté en la cola”

## Tasks
- T0 [S4] Implement and test backend resend outcomes and safe recreation; route inline; commit: pending user authorization. Passed 15/15 tests.
- T1 [S2-S3] Implement and test resend entry points; route inline; commit: pending user authorization. Passed 69/69 tests.
- T2 [S1] Verify registration, email delivery and resend in a new isolated SMTP stack; route inline; commit: pending user authorization. Verified end-to-end on ephemeral stack.

## Log
- L1: “hacemos la verificacion completa”
- L2: “primero investiga”
- L3: “lo hacemos”
- L4: “me parece correcto”
- L5: “lo hacemos asi”
- L6: “diseñemos y probemos el limite”
- L7: “implementemoslo”
- L8: Job 9 ran `./mvnw -q -Dtest=WelcomeResendServiceTest test`: 3 tests, 1 failure, 0 errors. The added missing-row test expected SCHEDULED and got COOLDOWN. Job 10 reran the test after adding assertions about the recipient and pending state: 3 tests, 1 failure, 0 errors, still SCHEDULED expected and COOLDOWN observed. The later assertions were not reached. This is unit-level RED evidence only; it does not establish enqueue, cooldown persistence, HTTP behavior or concurrency. Implementation remains pending.
- L9: User authorized direct isolated Testcontainers execution. Job 11 failed compilation due to an incorrect `AutoConfigureMockMvc` import. After correcting the import, job 12 ran one HTTP integration test and failed: expected 202, got 429 after the test removed its user's WELCOME row in the disposable test database. The test has not established delivery, cooldown persistence, boundary behavior or concurrency.
- L10: Job 21 passed the focused service and HTTP tests from the isolated worktree. Independent verification identified two unproven races: an unrelated User update may overwrite the cooldown, and cleanup may remove the outbox row between lookup and requeue. The user selected “si” to revising coordination and reproducing both races before implementing a fix.
- L11: Reproducción y resolución de carreras concurrentes en MariaDB descartable: Job 24 reprodujo carrera de limpieza (falso 429); Job 26 reprodujo borrado de timestamp por actualización desactualizada de User. Se corrigió exclusión de timestamp en JPA y actualización explícita con bloqueo pesimista.
- L12: Se atendieron observaciones de revisión independiente: se reforzó mock unitario, se agregó @PreAuthorize("isAuthenticated()") en endpoint, se probó rollback transaccional ante fallos y se verificaron fronteras exactas de tiempo (299s bloqueado con 429 Retry-After: 300; 300s permitido con 202). 15/15 tests backend pasaron en verde.
- L13: Frontend: se implementó authService.resendWelcomeEmail(), banner post-registro en Home (WelcomeEmailBanner) y modal de perfil en Header (UserProfileModal) conforme a la Opción A elegida por el usuario. 69/69 tests frontend pasaron en Vitest.
- L14: Verificación funcional en vivo en pila efímera (tuh-us19-mailpit, tuh-us19-db, tuh-us19-backend en tuh-us19-net): registro de usuario generó y entregó correo con asunto '¡Bienvenido a TuHospedaje!' en Mailpit (status DELIVERED); el primer reenvío autenticado retornó 202 y entregó un segundo correo en Mailpit; el reenvío inmediato posterior fue rechazado con 429 y cabecera Retry-After: 300. Toda la pila efímera fue desmantelada limpiamente sin afectar ningún contenedor preexistente.
- L15: Independent verification by gentle-ai-verify passed all checks (15/15 backend tests in Testcontainers, 69/69 frontend tests in Vitest). User explicitly accepted US #19 for this isolated observation. Status updated to accepted. Traceability matrix untouched; no commit without explicit authorization.

