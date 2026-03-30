"""Tests for HELIX command execution helpers."""

import os
import tempfile
import unittest
from pathlib import Path

from helix.actions import (
	_cmd_inner_line,
	_is_gradle_command,
	_normalize_gradle_command,
	_write_pause_runner_bat,
)


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


class PauseRunnerBatTests(unittest.TestCase):
	"""Validates temp runner script used for separate-console pause."""

	def test_runner_contains_call_and_pause(self) -> None:
		"""Runner bat cds, calls gradlew, then pause."""

		root = tempfile.gettempdir()
		bat = _write_pause_runner_bat(
			root,
			[os.path.join(root, "gradlew.bat"), "simulateJava"],
		)
		try:
			text = Path(bat).read_text(encoding="utf-8")
			self.assertIn("@echo off", text)
			self.assertIn("cd /d", text)
			self.assertIn("call ", text.lower())
			self.assertIn("pause", text.lower())
		finally:
			try:
				os.unlink(bat)
			except OSError:
				pass


class GradleDetectTests(unittest.TestCase):
	"""Driver-station sim script counts as Gradle for workspace cwd + separate console."""

	def test_wpilib_sim_script_is_gradle_like(self) -> None:
		"""PATH containing run_wpilib_sim_ds.ps1 must match."""

		self.assertTrue(
			_is_gradle_command(
				[
					"powershell",
					"-NoProfile",
					"-ExecutionPolicy",
					"Bypass",
					"-File",
					r"helix\scripts\run_wpilib_sim_ds.ps1",
				]
			)
		)


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
