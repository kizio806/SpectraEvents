from __future__ import annotations

import json
import pathlib
import subprocess
import sys
import tempfile
import unittest


SCRIPT = pathlib.Path(__file__).with_name("verify_release_gate.py")


class VerifyReleaseGateTest(unittest.TestCase):
    def write_config(self, payload: object) -> pathlib.Path:
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        path = pathlib.Path(directory.name) / "release-blockers.json"
        path.write_text(json.dumps(payload), encoding="utf-8")
        return path

    def run_gate(self, path: pathlib.Path, *arguments: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(SCRIPT), "--file", str(path), *arguments],
            check=False,
            capture_output=True,
            text=True,
        )

    def test_allows_release_when_no_blockers_exist(self) -> None:
        result = self.run_gate(self.write_config({"schemaVersion": 1, "blockers": []}))

        self.assertEqual(0, result.returncode)
        self.assertIn("RELEASE GATE PASS", result.stdout)

    def test_refuses_release_but_allows_ci_notice_for_a_blocker(self) -> None:
        config = {
            "schemaVersion": 1,
            "blockers": [
                {
                    "server": "folia",
                    "version": "26.3",
                    "family": "paper",
                    "status": "blocked-upstream",
                    "reason": "No upstream runtime",
                    "checkedOn": "2026-09-21",
                    "source": "https://example.test/runtime",
                }
            ],
        }

        blocked = self.run_gate(self.write_config(config))
        notice = self.run_gate(self.write_config(config), "--allow-blocked")

        self.assertEqual(1, blocked.returncode)
        self.assertIn("RELEASE GATE BLOCKED", blocked.stdout)
        self.assertEqual(0, notice.returncode)

    def test_rejects_invalid_or_duplicate_blockers(self) -> None:
        result = self.run_gate(self.write_config({"schemaVersion": 1, "blockers": [{"server": "x"}]}))

        self.assertEqual(2, result.returncode)
        self.assertIn("RELEASE GATE INVALID", result.stderr)


if __name__ == "__main__":
    unittest.main()
