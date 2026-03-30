"""Configuration models and loader for HELIX."""

from __future__ import annotations

from dataclasses import dataclass, field
import json
from pathlib import Path
from typing import Any, Literal


ControlMode = Literal["text", "voice", "both"]
WakewordStrategy = Literal["hey_jarvis", "custom_model", "disabled"]
VerificationMode = Literal["cli", "web", "both"]
NetworkTablesActionType = Literal["set", "get"]


@dataclass
class CommandBinding:
	"""Maps one or more phrases to a shell command."""

	name: str
	phrases: list[str]
	command: list[str]
	working_directory: str | None = None


@dataclass
class VoiceConfig:
	"""Voice activation and transcription settings."""

	wakeword_strategy: WakewordStrategy = "hey_jarvis"
	wakeword_name: str = "hey_jarvis"
	wakeword_model_path: str | None = None
	# Higher = fewer false wakes (TV/room noise); lower if real "Hey Jarvis" is often missed.
	wakeword_threshold: float = 0.55
	# Live wakeword uses max score over the last N chunks (~80ms each). predict_clip peaks across a phrase;
	# single chunks often score lower, so 1 = strict per-chunk only; 12-20 matches real-time behavior to mic-check.
	wakeword_score_window_chunks: int = 16
	wakeword_inference_framework: str = "onnx"
	sample_rate_hz: int = 16000
	chunk_size: int = 1280
	post_wake_record_seconds: float = 4.0
	# Extra delay when no wakeword yet (0 = continuous realtime; >0 gaps the stream and can miss phrases).
	loop_sleep_seconds: float = 0.0
	# Local Whisper (openai-whisper package): tiny, base, small, medium, large.
	whisper_model: str = "base"
	whisper_device: str = "cpu"
	# Optional cache dir for model weights (defaults to system cache / WHISPER_CACHE_DIR).
	whisper_download_root: str | None = None
	# sounddevice input index (see --mic-check device list); None = OS default recording device.
	input_device: int | None = None
	# Multiply captured PCM before wakeword/Whisper (1.0 = off). Use ~3-8 if RMS peaks below ~-55 dBFS when speaking.
	mic_gain: float = 1.0


@dataclass
class NetworkTablesAction:
	"""An optional NetworkTables action within a systems-check step."""

	action: NetworkTablesActionType
	table: str
	key: str
	value: str | float | int | bool | None = None
	expected_value: str | float | int | bool | None = None


@dataclass
class SystemsCheckStep:
	"""A single guided systems-check step."""

	id: str
	title: str
	instructions: str
	command: list[str] | None = None
	working_directory: str | None = None
	networktables_action: NetworkTablesAction | None = None
	expected_observation: str = ""
	safety_notes: str = ""


@dataclass
class SystemsCheckConfig:
	"""Systems-check flow configuration."""

	verification_mode: VerificationMode = "cli"
	web_host: str = "127.0.0.1"
	web_port: int = 8765
	nt_server: str = "127.0.0.1"
	steps: list[SystemsCheckStep] = field(default_factory=list)


@dataclass
class ExecutionConfig:
	"""How HELIX runs subprocesses (separate consoles, Gradle workspace root)."""

	text_mode_separate_console: bool = True
	voice_mode_separate_console: bool = False
	gradle_always_separate_console: bool = True
	gradle_force_workspace_root: bool = True
	# While waiting on a separate-console child, print heartbeat lines (0 = off).
	watch_heartbeat_seconds: float = 30.0
	# After the command: True = "pause" (wait for key); "persist" = cmd /k (shell stays open; HELIX does not wait).
	keep_separate_console_open: bool | str = True


@dataclass
class HelixConfig:
	"""Runtime configuration for HELIX."""

	control_mode: ControlMode = "text"
	command_cooldown_seconds: float = 2.0
	execution: ExecutionConfig = field(default_factory=ExecutionConfig)
	voice: VoiceConfig = field(default_factory=VoiceConfig)
	commands: list[CommandBinding] = field(default_factory=list)
	systems_check: SystemsCheckConfig = field(default_factory=SystemsCheckConfig)


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
	return CommandBinding(
		name=name,
		phrases=phrases,
		command=command,
		working_directory=str(working_directory) if working_directory is not None else None,
	)


def _parse_networktables_action(raw: dict[str, Any] | None) -> NetworkTablesAction | None:
	"""Parse optional NetworkTables step action."""

	if not raw:
		return None
	action = str(raw.get("action", "get")).strip().lower()
	table = str(raw.get("table", "")).strip()
	key = str(raw.get("key", "")).strip()
	if action not in ("set", "get"):
		raise ValueError("networktables_action.action must be 'set' or 'get'.")
	if not table or not key:
		raise ValueError("networktables_action requires both 'table' and 'key'.")
	return NetworkTablesAction(
		action=action,  # type: ignore[arg-type]
		table=table,
		key=key,
		value=raw.get("value"),
		expected_value=raw.get("expected_value"),
	)


def _parse_systems_check_step(raw: dict[str, Any]) -> SystemsCheckStep:
	"""Parse one systems-check step entry."""

	step_id = str(raw.get("id", "")).strip()
	title = str(raw.get("title", "")).strip()
	instructions = str(raw.get("instructions", "")).strip()
	command = [str(i) for i in raw.get("command", []) if str(i).strip()] if raw.get("command") else None
	if not step_id or not title or not instructions:
		raise ValueError("Each systems_check step needs id, title, and instructions.")
	return SystemsCheckStep(
		id=step_id,
		title=title,
		instructions=instructions,
		command=command,
		working_directory=(
			str(raw["working_directory"]).strip()
			if raw.get("working_directory") is not None
			else None
		),
		networktables_action=_parse_networktables_action(raw.get("networktables_action")),
		expected_observation=str(raw.get("expected_observation", "")).strip(),
		safety_notes=str(raw.get("safety_notes", "")).strip(),
	)


def _parse_voice(raw: dict[str, Any]) -> VoiceConfig:
	"""Parse voice configuration block."""

	strategy = str(raw.get("wakeword_strategy", "hey_jarvis")).strip().lower()
	if strategy not in ("hey_jarvis", "custom_model", "disabled"):
		raise ValueError("voice.wakeword_strategy must be hey_jarvis, custom_model, or disabled.")
	wakeword_model_path = str(raw["wakeword_model_path"]).strip() if raw.get("wakeword_model_path") else None
	if strategy == "custom_model" and not wakeword_model_path:
		raise ValueError("voice.wakeword_model_path is required when wakeword_strategy=custom_model.")
	wakeword_name = str(raw.get("wakeword_name", "hey_jarvis")).strip().lower()
	if strategy == "hey_jarvis":
		wakeword_name = "hey_jarvis"
	whisper_download_root = (
		str(raw["whisper_download_root"]).strip()
		if raw.get("whisper_download_root")
		else None
	)
	raw_input = raw.get("input_device")
	input_device: int | None = None
	if raw_input is not None and raw_input != "":
		input_device = int(raw_input)
	mic_gain = float(raw.get("mic_gain", 1.0))
	if mic_gain <= 0:
		raise ValueError("voice.mic_gain must be positive.")
	win = int(raw.get("wakeword_score_window_chunks", 16))
	if win < 1:
		raise ValueError("voice.wakeword_score_window_chunks must be >= 1.")
	return VoiceConfig(
		wakeword_strategy=strategy,  # type: ignore[arg-type]
		wakeword_name=wakeword_name,
		wakeword_model_path=wakeword_model_path,
		wakeword_threshold=float(raw.get("wakeword_threshold", 0.55)),
		wakeword_score_window_chunks=win,
		wakeword_inference_framework=str(raw.get("wakeword_inference_framework", "onnx")).strip().lower(),
		sample_rate_hz=int(raw.get("sample_rate_hz", 16000)),
		chunk_size=int(raw.get("chunk_size", 1280)),
		post_wake_record_seconds=float(raw.get("post_wake_record_seconds", 4.0)),
		loop_sleep_seconds=float(raw.get("loop_sleep_seconds", 0.0)),
		whisper_model=str(raw.get("whisper_model", "base")).strip(),
		whisper_device=str(raw.get("whisper_device", "cpu")).strip().lower(),
		whisper_download_root=whisper_download_root,
		input_device=input_device,
		mic_gain=mic_gain,
	)


def _parse_systems_check(raw: dict[str, Any]) -> SystemsCheckConfig:
	"""Parse systems-check configuration block."""

	verification_mode = str(raw.get("verification_mode", "cli")).strip().lower()
	if verification_mode not in ("cli", "web", "both"):
		raise ValueError("systems_check.verification_mode must be cli, web, or both.")
	steps = [_parse_systems_check_step(step) for step in raw.get("steps", [])]
	return SystemsCheckConfig(
		verification_mode=verification_mode,  # type: ignore[arg-type]
		web_host=str(raw.get("web_host", "127.0.0.1")).strip(),
		web_port=int(raw.get("web_port", 8765)),
		nt_server=str(raw.get("nt_server", "127.0.0.1")).strip(),
		steps=steps,
	)


def load_config(config_path: str | Path) -> HelixConfig:
	"""Load HELIX configuration from a JSON file."""

	path = Path(config_path).resolve()
	with path.open("r", encoding="utf-8") as handle:
		raw = json.load(handle)

	control_mode = str(raw.get("control_mode", "text")).strip().lower()
	if control_mode not in ("text", "voice", "both"):
		raise ValueError("control_mode must be text, voice, or both.")
	ex = raw.get("execution", {})
	keep_open = ex.get("keep_separate_console_open", True)
	if isinstance(keep_open, str) and keep_open.lower() not in ("pause", "persist"):
		raise ValueError("execution.keep_separate_console_open must be true, false, 'pause', or 'persist'.")
	if isinstance(keep_open, str):
		keep_parsed: bool | str = keep_open.lower()
	elif keep_open:
		keep_parsed = True
	else:
		keep_parsed = False
	return HelixConfig(
		control_mode=control_mode,  # type: ignore[arg-type]
		command_cooldown_seconds=float(raw.get("command_cooldown_seconds", 2.0)),
		execution=ExecutionConfig(
			text_mode_separate_console=bool(ex.get("text_mode_separate_console", True)),
			voice_mode_separate_console=bool(ex.get("voice_mode_separate_console", False)),
			gradle_always_separate_console=bool(ex.get("gradle_always_separate_console", True)),
			gradle_force_workspace_root=bool(ex.get("gradle_force_workspace_root", True)),
			watch_heartbeat_seconds=float(ex.get("watch_heartbeat_seconds", 30.0)),
			keep_separate_console_open=keep_parsed,
		),
		voice=_parse_voice(raw.get("voice", {})),
		commands=[_parse_command_binding(item) for item in raw.get("commands", [])],
		systems_check=_parse_systems_check(raw.get("systems_check", {})),
	)

