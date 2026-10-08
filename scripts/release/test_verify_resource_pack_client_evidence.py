from __future__ import annotations

import json
import pathlib
import subprocess
import sys
import tempfile
import unittest


SCRIPT = pathlib.Path(__file__).with_name("verify_resource_pack_client_evidence.py")


class VerifyResourcePackClientEvidenceTest(unittest.TestCase):
    def write_evidence(self, payload: object) -> pathlib.Path:
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        path = pathlib.Path(directory.name) / "resource-pack-client-evidence.json"
        path.write_text(json.dumps(payload), encoding="utf-8")
        return path

    def run_verifier(self, payload: object) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(SCRIPT), "--evidence", str(self.write_evidence(payload))],
            check=False,
            capture_output=True,
            text=True,
        )

    def test_allows_disabled_delivery_without_evidence_records(self) -> None:
        result = self.run_verifier({"schemaVersion": 1, "delivery": "disabled", "packs": []})

        self.assertEqual(0, result.returncode)
        self.assertIn("PASSED", result.stdout)

    def test_rejects_enabled_delivery_without_real_client_evidence(self) -> None:
        result = self.run_verifier({"schemaVersion": 1, "delivery": "verified", "packs": []})

        self.assertEqual(1, result.returncode)
        self.assertIn("requires at least one", result.stderr)

    def test_accepts_a_loaded_pack_with_required_metadata(self) -> None:
        result = self.run_verifier(
            {
                "schemaVersion": 1,
                "delivery": "verified",
                "packs": [
                    {
                        "sha1": "a" * 40,
                        "profile": "paper",
                        "clientVersion": "26.2",
                        "testedAt": "2026-10-03",
                        "result": "loaded",
                    }
                ],
            }
        )

        self.assertEqual(0, result.returncode)


if __name__ == "__main__":
    unittest.main()
