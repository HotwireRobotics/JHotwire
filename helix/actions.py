"""Allowlisted command execution for HELIX actions."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re
import subprocess
import sys
import time

from helix.config import CommandBinding, ExecutionConfig


@dataclass
class ActionResult:
	"""Captures the result of a HELIX action command."""

	success: bool
	command_name: str
	exit_code: int
	stdout: str
	stderr: str


@dataclass
class RunContext:
	"""Tells the runner how this command was invoked (for console + Gradle policy)."""

	control_mode: str = "text"
	invoked_from_text: bool = True


def _is_gradle_command(command: list[str]) -> bool:
	"""Return True if the argv looks like a Gradle / gradlew invocation."""

	return any("gradlew" in part.lower() for part in command)


def _normalize_gradle_command(command: list[str], workspace_root: Path) -> tuple[list[str], bool]:
	"""Rewrite gradlew invocations to use workspace_root gradlew and argv list form.

	Returns (new_argv, was_gradle).
	"""

	if "gradlew" not in " ".join(command).lower():
		return command, False
	gradlew_bat = workspace_root / "gradlew.bat"
	gradlew_sh = workspace_root / "gradlew"
	if gradlew_bat.is_file():
		gradlew_path = gradlew_bat
	elif gradlew_sh.is_file():
		gradlew_path = gradlew_sh
	else:
		return command, False
	for part in command:
		if "gradlew" not in part.lower():
			continue
		match = re.search(r"gradlew(?:\.bat)?\s+(.*)$", part.strip(), re.I | re.DOTALL)
		if match:
			tail = match.group(1).strip()
			args = tail.split() if tail else []
			return [str(gradlew_path)] + args, True
		if re.search(r"gradlew(?:\.bat)?\s*$", part.strip(), re.I):
			return [str(gradlew_path)], True
	return [str(gradlew_path)], True


def _should_use_separate_console(
	execution: ExecutionConfig,
	control_mode: str,
	invoked_from_text: bool,
	is_gradle: bool,
) -> bool:
	"""Decide whether to spawn a new OS console (Windows) for this run."""

	if is_gradle and execution.gradle_always_separate_console:
		return True
	if invoked_from_text and execution.text_mode_separate_console:
		return control_mode in ("text", "both")
	if not invoked_from_text and execution.voice_mode_separate_console:
		return control_mode in ("voice", "both")
	return False


def _resolve_working_directory(
	binding: CommandBinding,
	workspace_root: Path,
	execution: ExecutionConfig,
	is_gradle: bool,
) -> Path:
	"""Pick cwd: workspace root for Gradle when forced, else binding or workspace."""

	if is_gradle and execution.gradle_force_workspace_root:
		return workspace_root.resolve()
	if binding.working_directory:
		p = Path(binding.working_directory)
		if not p.is_absolute():
			return (workspace_root / p).resolve()
		return p.resolve()
	return workspace_root.resolve()


class ActionRunner:
	"""Executes configured commands and prevents rapid repeated runs."""

	def __init__(
		self,
		workspace_root: Path,
		cooldown_seconds: float,
		execution: ExecutionConfig,
	) -> None:
		"""Initialize runner state."""

		self._workspace_root = workspace_root.resolve()
		self._cooldown_seconds = cooldown_seconds
		self._execution = execution
		self._last_execution_at = 0.0

	def _cooldown_active(self) -> bool:
		"""Return True while the cooldown window is active."""

		return (time.monotonic() - self._last_execution_at) < self._cooldown_seconds

	def run(self, binding: CommandBinding, context: RunContext | None = None) -> ActionResult:
		"""Execute a configured command binding safely."""

		if self._cooldown_active():
			return ActionResult(
				success=False,
				command_name=binding.name,
				exit_code=1,
				stdout="",
				stderr="Cooldown active; command skipped.",
			)
		ctx = context or RunContext(control_mode="text", invoked_from_text=True)
		raw_cmd = list(binding.command)
		normalized, is_gradle = _normalize_gradle_command(raw_cmd, self._workspace_root)
		cmd = normalized
		cwd = _resolve_working_directory(
			binding,
			self._workspace_root,
			self._execution,
			is_gradle or _is_gradle_command(raw_cmd),
		)
		is_gradle = is_gradle or _is_gradle_command(raw_cmd)
		separate = _should_use_separate_console(
			self._execution,
			ctx.control_mode,
			ctx.invoked_from_text,
			is_gradle,
		)
		if separate and sys.platform == "win32":
			result = self._run_windows_separate_console(binding.name, cmd, cwd)
			self._last_execution_at = time.monotonic()
			return result
		if separate and sys.platform != "win32":
			print("[helix] separate_console is only supported on Windows; running inline.")
		completed = subprocess.run(
			cmd,
			cwd=str(cwd),
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

	def _run_windows_separate_console(self, command_name: str, cmd: list[str], cwd: Path) -> ActionResult:
		"""Run command in a new console window; optionally keep it open after exit."""

		creationflags = getattr(subprocess, "CREATE_NEW_CONSOLE", 0)
		if not creationflags:
			creationflags = 0x00000010
		keep = self._execution.keep_separate_console_open
		cwd_s = str(cwd.resolve())
		line = subprocess.list2cmdline(cmd)
		print(f"[helix] Spawning separate console for '{command_name}' (cwd={cwd_s})...")
		print(f"[helix] Command: {line}")

		# Persist: cmd /k leaves an interactive shell open; HELIX does not block on it.
		if keep == "persist":
			full = f'cd /d "{cwd_s}" && {line}'
			try:
				proc = subprocess.Popen(
					["cmd.exe", "/k", full],
					creationflags=creationflags,
					stdin=subprocess.DEVNULL,
					stdout=None,
					stderr=None,
				)
			except OSError as exc:
				return ActionResult(
					success=False,
					command_name=command_name,
					exit_code=1,
					stdout="",
					stderr=f"Failed to start process: {exc}",
				)
			print(f"[helix] Separate console PID {proc.pid} stays open (persist). Close that window when finished.")
			return ActionResult(
				success=True,
				command_name=command_name,
				exit_code=0,
				stdout="Separate window left open (persist). Review output there; close the window when done.",
				stderr="",
			)

		try:
			if keep is False:
				proc = subprocess.Popen(
					cmd,
					cwd=cwd_s,
					creationflags=creationflags,
					stdout=None,
					stderr=None,
					stdin=subprocess.DEVNULL,
				)
			else:
				# True or "pause": run command, then pause so the window does not vanish immediately.
				full = f'cd /d "{cwd_s}" && {line} & pause'
				proc = subprocess.Popen(
					["cmd.exe", "/c", full],
					creationflags=creationflags,
					stdin=subprocess.DEVNULL,
					stdout=None,
					stderr=None,
				)
		except OSError as exc:
			return ActionResult(
				success=False,
				command_name=command_name,
				exit_code=1,
				stdout="",
				stderr=f"Failed to start process: {exc}",
			)
		print(f"[helix] Watching separate-console process PID {proc.pid} (output appears in that window).")
		if keep is not False:
			print("[helix] That window will wait for a key after the command (pause).")
		heartbeat = self._execution.watch_heartbeat_seconds
		while True:
			try:
				if heartbeat and heartbeat > 0:
					proc.wait(timeout=heartbeat)
					break
				proc.wait()
				break
			except subprocess.TimeoutExpired:
				print(f"[helix] Still watching PID {proc.pid}...")
		code = proc.returncode if proc.returncode is not None else -1
		summary = (
			f"Separate-console run finished: exit_code={code}. "
			f"If you used pause, this reflects cmd.exe after you pressed a key."
		)
		return ActionResult(
			success=code == 0,
			command_name=command_name,
			exit_code=code,
			stdout=summary,
			stderr="",
		)
