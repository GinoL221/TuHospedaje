#!/usr/bin/env python3
"""Export paste-ready image generation briefs from canonical lodging identities."""

import argparse
import json
from pathlib import Path


IDENTITY_FIELDS = (
    ("Arquitectura", "architecture"),
    ("Pisos", "floors"),
    ("Muros y terminaciones", "wallsAndFinishes"),
    ("Carpinterías", "joinery"),
    ("Mobiliario", "furniture"),
    ("Iluminación", "lighting"),
)


def render_brief(document, scene):
    identity = document["identity"]
    scene_order = scene["order"]
    continuity = "\n".join(f"- {item}" for item in identity["continuityElements"])
    specifications = "\n\n".join(
        f"{label}: {identity[key]}" for label, key in IDENTITY_FIELDS
    )
    reference = (
        "Esta es la imagen ancla del alojamiento. Fijá con claridad la arquitectura, "
        "los materiales, la paleta y el paisaje para reutilizarlos en las escenas 2 a 5."
        if scene_order == 1
        else "Usá la imagen aprobada de la escena 1 como referencia visual obligatoria. "
        "Mostrá el mismo alojamiento, con idéntica arquitectura, materiales, paleta, "
        "paisaje y nivel de terminación; solo cambia el ambiente y el encuadre de esta escena."
    )

    return f"""Generá UNA sola fotografía arquitectónica hotelera hiperrealista de {document['name']}, en {document['location']['city']}, {document['location']['country']}.

IDENTIDAD DEL ALOJAMIENTO
{identity['positioning']}

ESCENA {scene_order} DE 5: {scene['title']}
{scene['brief']}
Esta imagen debe mostrar únicamente esta escena. No combines ambientes ni incluyas un collage.

CONTINUIDAD VISUAL
{reference}
Elementos que deben mantenerse en toda la serie:
{continuity}

ESPECIFICACIONES DEL DISEÑO
{specifications}

Paleta: {', '.join(identity['palette'])}.

CÁMARA Y ENTREGA
Composición horizontal 4:3, master final de 2048 x 1536 píxeles, sRGB. Perspectiva natural de fotografía arquitectónica profesional, líneas verticales corregidas, detalle realista en materiales y luz, sin distorsión de gran angular. Conservá el sujeto principal y los elementos esenciales dentro del 80 % central para permitir un recorte seguro.

RESTRICCIONES OBLIGATORIAS
Sin personas, texto legible, carteles, logos, marcas de agua, collage ni bordes. No agregues ambientes, muebles, materiales, decoración ni equipamiento ausentes de esta identidad. Respetá especialmente todas las exclusiones indicadas en el posicionamiento y en las especificaciones. Evitá una estética genérica de hotel boutique o de lujo: no agregues mármol brillante, dorados, espejos retroiluminados, televisores, placares abiertos, vanities flotantes ni mobiliario sobredimensionado salvo que la identidad los pida explícitamente.
"""


def load_identity(identities_dir, lodging_id):
    matches = list(identities_dir.glob(f"{lodging_id:02d}-*.json"))
    if len(matches) != 1:
        raise ValueError(f"Expected one identity for lodging {lodging_id}, found {len(matches)}")
    return json.loads(matches[0].read_text(encoding="utf-8"))


def validate_manifest(manifest_path, lodging_id, document):
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    entries = [entry for entry in manifest["entries"] if entry["lodgingId"] == lodging_id]
    orders = sorted(entry["sceneOrder"] for entry in entries)
    expected = [scene["order"] for scene in document["identity"]["recommendedScenes"]]
    if orders != expected or expected != [1, 2, 3, 4, 5]:
        raise ValueError(f"Manifest and identity must contain scenes 1-5 for lodging {lodging_id}")


def export_briefs(identities_dir, manifest_path, output_dir, lodging_id):
    document = load_identity(identities_dir, lodging_id)
    validate_manifest(manifest_path, lodging_id, document)
    output_dir.mkdir(parents=True, exist_ok=True)
    paths = []
    for scene in document["identity"]["recommendedScenes"]:
        path = output_dir / f"lodging-{lodging_id:03d}-scene-{scene['order']:02d}-codex-brief.txt"
        path.write_text(render_brief(document, scene), encoding="utf-8")
        paths.append(path)
    return paths


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--identities", type=Path, default=Path("content/lodgings"))
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--lodging-id", type=int, required=True)
    args = parser.parse_args()
    for path in export_briefs(args.identities, args.manifest, args.output, args.lodging_id):
        print(path)


if __name__ == "__main__":
    main()
