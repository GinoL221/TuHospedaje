"""Validate repository hygiene and .gitignore rules using git check-ignore."""

import subprocess
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
GITIGNORE = ROOT / ".gitignore"


class RepoHygieneTest(unittest.TestCase):

    def is_ignored(self, path: str) -> bool:
        result = subprocess.run(
            ["git", "check-ignore", "-q", path],
            cwd=ROOT,
            capture_output=True,
        )
        return result.returncode == 0

    def test_agents_md_is_not_in_gitignore(self):
        content = GITIGNORE.read_text(encoding="utf-8")
        lines = [line.strip() for line in content.splitlines()]
        self.assertNotIn(
            "AGENTS.md",
            lines,
            "AGENTS.md is a tracked documentation file and must not appear in .gitignore",
        )

    def test_env_files_are_ignored_while_examples_remain_tracked(self):
        self.assertTrue(self.is_ignored(".env"), ".env must be ignored")
        self.assertFalse(self.is_ignored(".env.example"), ".env.example must not be ignored")
        self.assertTrue(self.is_ignored("deploy/arbitrary.env"), "deploy/*.env must be ignored")
        self.assertFalse(self.is_ignored("deploy/arbitrary.env.example"), "deploy/*.env.example must not be ignored")

    def test_coverage_and_report_artifacts_are_ignored(self):
        self.assertTrue(self.is_ignored("coverage/lcov.info"), "coverage/ must be ignored")
        self.assertTrue(self.is_ignored("playwright-report/index.html"), "playwright-report/ must be ignored")
        self.assertTrue(self.is_ignored("e2e/playwright-report/index.html"), "e2e/playwright-report/ must be ignored")


if __name__ == "__main__":
    unittest.main()
