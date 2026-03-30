"""Tests for HELIX phrase routing."""

import unittest

from helix.config import CommandBinding
from helix.router import CommandRouter


class CommandRouterTests(unittest.TestCase):
	"""Validates phrase routing for common assistant commands."""

	def test_resolve_matches_phrase(self) -> None:
		"""Resolves command when phrase appears in input text."""

		router = CommandRouter(
			[
				CommandBinding(
					name="launch simulation",
					phrases=["launch simulation"],
					command=["echo", "sim"],
				)
			]
		)
		match = router.resolve("helix launch simulation now")
		self.assertIsNotNone(match)
		assert match is not None
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
		match = router.resolve("helix check battery")
		self.assertIsNone(match)


if __name__ == "__main__":
	unittest.main()

