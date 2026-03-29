# Jarvis Subsystem

This directory is an almost fully isolated voice-assistant subsystem for your robot workflow.

It uses:

- `openwakeword` for wakeword detection (`hey_jarvis`, built-in)
- OpenAI Whisper (`whisper-1`) for speech-to-text
- A strict command allowlist that maps spoken phrases to shell commands

## What It Does

1. Continuously listens to your microphone.
2. Waits until the `hey jarvis` wakeword is detected.
3. Records a short command utterance.
4. Sends that utterance to Whisper for transcription.
5. Routes the text to configured commands like simulation/deploy/build.

## Setup

1. Create and activate a Python virtual environment.
2. Install dependencies:

```powershell
pip install -r jarvis/requirements.txt
```

1. Set your OpenAI API key:

```powershell
$env:OPENAI_API_KEY="your_key_here"
```

1. Edit command mappings in `jarvis/config.json`.

## Run

```powershell
python -m jarvis.main --config jarvis/config.json --workspace-root .
```

## Notes

- This subsystem does not modify or hook directly into robot Java runtime code.
- The built-in `openwakeword` model key for Jarvis is `hey_jarvis`.
- If your local install needs a custom model file, set `wakeword_model_path` in `jarvis/config.json`.
- Command execution is intentionally allowlisted by config to keep behavior predictable.
