"""Tests for HELIX command execution helpers."""

import tempfile
import unittest
from pathlib import Path

from helix.actions import _normalize_gradle_command


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
