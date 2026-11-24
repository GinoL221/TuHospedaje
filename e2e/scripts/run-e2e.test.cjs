'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const {
  parsePort,
  assertDisposableProject,
  resolveConfig,
  verifyComposeStack,
  verifyReadiness,
  main,
} = require('./run-e2e.cjs');

test('parsePort accepts valid ports and rejects non-numeric, out-of-range, or trailing junk', () => {
  assert.equal(parsePort('5173', 'E2E_FRONTEND_PORT'), 5173);
  assert.equal(parsePort(8080, 'E2E_BACKEND_PORT'), 8080);
  assert.equal(parsePort('1'), 1);
  assert.equal(parsePort('65535'), 65535);

  for (const invalid of ['', '0', '-1', '65536', '99999', 'abc', '5173abc', '5173.5', ' 5173', '5173 ']) {
    assert.throws(() => parsePort(invalid, 'E2E_FRONTEND_PORT'), /Invalid E2E_FRONTEND_PORT/);
  }
});

test('resolveConfig applies defaults and resolves custom ports and URLs', () => {
  const defaults = resolveConfig({});
  assert.deepEqual(defaults, {
    project: 'tuhospedaje-dev-seeded',
    frontendPort: 5173,
    backendPort: 8080,
    baseUrl: 'http://localhost:5173',
    apiBaseUrl: 'http://localhost:8080',
  });

  const customPorts = resolveConfig({
    E2E_COMPOSE_PROJECT: 'tuhospedaje-test',
    E2E_FRONTEND_PORT: '4173',
    E2E_BACKEND_PORT: '18080',
  });
  assert.deepEqual(customPorts, {
    project: 'tuhospedaje-test',
    frontendPort: 4173,
    backendPort: 18080,
    baseUrl: 'http://localhost:4173',
    apiBaseUrl: 'http://localhost:18080',
  });

  const explicitUrls = resolveConfig({
    E2E_COMPOSE_PROJECT: 'e2e-test',
    BASE_URL: 'http://127.0.0.1:5174',
    API_BASE_URL: 'http://127.0.0.1:8081',
  });
  assert.equal(explicitUrls.baseUrl, 'http://127.0.0.1:5174');
  assert.equal(explicitUrls.apiBaseUrl, 'http://127.0.0.1:8081');

  assert.throws(() => resolveConfig({ E2E_FRONTEND_PORT: 'not-a-port' }), /Invalid E2E_FRONTEND_PORT/);
  assert.throws(() => resolveConfig({ E2E_BACKEND_PORT: '70000' }), /Invalid E2E_BACKEND_PORT/);
});

test('assertDisposableProject allows dev/test compose projects and rejects preserved or unsafe names', () => {
  for (const valid of ['tuhospedaje-dev-seeded', 'tuhospedaje-test', 'e2e-test', 'dev', 'test', 'app_dev_1']) {
    assert.equal(assertDisposableProject(valid), valid);
  }

  for (const invalid of [
    '',
    '   ',
    'tuhospedaje-local',
    'tuhospedaje-prod',
    'production',
    'mydev',
    'testing',
    'tuhospedaje-dev;rm',
    '../dev',
    'dev stack',
  ]) {
    assert.throws(() => assertDisposableProject(invalid), /disposable|invalid/i);
  }
});

test('verifyComposeStack passes when db, backend, and frontend all report <project>|running', () => {
  const calls = [];
  const fakeSpawn = (cmd, args) => {
    calls.push([cmd, args]);
    return { status: 0, stdout: 'tuhospedaje-dev-seeded|running\n', stderr: '' };
  };

  verifyComposeStack('tuhospedaje-dev-seeded', fakeSpawn);

  assert.equal(calls.length, 3);
  assert.deepEqual(
    calls.map(([, args]) => args[args.length - 1]),
    [
      'tuhospedaje-dev-seeded-db-1',
      'tuhospedaje-dev-seeded-backend-1',
      'tuhospedaje-dev-seeded-frontend-1',
    ],
  );
  for (const [cmd, args] of calls) {
    assert.equal(cmd, 'docker');
    assert.equal(args[0], 'inspect');
    assert.equal(args[1], '--format');
    assert.equal(args[2], '{{ index .Config.Labels "com.docker.compose.project" }}|{{.State.Status}}');
  }
});

test('verifyComposeStack fails closed when a container is stopped, missing, or has wrong project label', () => {
  assert.throws(
    () =>
      verifyComposeStack('tuhospedaje-dev-seeded', (_cmd, args) => {
        const container = args[args.length - 1];
        if (container.endsWith('-backend-1')) {
          return { status: 0, stdout: 'tuhospedaje-dev-seeded|exited\n', stderr: '' };
        }
        return { status: 0, stdout: 'tuhospedaje-dev-seeded|running\n', stderr: '' };
      }),
    /tuhospedaje-dev-seeded-backend-1.*exited/i,
  );

  assert.throws(
    () =>
      verifyComposeStack('tuhospedaje-dev-seeded', (_cmd, args) => {
        const container = args[args.length - 1];
        if (container.endsWith('-db-1')) {
          return { status: 0, stdout: 'tuhospedaje-local|running\n', stderr: '' };
        }
        return { status: 0, stdout: 'tuhospedaje-dev-seeded|running\n', stderr: '' };
      }),
    /tuhospedaje-local\|running/,
  );

  assert.throws(
    () =>
      verifyComposeStack('tuhospedaje-dev-seeded', () => ({
        status: 1,
        stdout: '',
        stderr: 'Error: No such object',
      })),
    /tuhospedaje-dev-seeded-db-1/,
  );
});

test('verifyReadiness passes on frontend 200-399 and backend 200 and fails closed on 503 or fetch error', async () => {
  const requested = [];
  await verifyReadiness(
    { baseUrl: 'http://localhost:5173', apiBaseUrl: 'http://localhost:8080' },
    async (url, opts) => {
      requested.push({ url, method: opts.method, hasSignal: Boolean(opts.signal) });
      if (url === 'http://localhost:5173/') return { status: 200 };
      if (url === 'http://localhost:8080/actuator/health/readiness') return { status: 200 };
      throw new Error(`Unexpected URL: ${url}`);
    },
  );
  assert.deepEqual(requested, [
    { url: 'http://localhost:5173/', method: 'GET', hasSignal: true },
    { url: 'http://localhost:8080/actuator/health/readiness', method: 'GET', hasSignal: true },
  ]);

  await assert.rejects(
    verifyReadiness(
      { baseUrl: 'http://localhost:5173', apiBaseUrl: 'http://localhost:8080' },
      async (url) => ({ status: url.includes('/actuator/health/readiness') ? 503 : 200 }),
    ),
    /503/,
  );

  await assert.rejects(
    verifyReadiness(
      { baseUrl: 'http://localhost:5173', apiBaseUrl: 'http://localhost:8080' },
      async () => ({ status: 500 }),
    ),
    /HTTP 500/,
  );

  await assert.rejects(
    verifyReadiness(
      { baseUrl: 'http://localhost:5173', apiBaseUrl: 'http://localhost:8080' },
      async () => {
        throw new Error('ECONNREFUSED');
      },
    ),
    /ECONNREFUSED/,
  );
});

test('--help and -h exit 0 without touching Docker, HTTP, or Playwright', async () => {
  for (const flag of ['--help', '-h']) {
    const calls = [];
    const logs = [];
    const code = await main([flag], {
      spawnSync: (...args) => {
        calls.push(['spawn', args]);
        return { status: 0, stdout: '' };
      },
      fetchImpl: async (...args) => {
        calls.push(['fetch', args]);
        return { status: 200 };
      },
      access: (...args) => {
        calls.push(['access', args]);
      },
      log: (msg) => logs.push(msg),
    });

    assert.equal(code, 0);
    assert.deepEqual(calls, []);
    assert.match(logs.join('\n'), /Usage:/);
  }
});

test('--dry-run runs compose and readiness checks without spawning Playwright', async () => {
  const spawned = [];
  const logs = [];
  const code = await main(['--dry-run', 'tests/smoke.spec.js', '--project=chromium'], {
    env: { E2E_COMPOSE_PROJECT: 'tuhospedaje-dev-seeded' },
    spawnSync: (cmd, args) => {
      spawned.push([cmd, args]);
      return { status: 0, stdout: 'tuhospedaje-dev-seeded|running\n', stderr: '' };
    },
    fetchImpl: async () => ({ status: 200 }),
    access: () => {},
    log: (msg) => logs.push(msg),
  });

  assert.equal(code, 0);
  assert.equal(spawned.length, 3);
  assert.ok(spawned.every(([cmd]) => cmd === 'docker'));
  assert.match(logs.join('\n'), /DRY RUN/i);
  assert.match(logs.join('\n'), /tests\/smoke\.spec\.js/);
});

test('main forwards args and resolved env vars to Playwright and propagates non-zero exit codes', async () => {
  const spawned = [];
  let checkedPath;
  let checkedMode;

  const code = await main(['tests/smoke.spec.js', '--project=chromium', '--workers=1'], {
    env: {
      PATH: '/usr/bin',
      E2E_COMPOSE_PROJECT: 'e2e-test',
      E2E_FRONTEND_PORT: '5174',
      E2E_BACKEND_PORT: '8081',
    },
    access: (file, mode) => {
      checkedPath = file;
      checkedMode = mode;
    },
    spawnSync: (cmd, args, options) => {
      spawned.push({ cmd, args, options });
      if (cmd === 'docker') {
        return { status: 0, stdout: 'e2e-test|running\n', stderr: '' };
      }
      return { status: 42 };
    },
    fetchImpl: async () => ({ status: 200 }),
  });

  assert.equal(code, 42);
  assert.match(checkedPath, /e2e[\\/]node_modules[\\/]\.bin[\\/]playwright$/);
  assert.equal(checkedMode, fs.constants.X_OK);

  const pwCall = spawned.find((call) => call.cmd !== 'docker');
  assert.ok(pwCall, 'expected Playwright to be spawned');
  assert.equal(pwCall.cmd, path.resolve(__dirname, '..', 'node_modules', '.bin', 'playwright'));
  assert.deepEqual(pwCall.args, ['test', 'tests/smoke.spec.js', '--project=chromium', '--workers=1']);
  assert.equal(pwCall.options.cwd, path.resolve(__dirname, '..'));
  assert.equal(pwCall.options.env.BASE_URL, 'http://localhost:5174');
  assert.equal(pwCall.options.env.API_BASE_URL, 'http://localhost:8081');
  assert.equal(pwCall.options.env.E2E_FRONTEND_PORT, '5174');
  assert.equal(pwCall.options.env.E2E_BACKEND_PORT, '8081');
});
