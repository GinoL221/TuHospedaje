"""Check production configuration without starting services or reading secrets."""

import json
import os
from pathlib import Path
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[2]


class ProductionDeliveryConfigTest(unittest.TestCase):
    def render(self, **overrides):
        environment = {
            "PATH": os.environ["PATH"],
            "COMPOSE_DISABLE_ENV_FILE": "1",
            **dict.fromkeys((
                "PROD_DB_NAME", "PROD_DB_ROOT_PASSWORD", "DB_RUNTIME_USERNAME",
                "DB_RUNTIME_PASSWORD", "DB_MIGRATION_USERNAME",
                "DB_MIGRATION_PASSWORD", "JWT_SECRET", "SESSION_ACTIVE_KEY_ID",
                "SESSION_REFRESH_KEY", "CORS_ALLOWED_ORIGINS",
                "TUHOSPEDAJE_WELCOME_PUBLIC_BASE_URL",
            ), "config-test-value"),
            "BACKEND_IMAGE": "example/backend:config-test",
            "FRONTEND_IMAGE": "example/frontend:config-test",
            **overrides,
        }
        result = subprocess.run(
            ["docker", "compose", "--env-file", "/dev/null",
             "--project-name", "prod-delivery-config-test",
             "-f", str(ROOT / "compose.yaml"),
             "-f", str(ROOT / "compose.prod.yaml"),
             "config", "--format", "json"],
            cwd=ROOT, env=environment, capture_output=True, text=True, timeout=20,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stderr, "")
        return json.loads(result.stdout)["services"]

    def test_outbox_and_smtp_can_be_enabled_together(self):
        services = self.render(
            TUHOSPEDAJE_EMAIL_OUTBOX_ENABLED="true", MAIL_SMTP_ENABLED="true",
        )
        backend = services["backend"]["environment"]
        self.assertEqual(backend.get("TUHOSPEDAJE_EMAIL_OUTBOX_ENABLED"), "true")
        self.assertEqual(backend["MAIL_SMTP_ENABLED"], "true")
        properties = (ROOT / "backend/src/main/resources/application.properties").read_text()
        self.assertIn(
            "tuhospedaje.email-outbox.enabled=${TUHOSPEDAJE_EMAIL_OUTBOX_ENABLED:false}",
            properties,
        )

    def test_mail_remains_opt_in_and_production_images_remain_prebuilt(self):
        services = self.render()
        backend = services["backend"]["environment"]
        self.assertEqual(backend.get("TUHOSPEDAJE_EMAIL_OUTBOX_ENABLED"), "false")
        self.assertEqual(backend["MAIL_SMTP_ENABLED"], "false")
        for name in ("backend", "frontend"):
            self.assertNotIn("build", services[name])
            self.assertEqual(services[name]["image"], f"example/{name}:config-test")
        self.assertNotIn("VITE_WHATSAPP_NUMBER", services["frontend"].get("environment", {}))

    def test_ci_passes_public_configuration_to_frontend_build(self):
        workflow = (ROOT / ".github/workflows/ci.yml").read_text()
        build = workflow.split("- name: Build frontend production image", 1)[1].split("- name:", 1)[0]
        self.assertIn("build-args: |", build)
        self.assertIn("VITE_API_URL=/api", build)
        self.assertIn("VITE_WHATSAPP_NUMBER=${{ vars.VITE_WHATSAPP_NUMBER }}", build)
        self.assertIn("push: false", build)
        dockerfile = (ROOT / "frontend/Dockerfile").read_text()
        self.assertIn("ARG VITE_API_URL=/api", dockerfile)
        self.assertIn("ARG VITE_WHATSAPP_NUMBER=", dockerfile)

    def test_packaged_jar_name_does_not_depend_on_project_version(self):
        pom = (ROOT / "backend/pom.xml").read_text()
        self.assertIn("<finalName>app</finalName>", pom)
        dockerfile = (ROOT / "backend/Dockerfile").read_text()
        self.assertIn("cp target/app.jar /workspace/app.jar", dockerfile)
        self.assertIn('ENTRYPOINT ["java", "-jar", "/app/app.jar"]', dockerfile)
        self.assertNotIn("backend-0.0.1-SNAPSHOT.jar", dockerfile)

    def test_optional_production_overrides_are_passed_to_backend(self):
        # 1. Unset case preserves defaults
        default_services = self.render()
        default_backend = default_services["backend"]["environment"]
        self.assertEqual(default_backend.get("APP_JWT_EXPIRATION"), "900000")
        self.assertEqual(default_backend.get("SPRINGDOC_API_DOCS_ENABLED"), "false")
        self.assertEqual(default_backend.get("SPRINGDOC_SWAGGER_UI_ENABLED"), "false")
        self.assertNotIn("TOMCAT_INTERNAL_PROXIES", default_backend)

        # 2. Overridden case passes explicit values
        services = self.render(
            APP_JWT_EXPIRATION="1800000",
            SPRINGDOC_API_DOCS_ENABLED="true",
            SPRINGDOC_SWAGGER_UI_ENABLED="true",
        )
        backend = services["backend"]["environment"]
        self.assertEqual(backend.get("APP_JWT_EXPIRATION"), "1800000")
        self.assertEqual(backend.get("SPRINGDOC_API_DOCS_ENABLED"), "true")
        self.assertEqual(backend.get("SPRINGDOC_SWAGGER_UI_ENABLED"), "true")

        prod_properties = (ROOT / "backend/src/main/resources/application-prod.properties").read_text()
        self.assertIn("app.jwt.expiration=${APP_JWT_EXPIRATION:900000}", prod_properties)
        self.assertIn("server.tomcat.remoteip.internal-proxies=${TOMCAT_INTERNAL_PROXIES:", prod_properties)
        self.assertIn("springdoc.api-docs.enabled=${SPRINGDOC_API_DOCS_ENABLED:false}", prod_properties)
        self.assertIn("springdoc.swagger-ui.enabled=${SPRINGDOC_SWAGGER_UI_ENABLED:false}", prod_properties)


if __name__ == "__main__":
    unittest.main()
