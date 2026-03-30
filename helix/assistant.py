"""HELIX runtime loop orchestration."""

from __future__ import annotations

from pathlib import Path
import threading
import time

from helix.actions import ActionRunner, RunContext
from helix.audio import Microphone, WakewordDetector
from helix.config import HelixConfig
from helix.router import CommandRouter
from helix.systems_check import SystemsCheckRunner
from helix.transcribe import WhisperTranscriber


class HelixSubsystem:
	"""Coordinates text input, optional wakeword voice, and execution."""

	def __init__(self, config: HelixConfig, workspace_root: Path) -> None:
		"""Construct HELIX dependencies."""

		self._config = config
		self._workspace_root = workspace_root
		self._router = CommandRouter(config.commands)
		self._runner = ActionRunner(
			workspace_root=workspace_root,
			cooldown_seconds=config.command_cooldown_seconds,
			execution=config.execution,
		)
		self._systems_runner = SystemsCheckRunner(config, self._runner, workspace_root)
		self._voice_enabled = config.control_mode in ("voice", "both")
		self._voice_ready = False
		self._microphone: Microphone | None = None
		self._detector: WakewordDetector | None = None
		self._transcriber: WhisperTranscriber | None = None
		self._shutdown = False
		self._operation_lock = threading.Lock()

		if self._voice_enabled and config.voice.wakeword_strategy != "disabled":
			self._microphone = Microphone(config.voice)
			self._detector = WakewordDetector(config.voice)
			self._transcriber = WhisperTranscriber(config.voice)
			self._voice_ready = True
		elif self._voice_enabled:
			print("[helix] Voice mode requested but wakeword strategy is disabled.")

	def _run_named_command(self, command_name: str) -> None:
		"""Run a configured command by exact name."""

		ctx = RunContext(
			control_mode=self._config.control_mode,
			invoked_from_text=True,
		)
		for binding in self._config.commands:
			if binding.name == command_name:
				print(f"[helix] Running command '{binding.name}'...")
				result = self._runner.run(binding, ctx)
				if result.success:
					print(f"[helix] '{binding.name}' completed.")
				else:
					print(f"[helix] '{binding.name}' failed (exit={result.exit_code}): {result.stderr}")
				if result.stdout:
					print(f"[helix] stdout: {result.stdout}")
				return
		print(f"[helix] Unknown command name: {command_name}")

	def _print_help(self) -> None:
		"""Print operator-facing command help."""

		print("\nHELIX text commands:")
		print("  help                    Show this help.")
		print("  list                    List configured commands.")
		print("  run <command_name>      Run one command by exact name (see list).")
		print("  systems check           Run guided systems check.")
		print("  quit                    Exit HELIX.")
		print("  or type any phrase mapped in config commands.")

	def _process_text(self, text: str, *, invoked_from_text: bool = True) -> None:
		"""Process typed/transcribed command text with lock safety."""

		entry = text.strip()
		if not entry:
			return
		ctx = RunContext(
			control_mode=self._config.control_mode,
			invoked_from_text=invoked_from_text,
		)
		with self._operation_lock:
			lower = entry.lower()
			if lower in ("help", "?"):
				self._print_help()
				return
			if lower == "list":
				print("[helix] Configured commands:")
				for binding in self._config.commands:
					print(f"  - {binding.name}")
				return
			if lower.startswith("run "):
				self._run_named_command(entry[4:].strip())
				return
			if lower in ("systems check", "systems_check"):
				self._systems_runner.run()
				return
			if lower in ("quit", "exit"):
				self._shutdown = True
				return
			binding = self._router.resolve(lower)
			if binding is None:
				print(f"[helix] No command match for: {entry!r}")
				return
			print(f"[helix] Running command '{binding.name}'...")
			result = self._runner.run(binding, ctx)
			if result.success:
				print(f"[helix] '{binding.name}' completed.")
			else:
				print(f"[helix] '{binding.name}' failed (exit={result.exit_code}): {result.stderr}")
			if result.stdout:
				print(f"[helix] stdout: {result.stdout}")

	def _voice_loop(self) -> None:
		"""Run wakeword + transcription loop for voice control."""

		assert self._microphone is not None
		assert self._detector is not None
		assert self._transcriber is not None
		print("[helix] Voice loop active. Listening for wakeword...")
		while not self._shutdown:
			chunk = self._microphone.read_chunk()
			if not self._detector.is_wakeword_detected(chunk):
				time.sleep(self._config.voice.loop_sleep_seconds)
				continue
			print("[helix] Wakeword detected. Listening for command...", flush=True)
			audio = self._microphone.record_utterance()
			transcript = self._transcriber.transcribe(
				audio=audio,
				sample_rate_hz=self._config.voice.sample_rate_hz,
			).strip()
			# Always echo the transcribed phrase so the operator sees what was understood.
			print(f"[helix] Heard: {transcript!r}", flush=True)
			if not transcript:
				print("[helix] (empty transcript — try speaking closer or check the mic.)", flush=True)
				continue
			self._process_text(transcript, invoked_from_text=False)

	def _text_loop(self) -> None:
		"""Run text input loop."""

		print("[helix] Text mode active. Type 'help' for commands.")
		while not self._shutdown:
			try:
				entry = input("HELIX> ")
			except EOFError:
				self._shutdown = True
				break
			self._process_text(entry)

	def run_forever(self) -> None:
		"""Run HELIX according to configured control mode."""

		mode = self._config.control_mode
		print(f"[helix] Starting in control_mode={mode}")
		if mode == "voice":
			if not self._voice_ready:
				raise RuntimeError("Voice mode configured, but voice components are not ready.")
			self._voice_loop()
			return
		if mode == "both":
			if self._voice_ready:
				thread = threading.Thread(target=self._voice_loop, daemon=True)
				thread.start()
			self._text_loop()
			return
		self._text_loop()

