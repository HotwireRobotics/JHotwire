"""Local web UI for HELIX systems-check human verification."""

from __future__ import annotations

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import threading
from typing import Any


class _WebState:
	"""Shared mutable state for the web verification server."""

	def __init__(self) -> None:
		"""Initialize server state."""

		self.current_step: dict[str, Any] = {}
		self.current_decision: str | None = None
		self.lock = threading.Lock()


class WebVerificationServer:
	"""Tiny local HTTP server used for pass/fail/retry/skip decisions."""

	def __init__(self, host: str, port: int) -> None:
		"""Construct server with host and port."""

		self._host = host
		self._port = port
		self._state = _WebState()
		self._httpd: ThreadingHTTPServer | None = None
		self._thread: threading.Thread | None = None

	def start(self) -> None:
		"""Start the local web server in a background thread."""

		state = self._state

		class Handler(BaseHTTPRequestHandler):
			"""Request handler bound to the outer server state."""

			def log_message(self, format: str, *args: Any) -> None:
				"""Silence default HTTP request logging."""

				return

			def _write_json(self, payload: dict[str, Any], status_code: int = 200) -> None:
				"""Write JSON response payload."""

				body = json.dumps(payload).encode("utf-8")
				self.send_response(status_code)
				self.send_header("Content-Type", "application/json")
				self.send_header("Content-Length", str(len(body)))
				self.end_headers()
				self.wfile.write(body)

			def _write_html(self, html: str) -> None:
				"""Write HTML response."""

				body = html.encode("utf-8")
				self.send_response(200)
				self.send_header("Content-Type", "text/html; charset=utf-8")
				self.send_header("Content-Length", str(len(body)))
				self.end_headers()
				self.wfile.write(body)

			def do_GET(self) -> None:
				"""Serve UI page or state endpoint."""

				if self.path == "/state":
					with state.lock:
						self._write_json(
							{
								"step": state.current_step,
								"decision": state.current_decision,
							}
						)
					return
				html = """<!doctype html>
<html>
<head>
	<meta charset="utf-8" />
	<title>HELIX Systems Check</title>
	<style>
		body { font-family: sans-serif; margin: 24px; }
		button { font-size: 18px; padding: 12px 16px; margin-right: 8px; margin-top: 8px; }
		.card { border: 1px solid #ccc; border-radius: 8px; padding: 16px; max-width: 900px; }
		.small { color: #555; font-size: 14px; }
	</style>
</head>
<body>
	<h1>HELIX Systems Check</h1>
	<div class="card">
		<h2 id="title">Waiting for step...</h2>
		<p id="instructions"></p>
		<p class="small"><strong>Expected:</strong> <span id="expected"></span></p>
		<p class="small"><strong>Safety:</strong> <span id="safety"></span></p>
		<div>
			<button onclick="decide('pass')">Pass</button>
			<button onclick="decide('fail')">Fail</button>
			<button onclick="decide('retry')">Retry</button>
			<button onclick="decide('skip')">Skip</button>
		</div>
		<p class="small">Last decision: <span id="decision">none</span></p>
	</div>
	<script>
		async function refresh() {
			const res = await fetch('/state');
			const data = await res.json();
			const step = data.step || {};
			document.getElementById('title').textContent = step.title || 'Waiting for step...';
			document.getElementById('instructions').textContent = step.instructions || '';
			document.getElementById('expected').textContent = step.expected_observation || '';
			document.getElementById('safety').textContent = step.safety_notes || '';
			document.getElementById('decision').textContent = data.decision || 'none';
		}
		async function decide(value) {
			await fetch('/decision', {
				method: 'POST',
				headers: {'Content-Type': 'application/json'},
				body: JSON.stringify({decision: value})
			});
			await refresh();
		}
		setInterval(refresh, 750);
		refresh();
	</script>
</body>
</html>"""
				self._write_html(html)

			def do_POST(self) -> None:
				"""Handle incoming decision updates."""

				if self.path != "/decision":
					self._write_json({"error": "not_found"}, status_code=404)
					return
				content_length = int(self.headers.get("Content-Length", "0"))
				payload = json.loads(self.rfile.read(content_length) or b"{}")
				decision = str(payload.get("decision", "")).strip().lower()
				if decision not in ("pass", "fail", "retry", "skip"):
					self._write_json({"error": "invalid decision"}, status_code=400)
					return
				with state.lock:
					state.current_decision = decision
				self._write_json({"ok": True, "decision": decision})

		self._httpd = ThreadingHTTPServer((self._host, self._port), Handler)
		self._thread = threading.Thread(target=self._httpd.serve_forever, daemon=True)
		self._thread.start()

	def stop(self) -> None:
		"""Stop the local web server."""

		if self._httpd is not None:
			self._httpd.shutdown()
			self._httpd.server_close()
			self._httpd = None

	def url(self) -> str:
		"""Return the URL for browser access."""

		return f"http://{self._host}:{self._port}/"

	def present_step(self, step_payload: dict[str, Any]) -> None:
		"""Publish the current step for the web UI."""

		with self._state.lock:
			self._state.current_step = step_payload
			self._state.current_decision = None

	def wait_for_decision(self) -> str:
		"""Block until the UI submits a decision."""

		while True:
			with self._state.lock:
				if self._state.current_decision is not None:
					return self._state.current_decision
			threading.Event().wait(0.2)

