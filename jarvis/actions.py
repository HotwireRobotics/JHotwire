"""Allowlisted command execution for Jarvis actions."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import subprocess
import time

from jarvis.config import CommandBinding


@dataclass
class ActionResult:
	"""Captures the result of a Jarvis action command."""

	success: bool
	command_name: str
	exit_code: int
	stdout: str
	stderr: str


class ActionRunner:
	"""Executes configured commands and prevents rapid repeated runs."""

	def __init__(self, workspace_root: Path, cooldown_seconds: float) -> None:
		"""Initialize runner state."""

		self._workspace_root = workspace_root
		self._cooldown_seconds = cooldown_seconds
		self._last_execution_at = 0.0

	def _cooldown_active(self) -> bool:
		"""Return True while the cooldown window is active."""

		return (time.monotonic() - self._last_execution_at) < self._cooldown_seconds

	def run(self, binding: CommandBinding) -> ActionResult:
		"""Execute a configured command binding safely."""

		if self._cooldown_active():
			return ActionResult(
				success=False,
				command_name=binding.name,
				exit_code=1,
				stdout="",
				stderr="Cooldown active; command skipped.",
			)

		working_directory = (
			Path(binding.working_directory).resolve()
			if binding.working_directory
			else self._workspace_root
		)
		completed = subprocess.run(
			binding.command,
			cwd=str(working_directory),
			capture_output=True,
			text=True,
			check=False,
		)
		self._last_execution_at = time.monotonic()
		return ActionResult(
			success=completed.returncode == 0,
			command_name=binding.name,
			exit_code=completed.returncode,
			stdout=completed.stdout.strip(),
			stderr=completed.stderr.strip(),
		)

