# Run the opt-in offline self-check

Use `scripts/safetyunit --self-test` to check the Testcontainers policy hooks and Maven profile without running the application integration suite. This is a developer convenience, **not a filesystem, credential, Docker, or network sandbox**.

## Run

From the repository root, with Java 17 or newer, Bash, and a locally installed Maven:

```bash
scripts/safetyunit --self-test
```

Maven must already have all required artifacts and plugins cached. Offline resolution failures are errors; the launcher never retries online or installs dependencies.

| Setting | Default | Requirement |
| --- | --- | --- |
| `SAFETYUNIT_MAVEN` | `mvn` resolved from the caller's PATH | Absolute executable file path |
| `SAFETYUNIT_REPOSITORY` | `$HOME/.m2/repository` | Existing absolute directory |

Overrides are inputs to this opt-in command, not trust boundaries. Only run trusted Maven executables, plugins, and tests. The Maven child receives a fixed `/usr/bin:/bin` PATH; Java must be available there. The launcher accepts exactly `--self-test`; any other arguments print usage and exit 2 before creating output.

## What runs

- The `safetyunit` Maven profile selects `OfflineImagePolicyTest` and `OfflineLauncherContractTest`, fails if no tests are found, and skips JaCoCo only for that profile.
- Tests check direct policy behavior, actual hook loading through the resolved Testcontainers version, and the profile's test selection. No Spring context or container is started by these tests.
- Maven still compiles application and test sources. This command does not replace `./mvnw verify` or the existing CI/coverage gate.
- The new Java tests also participate in ordinary test discovery. The hook-loading test skips outside the launcher when its hook environment is absent; the other tests are pure checks.

## Environment and retained output

Each invocation creates a private `backend/.safetyunit-output/run.*` directory with home, temporary, build, and generated configuration paths. User/global Maven settings are explicitly replaced with an empty generated file, startup rc loading is disabled for Maven, and inherited environment variables are removed from its child process.

Outputs are ignored by Git and retained on both success and failure for inspection, including Surefire reports under `build/surefire-reports`. The launcher propagates Maven's exit status. Missing Maven/cache paths and an output-root symlink are rejected before output creation. There is no automatic cleanup or timeout; review retained paths before removing them manually.

## Limits

`-o` controls Maven dependency resolution, not arbitrary networking by plugins or test code. The cache is shared and writable. Private directories and `env -i` do not revoke the current user's access to files, credentials, descriptors, or a Docker socket. Bash startup and commands before the final Maven `exec` are not retroactively sanitized; project Maven configuration can also affect execution.

The pull policy declines image pulls and the substitutor rejects names routed through its Testcontainers hook. These hooks do not protect against direct Docker clients, other network libraries, or alternate configuration. A successful self-check establishes those narrow contracts, not a security guarantee or full backend acceptance.

## Check the launcher without Maven

```bash
python3 scripts/test_safetyunit.py
shellcheck scripts/safetyunit
```

The Python tests copy the launcher into disposable fixtures below the ignored output directory and use a fake Maven executable/cache. They check argument rejection, child environment/argv, private paths, failure propagation, and symlink rejection without touching the shared cache or invoking Docker. Fixtures are removed after each test.
