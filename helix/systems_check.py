"""Guided systems-check workflow engine for HELIX."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
import json
from pathlib import Path
import webbrowser

from helix.actions import ActionResult, ActionRunner, RunContext
from helix.config import CommandBinding, HelixConfig, SystemsCheckStep
from helix.networktables_io import NetworkTablesClient, NetworkTablesResult
from helix.web_verify import WebVerificationServer


Decision = str


def should_advance_after_decision(decision: Decision) -> bool:
	"""Return whether systems-check should advance to next step."""

	return decision != "retry"


@dataclass
class StepExecutionResult:
	"""Captures execution and human decision for one check step."""

	step_id: str
	title: str
	decision: Decision
	action_result: ActionResult | None = None
	networktables_result: NetworkTablesResult | None = None
	notes: str = ""
	timestamp: str = ""


def _sanitize_filename_segment(value: str) -> str:
	"""Sanitize a string for filesystem-safe report names."""

	return "".join(ch if ch.isalnum() or ch in ("-", "_") else "_" for ch in value)


class SystemsCheckRunner:
	"""Executes configured systems-check steps and asks for human verification."""

	def __init__(self, config: HelixConfig, action_runner: ActionRunner, workspace_root: Path) -> None:
		"""Initialize check runner dependencies."""

		self._config = config
		self._action_runner = action_runner
		self._workspace_root = workspace_root
		self._nt_client = NetworkTablesClient(config.systems_check.nt_server)
		self._web_server: WebVerificationServer | None = None

	def _run_step_actions(self, step: SystemsCheckStep) -> tuple[ActionResult | None, NetworkTablesResult | None]:
		"""Run command and/or NetworkTables action for a step."""

		action_result: ActionResult | None = None
		networktables_result: NetworkTablesResult | None = None
		if step.command:
			action_result = self._action_runner.run(
				CommandBinding(
					name=f"systems-check:{step.id}",
					phrases=[],
					command=step.command,
					working_directory=step.working_directory,
				),
				RunContext(
					control_mode=self._config.control_mode,
					invoked_from_text=True,
				),
			)
		if step.networktables_action:
			if step.networktables_action.action == "set":
				networktables_result = self._nt_client.set_value(
					table=step.networktables_action.table,
					key=step.networktables_action.key,
					value=step.networktables_action.value,
				)
			else:
				networktables_result = self._nt_client.get_value(
					table=step.networktables_action.table,
					key=step.networktables_action.key,
					default=None,
				)
		return action_result, networktables_result

	def _prompt_cli_decision(self, step: SystemsCheckStep) -> Decision:
		"""Prompt operator decision in terminal."""

		print("\n[helix] Human verification required:")
		print(f"  Step: {step.title}")
		print(f"  Instructions: {step.instructions}")
		if step.expected_observation:
			print(f"  Expected: {step.expected_observation}")
		if step.safety_notes:
			print(f"  Safety: {step.safety_notes}")
		while True:
			entry = input("  Enter decision [pass/fail/retry/skip]: ").strip().lower()
			if entry in ("pass", "fail", "retry", "skip"):
				return entry
			print("  Invalid choice. Use pass, fail, retry, or skip.")

	def _prompt_web_decision(self, step: SystemsCheckStep) -> Decision:
		"""Prompt operator decision via local web UI."""

		if self._web_server is None:
			self._web_server = WebVerificationServer(
				host=self._config.systems_check.web_host,
				port=self._config.systems_check.web_port,
			)
			self._web_server.start()
			print(f"[helix] Web verification UI started: {self._web_server.url()}")
			try:
				webbrowser.open(self._web_server.url(), new=0, autoraise=True)
			except Exception:
				pass
		self._web_server.present_step(
			{
				"id": step.id,
				"title": step.title,
				"instructions": step.instructions,
				"expected_observation": step.expected_observation,
				"safety_notes": step.safety_notes,
			}
		)
		print(f"[helix] Waiting for web decision at {self._web_server.url()} ...")
		return self._web_server.wait_for_decision()

	def _select_decision_mode(self) -> str:
		"""Select verification mode for a step."""

		mode = self._config.systems_check.verification_mode
		if mode in ("cli", "web"):
			return mode
		while True:
			entry = input("Choose verifier for this step [cli/web]: ").strip().lower()
			if entry in ("cli", "web"):
				return entry
			print("Invalid choice. Enter cli or web.")

	def _write_reports(self, results: list[StepExecutionResult]) -> Path:
		"""Persist machine and human-readable reports for check run."""

		reports_dir = self._workspace_root / "helix" / "reports"
		reports_dir.mkdir(parents=True, exist_ok=True)
		timestamp = datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")
		base_name = f"systems_check_{_sanitize_filename_segment(timestamp)}"
		json_path = reports_dir / f"{base_name}.json"
		md_path = reports_dir / f"{base_name}.md"
		json_payload = {
			"timestamp_utc": timestamp,
			"summary": {
				"passed": sum(1 for result in results if result.decision == "pass"),
				"failed": sum(1 for result in results if result.decision == "fail"),
				"skipped": sum(1 for result in results if result.decision == "skip"),
			},
			"results": [
				{
					"step_id": result.step_id,
					"title": result.title,
					"decision": result.decision,
					"notes": result.notes,
					"timestamp": result.timestamp,
					"action_result": (
						{
							"success": result.action_result.success,
							"command_name": result.action_result.command_name,
							"exit_code": result.action_result.exit_code,
							"stdout": result.action_result.stdout,
							"stderr": result.action_result.stderr,
						}
						if result.action_result is not None
						else None
					),
					"networktables_result": (
						{
							"success": result.networktables_result.success,
							"message": result.networktables_result.message,
							"value": result.networktables_result.value,
						}
						if result.networktables_result is not None
						else None
					),
				}
				for result in results
			],
		}
		json_path.write_text(json.dumps(json_payload, indent=2), encoding="utf-8")
		md_lines = [
			"# HELIX Systems Check Report",
			"",
			f"- Timestamp (UTC): {timestamp}",
			f"- Passed: {json_payload['summary']['passed']}",
			f"- Failed: {json_payload['summary']['failed']}",
			f"- Skipped: {json_payload['summary']['skipped']}",
			"",
			"## Step Results",
			"",
		]
		for result in results:
			md_lines.extend(
				[
					f"### {result.step_id} - {result.title}",
					f"- Decision: {result.decision}",
					f"- Timestamp: {result.timestamp}",
					f"- Notes: {result.notes or 'n/a'}",
					"",
				]
			)
		md_path.write_text("\n".join(md_lines), encoding="utf-8")
		return md_path

	def run(self) -> None:
		"""Execute all configured systems-check steps in sequence."""

		steps = self._config.systems_check.steps
		if not steps:
			print("[helix] No systems-check steps configured.")
			return
		print(f"[helix] Starting systems check with {len(steps)} step(s).")
		results: list[StepExecutionResult] = []
		index = 0
		while index < len(steps):
			step = steps[index]
			print(f"\n[helix] Step {index + 1}/{len(steps)}: {step.title}")
			action_result, networktables_result = self._run_step_actions(step)
			if action_result is not None:
				print(f"[helix] Command exit={action_result.exit_code}")
				if action_result.stdout:
					print(f"[helix] stdout: {action_result.stdout}")
				if action_result.stderr:
					print(f"[helix] stderr: {action_result.stderr}")
			if networktables_result is not None:
				print(f"[helix] NT: {networktables_result.message}")
			selected_mode = self._select_decision_mode()
			decision = (
				self._prompt_cli_decision(step)
				if selected_mode == "cli"
				else self._prompt_web_decision(step)
			)
			timestamp = datetime.utcnow().isoformat() + "Z"
			results.append(
				StepExecutionResult(
					step_id=step.id,
					title=step.title,
					decision=decision,
					action_result=action_result,
					networktables_result=networktables_result,
					notes=f"verification_mode={selected_mode}",
					timestamp=timestamp,
				)
			)
			if not should_advance_after_decision(decision):
				print("[helix] Retrying current step.")
				continue
			index += 1
		report_path = self._write_reports(results)
		print(f"[helix] Systems check complete. Report: {report_path}")
		if self._web_server is not None:
			self._web_server.stop()

