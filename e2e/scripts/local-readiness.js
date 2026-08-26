const { execFile } = require('node:child_process');
const { promisify } = require('node:util');

const executeFile = promisify(execFile);
const EXPECTED_DATABASE = 'tuhospedaje_dev';
const DISPOSABLE_LABEL = 'com.tuhospedaje.disposable';

function commandRunner(command, args) {
  return executeFile(command, args, { encoding: 'utf8' }).then(({ stdout }) => stdout);
}

function createReadiness({ exec = commandRunner, fetch = global.fetch } = {}) {
  async function command(commandName, args) {
    try {
      return (await exec(commandName, args)).trim();
    } catch (error) {
      throw new Error(`Readiness command failed: ${commandName} ${args.join(' ')} (${error.message})`);
    }
  }

  async function check() {
    const containerId = await command('docker', ['compose', 'ps', '-q', 'db']);
    if (!containerId) throw new Error('Local database container is not running.');

    const health = await command('docker', ['inspect', '--format', '{{.State.Health.Status}}', containerId]);
    if (health !== 'healthy') throw new Error(`Local database health is ${health || 'unknown'}.`);

    const environment = await command('docker', ['inspect', '--format', '{{range .Config.Env}}{{println .}}{{end}}', containerId]);
    if (!environment.split('\n').includes(`MARIADB_DATABASE=${EXPECTED_DATABASE}`)) {
      throw new Error(`Local database name must be ${EXPECTED_DATABASE}.`);
    }

    const volume = await command('docker', ['inspect', '--format', '{{range .Mounts}}{{if eq .Type "volume"}}{{println .Name}}{{end}}{{end}}', containerId]);
    if (!volume) throw new Error('Local database must use a named disposable volume.');
    const [volumeDetails] = JSON.parse(await command('docker', ['volume', 'inspect', volume]));
    if (volumeDetails?.Labels?.[DISPOSABLE_LABEL] !== 'true') {
      throw new Error('Local database volume is not labeled disposable.');
    }

    const failedMigrations = await command('docker', [
      'compose', 'exec', '-T', 'db', 'sh', '-c',
      'mariadb --user="$MARIADB_USER" --password="$MARIADB_PASSWORD" "$MARIADB_DATABASE" -N -e "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0"',
    ]);
    if (failedMigrations !== '0') throw new Error('Flyway history contains failed migrations.');

    const backend = await fetch('http://localhost:8080/api/lodgings');
    if (!backend.ok) throw new Error('Local backend health check failed.');
    const frontend = await fetch('http://localhost:5173/');
    if (!frontend.ok) throw new Error('Local frontend health check failed.');
  }

  return { check };
}

async function main() {
  await createReadiness().check();
  console.log('Local E2E readiness passed. Chromium may start.');
}

module.exports = { createReadiness };

if (require.main === module) {
  main().catch((error) => {
    console.error(`Local E2E readiness failed: ${error.message}`);
    process.exitCode = 1;
  });
}
