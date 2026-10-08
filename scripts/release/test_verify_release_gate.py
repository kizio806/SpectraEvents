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
        result = self.run_gate(
            self.write_config({"schemaVersion": 1, "blockers": []}),
            "--runtime-matrix-status",
            "success",
        )

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

        blocked = self.run_gate(
            self.write_config(config), "--runtime-matrix-status", "success"
        )
        notice = self.run_gate(
            self.write_config(config),
            "--runtime-matrix-status",
            "success",
            "--allow-blocked",
        )

        self.assertEqual(1, blocked.returncode)
        self.assertIn("RELEASE GATE BLOCKED", blocked.stdout)
        self.assertEqual(0, notice.returncode)

    def test_refuses_to_pass_without_or_after_a_failed_runtime_matrix(self) -> None:
        path = self.write_config({"schemaVersion": 1, "blockers": []})

        missing = self.run_gate(path)
        failed = self.run_gate(path, "--runtime-matrix-status", "failure")

        self.assertEqual(2, missing.returncode)
        self.assertIn("runtime-matrix-status", missing.stderr)
        self.assertEqual(1, failed.returncode)
        self.assertIn("RELEASE GATE BLOCKED", failed.stdout)

    def test_default_configuration_allows_the_declared_runtime_matrix(self) -> None:
        result = subprocess.run(
            [sys.executable, str(SCRIPT), "--runtime-matrix-status", "success"],
            check=False,
            capture_output=True,
            text=True,
        )

        self.assertEqual(0, result.returncode)
        self.assertIn("RELEASE GATE PASS", result.stdout)

    def test_rejects_invalid_or_duplicate_blockers(self) -> None:
        result = self.run_gate(
            self.write_config({"schemaVersion": 1, "blockers": [{"server": "x"}]}),
            "--runtime-matrix-status",
            "success",
        )

        self.assertEqual(2, result.returncode)
        self.assertIn("RELEASE GATE INVALID", result.stderr)


if __name__ == "__main__":
    unittest.main()
