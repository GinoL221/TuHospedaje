"""Pins the status notices in the delivery documents (issue #442)."""

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CURRENT_MAIN = ROOT / "docs" / "markdown" / "delivery" / "current-main.md"
AUDIT = ROOT / "docs" / "markdown" / "audits" / "auditoria-estado-actual.md"
README = ROOT / "README.md"


def head(path: Path, lines: int = 12) -> str:
    return "\n".join(path.read_text(encoding="utf-8").splitlines()[:lines])


class DeliveryDocsStatusTest(unittest.TestCase):
    def test_current_main_opens_with_a_historical_notice(self):
        top = head(CURRENT_MAIN)
        self.assertIn("Registro histórico", top)
        self.assertIn("18d9898", top)
        self.assertIn("v1.0.0", top)

    def test_audit_opens_with_a_dated_findings_notice(self):
        top = head(AUDIT)
        self.assertIn("Registro histórico", top)
        self.assertIn("volver a comprobar", top)

    def test_readme_separates_configured_from_verified_in_production(self):
        text = README.read_text(encoding="utf-8")
        start = text.index("## Producción con Docker Compose")
        end = text.index("## Desarrollo sin Compose")
        section = text[start:end]
        self.assertIn("Configurado en el repositorio vs. comprobado en producción", section)
        for control in ("TLS y reverse proxy", "Entrega real de correo", "Backups externos"):
            row = next((line for line in section.splitlines() if control in line), "")
            self.assertIn("No comprobado", row, control)


if __name__ == "__main__":
    unittest.main()
