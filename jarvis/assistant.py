"""Jarvis runtime loop orchestration."""

from __future__ import annotations

from pathlib import Path
import time

from jarvis.actions import ActionRunner
from jarvis.audio import Microphone, WakewordDetector
from jarvis.config import JarvisConfig
from jarvis.router import CommandRouter
from jarvis.transcribe import WhisperTranscriber


class JarvisSubsystem:
	"""Coordinates wakeword, transcription, routing, and execution."""

	def __init__(self, config: JarvisConfig, workspace_root: Path) -> None:
		"""Construct all subsystem dependencies."""

		self._config = config
		self._microphone = Microphone(config)
		self._detector = WakewordDetector(config)
		self._transcriber = WhisperTranscriber()
		self._router = CommandRouter(config.commands)
		self._runner = ActionRunner(
			workspace_root=workspace_root,
			cooldown_seconds=config.command_cooldown_seconds,
		)

	def _on_command(self, transcript: str) -> None:
		"""Route transcript and execute a matching action."""

		binding = self._router.resolve(transcript)
		if binding is None:
			print(f"[jarvis] No command match for: {transcript!r}")
			return

		print(f"[jarvis] Running command '{binding.name}'...")
		result = self._runner.run(binding)
		if result.success:
			print(f"[jarvis] '{binding.name}' completed.")
		else:
			print(
				f"[jarvis] '{binding.name}' failed "
				f"(exit={result.exit_code}): {result.stderr or 'unknown error'}"
			)
		if result.stdout:
			print(f"[jarvis] stdout: {result.stdout}")

	def run_forever(self) -> None:
		"""Start the always-on listener loop."""

		print("[jarvis] Listening for wakeword...")
		while True:
			chunk = self._microphone.read_chunk()
			if not self._detector.is_wakeword_detected(chunk):
				time.sleep(self._config.loop_sleep_seconds)
				continue

			print("[jarvis] Wakeword detected. Listening for command...")
			audio = self._microphone.record_utterance(self._config.post_wake_record_seconds)
			transcript = self._transcriber.transcribe(
				audio=audio,
				sample_rate_hz=self._config.sample_rate_hz,
			)
			print(f"[jarvis] Heard: {transcript!r}")
			self._on_command(transcript)

