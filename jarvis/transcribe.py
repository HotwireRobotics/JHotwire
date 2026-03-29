"""Whisper transcription support using OpenAI's Python SDK."""

from __future__ import annotations

import os
from pathlib import Path
import tempfile
import wave

import numpy as np
from openai import OpenAI


class WhisperTranscriber:
	"""Transcribes short utterances with Whisper (`whisper-1`)."""

	def __init__(self) -> None:
		"""Initialize API client and ensure the key is present."""

		if not os.environ.get("OPENAI_API_KEY"):
			raise RuntimeError("OPENAI_API_KEY is required for Whisper transcription.")
		self._client = OpenAI()

	def _write_temp_wav(self, audio: np.ndarray, sample_rate_hz: int) -> Path:
		"""Persist audio as a temporary WAV file for API upload."""

		file_handle = tempfile.NamedTemporaryFile(suffix=".wav", delete=False)
		file_handle.close()
		file_path = Path(file_handle.name)

		with wave.open(str(file_path), "wb") as writer:
			writer.setnchannels(1)
			writer.setsampwidth(2)
			writer.setframerate(sample_rate_hz)
			writer.writeframes(audio.astype(np.int16).tobytes())

		return file_path

	def transcribe(self, audio: np.ndarray, sample_rate_hz: int) -> str:
		"""Return lowercase transcribed text for command matching."""

		file_path = self._write_temp_wav(audio, sample_rate_hz)
		try:
			with file_path.open("rb") as audio_file:
				result = self._client.audio.transcriptions.create(
					model="whisper-1",
					file=audio_file,
				)
			return str(result.text).strip().lower()
		finally:
			try:
				file_path.unlink(missing_ok=True)
			except OSError:
				pass

