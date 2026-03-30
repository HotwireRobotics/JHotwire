"""Backward-compatible wrapper to run HELIX from old script name."""

from helix.main import main


if __name__ == "__main__":
	print("[helix] run_jarvis.py is deprecated. Use run_helix.py instead.")
	main()

