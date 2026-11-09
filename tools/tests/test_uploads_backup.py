"""Validate uploads volume backup script and operational runbook."""

import os
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "ops/uploads/backup.sh"
README = ROOT / "ops/uploads/README.md"


class UploadsBackupTest(unittest.TestCase):

    def test_backup_script_exists_and_is_executable(self):
        self.assertTrue(SCRIPT.is_file(), f"{SCRIPT} must exist")
        self.assertTrue(os.access(SCRIPT, os.X_OK), f"{SCRIPT} must be executable")

    def test_help_flag_displays_usage_and_returns_zero(self):
        result = subprocess.run([str(SCRIPT), "--help"], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0)
        self.assertIn("Usage:", result.stdout)
        self.assertIn("--source-dir", result.stdout)
        self.assertIn("--destination-uri", result.stdout)
        self.assertIn("--kms-key-id", result.stdout)

    def test_missing_arguments_fails_with_code_one(self):
        result = subprocess.run([str(SCRIPT)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)
        self.assertIn("backup failed", result.stderr)

    def test_nonexistent_source_directory_fails(self):
        result = subprocess.run([
            str(SCRIPT),
            "--source-dir", "/nonexistent/directory/test",
            "--destination-uri", "s3://my-bucket/uploads",
            "--kms-key-id", "arn:aws:kms:region:account:key/id",
        ], capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)
        self.assertIn("source directory must exist", result.stderr)

    def test_unsafe_arguments_are_rejected(self):
        result = subprocess.run([
            str(SCRIPT),
            "--source-dir", str(ROOT / "ops"),
            "--destination-uri", "invalid://destination",
            "--kms-key-id", "safe-key-id",
        ], capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)
        self.assertIn("destination must be a safe s3://", result.stderr)

    def test_readme_documents_prod_volume_and_rpo(self):
        self.assertTrue(README.is_file(), f"{README} must exist")
        content = README.read_text(encoding="utf-8")
        self.assertIn("tuhospedaje-prod-uploads", content)
        self.assertIn("RPO", content)
        self.assertIn("RTO", content)
        self.assertIn("backup.sh", content)

    def test_gitignore_ignores_runtime_uploads_but_keeps_ops_uploads_tracked(self):
        def is_ignored(path: str) -> bool:
            return subprocess.run(
                ["git", "check-ignore", "-q", path], cwd=ROOT, capture_output=True
            ).returncode == 0

        self.assertTrue(is_ignored("uploads/image.jpg"), "Root uploads/ must be ignored")
        self.assertTrue(is_ignored("backend/uploads/image.jpg"), "backend/uploads/ must be ignored")
        self.assertFalse(is_ignored("ops/uploads/backup.sh"), "ops/uploads/backup.sh must NOT be ignored")
        self.assertFalse(is_ignored("ops/uploads/README.md"), "ops/uploads/README.md must NOT be ignored")

    def test_backup_creates_archive_and_uploads_with_mocked_aws(self):
        import tempfile
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            source_dir = temp_path / "source"
            source_dir.mkdir()
            (source_dir / "img1.jpg").write_text("image-1-data", encoding="utf-8")
            (source_dir / "img2.png").write_text("image-2-data", encoding="utf-8")

            bin_dir = temp_path / "bin"
            bin_dir.mkdir()
            mock_aws = bin_dir / "aws"
            log_file = temp_path / "aws_calls.log"
            mock_aws.write_text(f'#!/usr/bin/env bash\necho "$@" >> "{log_file}"\nexit 0\n', encoding="utf-8")
            mock_aws.chmod(0o755)

            env = os.environ.copy()
            env["PATH"] = f"{bin_dir}:{env['PATH']}"

            result = subprocess.run([
                str(SCRIPT),
                "--source-dir", str(source_dir),
                "--destination-uri", "s3://test-bucket/tuhospedaje/uploads",
                "--kms-key-id", "arn:aws:kms:region:123:key/abc",
            ], env=env, capture_output=True, text=True)

            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("uploads backup completed", result.stdout)

            calls = log_file.read_text(encoding="utf-8").splitlines()
            self.assertEqual(len(calls), 3)
            self.assertIn("s3 cp", calls[0])
            self.assertIn("s3://test-bucket/tuhospedaje/uploads/uploads-", calls[0])
            self.assertIn("--sse aws:kms", calls[0])
            self.assertIn(".sha256", calls[1])
            self.assertIn(".json", calls[2])


if __name__ == "__main__":
    unittest.main()
