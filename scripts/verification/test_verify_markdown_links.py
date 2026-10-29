import tempfile
import unittest
from pathlib import Path

from scripts.verification.verify_markdown_links import missing_links


class MarkdownLinkVerificationTest(unittest.TestCase):
    def test_accepts_existing_relative_target_and_external_link(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "docs").mkdir()
            (root / "docs" / "target.md").write_text("# Target\n", encoding="utf-8")
            (root / "README.md").write_text(
                "[local](docs/target.md) [web](https://example.com)\n", encoding="utf-8"
            )

            self.assertEqual([], missing_links(root))

    def test_reports_source_line_and_missing_target(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "README.md").write_text(
                "first line\n[missing](docs/missing.md#section)\n", encoding="utf-8"
            )

            self.assertEqual(
                ["README.md:2: missing docs/missing.md#section"], missing_links(root)
            )

    def test_ignores_generated_and_server_directories(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "build").mkdir()
            (root / "build" / "generated.md").write_text(
                "[missing](nope.md)\n", encoding="utf-8"
            )

            self.assertEqual([], missing_links(root))


if __name__ == "__main__":
    unittest.main()
