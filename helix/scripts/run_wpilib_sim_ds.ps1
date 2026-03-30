# HELIX helper: WPILib Java simulation with HAL driver-station socket (see build.gradle: wpi.sim.addDriverstation()).
# Team number must match .wpilib/wpilib_preferences.json (and the Driver Station team field).
#
# LOCAL (Driver Station + sim on THIS PC): in Driver Station, point at the robot address 127.0.0.1 or localhost - not 10.x.x.x.
# REMOTE / field-style LAN only: sim machine often 10.TE.AM.2, Driver Station laptop 10.TE.AM.21 (e.g. team 2990 -> 10.29.90.x).
# Windows: allow Java through Windows Firewall on Private networks (or run this script once as Administrator for the rule below).

$ErrorActionPreference = 'Continue'

# Repo root = current directory (HELIX sets cwd to workspace).
$RepoRoot = (Get-Location).Path
Set-Location -LiteralPath $RepoRoot

$admin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
	[Security.Principal.WindowsBuiltInRole]::Administrator
)
# One resolved path (Get-Command can return multiple Application entries).
$java = Get-Command java -ErrorAction SilentlyContinue | Select-Object -First 1
if ($admin -and $java -and $java.Source) {
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
	# Use -f instead of a long expandable string so parsers do not confuse ()/dots with subexpressions.
	try {
		$j = Get-Content -LiteralPath $pref -Raw | ConvertFrom-Json
		$tn = [int]$j.teamNumber
		$tn4 = '{0:D4}' -f $tn
		$te = [int]$tn4.Substring(0, 2)
		$am = [int]$tn4.Substring(2, 2)
		$teamMsg = '[helix] Driver Station team: {0} (wpilib_preferences). LOCAL sim: set robot address to 127.0.0.1 or localhost. Remote LAN example: sim 10.{1}.{2}.2 , DS 10.{1}.{2}.21' -f $tn, $te, $am
		Write-Host $teamMsg
	} catch {
		Write-Host '[helix] Could not read team number from wpilib_preferences.json'
	}
}

Write-Host '[helix] Starting simulateJavaRelease (release desktop JNI + driver-station socket)...'
$gw = Join-Path $RepoRoot 'gradlew.bat'
& $gw @('simulateJavaRelease')
$exitCode = 0
if ($null -ne $LASTEXITCODE) {
	$exitCode = $LASTEXITCODE
}
exit $exitCode
