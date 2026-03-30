"""Compatibility entrypoint forwarding Jarvis to HELIX."""

from helix.main import main


if __name__ == "__main__":
	print("[helix] python -m jarvis.main is deprecated. Use python -m helix.main.")
	main()

