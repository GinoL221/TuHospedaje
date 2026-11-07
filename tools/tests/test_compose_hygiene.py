"""Validate Docker Compose files, overlay configs, and environment examples without starting containers."""

import base64
import json
import os
from pathlib import Path
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[2]
TEST_DEV_SECRET = base64.b64encode(b"0" * 48).decode("ascii")


class ComposeHygieneTest(unittest.TestCase):

    def test_local_admin_env_example_exists_and_documents_bcrypt_hash(self):
        example_path = ROOT / "deploy/local-admin.env.example"
        self.assertTrue(
            example_path.exists(),
            "deploy/local-admin.env.example must exist so fresh clones know the format for the local admin seed",
        )
        content = example_path.read_text(encoding="utf-8")
        self.assertIn("DEV_ADMIN_PASSWORD_HASH", content)

    def test_dev_compose_validates_cleanly(self):
        env = {
            "PATH": os.environ["PATH"],
            "COMPOSE_DISABLE_ENV_FILE": "1",
            "DEV_DB_NAME": "tuhospedaje_dev",
            "DEV_DB_USERNAME": "dev_user",
            "DEV_DB_PASSWORD": "dev_password",
            "DEV_DB_ROOT_PASSWORD": "dev_root_password",
            "DEV_JWT_SECRET": TEST_DEV_SECRET,
            "DEV_SESSION_REFRESH_KEY": TEST_DEV_SECRET,
        }
        result = subprocess.run(
            ["docker", "compose", "--env-file", "/dev/null",
             "-f", str(ROOT / "compose.yaml"),
             "-f", str(ROOT / "compose.dev.yaml"),
             "config", "--format", "json"],
            cwd=ROOT, env=env, capture_output=True, text=True, timeout=10,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        data = json.loads(result.stdout)
        self.assertEqual(data["name"], "tuhospedaje-dev")
        self.assertIn("db", data["services"])
        self.assertIn("backend", data["services"])

    def test_dev_seed_compose_validates_cleanly(self):
        env = {
            "PATH": os.environ["PATH"],
            "COMPOSE_DISABLE_ENV_FILE": "1",
            "DEV_DB_NAME": "tuhospedaje_dev",
            "DEV_DB_USERNAME": "dev_user",
            "DEV_DB_PASSWORD": "dev_password",
            "DEV_DB_ROOT_PASSWORD": "dev_root_password",
            "DEV_JWT_SECRET": TEST_DEV_SECRET,
            "DEV_SESSION_REFRESH_KEY": TEST_DEV_SECRET,
        }
        result = subprocess.run(
            ["docker", "compose", "--env-file", "/dev/null",
             "-f", str(ROOT / "compose.yaml"),
             "-f", str(ROOT / "compose.dev.yaml"),
             "-f", str(ROOT / "compose.dev-seed.yaml"),
             "config", "--format", "json"],
            cwd=ROOT, env=env, capture_output=True, text=True, timeout=10,
        )
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_dev_assets_compose_validates_cleanly(self):
        env = {
            "PATH": os.environ["PATH"],
            "COMPOSE_DISABLE_ENV_FILE": "1",
            "DEV_DB_NAME": "tuhospedaje_dev",
            "DEV_DB_USERNAME": "dev_user",
            "DEV_DB_PASSWORD": "dev_password",
            "DEV_DB_ROOT_PASSWORD": "dev_root_password",
            "DEV_JWT_SECRET": TEST_DEV_SECRET,
            "DEV_SESSION_REFRESH_KEY": TEST_DEV_SECRET,
            "CANONICAL_ASSETS_HOST_DIR": str(ROOT / "test-fixture-canonical-assets"),
        }
        result = subprocess.run(
            ["docker", "compose", "--env-file", "/dev/null",
             "-f", str(ROOT / "compose.yaml"),
             "-f", str(ROOT / "compose.dev.yaml"),
             "-f", str(ROOT / "compose.dev-assets.yaml"),
             "config", "--format", "json"],
            cwd=ROOT, env=env, capture_output=True, text=True, timeout=10,
        )
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_env_example_files_do_not_contain_plaintext_secrets(self):
        for example_file in (ROOT / "deploy").glob("*.env.example"):
            content = example_file.read_text(encoding="utf-8")
            for line in content.splitlines():
                stripped = line.strip()
                if not stripped or stripped.startswith("#"):
                    continue
                key, _, value = stripped.partition("=")
                self.assertFalse(
                    len(value) > 20 and not value.startswith(("http", "/")),
                    f"Example file {example_file.name} appears to have a concrete secret value for {key}",
                )


if __name__ == "__main__":
    unittest.main()
