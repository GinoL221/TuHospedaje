"""Validate Docker Compose files, overlay configs, and environment examples without starting containers."""

import base64
import json
import os
import re
import subprocess
import unittest
from pathlib import Path


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
        for service_name in ("db", "backend", "frontend"):
            self.assertIn(service_name, data["services"])
            ports = data["services"][service_name]["ports"]
            self.assertEqual(len(ports), 1)
            self.assertEqual(
                ports[0].get("host_ip"),
                "127.0.0.1",
                f"{service_name} port must bind to 127.0.0.1, got {ports[0]}",
            )

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

    def test_frontend_nginx_includes_security_headers_and_csp(self):
        conf_path = ROOT / "frontend/docker/nginx/default.conf"
        self.assertTrue(conf_path.exists(), "frontend/docker/nginx/default.conf must exist")
        content = conf_path.read_text(encoding="utf-8")

        # Must include clickjacking, MIME sniffing and referrer protection
        self.assertIn('add_header X-Frame-Options "DENY" always;', content)
        self.assertIn('add_header X-Content-Type-Options "nosniff" always;', content)
        self.assertIn('add_header Referrer-Policy "strict-origin-when-cross-origin" always;', content)

        # Must include CSP with img-src allowing https: for lodging/category images
        self.assertIn("Content-Security-Policy", content)
        self.assertIn("default-src 'self'", content)
        self.assertIn("script-src 'self'", content)
        self.assertIn("style-src 'self' 'unsafe-inline'", content)
        self.assertIn("img-src 'self' data: blob: https:", content)
        self.assertIn("frame-ancestors 'none'", content)

        # Nginx does not inherit parent headers when a location defines its own add_header;
        # location = /index.html defines Cache-Control, so it must also repeat security headers.
        index_location = content.split("location = /index.html", 1)[1].split("}", 1)[0]
        self.assertIn('add_header X-Frame-Options "DENY" always;', index_location)
        self.assertIn('add_header X-Content-Type-Options "nosniff" always;', index_location)
        self.assertIn('add_header Referrer-Policy "strict-origin-when-cross-origin" always;', index_location)
        self.assertIn("Content-Security-Policy", index_location)

        # CSP in server block and in location = /index.html must match exactly
        csp_pattern = re.compile(r'add_header Content-Security-Policy "(.*?)" always;')
        server_block = content.split("location", 1)[0]
        server_csp = csp_pattern.search(server_block)
        index_csp = csp_pattern.search(index_location)
        self.assertIsNotNone(server_csp, "Server block must define Content-Security-Policy")
        self.assertIsNotNone(index_csp, "location = /index.html must define Content-Security-Policy")
        assert server_csp is not None and index_csp is not None
        self.assertEqual(server_csp.group(1), index_csp.group(1), "CSP policy must be identical in server and index.html")

        # Hashed assets location must also repeat security headers
        assets_match = re.search(r'location ~\* "(\^[^"]+)" \{(.*?)\}', content, re.DOTALL)
        self.assertIsNotNone(assets_match, "hashed assets location not found")
        assert assets_match is not None
        assets_location = assets_match.group(2)
        self.assertIn('add_header X-Frame-Options "DENY" always;', assets_location)
        self.assertIn('add_header X-Content-Type-Options "nosniff" always;', assets_location)
        self.assertIn('add_header Referrer-Policy "strict-origin-when-cross-origin" always;', assets_location)

    def test_hashed_assets_location_matches_real_request_uris(self):
        content = (ROOT / "frontend/docker/nginx/default.conf").read_text()
        match = re.search(r'location ~\* "(\^[^"]+)" \{(.*?)\}', content, re.DOTALL)
        self.assertIsNotNone(match, "hashed assets location not found")
        assert match is not None
        pattern = re.compile(match.group(1), re.IGNORECASE)
        # nginx matches the URI, which always starts with "/"
        for uri in ("/assets/Admin-aM67yFyX.css", "/assets/index-DKc9aB3x.js",
                    "/assets/vendor-react-Bq1_x-aZ.js", "/assets/hero-1a2B3c4D.webp"):
            self.assertRegex(uri, pattern, uri)
        for uri in ("/index.html", "/assets/app.js", "/login", "/uploads/photo-abcdefgh.png",
                    "/api/lodgings", "/assets/sub/dir-abcdefgh.js"):
            self.assertNotRegex(uri, pattern, uri)
        self.assertIn("immutable", match.group(2))

    def test_obsolete_ops_nginx_does_not_exist(self):
        self.assertFalse((ROOT / "ops/nginx").exists(), "ops/nginx is obsolete and must be removed")


if __name__ == "__main__":
    unittest.main()
