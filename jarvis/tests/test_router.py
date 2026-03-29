"""Tests for Jarvis phrase routing."""

import unittest

from jarvis.config import CommandBinding
from jarvis.router import CommandRouter


class CommandRouterTests(unittest.TestCase):
	"""Validates phrase routing for common voice commands."""

	def test_resolve_matches_phrase(self) -> None:
		"""Resolves a command when transcript contains a configured phrase."""

		router = CommandRouter(
			[
				CommandBinding(
					name="launch simulation",
					phrases=["launch simulation"],
					command=["echo", "sim"],
				)
			]
		)
		match = router.resolve("jarvis launch simulation now")
		self.assertIsNotNone(match)
		self.assertEqual(match.name, "launch simulation")

	def test_resolve_returns_none_without_match(self) -> None:
		"""Returns None when no configured phrase is present."""

		router = CommandRouter(
			[
				CommandBinding(
					name="deploy code",
					phrases=["deploy code"],
					command=["echo", "deploy"],
				)
			]
		)
		match = router.resolve("jarvis check battery")
		self.assertIsNone(match)


if __name__ == "__main__":
	unittest.main()

