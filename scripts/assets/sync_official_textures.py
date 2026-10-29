#!/usr/bin/env python3
"""Embed reviewed official PNG textures into Blockbench sources without changing geometry."""

from __future__ import annotations

import argparse
import base64
import json
import struct
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
TEXTURE_SIZE = 1024
OFFICIAL_ASSETS = {
    "airdrop_crate": "airdrop_crate.png",
    "boss_portal": "boss_portal.png",
    "meteor_core": "meteor_core.png",
    "metin_stone": "metin_stone.png",
    "pinata": "pinata.png",
}


def png_dimensions(content: bytes) -> tuple[int, int]:
    if content[:8] != b"\x89PNG\r\n\x1a\n" or content[12:16] != b"IHDR":
        raise ValueError("Texture is not a valid PNG")
    return struct.unpack(">II", content[16:24])


def scale_uvs(model: dict, factor: float) -> None:
    for element in model.get("elements", []):
        for face in element.get("faces", {}).values():
            if "uv" in face:
                face["uv"] = [round(float(value) * factor, 6) for value in face["uv"]]


def collapse_texture_references(model: dict, textures: list[dict]) -> None:
    primary_id = str(textures[0].get("id", "0"))
    secondary_ids = {str(texture.get("id", index)) for index, texture in enumerate(textures[1:], 1)}
    for element in model.get("elements", []):
        for face in element.get("faces", {}).values():
            reference = face.get("texture")
            normalized = str(reference).removeprefix("#")
            if normalized in secondary_ids or reference in range(1, len(textures)):
                face["texture"] = f"#{primary_id}" if isinstance(reference, str) else 0


def synchronized_content(model_id: str, texture_name: str) -> tuple[Path, str]:
    model_path = (
        ROOT
        / "spectraevents-application/src/main/resources/assets/source"
        / f"{model_id}.bbmodel"
    )
    texture_path = ROOT / "assets/textures/source" / texture_name
    texture = texture_path.read_bytes()
    dimensions = png_dimensions(texture)
    if dimensions != (TEXTURE_SIZE, TEXTURE_SIZE):
        raise ValueError(f"{texture_path} must be {TEXTURE_SIZE}x{TEXTURE_SIZE}, got {dimensions}")

    model = json.loads(model_path.read_text(encoding="utf-8"))
    textures = model.get("textures", [])
    if not textures:
        raise ValueError(f"{model_path} must contain an embedded texture")
    collapse_texture_references(model, textures)
    embedded = textures[0]
    model["textures"] = [embedded]
    resolution = model.setdefault("resolution", {})
    old_width = int(resolution.get("width", TEXTURE_SIZE))
    old_height = int(resolution.get("height", TEXTURE_SIZE))
    if old_width != old_height or old_width <= 0:
        raise ValueError(f"{model_path} must use a positive square UV resolution")
    if old_width != TEXTURE_SIZE:
        scale_uvs(model, TEXTURE_SIZE / old_width)

    resolution["width"] = TEXTURE_SIZE
    resolution["height"] = TEXTURE_SIZE
    embedded["name"] = texture_name
    embedded["width"] = TEXTURE_SIZE
    embedded["height"] = TEXTURE_SIZE
    embedded["uv_width"] = TEXTURE_SIZE
    embedded["uv_height"] = TEXTURE_SIZE
    embedded["source"] = "data:image/png;base64," + base64.b64encode(texture).decode("ascii")
    return model_path, json.dumps(model, ensure_ascii=False, separators=(",", ":")) + "\n"


def synchronize(model_id: str, texture_name: str, check: bool) -> None:
    model_path, content = synchronized_content(model_id, texture_name)
    if check:
        if model_path.read_text(encoding="utf-8") != content:
            raise ValueError(
                f"{model_path} is not synchronized with assets/textures/source/{texture_name}"
            )
        return
    model_path.write_text(content, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail when an embedded Blockbench texture differs from its canonical PNG",
    )
    arguments = parser.parse_args()
    for model_id, texture_name in OFFICIAL_ASSETS.items():
        synchronize(model_id, texture_name, arguments.check)
        action = "Verified" if arguments.check else "Embedded"
        print(f"{action} {texture_name} in {model_id}.bbmodel")


if __name__ == "__main__":
    main()
