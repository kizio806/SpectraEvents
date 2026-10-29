#!/usr/bin/env python3
"""Fail when a repository Markdown file contains a missing relative link target."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from urllib.parse import unquote, urlsplit


INLINE_LINK = re.compile(r"(?<!!)\[[^\]]*\]\(([^)]+)\)")
SKIPPED_SCHEMES = {"http", "https", "mailto", "tel"}
SKIPPED_DIRECTORIES = {".git", ".gradle", "build", "server"}


def markdown_files(root: Path) -> list[Path]:
    return sorted(
        path
        for path in root.rglob("*.md")
        if not any(part in SKIPPED_DIRECTORIES for part in path.relative_to(root).parts)
    )


def link_destination(raw: str) -> str:
    destination = raw.strip()
    if destination.startswith("<") and ">" in destination:
        return destination[1 : destination.index(">")]
    return destination.split(maxsplit=1)[0]


def missing_links(root: Path) -> list[str]:
    failures: list[str] = []
    for document in markdown_files(root):
        for line_number, line in enumerate(document.read_text(encoding="utf-8").splitlines(), 1):
            for match in INLINE_LINK.finditer(line):
                destination = link_destination(match.group(1))
                parsed = urlsplit(destination)
                if not parsed.path or parsed.scheme.lower() in SKIPPED_SCHEMES:
                    continue
                if parsed.scheme or destination.startswith("//"):
                    continue
                relative_path = Path(unquote(parsed.path))
                target = (
                    root / relative_path.relative_to("/")
                    if relative_path.is_absolute()
                    else document.parent / relative_path
                )
                if not target.resolve().exists():
                    failures.append(
                        f"{document.relative_to(root)}:{line_number}: missing {destination}"
                    )
    return failures


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    args = parser.parse_args()
    root = args.root.resolve()
    failures = missing_links(root)
    if failures:
        print("Markdown link verification failed:", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1
    print(f"Markdown link verification passed ({len(markdown_files(root))} files).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
