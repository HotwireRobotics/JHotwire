"""Phrase-to-command routing for HELIX command input."""

from __future__ import annotations

from helix.config import CommandBinding


class CommandRouter:
	"""Resolves transcribed or typed phrases to a configured command binding."""

	def __init__(self, commands: list[CommandBinding]) -> None:
		"""Store command bindings for phrase matching."""

		self._commands = commands

	def resolve(self, phrase_text: str) -> CommandBinding | None:
		"""Return the first command whose phrase appears in input text."""

		text = phrase_text.strip().lower()
		for command in self._commands:
			for phrase in command.phrases:
				if phrase in text:
					return command
		return None

