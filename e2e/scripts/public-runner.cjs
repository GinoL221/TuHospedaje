#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { randomBytes } = require('node:crypto');

const MODES = Object.freeze({
  list: ['--list', '--network', 'none'],
  smoke: ['--grep', 'Smoke'],
  public: [],
});
const E2E = path.resolve(__dirname, '..');
const CONFIG = '/workspace/e2e/playwright.public.config.cjs';
const IMAGE_PREFIX = 'mcr.microsoft.com/playwright:v';
const MAX_CPUS = 2;
const MEMORY = '2g';
const PIDS = '256';
const DEADLINES_SECONDS = Object.freeze({ list: 90, smoke: 180, public: 600 });

function pinnedVersion(manifest) {
  const version = manifest?.devDependencies?.['@playwright/test'];
  if (typeof version !== 'string' || !/^\d+\.\d+\.\d+$/.test(version)) {
    throw new Error('@playwright/test must have an exact three-part version pin');
  }
  return version;
}

function imageForVersion(version) {
  if (!/^\d+\.\d+\.\d+$/.test(version)) throw new Error('Invalid Playwright version');
  return `${IMAGE_PREFIX}${version}-noble`;
}

function parseMode(argv) {
  if (argv.length !== 1 || !Object.hasOwn(MODES, argv[0])) {
    throw new Error('Usage: node e2e/scripts/public-runner.cjs <list|smoke|public>');
  }
  return argv[0];
}

function dockerArgv({ image, repo, artifacts, mode, containerName }) {
  const args = [
    'run', '--rm', '--pull=never', '--init', '--name', containerName,
    '--network', mode === 'list' ? 'none' : 'host',
    '--read-only', '--shm-size', '512m', '--tmpfs', '/tmp:rw,noexec,nosuid,size=256m',
    '--user', `${process.getuid?.() ?? 65534}:${process.getgid?.() ?? 65534}`,
    '--security-opt', 'no-new-privileges:true', '--cap-drop', 'ALL',
    '--cpus', String(MAX_CPUS), '--memory', MEMORY, '--pids-limit', PIDS,
    '--mount', `type=bind,src=${repo},dst=/workspace/e2e,readonly`,
    '--mount', `type=bind,src=${artifacts},dst=/artifacts`,
    '--workdir', '/workspace/e2e',
    '--env', 'PUBLIC_ARTIFACTS_DIR=/artifacts',
    '--env', 'HOME=/tmp', '--env', 'XDG_CACHE_HOME=/tmp/fontcache',
    image,
    'node_modules/.bin/playwright', 'test', '--config', CONFIG,
  ];
  if (mode === 'list') args.push('--list');
  if (mode === 'smoke') args.push('--grep', 'Smoke');
  return args;
}

function verifyImage(image, run = spawnSync) {
  const result = run('docker', ['image', 'inspect', image, '--format', '{{json .RepoTags}}'], {
    encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'],
  });
  if (result.status !== 0) {
    throw new Error(`Required cached image is unavailable; no pull attempted: ${image}`);
  }
  let tags;
  try { tags = JSON.parse(result.stdout.trim()); } catch { throw new Error('Could not verify cached image tag'); }
  if (!Array.isArray(tags) || !tags.includes(image)) throw new Error(`Cached image tag mismatch: ${image}`);
}

async function checkDemo() {
  let response;
  try {
    response = await fetch('http://localhost:5173/', {
      method: 'GET', redirect: 'manual', signal: AbortSignal.timeout(3000),
    });
  } catch {
    throw new Error('Demo readiness GET failed at fixed URL http://localhost:5173/');
  }
  if (response.status < 200 || response.status >= 400) {
    throw new Error(`Demo readiness returned HTTP ${response.status}`);
  }
}

function createArtifacts() {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'tuhospedaje-playwright-public-'));
  fs.chmodSync(dir, 0o700);
  return dir;
}

async function main(argv = process.argv.slice(2), deps = {}) {
  const mode = parseMode(argv);
  let manifest;
  try {
    manifest = JSON.parse(fs.readFileSync(path.join(E2E, 'package.json'), 'utf8'));
  } catch {
    throw new Error('Could not read valid e2e/package.json');
  }
  const image = imageForVersion(pinnedVersion(manifest));
  const run = deps.spawnSync || spawnSync;
  verifyImage(image, run);
  if (mode !== 'list') await (deps.checkDemo || checkDemo)();
  const artifacts = (deps.createArtifacts || createArtifacts)();
  const containerName = `tuhospedaje-public-${(deps.randomBytes || randomBytes)(12).toString('hex')}`;
  const args = dockerArgv({ image, repo: E2E, artifacts, mode, containerName });
  const result = run('docker', args, {
    stdio: 'inherit', env: { PATH: process.env.PATH },
    timeout: DEADLINES_SECONDS[mode] * 1000,
  });
  console.log(`Artifacts: ${artifacts} (private directory; remove it manually when no longer needed)`);
  if (result.error?.code === 'ETIMEDOUT') {
    const stopped = run('docker', ['stop', '--time', '5', containerName], {
      encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'], timeout: 10000,
    });
    if (stopped.status === 0) {
      console.error(`Deadline exceeded (${DEADLINES_SECONDS[mode]}s); stopped this run's container ${containerName}; --rm will remove it.`);
    } else {
      const uncertainty = stopped.error?.code === 'ETIMEDOUT' || stopped.status === null
        ? ' stop timed out or returned no status; container state is uncertain.'
        : ' stop was not confirmed.';
      console.error(`Deadline exceeded (${DEADLINES_SECONDS[mode]}s);${uncertainty} Inspect read-only with: docker inspect ${containerName}`);
    }
    process.exitCode = 1;
    return;
  }
  if (result.error) {
    console.error(`Docker run failed: ${result.error.message}`);
    process.exitCode = 1;
    return;
  }
  if (result.status !== 0) process.exitCode = result.status ?? 1;
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}

module.exports = { pinnedVersion, imageForVersion, parseMode, dockerArgv, verifyImage, checkDemo, main };
