"""Local Whisper transcription (openai-whisper package; no cloud, no API keys)."""

from __future__ import annotations

import os
from typing import Any

import numpy as np

from helix.config import VoiceConfig


class WhisperTranscriber:
	"""Transcribes utterances with the open-source Whisper model running locally."""

	def __init__(self, voice: VoiceConfig) -> None:
		"""Load the Whisper model once; all inference stays on this machine."""

		self._voice = voice
		if voice.whisper_download_root:
			os.environ["WHISPER_CACHE_DIR"] = os.path.abspath(voice.whisper_download_root)
		import whisper  # type: ignore[import-untyped]

		download_root = voice.whisper_download_root
		device = voice.whisper_device
		model_name = voice.whisper_model
		print(f"[helix] Loading local Whisper model '{model_name}' on {device} (offline inference)...")
		self._whisper = whisper
		self._model = whisper.load_model(
			model_name,
			device=device,
			download_root=download_root,
		)

	def transcribe(self, audio: np.ndarray, sample_rate_hz: int) -> str:
		"""Return lowercase text for command routing."""

		audio_fp32 = self._to_whisper_audio(audio, sample_rate_hz)
		fp16 = self._voice.whisper_device == "cuda"
		result: dict[str, Any] = self._model.transcribe(
			audio_fp32,
			language="en",
			fp16=fp16,
			task="transcribe",
		)
		text = str(result.get("text", "")).strip().lower()
		return text

	def _to_whisper_audio(self, audio: np.ndarray, sample_rate_hz: int) -> np.ndarray:
		"""Convert int16 PCM to float32 [-1, 1] and resample to 16 kHz if needed."""

		flat = np.asarray(audio).squeeze()
		if flat.dtype == np.float32:
			samples = flat
		else:
			samples = flat.astype(np.float32) / 32768.0
		if sample_rate_hz != 16000:
			return self._whisper.audio.resample(samples, sample_rate_hz, 16000)
		return samples

