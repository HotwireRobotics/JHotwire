"""Entrypoint for the HELIX subsystem."""

from __future__ import annotations

import argparse
from dataclasses import replace
from pathlib import Path

from helix.assistant import HelixSubsystem
from helix.config import load_config
from helix.mic_diagnostic import run_mic_diagnostic


def build_parser() -> argparse.ArgumentParser:
	"""Create CLI argument parser for HELIX."""

	parser = argparse.ArgumentParser(
		description="HELIX: Heuristic Engine for Logging, Integration, and X-ecution."
	)
	parser.add_argument(
		"--config",
		default="helix/config.json",
		help="Path to HELIX JSON config file.",
	)
	parser.add_argument(
		"--workspace-root",
		default=".",
		help="Workspace root used for action command execution.",
	)
	parser.add_argument(
		"--mic-check",
		action="store_true",
		help="List input devices, show live RMS and wakeword scores, then exit (no main loop).",
	)
	parser.add_argument(
		"--input-device",
		type=int,
		default=None,
		metavar="N",
		help="sounddevice capture device index (overrides voice.input_device in config). See device list from --mic-check.",
	)
	parser.add_argument(
		"--voice-debug",
		action="store_true",
		help="Print rolling wakeword score vs threshold once per second (voice/both modes).",
	)
	return parser


def main() -> None:
	"""Load config and run HELIX forever."""

	args = build_parser().parse_args()
	config = load_config(args.config)
	voice = config.voice
	if args.input_device is not None:
		voice = replace(voice, input_device=args.input_device)
	if args.mic_check:
		run_mic_diagnostic(voice)
		return
	config = replace(config, voice=voice)
	workspace_root = Path(args.workspace_root).resolve()
	helix = HelixSubsystem(config=config, workspace_root=workspace_root, voice_debug=args.voice_debug)
	helix.run_forever()


if __name__ == "__main__":
	main()

