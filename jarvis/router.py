"""Phrase-to-command routing for transcribed voice input."""

from __future__ import annotations

from jarvis.config import CommandBinding


class CommandRouter:
	"""Resolves transcribed phrases to a configured command binding."""

	def __init__(self, commands: list[CommandBinding]) -> None:
		"""Store command bindings for phrase matching."""

		self._commands = commands

	def resolve(self, transcript: str) -> CommandBinding | None:
		"""Return the first command whose phrase appears in transcript."""

		text = transcript.strip().lower()
		for command in self._commands:
			for phrase in command.phrases:
				if phrase in text:
					return command
		return None

