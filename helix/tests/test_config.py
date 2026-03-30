"""Tests for HELIX configuration parsing."""

import json
import tempfile
import unittest
from pathlib import Path

from helix.config import load_config


class HelixConfigTests(unittest.TestCase):
	"""Validates config parsing for control modes and systems checks."""

	def _write_config(self, payload: dict) -> Path:
		"""Write temporary JSON config for parser tests."""

		handle = tempfile.NamedTemporaryFile("w", suffix=".json", delete=False)
		handle.write(json.dumps(payload))
		handle.close()
		return Path(handle.name)

	def test_load_config_text_mode(self) -> None:
		"""Parses text mode and systems-check steps successfully."""

		path = self._write_config(
			{
				"control_mode": "text",
				"voice": {"wakeword_strategy": "disabled"},
				"commands": [],
				"systems_check": {
					"verification_mode": "both",
					"steps": [
						{
							"id": "s1",
							"title": "Step One",
							"instructions": "Do thing",
							"networktables_action": {
								"action": "get",
								"table": "SmartDashboard",
								"key": "ready",
							},
						}
					],
				},
			}
		)
		config = load_config(path)
		self.assertEqual(config.control_mode, "text")
		self.assertEqual(config.voice.wakeword_strategy, "disabled")
		self.assertEqual(config.systems_check.verification_mode, "both")
		self.assertEqual(len(config.systems_check.steps), 1)

	def test_custom_model_requires_path(self) -> None:
		"""Rejects custom wakeword mode without a model file path."""

		path = self._write_config(
			{
				"control_mode": "voice",
				"voice": {"wakeword_strategy": "custom_model"},
				"commands": [],
				"systems_check": {"steps": []},
			}
		)
		with self.assertRaises(ValueError):
			load_config(path)


if __name__ == "__main__":
	unittest.main()

