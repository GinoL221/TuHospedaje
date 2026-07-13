#!/usr/bin/env python3
"""Negative mutation tests for the canonical lodging identity validator."""
import copy
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("lodging_validate", ROOT / "validate.py")
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)


class ValidatorMutationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        for path in ROOT.glob("*.json"):
            (self.root / path.name).write_bytes(path.read_bytes())

    def tearDown(self):
        self.temp.cleanup()

    def mutate(self, lodging_id, edit):
        path = next(self.root.glob(f"{lodging_id:02d}-*.json"))
        document = json.loads(path.read_text())
        edit(document)
        path.write_text(json.dumps(document, ensure_ascii=False, indent=2) + "\n")

    def assert_rejected(self, fragment):
        errors = validator.validate_bundle(self.root)
        self.assertTrue(errors, "mutated bundle was accepted")
        self.assertTrue(any(fragment in error for error in errors), errors)

    def test_canonical_bundle_passes(self):
        self.assertEqual([], validator.validate_bundle(ROOT))

    def test_index_drift_is_rejected(self):
        path = self.root / "index.json"
        index = json.loads(path.read_text())
        index["lodgings"][0]["name"] = "Drifted name"
        path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n")
        self.assert_rejected("Index entry 1")

    def test_index_missing_required_root_key_is_rejected(self):
        path = self.root / "index.json"
        index = json.loads(path.read_text())
        del index["source"]
        path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n")
        self.assert_rejected("Index root")

    def test_index_extra_root_key_is_rejected(self):
        path = self.root / "index.json"
        index = json.loads(path.read_text())
        index["status"] = "approved"
        path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n")
        self.assert_rejected("Index root")

    def test_index_schema_version_and_source_are_exact(self):
        path = self.root / "index.json"
        index = json.loads(path.read_text())
        index.update(schemaVersion=2, source="unapproved")
        path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n")
        errors = validator.validate_bundle(self.root)
        self.assertTrue(any("schemaVersion" in error for error in errors), errors)
        self.assertTrue(any("source" in error for error in errors), errors)

    def test_arbitrary_manifest_content_mutation_is_rejected(self):
        self.mutate(1, lambda d: d["identity"].__setitem__(
            "architecture", d["identity"]["architecture"] + " Cambio no aprobado."))
        self.assert_rejected("digest")

    def test_capacity_claim_is_rejected(self):
        self.mutate(18, lambda d: d["identity"].__setitem__(
            "positioning", d["identity"]["positioning"].replace("seis personas", "siete personas")))
        self.assert_rejected("capacity claim")

    def test_below_capacity_bed_claim_is_rejected(self):
        self.mutate(18, lambda d: d["identity"].__setitem__(
            "furniture", d["identity"]["furniture"] + " Se disponen cuatro camas para el alojamiento."))
        self.assert_rejected("bed claim")

    def test_inconsistent_table_claim_is_rejected(self):
        self.mutate(18, lambda d: d["identity"].__setitem__(
            "furniture", d["identity"]["furniture"].replace("seis personas", "cinco personas")))
        self.assert_rejected("table claim")

    def test_inconsistent_locker_claim_is_rejected(self):
        self.mutate(4, lambda d: d["identity"].__setitem__(
            "furniture", d["identity"]["furniture"].replace("ocho personas", "siete personas")))
        self.assert_rejected("locker claim")

    def test_inconsistent_room_claim_is_rejected(self):
        self.mutate(25, lambda d: d["identity"].__setitem__(
            "positioning", d["identity"]["positioning"].replace("dos dormitorios", "tres dormitorios")))
        self.assert_rejected("room claim")

    def test_unsupported_capacity_bearing_phrase_fails_closed(self):
        self.mutate(18, lambda d: d["identity"].__setitem__(
            "furniture", d["identity"]["furniture"] + " Dormitorio con cuatro plazas configurables."))
        self.assert_rejected("unsupported capacity-bearing phrasing")

    def test_supported_claim_does_not_hide_unsupported_claim_in_same_sentence(self):
        self.mutate(18, lambda d: d["identity"].__setitem__(
            "furniture", d["identity"]["furniture"] + " Se disponen seis camas y cuatro plazas configurables."))
        self.assert_rejected("unsupported capacity-bearing phrasing")

    def test_mixed_ordering_still_accounts_for_every_claim(self):
        self.mutate(18, lambda d: d["identity"].__setitem__(
            "furniture", d["identity"]["furniture"] + " Cuatro plazas configurables y seis camas disponibles."))
        self.assert_rejected("unsupported capacity-bearing phrasing")

    def test_approved_subgroup_allowance_passes(self):
        errors = validator.validate_bundle(ROOT)
        self.assertFalse(any("19-departamento-belgrano" in error and "bed claim" in error for error in errors), errors)

    def test_approved_allowance_and_full_capacity_claim_can_share_sentence(self):
        document = json.loads(next(ROOT.glob("19-*.json")).read_text())
        document["identity"]["furniture"] += " Dos camas individuales y mesa para cuatro personas."
        errors = []
        validator.validate_capacity(document, "allowance.json", errors)
        self.assertEqual([], errors)

    def test_capacity_claim_in_architecture_is_rejected(self):
        self.mutate(18, lambda d: d["identity"].__setitem__(
            "architecture", d["identity"]["architecture"] + " Alojamiento para siete personas."))
        self.assert_rejected("guest claim")

    def test_capacity_claim_in_palette_is_rejected(self):
        self.mutate(18, lambda d: d["identity"]["palette"].append(
            {"name": "Prueba", "hex": "#000000", "use": "Preparada para siete huéspedes."}))
        self.assert_rejected("guest claim")

    def test_capacity_claim_in_continuity_element_is_rejected(self):
        self.mutate(18, lambda d: d["identity"]["continuityElements"].__setitem__(
            0, d["identity"]["continuityElements"][0] + " para siete personas"))
        self.assert_rejected("guest claim")

    def test_capacity_claim_in_scene_title_is_rejected(self):
        self.mutate(18, lambda d: d["identity"]["recommendedScenes"][0].__setitem__(
            "title", "Dormitorio para siete huéspedes"))
        self.assert_rejected("guest claim")

    def test_forbidden_literal_theme_is_rejected(self):
        self.mutate(35, lambda d: d["identity"].__setitem__(
            "furniture", d["identity"]["furniture"] + " Decoración literal con tipis."))
        self.assert_rejected("prohibited")

    def test_accessibility_anchor_is_rejected(self):
        self.mutate(32, lambda d: d["identity"].__setitem__(
            "architecture", d["identity"]["architecture"].replace("continuas y sin desniveles innecesarios", "interrumpidas por escalones")))
        self.assert_rejected("accessibility route")

    def test_id18_correction_is_rejected(self):
        self.mutate(18, lambda d: d.__setitem__("name", "Cabaña del Lago"))
        self.assert_rejected("ID 18")

    def test_id20_location_is_rejected_independently_of_curves(self):
        self.mutate(20, lambda d: d["identity"].__setitem__(
            "positioning", d["identity"]["positioning"].replace("La Perla", "Playa Grande")))
        self.assert_rejected("ID 20 La Perla")

    def test_id24_impossible_view_is_rejected(self):
        self.mutate(24, lambda d: d["identity"]["recommendedScenes"][0].__setitem__(
            "brief", "Mostrar una vista directa al glaciar Perito Moreno."))
        self.assert_rejected("Perito Moreno")


if __name__ == "__main__":
    unittest.main()
