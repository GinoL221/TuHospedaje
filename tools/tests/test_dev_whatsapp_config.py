"""Render public Compose files with synthetic values; never start services."""

import json
import os
from pathlib import Path
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[2]


class DevelopmentWhatsAppConfigTest(unittest.TestCase):
    def test_fictional_contact_is_limited_to_development_frontend(self):
        # Do not inherit private values or let Compose read a local .env file.
        environment = {
            "PATH": os.environ["PATH"],
            "COMPOSE_DISABLE_ENV_FILE": "1",
            "DEV_DB_NAME": "whatsapp_config_test",
            "DEV_DB_USERNAME": "test_user",
            "DEV_DB_PASSWORD": "test-only-not-a-secret",
            "DEV_DB_ROOT_PASSWORD": "test-only-root-not-a-secret",
            "DEV_JWT_SECRET": "test-only-jwt-not-a-secret",
            "DEV_SESSION_REFRESH_KEY": "test-only-refresh-not-a-secret",
            "DEV_BACKEND_PORT": "19080",
            "DEV_FRONTEND_PORT": "15173",
        }
        completed = subprocess.run(
            [
                "docker", "compose", "--env-file", "/dev/null",
                "--project-name", "whatsapp-config-test",
                "-f", str(ROOT / "compose.yaml"),
                "-f", str(ROOT / "compose.dev.yaml"),
                "config", "--format", "json",
            ],
            cwd=ROOT, env=environment, capture_output=True, text=True, timeout=20,
        )
        self.assertEqual(completed.returncode, 0, completed.stderr)
        self.assertEqual(completed.stderr, "")
        config = json.loads(completed.stdout)
        services = config["services"]
        frontend = services["frontend"]["environment"]
        self.assertEqual(frontend.get("VITE_WHATSAPP_NUMBER"), "12025550100")
        # The existing API target and development build target must be preserved.
        self.assertEqual(frontend["VITE_API_URL"], "http://localhost:19080/api")
        self.assertEqual(services["frontend"]["build"]["target"], "dev")
        for name in ("backend", "db"):
            self.assertNotIn("VITE_WHATSAPP_NUMBER", services[name].get("environment", {}))


if __name__ == "__main__":
    unittest.main()
