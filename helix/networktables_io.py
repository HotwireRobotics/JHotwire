"""NetworkTables helpers for HELIX systems checks."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass
class NetworkTablesResult:
	"""Represents a NetworkTables operation result."""

	success: bool
	message: str
	value: Any = None


class NetworkTablesClient:
	"""Minimal wrapper for reading and writing NetworkTables values."""

	def __init__(self, server: str) -> None:
		"""Initialize NetworkTables client lazily."""

		self._server = server
		self._initialized = False
		self._nt = None

	def _initialize(self) -> None:
		"""Initialize the NetworkTables backend if available."""

		if self._initialized:
			return
		try:
			from networktables import NetworkTables
		except Exception as exc:
			raise RuntimeError(
				"pynetworktables is required for NetworkTables actions. "
				"Install with `pip install pynetworktables`."
			) from exc
		NetworkTables.initialize(server=self._server)
		self._nt = NetworkTables
		self._initialized = True

	def set_value(self, table: str, key: str, value: Any) -> NetworkTablesResult:
		"""Set a NetworkTables key and return status."""

		self._initialize()
		assert self._nt is not None
		subtable = self._nt.getTable(table)
		subtable.putValue(key, value)
		return NetworkTablesResult(
			success=True,
			message=f"Set {table}/{key}={value!r}",
			value=value,
		)

	def get_value(self, table: str, key: str, default: Any = None) -> NetworkTablesResult:
		"""Read a NetworkTables key and return status."""

		self._initialize()
		assert self._nt is not None
		subtable = self._nt.getTable(table)
		value = subtable.getValue(key, default)
		return NetworkTablesResult(
			success=True,
			message=f"Read {table}/{key}={value!r}",
			value=value,
		)

