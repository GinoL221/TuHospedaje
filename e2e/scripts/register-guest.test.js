const test = require('node:test');
const assert = require('node:assert/strict');
const { registerGuest } = require('./register-guest');
const { runDisposableBooking } = require('./run-disposable-booking');

const guest = { name: 'Disposable Guest', email: 'e2e-disposable-test@example.invalid', password: 'throwaway-password' };

test('registers one disposable guest with one direct 201 POST and no CSRF request', async () => {
  const calls = [];
  const registered = await registerGuest({
    guest,
    transport: async (request) => {
      calls.push(request);
      return { status: 201 };
    },
  });

  assert.equal(registered, guest);
  assert.deepEqual(calls, [{
    method: 'POST',
    url: 'http://localhost:8080/api/auth/register',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(guest),
  }]);
  assert.equal(calls.some(({ url }) => url.includes('/api/auth/csrf')), false);
});

test('fails closed with a sanitized error for non-201 responses and transport errors', async () => {
  for (const transport of [async () => ({ status: 400 }), async () => { throw new Error(guest.password); }]) {
    await assert.rejects(registerGuest({ guest, transport }), (error) =>
      error.message === 'Disposable guest registration failed.' && !error.message.includes(guest.password));
  }
});

test('adapts the default fetch transport without exposing guest values', async () => {
  const originalFetch = global.fetch;
  let request;
  global.fetch = async (url, options) => {
    request = { url, options };
    return { status: 201 };
  };
  try {
    await registerGuest({ guest });
    assert.equal(request.url, 'http://localhost:8080/api/auth/register');
    assert.equal(request.options.method, 'POST');
  } finally {
    global.fetch = originalFetch;
  }
});

test('runs readiness around setup and cleanup, sends only credential env to a shell-free booking child', async () => {
  const calls = [];
  const readinessPhases = ['setup', 'pre-cleanup', 'post-cleanup'];
  const result = await runDisposableBooking({
    guestFactory: () => guest,
    readiness: { check: async () => calls.push(`readiness:${readinessPhases.shift()}`) },
    register: async () => calls.push('register'),
    spawnPlaywright: async (command, args, options) => {
      calls.push({ command, args, options });
      return 0;
    },
    cleanup: async (email) => calls.push(`cleanup:${email}`),
  });

  assert.equal(result, 0);
  const child = calls[2];
  assert.deepEqual(calls, [
    'readiness:setup',
    'register',
    child,
    'readiness:pre-cleanup',
    `cleanup:${guest.email}`,
    'readiness:post-cleanup',
  ]);
  assert.match(child.command, /@playwright[\\/]test[\\/]cli/);
  assert.deepEqual(child.args, ['test', 'tests/booking.spec.js', '--project=chromium']);
  assert.equal(child.options.shell, false);
  assert.deepEqual(Object.keys(child.options.env).sort(), ['E2E_DISPOSABLE_BOOKING', 'TEST_USER_EMAIL', 'TEST_USER_NAME', 'TEST_USER_PASSWORD']);
});

test('blocks the child for unsafe setup and keeps the primary failure when cleanup also fails', async () => {
  let spawned = false;
  await assert.rejects(runDisposableBooking({
    guestFactory: () => ({ ...guest, email: 'outside@example.invalid' }),
    readiness: { check: async () => {} }, register: async () => {},
    spawnPlaywright: async () => { spawned = true; }, cleanup: async () => {},
  }), /unsafe disposable email/);
  assert.equal(spawned, false);

  await assert.rejects(runDisposableBooking({
    guestFactory: () => guest, readiness: { check: async () => {} }, register: async () => {},
    spawnPlaywright: async () => { throw new Error('booking failed'); },
    cleanup: async () => { throw new Error('cleanup failed'); },
  }), /booking failed/);
});

test('does not start Playwright when registration rejects and still cleans up the guest', async () => {
  const calls = [];
  const readinessPhases = ['setup', 'pre-cleanup', 'post-cleanup'];

  await assert.rejects(runDisposableBooking({
    guestFactory: () => guest,
    readiness: { check: async () => calls.push(`readiness:${readinessPhases.shift()}`) },
    register: async () => { throw new Error('registration failed'); },
    spawnPlaywright: async () => calls.push('spawn'),
    cleanup: async (email) => calls.push(`cleanup:${email}`),
  }), /registration failed/);

  assert.deepEqual(calls, [
    'readiness:setup',
    'readiness:pre-cleanup',
    `cleanup:${guest.email}`,
    'readiness:post-cleanup',
  ]);
  assert.equal(calls.includes('spawn'), false);
});

test('disposable Playwright mode skips dotenv and credential-bearing artifacts', () => {
  const configPath = require.resolve('../playwright.config');
  const dotenvPath = require.resolve('dotenv');
  const originalEnv = process.env.E2E_DISPOSABLE_BOOKING;
  require('dotenv');
  const originalDotenv = require.cache[dotenvPath].exports;
  let dotenvCalls = 0;
  require.cache[dotenvPath].exports = { config: () => { dotenvCalls += 1; } };
  process.env.E2E_DISPOSABLE_BOOKING = '1';
  delete require.cache[configPath];
  try {
    const config = require('../playwright.config');
    assert.equal(dotenvCalls, 0);
    assert.deepEqual(config.reporter, [['list']]);
    assert.equal(config.use.screenshot, 'off');
    assert.equal(config.use.video, 'off');
    assert.equal(config.use.trace, 'off');
  } finally {
    require.cache[dotenvPath].exports = originalDotenv;
    if (originalEnv === undefined) delete process.env.E2E_DISPOSABLE_BOOKING;
    else process.env.E2E_DISPOSABLE_BOOKING = originalEnv;
    delete require.cache[configPath];
  }
});
