# HELIX Subsystem

HELIX is an almost fully isolated assistant subsystem for robot workflow automation.

HELIX stands for **Heuristic Engine for Logging, Integration, and X-ecution**.

## What HELIX Can Do

- Run text commands (`help`, `systems check`, `run <command_name>`, `quit`)
- Optionally run voice commands (wakeword + Whisper transcription)
- Execute allowlisted robot workflow commands (simulation, build, deploy, etc.)
- Run a guided systems check where each step is human-verified as `pass/fail/retry/skip`

## Modes

- `text` (default): safest and simplest mode; no wakeword required
- `voice`: voice-only mode
- `both`: text loop + background voice loop

## Wakeword Strategy

`openwakeword` ships `hey_jarvis` as a built-in model, so HELIX supports:

- temporary built-in strategy (`hey_jarvis`)
- custom wakeword model file path (`custom_model`)
- disabled wakeword (`disabled`) for text-only use

## Setup

1. Create and activate a Python virtual environment.
1. Install dependencies:

```powershell
pip install -r helix/requirements.txt
```

1. Voice transcription uses **local Whisper** only (`openai-whisper` + PyTorch). There is **no cloud API** and **no network required at runtime**. Configure model size under `voice` in `helix/config.json`: `whisper_model` (`tiny`, `base`, `small`, `medium`, `large`), `whisper_device` (`cpu` or `cuda`), optional `whisper_download_root` for model weight cache (or set `WHISPER_CACHE_DIR`). The first time a model name is used, weights are stored in that cache; copy that cache to air‑gapped machines for fully offline installs.
1. `openwakeword` wakeword assets should also be present locally for offline use (cache under the package or pre-download ONNX models).

1. Edit behavior in `helix/config.json`.

## Execution (Windows)

- Pass `--workspace-root` to the repo root (where `gradlew.bat` lives). HELIX uses that as the **authoritative** base path.
- In **text** mode, allowlisted commands run in a **new OS console** by default so the HELIX prompt stays readable.
- Commands that invoke **Gradle** (`gradlew`) always run in a **separate console** and use the workspace root as `cwd`. HELIX rewrites `./gradlew.bat …` to the real `gradlew.bat` under `--workspace-root`.
- While the other window runs, HELIX **waits on the process** and prints occasional `Still watching PID …` lines (see `execution.watch_heartbeat_seconds` in config).
- Tune under `execution` in `helix/config.json`: `text_mode_separate_console`, `gradle_always_separate_console`, `gradle_force_workspace_root`, `watch_heartbeat_seconds`, `keep_separate_console_open`.
- `keep_separate_console_open`: default `true` runs the command then **`pause`** so the window stays until you press a key. Use `false` for the old behavior (window closes when the process exits). Use `"persist"` for `cmd /k` (shell stays open; HELIX returns immediately and does not wait for that window).

## Run

```powershell
python -m helix.main --config helix/config.json --workspace-root .
```

or

```powershell
python run_helix.py
```

## Systems Check (for non-team operators)

1. Start HELIX in `text` mode.
1. Type `systems check`.
1. Follow plain-language instructions for each step.
1. Mark each step `pass`, `fail`, `retry`, or `skip` using:
	- CLI prompt, and/or
	- local web UI buttons (`http://127.0.0.1:8765/` by default)
1. Review generated report in `helix/reports/`.

## Notes

- HELIX does not directly modify robot Java runtime code.
- On Windows + Python 3.13, keep ONNX wakeword inference.
- For NetworkTables actions, install `pynetworktables`.

