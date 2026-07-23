#!/usr/bin/env python3
"""Dependency-free structural and semantic validation for lodging identities."""
import json
import hashlib
import re
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent
IDENTITY_KEYS = ["positioning", "architecture", "palette", "floors", "wallsAndFinishes", "joinery", "furniture", "lighting", "continuityElements", "recommendedScenes"]
EXPECTED_MANIFEST_DIGEST = "71e13efa921d8bd254c1020477cf2ae08922e96303b0d850e72676257ee79c0e"
INDEX_ROOT = {"schemaVersion": 1, "source": "recovered-from-approved-context"}
CATEGORY_TOTALS = {"hotel": 9, "cabin": 8, "apartment": 7, "hostel": 6, "resort": 4, "glamping": 4}
NUMBER_WORDS = {"un": 1, "una": 1, "uno": 1, "dos": 2, "tres": 3, "cuatro": 4, "cinco": 5, "seis": 6, "siete": 7, "ocho": 8}
APPROVED_SUBGROUPS = {
    3: {"guest": {2}, "room": {2}}, 5: {"guest": {2}}, 7: {"guest": {2}},
    9: {"guest": {4}}, 12: {"guest": {4}}, 13: {"guest": {2}, "room": {1}},
    15: {"guest": {2}}, 16: {"guest": {2}}, 18: {"guest": {4}},
    19: {"bed": {2}, "room": {2}}, 21: {"guest": {3}}, 24: {"guest": {2}},
    25: {"bed": {2}, "room": {2}}, 31: {"guest": {2}},
    32: {"guest": {4}}, 33: {"guest": {2}}, 34: {"guest": {2}},
    35: {"guest": {2}}, 36: {"guest": {2}}, 38: {"guest": {2}},
}

# Tables are intentionally explicit: unsupported semantic phrasing fails until reviewed.
GUARDS_BY_ID = {
    18: {"name": "Cabaña San Roque", "required": ("san roque",), "forbidden": ()},
    20: {"name": "Hotel La Perla", "positioning": ("la perla",), "architecture": ("curv",), "forbidden": ("playa grande como ubicacion",)},
    24: {"required": ("lago argentino",), "forbidden": ("perito moreno",)},
}
ACCESSIBILITY_BY_ID = {
    24: {"route": ("accesibilidad incorporada desde el acceso",), "bath": ("ducha accesible",), "scene": ("bano accesible",)},
    32: {"route": ("circulaciones amplias continuas",), "bath": ("duchas accesibles", "bano accesible"), "scene": ("bano accesible",)},
    37: {"route": ("accesibilidad continua entre sendero interior bano y terraza",), "bath": ("ducha accesible", "bano privado accesible"), "scene": ("bano privado accesible",)},
}
FORBIDDEN_BY_CATEGORY = {
    "hotel": ("decoracion literal con anclas", "escenografia tropical"),
    "cabin": ("escenografia alpina", "decoracion nautica literal"),
    "apartment": ("decoracion turistica literal",),
    "hostel": ("escenografia tematica",),
    "resort": ("parque acuatico literal", "ambientacion clinica"),
    "glamping": ("decoracion literal con tipis", "campamento de expedicion literal"),
}


def normalized(value):
    text = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode().casefold()
    return " ".join(re.findall(r"[a-z0-9]+", text))


def prose(document):
    return normalized(json.dumps(document["identity"], ensure_ascii=False))


def validate_schema(value, rule, path="$", errors=None):
    errors = [] if errors is None else errors
    expected = rule.get("type")
    types = {"object": lambda v: isinstance(v, dict), "array": lambda v: isinstance(v, list), "string": lambda v: isinstance(v, str), "integer": lambda v: isinstance(v, int) and not isinstance(v, bool)}
    if expected and not types[expected](value):
        errors.append(f"{path}: expected {expected}")
        return errors
    if "const" in rule and value != rule["const"]: errors.append(f"{path}: differs from const")
    if "enum" in rule and value not in rule["enum"]: errors.append(f"{path}: outside enum")
    if isinstance(value, str) and len(value) < rule.get("minLength", 0): errors.append(f"{path}: shorter than minLength")
    if isinstance(value, int) and not rule.get("minimum", value) <= value <= rule.get("maximum", value): errors.append(f"{path}: outside range")
    if isinstance(value, list):
        if not rule.get("minItems", 0) <= len(value) <= rule.get("maxItems", len(value)): errors.append(f"{path}: invalid item count")
        if rule.get("uniqueItems") and len({json.dumps(x, sort_keys=True) for x in value}) != len(value): errors.append(f"{path}: items are not unique")
        for index, item in enumerate(value): validate_schema(item, rule.get("items", {}), f"{path}[{index}]", errors)
    if isinstance(value, dict):
        properties = rule.get("properties", {})
        for key in set(rule.get("required", [])) - set(value): errors.append(f"{path}: missing {key}")
        if rule.get("additionalProperties") is False:
            for key in set(value) - set(properties): errors.append(f"{path}: unexpected {key}")
        for key, item in value.items():
            if key in properties: validate_schema(item, properties[key], f"{path}.{key}", errors)
    return errors


def validate_capacity(document, filename, errors):
    capacity = document["capacity"]
    lodging_id = document["lodgingId"]
    identity = document["identity"]
    def strings(value):
        if isinstance(value, str):
            yield value
        elif isinstance(value, list):
            for item in value:
                yield from strings(item)
        elif isinstance(value, dict):
            for item in value.values():
                yield from strings(item)

    checked_fields = list(strings(identity))
    number = r"(\d+|" + "|".join(NUMBER_WORDS) + r")"
    relevant = r"personas?|huespedes?|camas?|dormitorios?|habitaciones?|cuchetas?|literas?|lockers?|casilleros?|mesas?|sillas?|comedores?|plazas?"

    def value(token):
        return int(token) if token.isdigit() else NUMBER_WORDS[token]

    def allowed(concept, amount):
        return amount == capacity or amount in APPROVED_SUBGROUPS.get(lodging_id, {}).get(concept, set())

    sentences = (normalized(part) for field in checked_fields for part in re.split(r"[.!?;]", field))
    for sentence in filter(None, sentences):
        consumed = []
        # Total-capacity language is never a subgroup.
        for match in re.finditer(rf"\bhasta\s+{number}\s+(?:personas?|huespedes?)\b", sentence):
            amount = value(match.group(1)); consumed.append(match.span())
            if amount != capacity: errors.append(f"{filename}: capacity claim {amount} differs from declared {capacity}")

        concept_patterns = {
            "bed": (rf"\b{number}\s+(?:camas?|cuchetas?|literas?)\b",),
            "room": (rf"\b{number}\s+(?:dormitorios?|habitaciones?)\b",),
            "table": (rf"\b(?:mesas?|comedores?)\b(?:\s+\w+){{0,5}}\s+para\s+{number}(?:\s+(?:personas?|huespedes?))?\b",),
            "chair": (rf"\b(?:sillas?)\b(?:\s+\w+){{0,5}}\s+para\s+{number}(?:\s+(?:personas?|huespedes?))?\b", rf"\b{number}\s+sillas?\b"),
            "locker": (rf"\b(?:lockers?|casilleros?)\b(?:\s+\w+){{0,5}}\s+para\s+{number}(?:\s+(?:personas?|huespedes?))?\b", rf"\b{number}\s+(?:lockers?|casilleros?)\b"),
        }
        occupied = []
        for concept, patterns in concept_patterns.items():
            for pattern in patterns:
                for match in re.finditer(pattern, sentence):
                    amount = value(match.group(1)); occupied.append(match.span()); consumed.append(match.span())
                    if not allowed(concept, amount): errors.append(f"{filename}: {concept} claim {amount} is inconsistent with capacity {capacity}")

        # Guest counts not already owned by a table/locker/etc. are total or approved subgroup configurations.
        guest_pattern = rf"\b(?:para|preparad[oa]s? para)\s+(?:una?\s+familia\s+de\s+)?{number}\s+(?:personas?|huespedes?)\b"
        for match in re.finditer(guest_pattern, sentence):
            if any(start <= match.start() and match.end() <= end for start, end in occupied): continue
            amount = value(match.group(1)); consumed.append(match.span())
            if not allowed("guest", amount): errors.append(f"{filename}: guest claim {amount} is inconsistent with capacity {capacity}")

        # A numeric capacity concept outside the supported grammar is rejected rather than guessed.
        residual = list(sentence)
        for start, end in consumed:
            residual[start:end] = " " * (end - start)
        residual = re.sub(r"\b\d+\s*k\b", "", "".join(residual))
        unsupported = rf"\b{number}\s+(?:\w+\s+){{0,2}}(?:{relevant})\b"
        if any(value(match.group(1)) >= 2 for match in re.finditer(unsupported, residual)):
            errors.append(f"{filename}: unsupported capacity-bearing phrasing: {sentence.strip()}")


def require_any(text, anchors):
    return any(normalized(anchor) in text for anchor in anchors)


def validate_bundle(root):
    root = Path(root)
    errors = []
    if (root / "generate_manifests.py").exists(): errors.append("Duplicate identity source generate_manifests.py remains")
    schema = json.loads((root / "lodging-identity.schema.json").read_text())
    if schema.get("$schema") != "https://json-schema.org/draft/2020-12/schema": errors.append("Schema is not Draft 2020-12")
    index = json.loads((root / "index.json").read_text())
    files = sorted(root.glob("[0-9][0-9]-*.json"))
    if len(files) != 38: errors.append(f"Expected 38 manifests, found {len(files)}")
    digest = hashlib.sha256()
    for path in files:
        digest.update(path.read_bytes())
    if digest.hexdigest() != EXPECTED_MANIFEST_DIGEST:
        errors.append("Approved manifest content digest differs from canonical bundle")
    documents = [json.loads(path.read_text()) for path in files]
    ids = [document.get("lodgingId") for document in documents]
    if ids != list(range(1, 39)): errors.append(f"Manifest order/IDs are not exact 1..38: {ids}")
    if len(set(ids)) != len(ids): errors.append("Duplicate lodging IDs")
    for path, document in zip(files, documents):
        errors.extend(f"{path.name}: {error}" for error in validate_schema(document, schema))
        identity = document.get("identity", {})
        lodging_id = document.get("lodgingId")
        guard = GUARDS_BY_ID.get(lodging_id, {})
        if set(document) != {"$schema", "lodgingId", "name", "category", "location", "capacity", "version", "status", "source", "identity"}: errors.append(f"{path.name}: unexpected or missing root properties")
        if list(identity) != IDENTITY_KEYS: errors.append(f"{path.name}: identity sections missing or out of agreed order")
        if document.get("status") != "approved" or document.get("source") != "recovered-from-approved-context" or document.get("version") != 1: errors.append(f"{path.name}: approval/provenance metadata invalid")
        expected_name = guard.get("name")
        if expected_name and document.get("name") != expected_name: errors.append(f"{path.name}: ID {lodging_id} name differs from canonical value {expected_name}")
        scenes = identity.get("recommendedScenes", [])
        if len(scenes) != 5 or [scene.get("order") for scene in scenes] != [1, 2, 3, 4, 5]: errors.append(f"{path.name}: scenes must be exactly five and ordered")
        if len({normalized(scene.get("title", "") + " " + scene.get("brief", "")) for scene in scenes}) != 5: errors.append(f"{path.name}: scene intents are not distinct")
        expected_continuity = 7 if lodging_id == 1 else 8
        if len(identity.get("continuityElements", [])) != expected_continuity: errors.append(f"{path.name}: continuity count must be {expected_continuity}")
        text = prose(document)
        for anchor in guard.get("required", ()):
            if normalized(anchor) not in text: errors.append(f"{path.name}: ID {lodging_id} required phrase missing: {anchor}")
        positioning = normalized(identity.get("positioning", ""))
        for anchor in guard.get("positioning", ()):
            if normalized(anchor) not in positioning: errors.append(f"{path.name}: ID {lodging_id} La Perla identity/location phrase missing")
        architecture = normalized(identity.get("architecture", ""))
        for anchor in guard.get("architecture", ()):
            if normalized(anchor) not in architecture: errors.append(f"{path.name}: ID {lodging_id} architecture invariant missing: {anchor}")
        for phrase in guard.get("forbidden", ()):
            if normalized(phrase) in text:
                label = "Perito Moreno" if normalized(phrase) == "perito moreno" else phrase
                errors.append(f"{path.name}: prohibited correction/geography phrase: {label}")
        for phrase in FORBIDDEN_BY_CATEGORY.get(document.get("category"), ()):
            if normalized(phrase) in text: errors.append(f"{path.name}: prohibited category theming: {phrase}")
        validate_capacity(document, path.name, errors)
        if lodging_id in ACCESSIBILITY_BY_ID:
            access = ACCESSIBILITY_BY_ID[lodging_id]
            route_text = normalized(" ".join([identity.get("architecture", "")] + identity.get("continuityElements", [])))
            bath_text = normalized(" ".join([identity.get("floors", ""), identity.get("joinery", "")] + [scene.get("title", "") + " " + scene.get("brief", "") for scene in scenes]))
            scene_text = normalized(" ".join(scene.get("title", "") + " " + scene.get("brief", "") for scene in scenes))
            if not require_any(route_text, access["route"]): errors.append(f"{path.name}: accessibility route anchor missing")
            if not require_any(bath_text, access["bath"]): errors.append(f"{path.name}: accessible bathroom anchor missing")
            if not require_any(scene_text, access["scene"]): errors.append(f"{path.name}: accessible scene anchor missing")
    if set(index) != {*INDEX_ROOT, "lodgings"}:
        errors.append("Index root must contain exactly schemaVersion, source and lodgings")
    for key, expected in INDEX_ROOT.items():
        if index.get(key) != expected: errors.append(f"Index {key} differs from canonical value {expected}")
    expected_index = {**INDEX_ROOT, "lodgings": [{"lodgingId": doc["lodgingId"], "name": doc["name"], "category": doc["category"], "file": path.name, "status": doc["status"], "version": doc["version"]} for path, doc in zip(files, documents)]}
    if index != expected_index:
        for expected, actual in zip(expected_index["lodgings"], index.get("lodgings", [])):
            if actual != expected: errors.append(f"Index entry {expected['lodgingId']} differs from manifest")
        if len(index.get("lodgings", [])) != 38: errors.append("Index coverage/order is invalid")
    totals = {category: sum(doc.get("category") == category for doc in documents) for category in CATEGORY_TOTALS}
    if totals != CATEGORY_TOTALS: errors.append(f"Category totals differ: {totals}")
    return errors


def main():
    errors = validate_bundle(ROOT)
    if errors:
        print("FAILED")
        print("\n".join(f"- {error}" for error in errors))
        return 1
    print("PASS: schema and semantic guards validate all 38 canonical manifests")
    print("PASS: exact index projection, category totals, corrections and accessibility anchors")
    print("PASS: capacity, prohibited-theme/geography and duplicate-source guards")
    return 0


if __name__ == "__main__":
    sys.exit(main())
