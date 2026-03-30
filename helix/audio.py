"""Audio capture and wakeword detection helpers for HELIX."""

from __future__ import annotations

import os
from collections import deque
from typing import Any

import numpy as np
import openwakeword
from openwakeword import utils as oww_utils
from openwakeword.model import Model
import sounddevice as sd

from helix.config import VoiceConfig


class WakewordDetector:
	"""Detects the configured wakeword from microphone audio chunks."""

	def __init__(self, voice: VoiceConfig) -> None:
		"""Initialize an openwakeword model using safe fallbacks."""

		if voice.wakeword_strategy == "disabled":
			raise RuntimeError("Voice wakeword is disabled in config.")
		self._voice = voice
		self._model = self._build_model(voice)
		# Rolling max over recent chunk scores so live detection matches multi-frame phrase tests.
		self._score_ring: deque[float] = deque(maxlen=max(1, voice.wakeword_score_window_chunks))
		self._last_smoothed: float = 0.0

	def _effective_model_name(self, voice: VoiceConfig) -> str:
		"""Get the effective wakeword model key."""

		if voice.wakeword_strategy == "hey_jarvis":
			return "hey_jarvis"
		return voice.wakeword_name

	def _build_model(self, voice: VoiceConfig) -> Model:
		"""Build the openwakeword model instance."""

		framework = voice.wakeword_inference_framework
		errors: list[str] = []
		self._ensure_onnx_models_available(voice)
		model_name = self._effective_model_name(voice)

		def _try_model(use_named_model: bool, inference_framework: str) -> Model | None:
			"""Try loading a model configuration and capture failures."""

			try:
				if voice.wakeword_strategy == "custom_model" and voice.wakeword_model_path:
					return Model(
						wakeword_models=[voice.wakeword_model_path],
						inference_framework=inference_framework,
					)
				if use_named_model:
					return Model(
						wakeword_models=[model_name],
						inference_framework=inference_framework,
					)
				return Model(inference_framework=inference_framework)
			except Exception as exc:
				errors.append(f"{inference_framework}:{'named' if use_named_model else 'default'}: {exc}")
				return None

		model = _try_model(use_named_model=True, inference_framework=framework)
		if model is not None:
			return model
		print("[helix] Could not preload wakeword by name; falling back to default openwakeword models.")
		model = _try_model(use_named_model=False, inference_framework=framework)
		if model is not None:
			return model
		if framework != "tflite":
			print("[helix] Attempting tflite fallback as last resort...")
			model = _try_model(use_named_model=True, inference_framework="tflite")
			if model is not None:
				return model
			model = _try_model(use_named_model=False, inference_framework="tflite")
			if model is not None:
				return model
		raise RuntimeError(
			"Failed to initialize wakeword model. "
			"For Windows/Python 3.13, prefer ONNX by setting "
			"'voice.wakeword_inference_framework' to 'onnx' in helix/config.json. "
			f"Collected errors: {' | '.join(errors)}"
		)

	def _ensure_onnx_models_available(self, voice: VoiceConfig) -> None:
		"""Download ONNX models when missing for ONNX runtime usage."""

		if voice.wakeword_inference_framework != "onnx":
			return
		if voice.wakeword_strategy == "custom_model":
			return
		model_key = self._effective_model_name(voice)
		model_info = openwakeword.MODELS.get(model_key)
		if model_info is None:
			return
		onnx_model_path = model_info["model_path"].replace(".tflite", ".onnx")
		if os.path.exists(onnx_model_path):
			return
		target_directory = os.path.dirname(model_info["model_path"])
		print(f"[helix] ONNX model missing for '{model_key}'. Downloading openwakeword model assets...")
		try:
			oww_utils.download_models(
				model_names=[model_key],
				target_directory=target_directory,
			)
		except Exception as exc:
			raise RuntimeError(
				"Unable to download ONNX wakeword models automatically. "
				"Check internet access and try again."
			) from exc

	def _extract_score(self, prediction: Any) -> float:
		"""Extract a confidence score from different prediction shapes."""

		if isinstance(prediction, dict):
			for key, value in prediction.items():
				if self._effective_model_name(self._voice) in str(key).lower():
					try:
						return float(value)
					except (TypeError, ValueError):
						continue
			for value in prediction.values():
				try:
					return max(float(value), 0.0)
				except (TypeError, ValueError):
					continue
		try:
			return float(prediction)
		except (TypeError, ValueError):
			return 0.0

	def wakeword_score(self, audio_chunk: np.ndarray) -> float:
		"""Return openwakeword confidence for this chunk (0..1 typical)."""

		prediction = self._model.predict(audio_chunk)
		return self._extract_score(prediction)

	def streaming_gate_score(self, audio_chunk: np.ndarray) -> float:
		"""Append this chunk's score and return the rolling max (matches live trigger logic)."""

		s = self.wakeword_score(audio_chunk)
		self._score_ring.append(s)
		smoothed = max(self._score_ring)
		self._last_smoothed = smoothed
		return smoothed

	@property
	def last_smoothed_score(self) -> float:
		"""Most recent rolling-max score (for diagnostics)."""

		return self._last_smoothed

	def clear_streaming_window(self) -> None:
		"""Clear rolling scores after a wake trigger so the same phrase does not re-fire."""

		self._score_ring.clear()

	def is_wakeword_detected(self, audio_chunk: np.ndarray) -> bool:
		"""Return True when smoothed wakeword confidence exceeds threshold."""

		return self.streaming_gate_score(audio_chunk) >= self._voice.wakeword_threshold

	def reset(self) -> None:
		"""Clear openWakeWord internal buffers (call before a fresh clip-based test)."""

		self._model.reset()
		self._score_ring.clear()

	def wakeword_scores_for_clip(self, audio_int16: np.ndarray) -> list[float]:
		"""Run predict_clip and return per-chunk scores for the active wakeword model."""

		name = self._effective_model_name(self._voice)
		chunk = self._voice.chunk_size
		frames = self._model.predict_clip(audio_int16, padding=1, chunk_size=chunk)
		scores: list[float] = []
		for pred in frames:
			if isinstance(pred, dict):
				scores.append(float(pred.get(name, 0.0)))
			else:
				scores.append(0.0)
		return scores


class Microphone:
	"""Provides fixed-rate microphone reads compatible with openwakeword.

	Uses one long-lived InputStream so chunks are gapless. Repeated sd.rec() opens short
	captures and breaks openWakeWord's streaming preprocessor (mic-check phrase test still
	works because predict_clip feeds contiguous synthetic chunks).
	"""

	def __init__(self, voice: VoiceConfig) -> None:
		"""Initialize microphone settings from voice configuration."""

		self._voice = voice
		self._stream: sd.InputStream | None = None

	def _apply_gain(self, audio: np.ndarray) -> np.ndarray:
		"""Scale int16 PCM by voice.mic_gain (helps quiet laptop mics)."""

		g = float(self._voice.mic_gain)
		flat = np.asarray(audio, dtype=np.int16)
		if g == 1.0:
			return np.squeeze(flat)
		out = np.clip(flat.astype(np.float32) * g, -32768.0, 32767.0).astype(np.int16)
		return np.squeeze(out)

	def _open_stream(self) -> None:
		"""Start a single continuous input stream (idempotent)."""

		if self._stream is not None:
			return
		kw: dict[str, Any] = dict(
			samplerate=self._voice.sample_rate_hz,
			channels=1,
			dtype="int16",
			blocksize=self._voice.chunk_size,
			latency="low",
		)
		if self._voice.input_device is not None:
			kw["device"] = self._voice.input_device
		self._stream = sd.InputStream(**kw)
		self._stream.start()

	def close(self) -> None:
		"""Stop and release the input stream."""

		if self._stream is None:
			return
		self._stream.stop()
		self._stream.close()
		self._stream = None

	def read_chunk(self) -> np.ndarray:
		"""Capture a single mono audio chunk as int16 from the continuous stream."""

		self._open_stream()
		assert self._stream is not None
		data, overflowed = self._stream.read(self._voice.chunk_size)
		if overflowed:
			print("[helix] Audio input overflow (CPU too slow); wakeword may miss speech.", flush=True)
		arr = np.asarray(data, dtype=np.int16).squeeze()
		return self._apply_gain(arr)

	def record_utterance(self, seconds: float | None = None) -> np.ndarray:
		"""Record a short utterance on the same stream so timing stays contiguous."""

		self._open_stream()
		assert self._stream is not None
		duration = seconds if seconds is not None else self._voice.post_wake_record_seconds
		total = int(duration * self._voice.sample_rate_hz)
		cs = self._voice.chunk_size
		parts: list[np.ndarray] = []
		remaining = total
		while remaining > 0:
			n = min(cs, remaining)
			data, overflowed = self._stream.read(n)
			if overflowed:
				print("[helix] Audio overflow during command capture.", flush=True)
			parts.append(np.asarray(data, dtype=np.int16).squeeze())
			remaining -= n
		audio = np.concatenate(parts) if len(parts) > 1 else parts[0]
		return self._apply_gain(audio)

