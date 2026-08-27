const { spawn } = require('node:child_process');
const { createReadiness } = require('./local-readiness');
const { createGuest, registerGuest } = require('./register-guest');

const DISPOSABLE_EMAIL = /^e2e-disposable-[a-z0-9-]+@example\.invalid$/;
const PLAYWRIGHT_CLI = require.resolve('@playwright/test/cli');

function isDisposableEmail(email) {
  return DISPOSABLE_EMAIL.test(email);
}

function run(command, args, options, input) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, options);
    child.once('error', reject);
    child.once('exit', (code) => code === 0 ? resolve(0) : reject(new Error('Disposable booking process failed.')));
    if (input) child.stdin.end(input);
  });
}

function cleanupSql(email) {
  return `START TRANSACTION;
SET @e2e_user_id = (SELECT id FROM users WHERE email = '${email}');
DELETE FROM session_security_events WHERE user_id = @e2e_user_id;
DELETE FROM refresh_tokens WHERE family_id IN (SELECT id FROM refresh_token_families WHERE user_id = @e2e_user_id);
DELETE FROM refresh_token_families WHERE user_id = @e2e_user_id;
DELETE FROM email_outbox WHERE user_id = @e2e_user_id;
DELETE FROM ratings WHERE user_id = @e2e_user_id;
DELETE FROM favorites WHERE user_id = @e2e_user_id;
DELETE FROM reservations WHERE user_id = @e2e_user_id;
DELETE FROM users WHERE id = @e2e_user_id;
COMMIT;`;
}

async function cleanupDisposableGuest(email) {
  if (!isDisposableEmail(email)) throw new Error('unsafe disposable email');
  await run('docker', ['compose', 'exec', '-T', 'db', 'sh', '-c', 'mariadb --user="$MARIADB_USER" --password="$MARIADB_PASSWORD" "$MARIADB_DATABASE"'], { shell: false, stdio: ['pipe', 'ignore', 'pipe'] }, cleanupSql(email));
}

async function runDisposableBooking({
  readiness = createReadiness(), register = registerGuest, spawnPlaywright = run,
  cleanup = cleanupDisposableGuest, guestFactory = createGuest,
} = {}) {
  const guest = guestFactory();
  let primaryError;
  try {
    if (!isDisposableEmail(guest.email)) throw new Error('unsafe disposable email');
    await readiness.check();
    await register({ guest });
    return await spawnPlaywright(PLAYWRIGHT_CLI, ['test', 'tests/booking.spec.js', '--project=chromium'], {
      shell: false,
      env: { E2E_DISPOSABLE_BOOKING: '1', TEST_USER_EMAIL: guest.email, TEST_USER_PASSWORD: guest.password, TEST_USER_NAME: guest.name },
    });
  } catch (error) {
    primaryError = error;
    throw error;
  } finally {
    if (isDisposableEmail(guest.email)) {
      try {
        await readiness.check();
        await cleanup(guest.email);
        await readiness.check();
      } catch (cleanupError) {
        if (!primaryError) throw cleanupError;
      }
    }
  }
}

if (require.main === module) {
  runDisposableBooking().catch(() => { process.exitCode = 1; });
}

module.exports = { cleanupDisposableGuest, runDisposableBooking };
