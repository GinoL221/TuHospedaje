"""Black-box launcher checks; never invoke real Maven, Docker, or a shared cache."""
import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class LauncherTest(unittest.TestCase):
    def setUp(self):
        artifacts = ROOT / "backend/.safetyunit-output"
        artifacts.mkdir(exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(prefix="launcher-test.", dir=artifacts)
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / "workspace with spaces"
        (self.root / "scripts").mkdir(parents=True)
        (self.root / "backend").mkdir()
        self.launcher = self.root / "scripts/safetyunit"
        shutil.copyfile(ROOT / "scripts/safetyunit", self.launcher)
        self.cache = self.root / "cache"
        self.cache.mkdir()
        self.marker = self.root / "calls"
        self.maven = self.root / "fake maven"
        self.stub(0)
        self.env = {
            **os.environ,
            "SAFETYUNIT_MAVEN": str(self.maven),
            "SAFETYUNIT_REPOSITORY": str(self.cache),
            "HOME": str(self.root / "original-home"),
            "JWT_SECRET": "sentinel-not-a-secret",
            "MAVEN_OPTS": "-Dsentinel=must-not-propagate",
            "JAVA_TOOL_OPTIONS": "-Dsentinel=must-not-propagate",
            "DOCKER_HOST": "sentinel-must-not-propagate",
        }
        # These files must not be read by the fake Maven or changed by the launcher.
        home = self.root / "original-home"
        (home / ".m2").mkdir(parents=True)
        (home / ".m2/settings.xml").write_text("private-settings-sentinel")
        self.env.pop("BASH_ENV", None)

    def stub(self, status):
        self.maven.write_text(
            f"#!{sys.executable}\n"
            "import json, os, pathlib, sys\n"
            f"p = pathlib.Path({str(self.marker)!r})\n"
            "p.write_text(p.read_text() + 'call\\n' if p.exists() else 'call\\n')\n"
            "print(json.dumps({'argv': sys.argv[1:], 'env': dict(os.environ), 'cwd': os.getcwd()}))\n"
            + ("sys.stderr.write('simulated Maven failure\\n')\n" if status else "")
            + f"sys.exit({status})\n"
        )
        self.maven.chmod(0o700)

    def run_launcher(self, *args):
        return subprocess.run(
            ["/bin/bash", str(self.launcher), *args], env=self.env,
            text=True, capture_output=True, timeout=10,
        )

    def test_rejects_arguments_before_writes(self):
        for args in [(), ("--other",), ("--self-test", "extra")]:
            with self.subTest(args=args):
                result = self.run_launcher(*args)
                self.assertEqual(result.returncode, 2)
                self.assertEqual(result.stdout, "")
                self.assertEqual(result.stderr, "usage: scripts/safetyunit --self-test\n")
                self.assertFalse((self.root / "backend/.safetyunit-output").exists())
                self.assertFalse(self.marker.exists())

    def test_portable_offline_invocation_with_clean_child_environment(self):
        result = self.run_launcher("--self-test")
        self.assertEqual((result.returncode, result.stderr), (0, ""))
        call = json.loads(result.stdout)
        self.assertEqual(self.marker.read_text(), "call\n")
        self.assertEqual(call["cwd"], str(self.root / "backend"))
        env, argv = call["env"], call["argv"]
        for name in ["JWT_SECRET", "MAVEN_OPTS", "DOCKER_HOST", "SAFETYUNIT_MAVEN"]:
            self.assertNotIn(name, env)
        self.assertNotIn("sentinel", env["JAVA_TOOL_OPTIONS"])
        self.assertEqual(env["MAVEN_SKIP_RC"], "true")
        self.assertEqual(argv[:1], ["-o"])
        self.assertEqual(argv[-2:], ["-Psafetyunit", "test"])
        self.assertIn(f"-Dmaven.repo.local={self.cache}", argv)
        settings = Path(argv[argv.index("-s") + 1])
        self.assertEqual(argv[argv.index("-gs") + 1], str(settings))
        self.assertEqual(settings.read_text(), '<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"></settings>\n')
        self.assertEqual(settings.stat().st_mode & 0o777, 0o600)
        run = settings.parent.parent
        for directory in [run, Path(env["HOME"]), Path(env["TMPDIR"]), Path(env["DOCKER_CONFIG"])]:
            self.assertTrue(directory.is_relative_to(self.root / "backend/.safetyunit-output"))
            self.assertEqual(directory.stat().st_mode & 0o777, 0o700)
        self.assertEqual(env["TESTCONTAINERS_PULL_POLICY"], "com.tuhospedaje.safety.OfflineImagePolicy$NeverPullPolicy")
        self.assertEqual(env["TESTCONTAINERS_IMAGE_SUBSTITUTOR"], "com.tuhospedaje.safety.OfflineImagePolicy$DenyImageSubstitutor")
        self.assertEqual(list(self.cache.iterdir()), [])
        self.assertEqual((self.root / "original-home/.m2/settings.xml").read_text(), "private-settings-sentinel")

    def test_propagates_maven_failure_and_retains_its_run(self):
        self.stub(17)
        result = self.run_launcher("--self-test")
        self.assertEqual(result.returncode, 17)
        self.assertEqual(result.stderr, "simulated Maven failure\n")
        self.assertEqual(self.marker.read_text(), "call\n")
        self.assertTrue(Path(json.loads(result.stdout)["env"]["HOME"]).is_dir())

    def test_rejects_missing_tools_or_cache_before_output_creation(self):
        for name, value, error in [
            ("SAFETYUNIT_MAVEN", str(self.root / "missing"), "safetyunit: Maven must be an absolute executable path\n"),
            ("SAFETYUNIT_REPOSITORY", str(self.root / "missing"), "safetyunit: repository must be an existing absolute directory\n"),
        ]:
            with self.subTest(name=name):
                old = self.env[name]
                self.env[name] = value
                result = self.run_launcher("--self-test")
                self.env[name] = old
                self.assertEqual((result.returncode, result.stdout, result.stderr), (2, "", error))
                self.assertFalse((self.root / "backend/.safetyunit-output").exists())
                self.assertFalse(self.marker.exists())

    def test_rejects_output_symlink_without_changing_target(self):
        output = self.root / "backend/.safetyunit-output"
        output.symlink_to(self.cache, target_is_directory=True)
        result = self.run_launcher("--self-test")
        self.assertEqual((result.returncode, result.stdout, result.stderr), (2, "", "refusing symlink safetyunit output root\n"))
        self.assertEqual(list(self.cache.iterdir()), [])
        self.assertFalse(self.marker.exists())


if __name__ == "__main__":
    unittest.main()
