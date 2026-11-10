"""Guards the reproducible generation of the deliverable PDFs (issue #441)."""

import hashlib
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOL_DIR = ROOT / "tools" / "docs-pdf"
DELIVERABLES = ROOT / "docs" / "entregables"
MANIFEST = DELIVERABLES / "SOURCES.sha256"

EXPECTED_SOURCES = {
    "docs/markdown/project-definition.md",
    "docs/markdown/sprint-1/sprint-1-report.md",
    "docs/markdown/sprint-1/sprint-1-test-plan.md",
    "docs/markdown/sprint-2/sprint-2-report.md",
    "docs/markdown/sprint-2/sprint-2-test-plan.md",
    "docs/markdown/sprint-3/sprint-3-report.md",
    "docs/markdown/sprint-3/sprint-3-test-plan.md",
    "docs/markdown/sprint-4/sprint-4-report.md",
    "docs/markdown/sprint-4/sprint-4-test-plan.md",
}


def read_manifest() -> dict[str, str]:
    entries = {}
    for line in MANIFEST.read_text(encoding="utf-8").splitlines():
        match = re.fullmatch(r"([0-9a-f]{64})  (\S+)", line)
        if match:
            entries[match.group(2)] = match.group(1)
    return entries


class DeliverablePdfsTest(unittest.TestCase):
    def test_toolchain_is_pinned_to_exact_versions(self):
        package = json.loads((TOOL_DIR / "package.json").read_text(encoding="utf-8"))
        dependencies = package.get("dependencies", {})
        for name in ("md-to-pdf", "@fontsource-variable/inter"):
            self.assertIn(name, dependencies)
            self.assertRegex(dependencies[name], r"^\d+\.\d+\.\d+$", name)
        self.assertEqual(package.get("scripts", {}).get("build"), "node build.mjs")
        self.assertTrue((TOOL_DIR / "package-lock.json").is_file())

    def test_manifest_lists_exactly_the_nine_deliverable_sources(self):
        self.assertEqual(set(read_manifest()), EXPECTED_SOURCES)

    def test_manifest_hashes_match_current_sources(self):
        for source, expected in read_manifest().items():
            actual = hashlib.sha256((ROOT / source).read_bytes()).hexdigest()
            self.assertEqual(
                actual,
                expected,
                f"{source} changed: run `npm run build` in tools/docs-pdf and commit the PDFs",
            )

    def test_every_source_has_a_generated_pdf(self):
        for source in EXPECTED_SOURCES:
            pdf = DELIVERABLES / (Path(source).stem + ".pdf")
            self.assertTrue(pdf.is_file(), pdf.name)
            self.assertTrue(pdf.read_bytes().startswith(b"%PDF-"), pdf.name)

    def test_brand_stylesheet_uses_manual_tokens(self):
        css = (TOOL_DIR / "brand.css").read_text(encoding="utf-8")
        for token in ("#264653", "#FF6B35", "#2A9D8F", "#F4F4F9", "Inter"):
            self.assertIn(token, css)

    def test_docs_index_documents_the_build_command(self):
        index = (ROOT / "docs" / "README.md").read_text(encoding="utf-8")
        self.assertIn("tools/docs-pdf", index)
        self.assertIn("npm run build", index)


if __name__ == "__main__":
    unittest.main()
