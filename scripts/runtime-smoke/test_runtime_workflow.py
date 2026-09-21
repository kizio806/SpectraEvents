from __future__ import annotations

import importlib.util
import pathlib
import types
import unittest


SCRIPT = pathlib.Path(__file__).with_name("runtime_workflow.py")
SPEC = importlib.util.spec_from_file_location("runtime_workflow", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
RUNTIME_WORKFLOW = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RUNTIME_WORKFLOW)


class RuntimeWorkflowTest(unittest.TestCase):
    def session_with_exit_code(self, exit_code: int) -> object:
        session = object.__new__(RUNTIME_WORKFLOW.ServerSession)
        session.process = types.SimpleNamespace(returncode=exit_code)
        return session

    def test_ignores_only_spigot_console_reader_error_after_clean_exit(self) -> None:
        session = self.session_with_exit_code(0)

        self.assertTrue(
            session._is_benign_console_shutdown_error(
                "[Server console handler/ERROR]: Exception handling console input"
            )
        )
        self.assertFalse(session._is_benign_console_shutdown_error("[Server thread/ERROR]: plugin error"))

    def test_does_not_ignore_console_error_after_failed_exit(self) -> None:
        session = self.session_with_exit_code(1)

        self.assertFalse(
            session._is_benign_console_shutdown_error(
                "[Server console handler/ERROR]: Exception handling console input"
            )
        )


if __name__ == "__main__":
    unittest.main()
