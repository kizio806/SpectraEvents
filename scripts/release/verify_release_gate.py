#!/usr/bin/env python3
"""Refuse publication without a successful real-server runtime matrix."""

from __future__ import annotations

import argparse
import json
import pathlib
import sys
from typing import Any


ROOT = pathlib.Path(__file__).resolve().parents[2]
DEFAULT_BLOCKERS = ROOT / "config" / "release" / "release-blockers.json"
REQUIRED_FIELDS = {"server", "version", "family", "status", "reason", "checkedOn", "source"}
ALLOWED_STATUSES = {"blocked-upstream", "blocked-failed", "blocked-unverified"}


def load_blockers(path: pathlib.Path) -> list[dict[str, str]]:
    try:
        payload: Any = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as error:
        raise ValueError(f"Missing release blocker file: {path}") from error
    except json.JSONDecodeError as error:
        raise ValueError(f"Invalid release blocker JSON at {path}: {error.msg}") from error

    if not isinstance(payload, dict) or payload.get("schemaVersion") != 1:
        raise ValueError("Release blocker file must declare schemaVersion 1")
    blockers = payload.get("blockers")
    if not isinstance(blockers, list):
        raise ValueError("Release blocker file must contain a blockers list")

    seen: set[tuple[str, str, str]] = set()
    validated: list[dict[str, str]] = []
    for index, blocker in enumerate(blockers):
        if not isinstance(blocker, dict) or set(blocker) != REQUIRED_FIELDS:
            raise ValueError(f"Blocker #{index + 1} must contain exactly {sorted(REQUIRED_FIELDS)}")
        if not all(isinstance(value, str) and value.strip() for value in blocker.values()):
            raise ValueError(f"Blocker #{index + 1} contains an empty or non-string field")
        if blocker["status"] not in ALLOWED_STATUSES:
            raise ValueError(f"Blocker #{index + 1} has unsupported status {blocker['status']!r}")
        if not blocker["source"].startswith("https://"):
            raise ValueError(f"Blocker #{index + 1} source must use HTTPS")
        identity = (blocker["server"], blocker["version"], blocker["family"])
        if identity in seen:
            raise ValueError(f"Duplicate runtime blocker for {identity}")
        seen.add(identity)
        validated.append(blocker)
    return validated


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", type=pathlib.Path, default=DEFAULT_BLOCKERS)
    parser.add_argument(
        "--allow-blocked",
        action="store_true",
        help="Validate and report blockers without failing; intended only for non-release CI.",
    )
    parser.add_argument(
        "--runtime-matrix-status",
        choices={"success", "failure", "cancelled", "skipped"},
        help="Result of the required real-server matrix; release eligibility must provide it.",
    )
    args = parser.parse_args()

    try:
        blockers = load_blockers(args.file)
    except ValueError as error:
        print(f"RELEASE GATE INVALID: {error}", file=sys.stderr)
        return 2

    if args.runtime_matrix_status is None:
        print(
            "RELEASE GATE INVALID: --runtime-matrix-status is required; "
            "a source-only check is not release evidence.",
            file=sys.stderr,
        )
        return 2
    if args.runtime_matrix_status != "success":
        print(
            "RELEASE GATE BLOCKED: required real-server runtime matrix result is "
            f"{args.runtime_matrix_status}.",
        )
        return 0 if args.allow_blocked else 1

    if not blockers:
        print("RELEASE GATE PASS: runtime matrix succeeded and no required runtime rows are blocked.")
        return 0

    print("RELEASE GATE BLOCKED: publication is forbidden until every row below is verified.")
    for blocker in blockers:
        print(
            "- "
            f"{blocker['server']} {blocker['version']} ({blocker['family']}): "
            f"{blocker['status']}; {blocker['reason']} "
            f"(last checked {blocker['checkedOn']}; source {blocker['source']})"
        )
    return 0 if args.allow_blocked else 1


if __name__ == "__main__":
    raise SystemExit(main())
