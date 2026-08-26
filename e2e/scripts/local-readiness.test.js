const test = require('node:test');
const assert = require('node:assert/strict');
const { createReadiness } = require('./local-readiness');

function healthyDependencies() {
  return {
    exec: async (command, args) => {
      const key = `${command} ${args.join(' ')}`;
      if (key === 'docker compose ps -q db') return 'db-container\n';
      if (key.includes("{{.State.Health.Status}}")) return 'healthy\n';
      if (key.includes("{{range .Config.Env}}")) return 'MARIADB_DATABASE=tuhospedaje_dev\n';
      if (key.includes("{{range .Mounts}}")) return 'tuhospedaje_tuhospedaje_dev_data\n';
      if (key === 'docker volume inspect tuhospedaje_tuhospedaje_dev_data') {
        return JSON.stringify([{ Labels: { 'com.tuhospedaje.disposable': 'true' } }]);
      }
      if (key.includes('flyway_schema_history')) return '0\n';
      throw new Error(`Unexpected command: ${key}`);
    },
    fetch: async (url) => ({ ok: url === 'http://localhost:8080/api/lodgings' || url === 'http://localhost:5173/' }),
  };
}

test('accepts only a healthy disposable local stack', async () => {
  const readiness = createReadiness(healthyDependencies());

  await assert.doesNotReject(readiness.check());
});

test('passes MariaDB credentials through the container environment', async () => {
  const calls = [];
  const dependencies = healthyDependencies();
  const originalExec = dependencies.exec;
  dependencies.exec = async (command, args) => {
    calls.push({ command, args });
    return originalExec(command, args);
  };

  await createReadiness(dependencies).check();

  const migrationCheck = calls.find(({ args }) => args.join(' ').includes('flyway_schema_history'));
  assert.ok(migrationCheck);
  const commandText = migrationCheck.args.join(' ');
  assert.match(commandText, /--user="\$MARIADB_USER"/);
  assert.match(commandText, /--password="\$MARIADB_PASSWORD"/);
  assert.match(commandText, /"\$MARIADB_DATABASE"/);
  assert.equal(migrationCheck.args.some((argument) => /^-p.+/.test(argument)), false);
});

for (const [name, change, expected] of [
  ['wrong database name', (deps) => {
    deps.exec = async (command, args) => args.join(' ').includes('{{range .Config.Env}}')
      ? 'MARIADB_DATABASE=tuhospedaje\n' : healthyDependencies().exec(command, args);
  }, /database name/],
  ['missing disposable volume label', (deps) => {
    deps.exec = async (command, args) => command === 'docker' && args[0] === 'volume'
      ? JSON.stringify([{ Labels: {} }]) : healthyDependencies().exec(command, args);
  }, /disposable/],
  ['unhealthy database', (deps) => {
    deps.exec = async (command, args) => args.join(' ').includes('{{.State.Health.Status}}')
      ? 'unhealthy\n' : healthyDependencies().exec(command, args);
  }, /database health/],
  ['failed Flyway migration', (deps) => {
    deps.exec = async (command, args) => args.join(' ').includes('flyway_schema_history')
      ? '1\n' : healthyDependencies().exec(command, args);
  }, /Flyway/],
  ['unhealthy backend', (deps) => {
    deps.fetch = async (url) => ({ ok: url === 'http://localhost:5173/' });
  }, /backend/],
  ['unhealthy frontend', (deps) => {
    deps.fetch = async (url) => ({ ok: url === 'http://localhost:8080/api/lodgings' });
  }, /frontend/],
]) {
  test(`blocks Chromium when ${name}`, async () => {
    const dependencies = healthyDependencies();
    change(dependencies);

    await assert.rejects(createReadiness(dependencies).check(), expected);
  });
}
