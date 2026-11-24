#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const E2E_DIR = path.resolve(__dirname, '..');
const PLAYWRIGHT_BIN = path.join(E2E_DIR, 'node_modules', '.bin', 'playwright');
const DEFAULT_PROJECT = 'tuhospedaje-dev-seeded';
const DEFAULT_FRONTEND_PORT = '5173';
const DEFAULT_BACKEND_PORT = '8080';
const READINESS_TIMEOUT_MS = 5000;
const COMPOSE_SERVICES = Object.freeze(['db', 'backend', 'frontend']);
const INSPECT_FORMAT = '{{ index .Config.Labels "com.docker.compose.project" }}|{{.State.Status}}';
const SAFE_PROJECT_RE = /^[a-z0-9][a-z0-9_-]*$/i;
const DISPOSABLE_SEGMENT_RE = /(?:^|[-_])(dev|test)(?:$|[-_])/i;

const USAGE = `Usage: node e2e/scripts/run-e2e.cjs [options] [playwright-args...]

Lightweight local native E2E runner for TuHospedaje.
Verifies a running disposable Docker Compose stack and HTTP readiness before invoking Playwright.
Does NOT start servers, pull images, or delete volumes.

Options:
  -h, --help      Show this help message and exit 0
  --dry-run       Run compose, readiness, and binary checks and print the Playwright command without running it

Environment variables:
  E2E_COMPOSE_PROJECT   Disposable compose project name (default: ${DEFAULT_PROJECT}; must contain a standalone dev or test segment)
  E2E_FRONTEND_PORT     Frontend port 1..65535 (default: ${DEFAULT_FRONTEND_PORT})
  E2E_BACKEND_PORT      Backend port 1..65535 (default: ${DEFAULT_BACKEND_PORT})
  BASE_URL              Frontend base URL (default: http://localhost:<E2E_FRONTEND_PORT>)
  API_BASE_URL          Backend API base URL (default: http://localhost:<E2E_BACKEND_PORT>)

Examples:
  node e2e/scripts/run-e2e.cjs --dry-run
  node e2e/scripts/run-e2e.cjs tests/smoke.spec.js --project=chromium --workers=1
  cd e2e && npm run test:local -- --project=chromium`;

function parsePort(value, label = 'port') {
  const raw = typeof value === 'number' ? String(value) : value;
  if (typeof raw !== 'string' || !/^\d+$/.test(raw)) {
    throw new Error(`Invalid ${label}: "${value}" (expected integer 1..65535)`);
  }
  const port = Number(raw);
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error(`Invalid ${label}: "${value}" (expected integer 1..65535)`);
  }
  return port;
}

function assertDisposableProject(project) {
  if (typeof project !== 'string' || !SAFE_PROJECT_RE.test(project)) {
    throw new Error(
      `Invalid E2E_COMPOSE_PROJECT "${project}": must match ${SAFE_PROJECT_RE} and target a disposable dev/test stack`,
    );
  }
  if (!DISPOSABLE_SEGMENT_RE.test(project)) {
    throw new Error(
      `Refusing to run against non-disposable E2E_COMPOSE_PROJECT "${project}": name must include a standalone "dev" or "test" segment (e.g. "${DEFAULT_PROJECT}")`,
    );
  }
  return project;
}

function resolveConfig(env = process.env) {
  const rawProject = env.E2E_COMPOSE_PROJECT !== undefined ? env.E2E_COMPOSE_PROJECT : DEFAULT_PROJECT;
  const project = assertDisposableProject(rawProject);

  const rawFrontendPort = env.E2E_FRONTEND_PORT !== undefined ? env.E2E_FRONTEND_PORT : DEFAULT_FRONTEND_PORT;
  const rawBackendPort = env.E2E_BACKEND_PORT !== undefined ? env.E2E_BACKEND_PORT : DEFAULT_BACKEND_PORT;
  const frontendPort = parsePort(rawFrontendPort, 'E2E_FRONTEND_PORT');
  const backendPort = parsePort(rawBackendPort, 'E2E_BACKEND_PORT');

  const baseUrl = env.BASE_URL && env.BASE_URL.trim()
    ? env.BASE_URL.trim()
    : `http://localhost:${frontendPort}`;
  const apiBaseUrl = env.API_BASE_URL && env.API_BASE_URL.trim()
    ? env.API_BASE_URL.trim()
    : `http://localhost:${backendPort}`;

  return {
    project,
    frontendPort,
    backendPort,
    baseUrl,
    apiBaseUrl,
  };
}

function verifyComposeStack(project, run = spawnSync) {
  const expected = `${project}|running`;
  for (const service of COMPOSE_SERVICES) {
    const container = `${project}-${service}-1`;
    const result = run('docker', ['inspect', '--format', INSPECT_FORMAT, container], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      timeout: 5000,
    });
    if (result.error) {
      throw new Error(`Failed to inspect Docker container "${container}": ${result.error.message}`);
    }
    if (result.status !== 0) {
      const detail = (result.stderr || '').trim() || `exit code ${result.status}`;
      throw new Error(
        `Required disposable stack container "${container}" is missing or unreadable (${detail}). Start the disposable stack "${project}" first.`,
      );
    }
    const actual = String(result.stdout || '').trim();
    if (actual !== expected) {
      throw new Error(
        `Container "${container}" reported "${actual}" (expected "${expected}"). Refusing to run E2E against an unready or mismatched stack.`,
      );
    }
  }
}

async function checkEndpoint(url, isOk, label, fetchImpl, timeoutMs) {
  let response;
  try {
    response = await fetchImpl(url, {
      method: 'GET',
      redirect: 'manual',
      signal: AbortSignal.timeout(timeoutMs),
    });
  } catch (err) {
    throw new Error(`${label} readiness check failed for ${url}: ${err.message || err}`);
  }
  if (!isOk(response.status)) {
    throw new Error(`${label} readiness check failed for ${url}: HTTP ${response.status}`);
  }
}

async function verifyReadiness({ baseUrl, apiBaseUrl }, fetchImpl = globalThis.fetch, timeoutMs = READINESS_TIMEOUT_MS) {
  const frontendUrl = `${baseUrl.replace(/\/+$/, '')}/`;
  const backendOrigin = apiBaseUrl.replace(/\/+$/, '').replace(/\/api$/, '');
  const backendUrl = `${backendOrigin}/actuator/health/readiness`;

  await checkEndpoint(frontendUrl, (s) => s >= 200 && s < 400, 'Frontend', fetchImpl, timeoutMs);
  await checkEndpoint(backendUrl, (s) => s === 200, 'Backend', fetchImpl, timeoutMs);
}

function verifyPlaywright(access = fs.accessSync) {
  try {
    access(PLAYWRIGHT_BIN, fs.constants.X_OK);
  } catch {
    throw new Error('Playwright executable is missing; run: cd e2e && npm ci');
  }
}

async function main(argv = process.argv.slice(2), deps = {}) {
  const log = deps.log || console.log;

  if (argv.includes('--help') || argv.includes('-h')) {
    log(USAGE);
    return 0;
  }

  let dryRun = false;
  const playwrightArgs = [];
  for (const arg of argv) {
    if (arg === '--dry-run') {
      dryRun = true;
    } else {
      playwrightArgs.push(arg);
    }
  }

  const env = deps.env || process.env;
  const run = deps.spawnSync || spawnSync;
  const fetchImpl = deps.fetchImpl || globalThis.fetch;
  const access = deps.access || fs.accessSync;

  const config = resolveConfig(env);
  verifyComposeStack(config.project, run);
  await verifyReadiness(config, fetchImpl);
  verifyPlaywright(access);

  const cmdArgs = ['test', ...playwrightArgs];
  if (dryRun) {
    log(
      `[DRY RUN] Stack "${config.project}" and endpoints (${config.baseUrl}, ${config.apiBaseUrl}) are ready.\n` +
      `[DRY RUN] Would run: ${PLAYWRIGHT_BIN} ${cmdArgs.join(' ')}`,
    );
    return 0;
  }

  const childEnv = {
    ...env,
    BASE_URL: config.baseUrl,
    API_BASE_URL: config.apiBaseUrl,
    E2E_FRONTEND_PORT: String(config.frontendPort),
    E2E_BACKEND_PORT: String(config.backendPort),
  };

  const result = run(PLAYWRIGHT_BIN, cmdArgs, {
    cwd: E2E_DIR,
    stdio: 'inherit',
    env: childEnv,
  });

  if (result.error) {
    throw new Error(`Failed to run Playwright: ${result.error.message}`);
  }
  return typeof result.status === 'number' ? result.status : 1;
}

module.exports = {
  parsePort,
  assertDisposableProject,
  resolveConfig,
  verifyComposeStack,
  verifyReadiness,
  verifyPlaywright,
  main,
};

if (require.main === module) {
  main()
    .then((code) => {
      process.exitCode = code;
    })
    .catch((err) => {
      console.error(err.message || err);
      process.exitCode = 1;
    });
}
