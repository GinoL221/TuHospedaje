"""Exercise the seed entrypoint in disposable, offline containers with fake input.

Run with SEED_TEST_IMAGE=<installed-image-id> python3 -m unittest discover
-s backend/docker -p 'test_seed_entrypoint.py' -v. No image is downloaded.
"""

import os
from pathlib import Path
import subprocess
import tempfile
import unittest


DIRECTORY = Path(__file__).resolve().parent
SCRIPT = DIRECTORY / "seed-entrypoint.sh"
VALID_HASH = "$2b$12$" + "." * 53  # Synthetic bcrypt-format fixture, never a credential.
MALFORMED = "dev admin hash is missing or malformed\n"
WRONG_LENGTH = "dev admin hash has the wrong length\n"


class SeedEntrypointTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.image = os.environ.get("SEED_TEST_IMAGE")
        if not cls.image:
            raise RuntimeError("Set SEED_TEST_IMAGE to an already installed Docker image")
        subprocess.run(
            ["docker", "image", "inspect", cls.image],
            check=True, capture_output=True, timeout=10,
        )

    def exercise(self, contents, expected_error=None):
        with tempfile.TemporaryDirectory(prefix=".seed-test-", dir=DIRECTORY) as tmp:
            root = Path(tmp)
            source = root / "dev-admin.env"
            source.write_text(contents, encoding="utf-8")
            result = root / "result"
            result.mkdir()
            (result / "hash").write_text("unchanged", encoding="utf-8")
            (result / "calls").write_text("0\n", encoding="utf-8")
            spy = root / "spy.sh"
            spy.write_text(
                '#!/bin/sh\nset -eu\n'
                'printf "%s" "$DEV_ADMIN_PASSWORD_HASH" > /result/hash\n'
                'printf "1\\n" > /result/calls\n',
                encoding="utf-8",
            )
            spy.chmod(0o755)
            completed = subprocess.run(
                [
                    "docker", "run", "--rm", "--pull=never", "--network=none",
                    "--read-only", "--cap-drop=ALL",
                    "--security-opt=no-new-privileges:true",
                    "--user", f"{os.getuid()}:{os.getgid()}",
                    "--mount", f"type=bind,src={SCRIPT},dst=/seed-entrypoint.sh,readonly",
                    "--mount", f"type=bind,src={source},dst=/run/dev-admin.env,readonly",
                    "--mount", f"type=bind,src={spy},dst=/usr/local/bin/dev-entrypoint.sh,readonly",
                    "--mount", f"type=bind,src={result},dst=/result",
                    "--entrypoint", "/bin/sh", self.image, "/seed-entrypoint.sh",
                ],
                capture_output=True, text=True, timeout=20,
            )
            self.assertEqual(completed.stdout, "")
            self.assertEqual(completed.stderr, expected_error or "")
            self.assertEqual(completed.returncode, 1 if expected_error else 0)
            self.assertEqual(source.read_text(encoding="utf-8"), contents)
            self.assertEqual(
                (result / "hash").read_text(encoding="utf-8"),
                "unchanged" if expected_error else contents.split("DEV_ADMIN_PASSWORD_HASH=", 1)[1].splitlines()[0],
            )
            self.assertEqual(
                (result / "calls").read_text(encoding="utf-8"),
                "0\n" if expected_error else "1\n",
            )

    def test_valid_hash_is_preserved_without_interpolation_or_logging(self):
        self.exercise(f"DEV_ADMIN_PASSWORD_HASH={VALID_HASH}\n")

    def test_missing_value_stops_before_backend(self):
        self.exercise("UNRELATED=test-only\n", MALFORMED)

    def test_empty_value_is_rejected(self):
        self.exercise("DEV_ADMIN_PASSWORD_HASH=\n", MALFORMED)

    def test_invalid_prefix_is_rejected(self):
        self.exercise("DEV_ADMIN_PASSWORD_HASH=$2x$12$" + "." * 53 + "\n", MALFORMED)

    def test_wrong_length_is_rejected(self):
        self.exercise(f"DEV_ADMIN_PASSWORD_HASH={VALID_HASH[:-1]}\n", WRONG_LENGTH)

    def test_malformed_payload_of_correct_length_is_rejected(self):
        self.exercise("DEV_ADMIN_PASSWORD_HASH=$2b$12$" + "!" * 53 + "\n", MALFORMED)

    def test_invalid_cost_of_correct_length_is_rejected(self):
        self.exercise("DEV_ADMIN_PASSWORD_HASH=$2b$xx$" + "." * 53 + "\n", MALFORMED)

    def test_cost_below_bcrypt_range_is_rejected(self):
        self.exercise("DEV_ADMIN_PASSWORD_HASH=$2b$03$" + "." * 53 + "\n", MALFORMED)

    def test_cost_above_bcrypt_range_is_rejected(self):
        self.exercise("DEV_ADMIN_PASSWORD_HASH=$2b$32$" + "." * 53 + "\n", MALFORMED)

    def test_existing_supported_prefixes_are_preserved(self):
        for prefix in ("2a", "2b", "2y"):
            with self.subTest(prefix=prefix):
                self.exercise(f"DEV_ADMIN_PASSWORD_HASH=${prefix}$12$" + "." * 53 + "\n")

    def test_bcrypt_cost_boundaries_are_accepted_without_hashing(self):
        for cost in ("04", "31"):
            with self.subTest(cost=cost):
                self.exercise(f"DEV_ADMIN_PASSWORD_HASH=$2b${cost}$" + "." * 53 + "\n")


if __name__ == "__main__":
    unittest.main()
