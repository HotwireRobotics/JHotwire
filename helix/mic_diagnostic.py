"""Microphone level and wakeword scoring diagnostics (run with --mic-check)."""

from __future__ import annotations

import time

import numpy as np
import sounddevice as sd

from helix.audio import Microphone, WakewordDetector
from helix.config import VoiceConfig


def _ascii_safe(text: str) -> str:
	"""Avoid UnicodeEncodeError on Windows consoles that are not UTF-8."""

	return "".join(ch if ord(ch) < 128 else "?" for ch in text)


def _rms_dbfs(audio: np.ndarray) -> float:
	"""Compute RMS level of int16 mono audio in dBFS (approximate)."""

	x = audio.astype(np.float64) / 32768.0
	rms = float(np.sqrt(np.mean(x * x)))
	return 20.0 * np.log10(rms + 1e-12)


def run_mic_diagnostic(voice: VoiceConfig) -> None:
	"""Print input devices, sample a clip, then stream RMS + wakeword scores."""

	print("[helix] --- Microphone / wakeword diagnostic ---")
	print(
		"[helix] If input level stays very low, check: Windows Settings -> Privacy -> Microphone, "
		"correct default recording device, and that the mic is not muted."
	)
	try:
		devs = sd.query_devices()
		default_in, default_out = sd.default.device
	except Exception as exc:
		print(f"[helix] Could not query audio devices: {exc}")
		return

	print(f"[helix] Default input device index: {default_in}")
	for i, d in enumerate(devs):
		if int(d["max_input_channels"]) > 0:
			marker = " (default input)" if i == default_in else ""
			print(f"[helix]   [{i}] {_ascii_safe(str(d['name']))}{marker}")

	print(f"[helix] Using sample_rate={voice.sample_rate_hz} Hz, chunk_size={voice.chunk_size} (voice config).")
	if voice.input_device is not None:
		try:
			chosen = devs[voice.input_device]
			chosen_name = _ascii_safe(str(chosen["name"]))
		except (IndexError, KeyError):
			chosen_name = "unknown"
		print(
			f"[helix] Capturing from voice.input_device={voice.input_device}: {chosen_name} "
			"(overrides OS default)."
		)
	else:
		print(f"[helix] Using OS default input device index {default_in}.")

	detector: WakewordDetector | None = None
	if voice.wakeword_strategy != "disabled":
		try:
			detector = WakewordDetector(voice)
		except Exception as exc:
			print(f"[helix] Wakeword model failed to load: {exc}")
			print("[helix] You can still verify the mic using RMS below; fix ONNX/models per helix README.")
	else:
		print("[helix] Wakeword is disabled in config; showing RMS only.")

	mic = Microphone(voice)
	threshold = voice.wakeword_threshold
	if voice.mic_gain != 1.0:
		print(f"[helix] voice.mic_gain={voice.mic_gain} (applies to all captured audio).")

	try:
		silent = mic.read_chunk()
		print(f"[helix] One idle chunk: RMS ~ {_rms_dbfs(silent):.1f} dBFS (after gain).")
	except Exception as exc:
		print(f"[helix] Recording failed (permission or device problem?): {_ascii_safe(str(exc))}")
		return

	print(
		f"[helix] Speak clearly; say the wakeword (e.g. Hey Jarvis). "
		f"Watch RMS (should rise when you talk) and score vs threshold={threshold}."
	)
	print("[helix] Streaming ~8 seconds... (Ctrl+C to stop)\n")
	print(
		"[helix] Column 'smooth' is the rolling max over the last voice.wakeword_score_window_chunks chunks "
		"(same rule as live HELIX). Raw single-chunk scores can stay low while 'smooth' spikes during the phrase.\n"
	)
	print(
		"[helix] If RMS peaks below about -50 dBFS while speaking, raise OS mic level or set voice.mic_gain (e.g. 4.0).\n"
	)

	t0 = time.monotonic()
	last_print = 0.0
	peak_dbfs = -200.0
	max_score = 0.0
	try:
		while time.monotonic() - t0 < 8.0:
			chunk = mic.read_chunk()
			rms = _rms_dbfs(chunk)
			peak_dbfs = max(peak_dbfs, rms)
			if detector is not None:
				smooth = detector.streaming_gate_score(chunk)
				max_score = max(max_score, smooth)
				hit = " *** HIT ***" if smooth >= threshold else ""
				line = f"  RMS {rms:6.1f} dBFS   smooth {smooth:.3f} / {threshold}{hit}"
			else:
				line = f"  RMS {rms:6.1f} dBFS   (wakeword model unavailable)"
			now = time.monotonic()
			if now - last_print >= 0.25:
				print(line, flush=True)
				last_print = now
	except KeyboardInterrupt:
		print("\n[helix] Stopped.")

	phrase_max = 0.0
	if detector is not None:
		print("\n[helix] Phrase test: recording 4 seconds starting NOW — say \"Hey Jarvis\" clearly...")
		time.sleep(0.15)
		try:
			detector.reset()
			clip = mic.record_utterance(4.0)
			scores = detector.wakeword_scores_for_clip(clip)
			phrase_max = max(scores) if scores else 0.0
			clip_rms = _rms_dbfs(clip)
			print(f"[helix] Phrase clip average RMS: {clip_rms:.1f} dBFS")
			print(
				f"[helix] Phrase test max wakeword score (best of ~{len(scores)} frames): "
				f"{phrase_max:.3f} (threshold {threshold})"
			)
			if phrase_max >= threshold:
				print("[helix] Phrase test: wakeword would trigger at least once in this clip.")
		except KeyboardInterrupt:
			print("\n[helix] Phrase test skipped.")

	print("\n[helix] Summary:")
	print(f"[helix]   Loudest RMS (8s stream): {peak_dbfs:.1f} dBFS (typical speech often ~-35 to -55).")
	if detector is not None:
		print(f"[helix]   Peak score (stream, 80ms chunks): {max_score:.3f}")
		print(f"[helix]   Peak score (4s phrase test): {phrase_max:.3f}")

	# Phrase test is the ground truth for mic + model; stream can look weak if you barely spoke in those 8s.
	if detector is not None and phrase_max >= threshold:
		print(
			"[helix] RESULT: Phrase test passed — capture and wakeword are working for \"Hey Jarvis\"."
		)
		if max_score < threshold and peak_dbfs < -50.0:
			print(
				"[helix]   The 8s stream often stays low if you were not speaking for most of that window "
				"(it only scores 80ms chunks). Live HELIX uses the same chunks; say the wakeword clearly while it listens."
			)
	elif detector is not None and phrase_max >= 0.15:
		print(
			"[helix] RESULT: Phrase test shows a weak wakeword signal. Try mic_gain, OS level, "
			"or try a slightly lower wakeword_threshold (not below ~0.45 if you get TV false wakes)."
		)
	elif peak_dbfs < -60.0 and phrase_max < 0.15:
		print(
			"[helix] RESULT: Input stayed very quiet in both tests. Raise Windows mic level / boost, "
			"set voice.mic_gain to 4.0-8.0, or use voice.input_device to pick another mic."
		)
	elif peak_dbfs < -50.0 and phrase_max < 0.15:
		print(
			"[helix] RESULT: Levels are borderline. Increase mic_gain or OS level, say \"Hey Jarvis\" "
			"close to the mic, or slightly lower wakeword_threshold (raise it if background audio triggers wakes)."
		)
	elif detector is not None and phrase_max < 0.1 and peak_dbfs >= -50.0:
		print(
			"[helix] RESULT: Stream level looks OK but wakeword still low — try a quieter room, "
			"clearer \"Hey Jarvis\", or a slightly lower wakeword_threshold."
		)
	print("\n[helix] --- End diagnostic ---")
