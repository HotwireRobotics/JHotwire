"""Configuration models and loader for Jarvis."""

from __future__ import annotations

from dataclasses import dataclass, field
import json
from pathlib import Path
from typing import Any


@dataclass
class CommandBinding:
	"""Maps one or more phrases to a shell command."""

	name: str
	phrases: list[str]
	command: list[str]
	working_directory: str | None = None


@dataclass
class JarvisConfig:
	"""Runtime configuration for the Jarvis subsystem."""

	wakeword_threshold: float = 0.5
	wakeword_name: str = "hey_jarvis"
	wakeword_model_path: str | None = None
	sample_rate_hz: int = 16000
	chunk_size: int = 1280
	post_wake_record_seconds: float = 4.0
	loop_sleep_seconds: float = 0.05
	command_cooldown_seconds: float = 2.0
	commands: list[CommandBinding] = field(default_factory=list)


def _parse_command_binding(raw: dict[str, Any]) -> CommandBinding:
	"""Convert a raw dictionary into a validated command binding."""

	name = str(raw.get("name", "")).strip()
	phrases = [str(i).strip().lower() for i in raw.get("phrases", []) if str(i).strip()]
	command = [str(i) for i in raw.get("command", []) if str(i).strip()]
	working_directory = raw.get("working_directory")

	if not name:
		raise ValueError("Each command must include a non-empty 'name'.")
	if not phrases:
		raise ValueError(f"Command '{name}' requires at least one phrase.")
	if not command:
		raise ValueError(f"Command '{name}' requires a non-empty command array.")
	if working_directory is not None:
		working_directory = str(working_directory)

	return CommandBinding(
		name=name,
		phrases=phrases,
		command=command,
		working_directory=working_directory,
	)


def load_config(config_path: str | Path) -> JarvisConfig:
	"""Load Jarvis configuration from a JSON file."""

	path = Path(config_path).resolve()
	with path.open("r", encoding="utf-8") as handle:
		raw = json.load(handle)

	commands = [_parse_command_binding(item) for item in raw.get("commands", [])]
	return JarvisConfig(
		wakeword_threshold=float(raw.get("wakeword_threshold", 0.5)),
		wakeword_name=str(raw.get("wakeword_name", "hey_jarvis")).strip().lower(),
		wakeword_model_path=(
			str(raw["wakeword_model_path"]).strip()
			if raw.get("wakeword_model_path")
			else None
		),
		sample_rate_hz=int(raw.get("sample_rate_hz", 16000)),
		chunk_size=int(raw.get("chunk_size", 1280)),
		post_wake_record_seconds=float(raw.get("post_wake_record_seconds", 4.0)),
		loop_sleep_seconds=float(raw.get("loop_sleep_seconds", 0.05)),
		command_cooldown_seconds=float(raw.get("command_cooldown_seconds", 2.0)),
		commands=commands,
	)

