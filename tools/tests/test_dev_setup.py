"""Tests for scripts/dev-setup.sh via its public CLI interface."""

import base64
import os
import re
import secrets
import stat
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts/dev-setup.sh"

BCRYPT_ALPHABET = "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
BCRYPT_RE = re.compile(r"^\$2[aby]\$1[0-4]\$[./A-Za-z0-9]{53}$")
DEV_NAME_RE = re.compile(r"(^|_)(dev|test)(_|$)")
NON_DEV_NAME_RE = re.compile(r"(^|_)(prod|production|stage|staging|uat)(_|$)")

EXPECTED_SUCCESS_STDOUT = (
    "Created deploy/dev.env\n"
    "Demo admin: admin@tuhospedaje.com / Admin1234\n"
    "Demo guest: huesped@tuhospedaje.com / Huesped1234\n"
)
EXPECTED_EXISTS_STDERR = "deploy/dev.env already exists; rerun with --force to replace it\n"
EXPECTED_HASH_FAIL_STDERR = (
    "could not generate a bcrypt hash: install htpasswd (apache2-utils) or start Docker\n"
)
EXPECTED_USAGE_STDERR = "usage: scripts/dev-setup.sh [--force]\n"

REQUIRED_KEYS = (
    "DEV_DB_NAME",
    "DEV_DB_USERNAME",
    "DEV_DB_PASSWORD",
    "DEV_DB_ROOT_PASSWORD",
    "DEV_JWT_SECRET",
    "DEV_SESSION_REFRESH_KEY",
    "DEV_ADMIN_PASSWORD_HASH",
    "DEV_GUEST_PASSWORD_HASH",
)


def _make_fake_htpasswd(bin_dir: Path, body: str) -> None:
    htpasswd_path = bin_dir / "htpasswd"
    htpasswd_path.write_text(f"#!/usr/bin/env bash\n{body}\n", encoding="utf-8")
    htpasswd_path.chmod(0o755)


def _random_bcrypt_line() -> str:
    salt_and_hash = "".join(secrets.choice(BCRYPT_ALPHABET) for _ in range(53))
    return f":$2y$10${salt_and_hash}"


def _parse_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        key, sep, value = stripped.partition("=")
        if sep:
            values[key] = value
    return values


class DevSetupTest(unittest.TestCase):

    def _run_script(
        self,
        target: Path,
        bin_dir: Path,
        *args: str,
    ) -> subprocess.CompletedProcess[str]:
        env = {
            "PATH": f"{bin_dir}:{os.environ['PATH']}",
            "DEV_SETUP_TARGET": str(target),
        }
        return subprocess.run(
            ["bash", str(SCRIPT), *args],
            cwd=ROOT,
            env=env,
            capture_output=True,
            text=True,
            timeout=10,
        )

    def test_success_generates_valid_env_and_rotates_secrets(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            bin_dir = tmp_path / "bin"
            bin_dir.mkdir()
            hash_a = _random_bcrypt_line()
            hash_b = _random_bcrypt_line()
            _make_fake_htpasswd(
                bin_dir,
                f'if [ "${{@: -1}}" = "Admin1234" ]; then printf "%s\\n" \'{hash_a}\'; '
                f'else printf "%s\\n" \'{hash_b}\'; fi',
            )
            target = tmp_path / "dev.env"

            first = self._run_script(target, bin_dir)
            self.assertEqual(first.returncode, 0, first.stderr)
            self.assertEqual(first.stdout, EXPECTED_SUCCESS_STDOUT)
            self.assertEqual(first.stderr, "")
            self.assertTrue(target.exists())
            self.assertEqual(stat.S_IMODE(target.stat().st_mode), 0o600)

            env_first = _parse_env_file(target)
            for key in REQUIRED_KEYS:
                self.assertIn(key, env_first)
                self.assertTrue(env_first[key], f"{key} must be non-empty")

            for secret_key in ("DEV_JWT_SECRET", "DEV_SESSION_REFRESH_KEY"):
                decoded = base64.b64decode(env_first[secret_key], validate=True)
                self.assertGreaterEqual(len(decoded), 32, f"{secret_key} must decode to >=32 bytes")

            self.assertRegex(env_first["DEV_DB_NAME"], DEV_NAME_RE)
            self.assertNotRegex(env_first["DEV_DB_NAME"], NON_DEV_NAME_RE)
            # Hashes are single-quoted so Compose does not interpolate their '$' signs;
            # seed-entrypoint.sh strips one pair of surrounding single quotes.
            for hash_key in ("DEV_ADMIN_PASSWORD_HASH", "DEV_GUEST_PASSWORD_HASH"):
                value = env_first[hash_key]
                self.assertTrue(value.startswith("'") and value.endswith("'"), value)
                self.assertRegex(value[1:-1], BCRYPT_RE)

            second = self._run_script(target, bin_dir, "--force")
            self.assertEqual(second.returncode, 0, second.stderr)
            self.assertEqual(second.stdout, EXPECTED_SUCCESS_STDOUT)
            self.assertEqual(second.stderr, "")

            env_second = _parse_env_file(target)
            for key in (
                "DEV_DB_PASSWORD",
                "DEV_DB_ROOT_PASSWORD",
                "DEV_JWT_SECRET",
                "DEV_SESSION_REFRESH_KEY",
            ):
                self.assertNotEqual(env_first[key], env_second[key], f"{key} must differ across runs")

    def test_existing_target_without_force_fails_and_preserves_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            bin_dir = tmp_path / "bin"
            bin_dir.mkdir()
            _make_fake_htpasswd(bin_dir, f'printf "%s\\n" \'{_random_bcrypt_line()}\'')
            target = tmp_path / "dev.env"
            original_content = "KEEP_ME=1\n"
            target.write_text(original_content, encoding="utf-8")

            result = self._run_script(target, bin_dir)
            self.assertEqual(result.returncode, 1)
            self.assertEqual(result.stdout, "")
            self.assertEqual(result.stderr, EXPECTED_EXISTS_STDERR)
            self.assertEqual(target.read_text(encoding="utf-8"), original_content)

    def test_force_replaces_existing_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            bin_dir = tmp_path / "bin"
            bin_dir.mkdir()
            _make_fake_htpasswd(bin_dir, f'printf "%s\\n" \'{_random_bcrypt_line()}\'')
            target = tmp_path / "dev.env"
            original_content = "OLD_CONTENT=1\n"
            target.write_text(original_content, encoding="utf-8")

            result = self._run_script(target, bin_dir, "--force")
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(result.stdout, EXPECTED_SUCCESS_STDOUT)
            self.assertEqual(result.stderr, "")
            new_content = target.read_text(encoding="utf-8")
            self.assertNotEqual(new_content, original_content)
            self.assertIn("DEV_ADMIN_PASSWORD_HASH=", new_content)

    def test_hashing_failure_garbage_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            bin_dir = tmp_path / "bin"
            bin_dir.mkdir()
            _make_fake_htpasswd(bin_dir, 'printf "not-a-bcrypt-hash\\n"')
            target = tmp_path / "dev.env"

            result = self._run_script(target, bin_dir)
            self.assertEqual(result.returncode, 1)
            self.assertEqual(result.stdout, "")
            self.assertEqual(result.stderr, EXPECTED_HASH_FAIL_STDERR)
            self.assertFalse(target.exists())
            self.assertEqual(list(tmp_path.glob(".dev.env.*")), [])

    def test_hashing_failure_nonzero_exit(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            bin_dir = tmp_path / "bin"
            bin_dir.mkdir()
            _make_fake_htpasswd(bin_dir, "exit 1")
            target = tmp_path / "dev.env"

            result = self._run_script(target, bin_dir)
            self.assertEqual(result.returncode, 1)
            self.assertEqual(result.stdout, "")
            self.assertEqual(result.stderr, EXPECTED_HASH_FAIL_STDERR)
            self.assertFalse(target.exists())
            self.assertEqual(list(tmp_path.glob(".dev.env.*")), [])

    def test_invalid_arguments_exit_with_usage(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            bin_dir = tmp_path / "bin"
            bin_dir.mkdir()
            _make_fake_htpasswd(bin_dir, f'printf "%s\\n" \'{_random_bcrypt_line()}\'')
            target = tmp_path / "dev.env"

            for args in (("--unknown",), ("--force", "--extra")):
                with self.subTest(args=args):
                    result = self._run_script(target, bin_dir, *args)
                    self.assertEqual(result.returncode, 2)
                    self.assertEqual(result.stdout, "")
                    self.assertEqual(result.stderr, EXPECTED_USAGE_STDERR)
                    self.assertFalse(target.exists())

    def test_compose_accepts_generated_env_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            bin_dir = tmp_path / "bin"
            bin_dir.mkdir()
            _make_fake_htpasswd(bin_dir, f'printf "%s\\n" \'{_random_bcrypt_line()}\'')
            target = tmp_path / "dev.env"

            gen_result = self._run_script(target, bin_dir)
            self.assertEqual(gen_result.returncode, 0, gen_result.stderr)

            compose_env = {
                "PATH": os.environ["PATH"],
                "COMPOSE_DISABLE_ENV_FILE": "1",
            }
            compose_result = subprocess.run(
                [
                    "docker", "compose",
                    "--env-file", str(target),
                    "-f", str(ROOT / "compose.yaml"),
                    "-f", str(ROOT / "compose.dev.yaml"),
                    "-f", str(ROOT / "compose.dev-seed.yaml"),
                    "config", "--quiet",
                ],
                cwd=ROOT,
                env=compose_env,
                capture_output=True,
                text=True,
                timeout=10,
            )
            self.assertEqual(compose_result.returncode, 0, compose_result.stderr)
            # No interpolation warnings about the '$' signs inside the bcrypt hashes.
            self.assertEqual(compose_result.stderr, "")
