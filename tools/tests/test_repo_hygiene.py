"""Validate repository hygiene and .gitignore rules using git check-ignore."""

import json
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

    def test_local_worktrees_and_backups_are_ignored_by_gitignore_itself(self):
        # git check-ignore also honours .git/info/exclude, which is local to one clone,
        # so the committed .gitignore must list these paths on its own.
        lines = {line.strip() for line in GITIGNORE.read_text(encoding="utf-8").splitlines()}
        self.assertIn("/.worktrees/", lines)
        self.assertIn("/backups/", lines)

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

    def test_project_versions_are_aligned_to_release(self):
        # backend pom.xml
        pom = (ROOT / "backend/pom.xml").read_text(encoding="utf-8")
        self.assertRegex(pom, r"<artifactId>backend</artifactId>\s*<version>1\.0\.1</version>")

        # frontend package.json
        frontend_pkg = json.loads((ROOT / "frontend/package.json").read_text(encoding="utf-8"))
        self.assertEqual(frontend_pkg.get("version"), "1.0.1")

        # e2e package.json
        e2e_pkg = json.loads((ROOT / "e2e/package.json").read_text(encoding="utf-8"))
        self.assertEqual(e2e_pkg.get("version"), "1.0.1")

        # OpenApiConfig
        openapi = (ROOT / "backend/src/main/java/com/tuhospedaje/openapi/OpenApiConfig.java").read_text(encoding="utf-8")
        self.assertIn('.version("1.0.1")', openapi)


if __name__ == "__main__":
    unittest.main()
