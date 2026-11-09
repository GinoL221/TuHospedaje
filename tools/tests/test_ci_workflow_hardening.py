"""Test CI workflow hardening rules (least privilege permissions, SHA-pinned actions, push retries, tags)."""

from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
CI_WORKFLOW = ROOT / ".github/workflows/ci.yml"


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



if __name__ == "__main__":
    unittest.main()
