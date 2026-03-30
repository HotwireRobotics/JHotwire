# HELIX helper: WPILib Java simulation with HAL driver-station socket (see build.gradle: wpi.sim.addDriverstation()).
# Use the official FRC Driver Station on this PC or another machine; team must match .wpilib/wpilib_preferences.json.
#
# Typical static IPs (field-style): simulation PC 10.TE.AM.2, Driver Station 10.TE.AM.21 (TE.AM = team, e.g. 2990 -> 10.29.90.x).
# Windows: allow Java through Windows Firewall on Private networks (or run this script once as Administrator for the rule below).

$ErrorActionPreference = 'Continue'

# Repo root = current directory (HELIX sets cwd to workspace).
$RepoRoot = (Get-Location).Path
Set-Location -LiteralPath $RepoRoot

$admin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
	[Security.Principal.WindowsBuiltInRole]::Administrator
)
$java = Get-Command java -ErrorAction SilentlyContinue
if ($admin -and $java) {
	$existing = Get-NetFirewallRule -DisplayName 'HELIX WPILib Java (sim)' -ErrorAction SilentlyContinue
	if (-not $existing) {
		try {
			New-NetFirewallRule -DisplayName 'HELIX WPILib Java (sim)' -Direction Inbound -Action Allow -Program $java.Source |
				Out-Null
			Write-Host "[helix] Added inbound firewall allow rule for Java ( Administrator )."
		} catch {
			Write-Host "[helix] Could not add firewall rule: $($_.Exception.Message)"
		}
	}
} elseif (-not $admin) {
	Write-Host "[helix] Tip: run PowerShell as Administrator once so this script can add a Java firewall allow rule, or allow Java when Windows prompts."
}

try {
	Get-NetConnectionProfile | Where-Object { $_.NetworkCategory -ne 'Private' } | ForEach-Object {
		Set-NetConnectionProfile -InterfaceIndex $_.InterfaceIndex -NetworkCategory Private -ErrorAction SilentlyContinue
	}
} catch {
	# Needs elevation on some systems.
}

$pref = Join-Path $RepoRoot '.wpilib\wpilib_preferences.json'
if (Test-Path -LiteralPath $pref) {
	try {
		$j = Get-Content -LiteralPath $pref -Raw | ConvertFrom-Json
		$tn = [int]$j.teamNumber
		$te = [math]::Floor($tn / 100)
		$am = $tn % 100
		Write-Host "[helix] Driver Station team should be $tn (from wpilib_preferences). Typical IPs: sim PC 10.$te.$am.2 , DS 10.$te.$am.21"
	} catch {
		Write-Host "[helix] Could not read team number from wpilib_preferences.json"
	}
}

Write-Host "[helix] Starting simulateJavaRelease (release desktop JNI + driver-station socket)..."
$gw = Join-Path $RepoRoot 'gradlew.bat'
& $gw @('simulateJavaRelease')
exit $LASTEXITCODE
