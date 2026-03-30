"""Tests for HELIX command execution helpers."""

import tempfile
import unittest
from pathlib import Path

from helix.actions import _cmd_inner_line, _normalize_gradle_command


class CmdInnerLineTests(unittest.TestCase):
	"""Validates cmd.exe inner line uses ``call`` for batch files."""

	def test_call_prefix_for_bat(self) -> None:
		"""Batch invocations use call so pause runs after the script exits."""

		inner = _cmd_inner_line([r"C:\repo\gradlew.bat", "simulateJava"])
		self.assertTrue(inner.lower().startswith("call "))
		self.assertIn("gradlew.bat", inner)

	def test_no_call_for_exe(self) -> None:
		"""Non-batch commands are unchanged aside from list2cmdline."""

		inner = _cmd_inner_line(["powershell", "-Command", "Write-Host hi"])
		self.assertFalse(inner.lower().startswith("call "))


class GradleNormalizeTests(unittest.TestCase):
	"""Validates Gradle argv normalization against workspace root."""

	def test_normalize_powershell_gradlew_line(self) -> None:
		"""Rewrites PowerShell gradlew invocation to absolute gradlew.bat argv."""

		with tempfile.TemporaryDirectory() as tmp:
			root = Path(tmp)
			(root / "gradlew.bat").write_text("@echo off\r\n", encoding="utf-8")
			cmd = ["powershell", "-NoProfile", "-Command", "./gradlew.bat simulateJava"]
			new_cmd, is_gradle = _normalize_gradle_command(cmd, root)
			self.assertTrue(is_gradle)
			self.assertEqual(Path(new_cmd[0]).resolve(), (root / "gradlew.bat").resolve())
			self.assertEqual(new_cmd[1:], ["simulateJava"])


if __name__ == "__main__":
	unittest.main()
