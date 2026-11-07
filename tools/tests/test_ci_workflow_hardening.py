"""Test CI workflow hardening rules (least privilege permissions, SHA-pinned actions, push retries, tags)."""

import re
import unittest
from pathlib import Path


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


if __name__ == "__main__":
    unittest.main()
