"""Test CI workflow hardening rules (least privilege permissions, SHA-pinned actions, push retries, tags)."""

import json
import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CI_WORKFLOW = ROOT / ".github/workflows/ci.yml"
E2E_PACKAGE_JSON = ROOT / "e2e/package.json"
PLAYWRIGHT_CONFIG = ROOT / "e2e/playwright.config.js"


class CiWorkflowHardeningTest(unittest.TestCase):

    def setUp(self):
        self.content = CI_WORKFLOW.read_text(encoding="utf-8")

    def test_root_permissions_enforces_contents_read(self):
        # Must declare top-level permissions: contents: read
        # Before any job definition
        header = self.content.split("jobs:", 1)[0]
        self.assertIn("permissions:", header)
        self.assertRegex(header, r"permissions:\s*\n\s+contents:\s*read")

    def test_all_external_actions_are_pinned_to_40_char_shas(self):
        # Find all uses: lines
        uses_lines = re.findall(r"uses:\s*([^\s]+)", self.content)
        self.assertTrue(uses_lines, "CI workflow must have uses: entries")

        sha_pattern = re.compile(r"^[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+@[a-f0-9]{40}$")
        unpinned = [action for action in uses_lines if not sha_pattern.match(action)]

        self.assertEqual(
            unpinned,
            [],
            f"All external actions must be pinned to full 40-character SHAs with version comments, but found: {unpinned}",
        )

    def test_triggers_include_version_tags_and_remove_stale_branches(self):
        header = self.content.split("jobs:", 1)[0]
        self.assertNotIn("sprint-4", header, "Stale branch sprint-4 should not be in push triggers")
        self.assertIn("tags:", header, "CI must trigger on release tags")
        self.assertRegex(header, r"tags:\s*\n\s+-\s+['\"]?v\*['\"]?")

    def test_docker_push_includes_retry_mechanism(self):
        containers_job = self.content.split("containers:", 1)[1]
        self.assertIn("docker push", containers_job)
        # Verify retry loop is present for docker push
        self.assertRegex(containers_job, r"for\s+attempt\s+in|retry|for\s+_\s+in\s+\$\(seq", "docker push must have retry logic")

    def test_ci_workflow_uses_node_24(self):
        # Must configure Node 24 and leave Node 20 behind
        node_versions = re.findall(r"node-version:\s*['\"]?([^\s'\"]+)['\"]?", self.content)
        self.assertTrue(node_versions, "Must have setup-node node-version configured")
        for v in node_versions:
            self.assertTrue(
                v == "24" or v.startswith("24."),
                f"CI setup-node must use Node 24 LTS, but found: {v}",
            )
        self.assertNotIn("20.19", self.content, "Node 20.19 must not be referenced in CI workflow")

    def test_frontend_dockerfile_uses_node_24(self):
        dockerfile = ROOT / "frontend/Dockerfile"
        content = dockerfile.read_text(encoding="utf-8")
        self.assertIn("node:24-alpine", content, "frontend/Dockerfile must use node:24-alpine")
        self.assertNotIn("node:20", content, "frontend/Dockerfile must not reference node:20")

    def test_ci_actions_use_node_24_compatible_releases(self):
        # Actions must be pinned to Node 24 runtime SHAs
        expected_pins = {
            "actions/checkout": "3d3c42e5aac5ba805825da76410c181273ba90b1",
            "actions/setup-node": "949feb2413d6458794dcd2491c4babbbce0c15c1",
            "actions/setup-java": "77dd5b2ce225ea11469928ac317850245a74ae78",
            "actions/upload-artifact": "cf430e030ddbb5b0abf93d22962f4752f3646cd9",
            "docker/setup-buildx-action": "f87e5991a6d7451dcb8d9637bfbc97413f497069",
            "docker/build-push-action": "c3c9e263c25d99ce0380d002d59b67737d91b0dc",
            "docker/login-action": "8aa5831fb8d48d8f76594c7ddde61ae58019f550",
        }
        for action, expected_sha in expected_pins.items():
            pattern = re.compile(rf"uses:\s+{re.escape(action)}@([a-f0-9]{{40}})")
            matches = pattern.findall(self.content)
            self.assertTrue(matches, f"Action {action} must be present in CI workflow")
            for found_sha in matches:
                self.assertEqual(
                    found_sha,
                    expected_sha,
                    f"Action {action} must be pinned to Node 24 release SHA {expected_sha}, but found {found_sha}",
                )


class PlaywrightCiAntiDriftTest(unittest.TestCase):

    def setUp(self):
        self.ci_content = CI_WORKFLOW.read_text(encoding="utf-8")
        self.e2e_pkg = json.loads(E2E_PACKAGE_JSON.read_text(encoding="utf-8"))
        self.playwright_config = PLAYWRIGHT_CONFIG.read_text(encoding="utf-8")
        self.e2e_job = self.ci_content.split("\n  e2e:\n", 1)[1].split("\n  containers:\n", 1)[0]

    def test_playwright_package_uses_exact_semver_pin(self):
        pin = self.e2e_pkg.get("devDependencies", {}).get("@playwright/test")
        self.assertIsInstance(pin, str, "@playwright/test must be declared in e2e/package.json devDependencies")
        self.assertRegex(
            pin,
            r"^\d+\.\d+\.\d+$",
            f"@playwright/test must be pinned to an exact 3-part semver without ^ or ~, found: {pin!r}",
        )

    def test_ci_uses_digest_pinned_playwright_noble_image_matching_package_json(self):
        pin = self.e2e_pkg["devDependencies"]["@playwright/test"]
        self.assertNotIn(
            "playwright install --with-deps",
            self.ci_content,
            "CI must use the prebuilt Playwright container image instead of playwright install --with-deps",
        )

        image_refs = re.findall(r"mcr\.microsoft\.com/playwright:[^\s\"']+", self.ci_content)
        self.assertTrue(
            image_refs,
            "CI workflow must reference a digest-pinned mcr.microsoft.com/playwright Noble image",
        )
        expected_pattern = re.compile(
            rf"^mcr\.microsoft\.com/playwright:v{re.escape(pin)}-noble@sha256:[a-f0-9]{{64}}$"
        )
        for ref in image_refs:
            self.assertRegex(
                ref,
                expected_pattern,
                f"Playwright CI image must match exact e2e/package.json pin v{pin}-noble@sha256:<64-hex>, found: {ref}",
            )

        self.assertIn("docker pull", self.e2e_job, "e2e job must pull the Playwright container image explicitly")
        self.assertIn("docker run", self.e2e_job, "e2e job must run Playwright inside the container image")

    def test_ci_matrix_and_package_scripts_projects_are_declared_in_playwright_config(self):
        declared_projects = set(
            re.findall(r"name:\s*['\"]([a-zA-Z0-9_-]+)['\"]", self.playwright_config)
        )
        self.assertTrue(declared_projects, "e2e/playwright.config.js must declare at least one project")

        scripts = self.e2e_pkg.get("scripts", {})
        script_projects = []
        for script_cmd in scripts.values():
            script_projects.extend(re.findall(r"--project=([a-zA-Z0-9_-]+)", script_cmd))
        self.assertTrue(script_projects, "e2e/package.json scripts must reference --project=<name>")
        for project in script_projects:
            self.assertIn(
                project,
                declared_projects,
                f"e2e/package.json script references undeclared Playwright project: {project}",
            )

        matrix_match = re.search(r"browser:\s*\[([^\]]+)\]", self.e2e_job)
        self.assertIsNotNone(matrix_match, "e2e job must define strategy.matrix.browser")
        assert matrix_match is not None
        matrix_browsers = [
            item.strip().strip("'\"")
            for item in matrix_match.group(1).split(",")
            if item.strip()
        ]
        expected_browsers = {"chromium", "firefox", "mobile-chromium", "mobile-webkit"}
        self.assertTrue(
            expected_browsers.issubset(set(matrix_browsers)),
            f"e2e matrix must include {sorted(expected_browsers)}, found: {matrix_browsers}",
        )
        for browser in matrix_browsers:
            self.assertIn(
                browser,
                declared_projects,
                f"CI e2e matrix browser {browser!r} is not declared in e2e/playwright.config.js",
            )

    def test_ci_e2e_configures_webkit_retry_summary_and_artifacts(self):
        self.assertIn("E2E_ENABLE_WEBKIT", self.e2e_job)
        self.assertIn("GITHUB_STEP_SUMMARY", self.e2e_job)
        self.assertIn("RECOVERED_PASS", self.e2e_job)
        self.assertIn("E2E_PASS_FIRST_ATTEMPT", self.e2e_job)
        self.assertIn("FINAL_E2E_FAILURE", self.e2e_job)
        self.assertIn("e2e/playwright-report/", self.e2e_job)
        self.assertIn("e2e/test-results/", self.e2e_job)
        self.assertIn("node --test e2e/scripts/run-e2e.test.cjs", self.ci_content)

    def test_playwright_config_hardening_options(self):
        self.assertRegex(
            self.playwright_config,
            r"retries:\s*process\.env\.CI\s*\?\s*1\s*:\s*0",
            "e2e/playwright.config.js must configure retries: process.env.CI ? 1 : 0",
        )
        self.assertRegex(
            self.playwright_config,
            r"trace:\s*['\"]retain-on-failure['\"]",
            "e2e/playwright.config.js must configure trace: 'retain-on-failure'",
        )
        self.assertRegex(
            self.playwright_config,
            r"open:\s*['\"]never['\"]",
            "e2e/playwright.config.js must configure HTML reporter with open: 'never'",
        )
        self.assertIn(
            "test-results/results.json",
            self.playwright_config,
            "e2e/playwright.config.js must configure JSON reporter outputFile test-results/results.json",
        )


if __name__ == "__main__":
    unittest.main()
