"""Entrypoint for the isolated Jarvis subsystem."""

from __future__ import annotations

import argparse
from pathlib import Path

from jarvis.assistant import JarvisSubsystem
from jarvis.config import load_config


def build_parser() -> argparse.ArgumentParser:
	"""Create CLI argument parser for Jarvis."""

	parser = argparse.ArgumentParser(description="Jarvis voice assistant subsystem.")
	parser.add_argument(
		"--config",
		default="jarvis/config.json",
		help="Path to Jarvis JSON config file.",
	)
	parser.add_argument(
		"--workspace-root",
		default=".",
		help="Workspace root used for action command execution.",
	)
	return parser


def main() -> None:
	"""Load config and run Jarvis forever."""

	args = build_parser().parse_args()
	config = load_config(args.config)
	workspace_root = Path(args.workspace_root).resolve()

	jarvis = JarvisSubsystem(config=config, workspace_root=workspace_root)
	jarvis.run_forever()


if __name__ == "__main__":
	main()

