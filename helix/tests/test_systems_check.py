"""Tests for HELIX systems-check decision handling."""

import unittest

from helix.systems_check import should_advance_after_decision


class SystemsCheckDecisionTests(unittest.TestCase):
	"""Verifies decision transitions in systems-check flow."""

	def test_retry_does_not_advance(self) -> None:
		"""Retry keeps the step pointer on the same step."""

		self.assertFalse(should_advance_after_decision("retry"))

	def test_terminal_decisions_advance(self) -> None:
		"""Pass/fail/skip should advance to next step."""

		self.assertTrue(should_advance_after_decision("pass"))
		self.assertTrue(should_advance_after_decision("fail"))
		self.assertTrue(should_advance_after_decision("skip"))


if __name__ == "__main__":
	unittest.main()

