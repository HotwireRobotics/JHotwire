"""Audio capture and wakeword detection helpers."""

from __future__ import annotations

import time
from typing import Any

import numpy as np
import sounddevice as sd
from openwakeword.model import Model

from jarvis.config import JarvisConfig


class WakewordDetector:
	"""Detects the configured wakeword from microphone audio chunks."""

	def __init__(self, config: JarvisConfig) -> None:
		"""Initialize an openwakeword model using safe fallbacks."""

		self._config = config
		self._model = self._build_model(config)

	def _build_model(self, config: JarvisConfig) -> Model:
		"""Build the openwakeword model instance."""

		if config.wakeword_model_path:
			return Model(wakeword_models=[config.wakeword_model_path])
		try:
			return Model(wakeword_models=[config.wakeword_name])
		except Exception:
			# Fallback for installs where built-in model lookup differs.
			print(
				"[jarvis] Could not preload wakeword by name; "
				"falling back to default openwakeword models."
			)
			return Model()

	def _extract_score(self, prediction: Any) -> float:
		"""Extract a confidence score from different prediction shapes."""

		if isinstance(prediction, dict):
			for key, value in prediction.items():
				if self._config.wakeword_name in str(key).lower():
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

	def is_wakeword_detected(self, audio_chunk: np.ndarray) -> bool:
		"""Return True when the wakeword confidence exceeds the threshold."""

		prediction = self._model.predict(audio_chunk)
		score = self._extract_score(prediction)
		return score >= self._config.wakeword_threshold


class Microphone:
	"""Provides fixed-rate microphone reads compatible with openwakeword."""

	def __init__(self, config: JarvisConfig) -> None:
		"""Initialize microphone settings from configuration."""

		self._config = config

	def read_chunk(self) -> np.ndarray:
		"""Capture a single mono audio chunk as int16."""

		audio = sd.rec(
			frames=self._config.chunk_size,
			samplerate=self._config.sample_rate_hz,
			channels=1,
			dtype="int16",
			blocking=True,
		)
		return np.squeeze(audio)

	def record_utterance(self, seconds: float) -> np.ndarray:
		"""Record a short utterance after wakeword detection."""

		frames = int(seconds * self._config.sample_rate_hz)
		audio = sd.rec(
			frames=frames,
			samplerate=self._config.sample_rate_hz,
			channels=1,
			dtype="int16",
			blocking=True,
		)
		time.sleep(0.02)
		return np.squeeze(audio)

