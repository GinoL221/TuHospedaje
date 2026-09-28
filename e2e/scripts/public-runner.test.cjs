'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const {
  pinnedVersion, imageForVersion, parseMode, dockerArgv, verifyPlaywright, verifyImage, main,
} = require('./public-runner.cjs');

const image = 'mcr.microsoft.com/playwright:v1.60.0-noble';

test('requires an exact pinned Playwright version and derives the official Noble tag', () => {
  assert.equal(pinnedVersion({ devDependencies: { '@playwright/test': '1.60.0' } }), '1.60.0');
  assert.equal(imageForVersion('1.60.0'), image);
  for (const version of ['^1.60.0', '1.60', 'latest', '../1.2.3']) {
    assert.throws(() => pinnedVersion({ devDependencies: { '@playwright/test': version } }));
  }
  assert.throws(() => imageForVersion('1.60.0;evil'));
});

test('accepts only fixed modes and no user-supplied Playwright args', () => {
  for (const mode of ['list', 'smoke', 'public']) assert.equal(parseMode([mode]), mode);
  for (const args of [[], ['all'], ['public', '--project=chromium'], ['public', '../tests/admin.spec.js']]) {
    assert.throws(() => parseMode(args), /Usage:/);
  }
});

test('builds bounded read-only container argv with only the isolated artifact mount', () => {
  const containerName = 'tuhospedaje-public-test';
  const args = dockerArgv({ image, repo: '/repo/e2e', artifacts: '/private/run', mode: 'public', containerName });
  assert.equal(args[0], 'run');
  assert.deepEqual(args.slice(0, 7), ['run', '--rm', '--pull=never', '--init', '--name', containerName, '--network']);
  assert.ok(args.includes(containerName));
  for (const required of ['--rm', '--pull=never', '--read-only', '--shm-size', '512m', '--cap-drop', 'ALL', '--network', 'host', '--cpus', '2', '--memory', '2g', '--pids-limit', '256']) {
    assert.ok(args.includes(required), `missing ${required}`);
  }
  assert.ok(args.includes('no-new-privileges:true'));
  assert.ok(args.includes('/tmp:rw,noexec,nosuid,size=256m'));
  assert.ok(args.includes('HOME=/tmp'));
  assert.ok(args.includes('XDG_CACHE_HOME=/tmp/fontcache'));
  assert.ok(args.some((arg) => arg.includes('src=/repo/e2e,dst=/workspace/e2e,readonly')));
  assert.ok(args.some((arg) => arg.includes('src=/private/run,dst=/artifacts')));
  assert.equal(args.filter((arg) => arg.startsWith('--mount')).length, 2);
  assert.ok(args.includes('/workspace/e2e/playwright.public.config.cjs'));
  assert.deepEqual(dockerArgv({ image, repo: '/repo/e2e', artifacts: '/private/run', mode: 'list', containerName }).slice(0, 8),
    ['run', '--rm', '--pull=never', '--init', '--name', containerName, '--network', 'none']);
});

test('requires the host Playwright executable before Docker or readiness checks', async () => {
  let checkedPath;
  assert.throws(() => verifyPlaywright((file, mode) => {
    checkedPath = file;
    assert.equal(mode, require('node:fs').constants.X_OK);
    throw new Error('missing');
  }), /cd e2e && npm ci/);
  assert.match(checkedPath, /e2e[\\/]node_modules[\\/]\.bin[\\/]playwright$/);

  const calls = [];
  await assert.rejects(main(['smoke'], {
    access: () => { throw new Error('missing'); },
    spawnSync: (...args) => { calls.push(args); },
    checkDemo: async () => { calls.push('readiness'); },
  }), /cd e2e && npm ci/);
  assert.deepEqual(calls, []);
});

test('verifies cached image identity without pulling', () => {
  const calls = [];
  const success = (command, args) => {
    calls.push([command, args]);
    return { status: 0, stdout: JSON.stringify([image]) };
  };
  verifyImage(image, success);
  assert.equal(calls.length, 1);
  assert.deepEqual(calls[0][1], ['image', 'inspect', image, '--format', '{{json .RepoTags}}']);
  assert.throws(() => verifyImage(image, () => ({ status: 1, stdout: '' })), /no pull attempted/);
  assert.throws(() => verifyImage(image, () => ({ status: 0, stdout: '["other"]' })), /tag mismatch/);
});

test('list mode does not make an application readiness request', () => {
  const script = require.resolve('./public-runner.cjs');
  const source = require('node:fs').readFileSync(script, 'utf8');
  assert.match(source, /if \(mode !== 'list'\) await/);
  assert.match(source, /method: 'GET'/);
  assert.match(source, /http:\/\/localhost:5173\//);
  assert.doesNotMatch(source, /process\.env\.BASE_URL/);
});

test('sets finite per-mode deadlines and stops only the invocation container on timeout', async () => {
  for (const [mode, seconds] of Object.entries({ list: 90, smoke: 180, public: 600 })) {
    const calls = [];
    const fakeRun = (command, args, options) => {
      calls.push({ command, args, options });
      if (args[0] === 'image') return { status: 0, stdout: JSON.stringify([image]) };
      if (args[0] === 'run') return { status: null, error: Object.assign(new Error('timed out'), { code: 'ETIMEDOUT' }) };
      return { status: 0 };
    };
    const oldExitCode = process.exitCode;
    process.exitCode = 0;
    await main([mode], {
      spawnSync: fakeRun,
      access: () => {},
      checkDemo: async () => {},
      createArtifacts: () => '/private/run',
      randomBytes: () => Buffer.from('0123456789abcdef01234567', 'hex'),
    });
    assert.equal(calls[1].options.timeout, seconds * 1000);
    assert.match(calls[1].args[calls[1].args.indexOf('--name') + 1], /^tuhospedaje-public-[0-9a-f]{24}$/);
    const runName = calls[1].args[calls[1].args.indexOf('--name') + 1];
    assert.deepEqual(calls[2].args, ['stop', '--time', '5', runName]);
    assert.equal(calls[2].options.timeout, 10000);
    process.exitCode = oldExitCode;
  }
});

test('does not stop a container for non-timeout failure and reports stop uncertainty', async () => {
  for (const [runResult, stopResult, expectedStopCalls] of [
    [{ status: 17 }, { status: 0 }, 0],
    [{ status: null, error: Object.assign(new Error('timed out'), { code: 'ETIMEDOUT' }) }, { status: 1 }, 1],
  ]) {
    const calls = [];
    const oldExitCode = process.exitCode;
    process.exitCode = 0;
    await main(['list'], {
      access: () => {},
      spawnSync: (_command, args) => {
        calls.push(args);
        if (args[0] === 'image') return { status: 0, stdout: JSON.stringify([image]) };
        return args[0] === 'run' ? runResult : stopResult;
      },
      createArtifacts: () => '/private/run',
      randomBytes: () => Buffer.from('0123456789abcdef01234567', 'hex'),
    });
    assert.equal(calls.filter((args) => args[0] === 'stop').length, expectedStopCalls);
    assert.equal(process.exitCode, runResult.status === 17 ? 17 : 1);
    process.exitCode = oldExitCode;
  }
});

test('mode guard runs without invoking Docker for invalid arguments', () => {
  const result = spawnSync(process.execPath, [require.resolve('./public-runner.cjs'), 'public', '--project=chromium'], { encoding: 'utf8' });
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Usage:/);
});
