import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[1] / "export_codex_briefs.py"
SPEC = importlib.util.spec_from_file_location("export_codex_briefs", SCRIPT)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"Unable to load {SCRIPT}")
exporter = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(exporter)


class ExportCodexBriefsTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.identities = self.root / "identities"
        self.identities.mkdir()
        identity = {
            "lodgingId": 38,
            "name": "Test Glamping",
            "location": {"city": "Mendoza", "country": "Argentina"},
            "identity": {
                "positioning": "Glamping para parejas; sin parecer un hotel urbano.",
                "architecture": "Domo beige.",
                "palette": ["beige arena", "nogal"],
                "floors": "Madera natural.",
                "wallsAndFinishes": "Lona beige.",
                "joinery": "Estructura grafito.",
                "furniture": "Cama king.",
                "lighting": "Luz cálida.",
                "continuityElements": ["Domo beige.", "Madera natural."],
                "recommendedScenes": [
                    {"order": order, "title": f"Scene {order}", "brief": f"Show scene {order}."}
                    for order in range(1, 6)
                ],
            },
        }
        (self.identities / "38-test.json").write_text(json.dumps(identity), encoding="utf-8")
        manifest = {"entries": [{"lodgingId": 38, "sceneOrder": order} for order in range(1, 6)]}
        self.manifest = self.root / "manifest.json"
        self.manifest.write_text(json.dumps(manifest), encoding="utf-8")

    def tearDown(self):
        self.temp.cleanup()

    def test_exports_five_single_scene_briefs_with_delivery_constraints(self):
        paths = exporter.export_briefs(self.identities, self.manifest, self.root / "output", 38)

        self.assertEqual(5, len(paths))
        first = paths[0].read_text(encoding="utf-8")
        self.assertIn("UNA sola fotografía", first)
        self.assertIn("2048 x 1536", first)
        self.assertIn("horizontal 4:3", first)
        self.assertIn("80 % central", first)
        self.assertNotIn("Scene 2", first)

    def test_scenes_after_anchor_require_scene_one_reference(self):
        paths = exporter.export_briefs(self.identities, self.manifest, self.root / "output", 38)

        second = paths[1].read_text(encoding="utf-8")
        self.assertIn("imagen aprobada de la escena 1 como referencia visual obligatoria", second)
        self.assertIn("Domo beige", second)
        self.assertIn("estética genérica de hotel boutique", second)

    def test_rejects_incomplete_manifest_coverage(self):
        self.manifest.write_text(json.dumps({"entries": []}), encoding="utf-8")

        with self.assertRaisesRegex(ValueError, "scenes 1-5"):
            exporter.export_briefs(self.identities, self.manifest, self.root / "output", 38)


if __name__ == "__main__":
    unittest.main()
