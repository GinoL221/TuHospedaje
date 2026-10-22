"""Validate local persistence without secrets, containers, or database access."""
import json
import os
import re
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class LocalPersistentConfigTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        env = {"PATH": os.environ["PATH"], "HOME": os.environ["HOME"],
               "LOCAL_DB_PASSWORD": "fictional-db", "LOCAL_DB_ROOT_PASSWORD": "fictional-root",
               "LOCAL_JWT_SECRET": "fictional-jwt", "LOCAL_SESSION_REFRESH_KEY": "fictional-refresh",
               "CANONICAL_ASSETS_HOST_DIR": str(ROOT / "test-fixture-canonical-assets")}
        result = subprocess.run(
            ["docker", "compose", "--env-file", "/dev/null", "-f", "compose.local.yaml",
             "config", "--format", "json"], cwd=ROOT, env=env,
            capture_output=True, text=True, timeout=10)
        if result.returncode:
            raise AssertionError("Local Compose config failed: " + result.stderr)
        cls.config = json.loads(result.stdout)

    def test_isolated_persistent_database(self):
        self.assertEqual(self.config["name"], "tuhospedaje-local")
        self.assertEqual(self.config["volumes"]["db-data"]["name"], "tuhospedaje-local-db")
        db = self.config["services"]["db"]
        self.assertFalse(db.get("ports"))
        self.assertEqual(db["environment"]["MARIADB_DATABASE"], "tuhospedaje_local_dev")
        for service in self.config["services"].values():
            self.assertEqual(service["restart"], "unless-stopped")

    def test_loopback_and_stable_browser_endpoints(self):
        services = self.config["services"]
        for name, port in (("frontend", "5173"), ("backend", "8080")):
            self.assertEqual(len(services[name]["ports"]), 1)
            binding = services[name]["ports"][0]
            self.assertEqual(binding["host_ip"], "127.0.0.1")
            self.assertEqual(binding["published"], port)
        backend = services["backend"]["environment"]
        self.assertEqual(backend["CORS_ALLOWED_ORIGINS"], "http://127.0.0.1:5173,http://localhost:5173")
        # This directly overrides app.cors.allowed-origins in the dev profile.
        self.assertEqual(backend["APP_CORS_ALLOWED_ORIGINS"], "http://127.0.0.1:5173,http://localhost:5173")
        self.assertEqual(services["frontend"]["environment"]["VITE_API_URL"], "http://127.0.0.1:8080/api")

    def test_seed_and_masters_only_readonly_mounts(self):
        backend = self.config["services"]["backend"]
        self.assertEqual(backend["environment"]["SPRING_FLYWAY_LOCATIONS"],
                         "classpath:db/migration,classpath:db/dev")
        self.assertNotIn("DEV_ADMIN_PASSWORD_HASH", backend["environment"])
        self.assertEqual(backend["entrypoint"], ["/bin/sh", "/workspace/docker/seed-entrypoint.sh"])
        mounts = backend["volumes"]
        seed = [m for m in mounts if m["target"] == "/run/dev-admin.env"]
        self.assertEqual(len(seed), 1)
        self.assertTrue(seed[0]["read_only"])
        self.assertEqual(Path(seed[0]["source"]), ROOT / "deploy/local-admin.env")
        assets = [m for m in mounts if m["target"].startswith("/canonical/")]
        sql = (ROOT / "backend/src/main/resources/db/dev/V1_9000__dev_demo_data.sql").read_text()
        folders = set(re.findall(r"/canonical-lodging-images/(lodging-[^/']+)/masters/", sql))
        self.assertEqual(len(assets), 38)
        self.assertEqual({m["target"] for m in assets}, {f"/canonical/{f}/masters" for f in folders})
        for mount in assets:
            self.assertTrue(mount["read_only"])
            self.assertFalse(mount.get("bind", {}).get("create_host_path", False))
            self.assertEqual(Path(mount["source"]), ROOT / "test-fixture-canonical-assets" / mount["target"].removeprefix("/canonical/"))


if __name__ == "__main__":
    unittest.main()
