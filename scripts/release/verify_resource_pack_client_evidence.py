#!/usr/bin/env python3
"""Fail closed when a release enables resource-pack delivery without real-client evidence."""

from __future__ import annotations

import argparse
import json
import pathlib
import sys


def verify(path: pathlib.Path) -> None:
    data = json.loads(path.read_text(encoding="utf-8"))
    if data.get("schemaVersion") != 1:
        raise ValueError("schemaVersion must be 1")
    delivery = data.get("delivery")
    packs = data.get("packs")
    if delivery == "disabled":
        if packs != []:
            raise ValueError("disabled delivery must not declare packs")
        return
    if delivery != "verified":
        raise ValueError("delivery must be disabled or verified")
    if not isinstance(packs, list) or not packs:
        raise ValueError("verified delivery requires at least one real-client evidence record")
    for index, pack in enumerate(packs):
        if not isinstance(pack, dict):
            raise ValueError(f"packs[{index}] must be an object")
        required = ("sha1", "profile", "clientVersion", "testedAt", "result")
        missing = [key for key in required if not isinstance(pack.get(key), str) or not pack[key]]
        if missing:
            raise ValueError(f"packs[{index}] is missing {', '.join(missing)}")
        if len(pack["sha1"]) != 40 or any(char not in "0123456789abcdef" for char in pack["sha1"].lower()):
            raise ValueError(f"packs[{index}].sha1 must be a SHA-1 digest")
        if pack["result"] != "loaded":
            raise ValueError(f"packs[{index}].result must be loaded")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence", type=pathlib.Path, required=True)
    args = parser.parse_args()
    try:
        verify(args.evidence)
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"RESOURCE PACK CLIENT EVIDENCE FAILED: {error}", file=sys.stderr)
        return 1
    print("RESOURCE PACK CLIENT EVIDENCE PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
