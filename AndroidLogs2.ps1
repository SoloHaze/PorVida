<#
.SYNOPSIS
  Automatiza la captura de logs (adb logcat) para una app Android (reservas / calendario).

.DESCRIPTION
  Funcionalidades:
    - Resolución de adb (parámetro, variables ANDROID_* o PATH).
    - Verificación de dispositivo conectado.
    - Detección de package por fragmento o exacto (interactivo si múltiples).
    - Captura continua filtrada por keywords y/o TagFilter y/o PID(s).
    - Event dump (limpia buffer, reproduces escenario, descarga -d).
    - Rotación por tamaño (MB) o tiempo (minutos).
    - Refresco periódico de PID(s) (reinicios de proceso).
    - Resumen final de ocurrencias por keyword.
    - Opción Raw (sin filtro de keywords).
    - Filtro por múltiples procesos del package.
    - TagFilter estilo logcat (ej: "MyTag:D OtherTag:I *:S").

.PARAMETER Package
  Nombre completo de package (com.xxx.yyy) o fragmento (ej: zubale).

.PARAMETER OutputDir
  Carpeta de salida. Default: ./logs

.PARAMETER Keywords
  Palabras clave para filtrar (OR). Default: order, booking, reserva, calendar, slot, agenda

.PARAMETER Continuous
  Modo seguimiento (hasta Ctrl+C).

.PARAMETER EventDump
  Modo captura puntual (-d) tras limpiar buffer y esperar Enter.

.PARAMETER Raw
  Desactiva filtrado por keywords (muestra todo lo aceptado por otras condiciones).

.PARAMETER TagFilter
  Cadena de especificación de filtros de logcat (tags:nivel ...). Si presente, se pasa tal cual.

.PARAMETER RotateSizeMB
  Rota archivo continuo al alcanzar tamaño (MB). 0 = deshabilitado.

.PARAMETER RotateMinutes
  Rota archivo continuo cada N minutos. 0 = deshabilitado.

.PARAMETER IncludeAllProcesses
  Intenta incluir todos los procesos cuyo ps contenga el package (además del principal).

.PARAMETER RefreshPidSeconds
  Intervalo de refresco de PIDs en modo continuo (default 15).

.PARAMETER AdbPath
  Ruta explícita a adb.exe (opcional). Si no se usa, intenta ANDROID_SDK_ROOT / ANDROID_HOME / PATH.

.EXAMPLE
  .\AndroidLogs.ps1 -Package com.zubale.app -Continuous

.EXAMPLE
  .\AndroidLogs.ps1 -Package zubale -EventDump

.EXAMPLE
  .\AndroidLogs.ps1 -Package com.zubale.app -Continuous -Raw -TagFilter "BookingFlow:D *:S"

.NOTES
  Requiere dispositivo en estado 'device'. Ctrl + C para detener modo continuo (imprime resumen).
#>

[CmdletBinding()]
param(
    [string]  $Package,
    [string]  $OutputDir = "./logs",
    [string[]]$Keywords = @("order","booking","reserva","calendar","slot","agenda"),
    [switch]  $Continuous,
    [switch]  $EventDump,
    [switch]  $Raw,
    [string]  $TagFilter,
    [int]     $RotateSizeMB = 0,
    [int]     $RotateMinutes = 0,
    [switch]  $IncludeAllProcesses,
    [int]     $RefreshPidSeconds = 15,
    [string]  $AdbPath
)

# ===================== Helpers =====================

function Write-Info($msg){ Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Warn($msg){ Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Write-Err ($msg){ Write-Host "[ERR ] $msg" -ForegroundColor Red }

function Resolve-Adb {
    param([string]$Explicit)
    if ($Explicit -and (Test-Path $Explicit)) { return (Resolve-Path $Explicit).Path }

    $candidates = @(
        $Explicit
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
        "$env:ANDROID_HOME\platform-tools\adb.exe"
        "C:\Android\Sdk\platform-tools\adb.exe"
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    ) | Where-Object { $_ -and (Test-Path $_) }

    if ($candidates.Count -gt 0) { return (Resolve-Path $candidates[0]).Path }

    $fromPath = (Get-Command adb -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source)
    if ($fromPath) { return $fromPath }

    throw "adb no encontrado (usa -AdbPath o agrega platform-tools al PATH)."
}

function Assert-Device {
    param($Adb)
    $raw = & $Adb devices 2>$null
    $devices = $raw -split "\r?\n" | Where-Object { $_ -match "`tdevice$" }
    if (-not $devices){
        Write-Err "No hay dispositivo en estado 'device'. Revisa: adb devices"
        exit 1
    }
}

function Resolve-Package {
    param([string]$Pattern,[string]$Adb)
    if (-not $Pattern){
        Write-Err "Debes proporcionar -Package."
        exit 1
    }
    $isFull = ($Pattern -match '^[a-zA-Z0-9_]+\.[a-zA-Z0-9_.]+$')
    $all = & $Adb shell pm list packages 2>$null | ForEach-Object {
        ($_ -replace '^package:','').Trim()
    }

    if ($isFull -and $all -contains $Pattern){
        return $Pattern
    }

    $matches = $all | Where-Object { $_ -imatch [Regex]::Escape($Pattern) }
    if (-not $matches){
        Write-Err "No se encontró package que contenga: $Pattern"
        exit 1
    }
    if ($matches.Count -eq 1){
        Write-Info "Package resuelto: $($matches[0])"
        return $matches[0]
    }

    Write-Warn "Múltiples coincidencias:"
    $i=0
    $matches | ForEach-Object { "{0}. {1}" -f (++$i), $_ | Write-Host }
    $sel = Read-Host "Elige número"
    if ($sel -as [int] -and $sel -ge 1 -and $sel -le $matches.Count){
        $chosen = $matches[$sel-1]
        Write-Info "Seleccionado: $chosen"
        return $chosen
    } else {
        Write-Err "Selección inválida."
        exit 1
    }
}

function Get-Pids {
    param(
        [string]$Pkg,
        [switch]$All,
        [string]$Adb
    )
    $pids = @()
    $pidof = (& $Adb shell pidof -s $Pkg 2>$null).Trim()
    if ($pidof){ $pids += $pidof }

    if ($All){
        $psLines = & $Adb shell ps 2>$null | Select-String -Pattern $Pkg
        foreach($l in $psLines){
            $parts = ($l.ToString() -split "\s+")
            $candidate = ($parts | Where-Object { $_ -match '^\d+$'} | Select-Object -First 1)
            if ($candidate -and -not ($pids -contains $candidate)){ $pids += $candidate }
        }
    }
    $pids | Where-Object { $_ -match '^\d+$'} | Sort-Object -Unique
}

function New-LogFileName {
    param([string]$BaseDir,[string]$Pkg,[string]$Suffix)
    $ts = Get-Date -Format "yyyyMMdd_HHmmss"
    Join-Path $BaseDir "${Pkg}_$Suffix_$ts.log"
}

function Show-KeywordSummary {
    param([string]$File,[string[]]$Keywords)
    if (-not (Test-Path $File) -or -not $Keywords -or $Keywords.Count -eq 0){ return }
    Write-Host "`n=== Resumen por palabra clave ===" -ForegroundColor Green
    $content = Get-Content $File -ErrorAction SilentlyContinue
    foreach($k in $Keywords){
        $count = ($content | Select-String -Pattern $k -SimpleMatch -CaseSensitive:$false).Count
        "{0,-12} : {1}" -f $k, $count | Write-Host
    }
    Write-Host "================================="
}

function Start-EventDump {
    param(
        [string]$Pkg,
        [string]$Dir,
        [string[]]$Pids,
        [string[]]$Kw,
        [switch]$RawMode,
        [string]$TagFilterSpec,
        [string]$Adb
    )

    Write-Info "Limpiando buffer..."
    & $Adb logcat -c

    Write-Host ""
    Write-Host "Reproduce ahora la acción en la app (escenario que quieres capturar)."
    Read-Host "Presiona Enter cuando hayas terminado"

    $file = New-LogFileName -BaseDir $Dir -Pkg $Pkg -Suffix "EVENT"
    Write-Info "Volcando logs (-d) a: $file"

    $baseArgs = @("logcat","-d","-v","time")

    if ($TagFilterSpec){
        $baseArgs += ($TagFilterSpec -split '\s+')
    }
    elseif ($Pids -and $Pids.Count -gt 0){
        foreach($p in $Pids){ $baseArgs += @("--pid",$p) }
    }

    $rawOut = & $Adb @baseArgs 2>&1

    if ($RawMode -or -not $Kw -or $Kw.Count -eq 0){
        $rawOut | Out-File -Encoding UTF8 $file
    } else {
        $pattern = ($Kw | ForEach-Object { [Regex]::Escape($_) }) -join '|'
        $filtered = $rawOut | Where-Object { $_ -imatch $pattern }
        if ($filtered.Count -eq 0){
            Write-Warn "Sin coincidencias de keywords. Guardando salida completa."
            $rawOut | Out-File -Encoding UTF8 $file
        } else {
            $filtered | Out-File -Encoding UTF8 $file
            $fullFile = $file -replace '\.log$', '.full.log'
            $rawOut | Out-File -Encoding UTF8 $fullFile
            Write-Info "Archivo filtrado: $file"
            Write-Info "Archivo completo:  $fullFile"
        }
    }

    Show-KeywordSummary -File $file -Keywords $Kw
    Write-Info "Dump terminado."
}

function Start-ContinuousCapture {
    param(
        [string]$Pkg,
        [string]$Dir,
        [string[]]$Kw,
        [switch]$RawMode,
        [string]$TagFilterSpec,
        [int]$RotateSizeMB,
        [int]$RotateMinutes,
        [switch]$AllProcesses,
        [int]$RefreshPidSeconds,
        [string]$Adb
    )

    $currentFile = New-LogFileName -BaseDir $Dir -Pkg $Pkg -Suffix "LIVE"
    Write-Info "Archivo actual: $currentFile"
    Write-Info "Ctrl + C para detener."

    $lastRotateTime = Get-Date
    $pidSnapshot = @()

    $rotateNeeded = {
        param($file,$RotateSizeMB,$RotateMinutes,$lastRotateTimeRef)
        $rotate = $false
        if ($RotateSizeMB -gt 0 -and (Test-Path $file)){
            $sizeMB = ((Get-Item $file).Length / 1MB)
            if ($sizeMB -ge $RotateSizeMB){ $rotate = $true }
        }
        if ($RotateMinutes -gt 0){
            $elapsed = (New-TimeSpan -Start $lastRotateTimeRef -End (Get-Date)).TotalMinutes
            if ($elapsed -ge $RotateMinutes){ $rotate = $true }
        }
        return $rotate
    }

    # Job de refresco de PIDs si no se usa TagFilter (porque TagFilter ya filtra en logcat)
    $pidJob = if (-not $TagFilterSpec) {
        Start-Job -ScriptBlock {
            param($Pkg,$AllProcesses,$RefreshSeconds,$AdbPath)
            while($true){
                $pids = @()
                $pidof = (& $AdbPath shell pidof -s $Pkg 2>$null).Trim()
                if ($pidof){ $pids += $pidof }
                if ($AllProcesses){
                    $psOut = & $AdbPath shell ps 2>$null | Select-String -Pattern $Pkg
                    foreach($line in $psOut){
                        $parts = ($line.ToString() -split "\s+")
                        $candidate = ($parts | Where-Object {$_ -match '^\d+$'} | Select-Object -First 1)
                        if ($candidate -and -not ($pids -contains $candidate)){ $pids += $candidate }
                    }
                }
                $pids = $pids | Sort-Object -Unique
                [pscustomobject]@{ Timestamp = Get-Date; Pids = $pids }
                Start-Sleep -Seconds $RefreshSeconds
            }
        } -ArgumentList $Pkg,$AllProcesses,$RefreshPidSeconds,$Adb
    }

    $logArgs = @("logcat","-v","time")
    if ($TagFilterSpec){
        $logArgs += ($TagFilterSpec -split '\s+')
    }

    Write-Info "Iniciando adb logcat..."
    $proc = Start-Process -FilePath $Adb -ArgumentList $logArgs -NoNewWindow -RedirectStandardOutput Pipe -PassThru
    $reader = $proc.StandardOutput

    $kwRegex = $null
    if (-not $RawMode -and $Kw -and $Kw.Count -gt 0){
        $kwRegex = ($Kw | ForEach-Object { [Regex]::Escape($_) }) -join '|'
    }

    try {
        while(-not $proc.HasExited){
            $line = $reader.ReadLine()
            if ($null -eq $line){ Start-Sleep -Milliseconds 30; continue }

            if ($pidJob){
                $latest = Receive-Job -Job $pidJob -Keep | Select-Object -Last 1
                if ($latest){ $pidSnapshot = $latest.Pids }
            }

            $accept = $true

            if (-not $TagFilterSpec -and $pidSnapshot.Count -gt 0){
                # Intento de extraer PID (threadtime/time format -> primer número candidato)
                $pidInLine = ($line -split "\s+" | Where-Object { $_ -match '^\d+$' } | Select-Object -First 1)
                if ($pidInLine -and -not ($pidSnapshot -contains $pidInLine)){
                    $accept = $false
                }
            }

            if ($accept -and $kwRegex -and -not $line -imatch $kwRegex){
                $accept = $false
            }

            if ($accept){
                Add-Content -Path $currentFile -Value $line
            }

            if (& $rotateNeeded $currentFile $RotateSizeMB $RotateMinutes $lastRotateTime){
                Write-Info "Rotando archivo..."
                Show-KeywordSummary -File $currentFile -Keywords $Kw
                $currentFile = New-LogFileName -BaseDir $Dir -Pkg $Pkg -Suffix "LIVE"
                Write-Info "Nuevo archivo: $currentFile"
                $lastRotateTime = Get-Date
            }
        }
    }
    catch {
        Write-Err "Error en captura continua: $_"
    }
    finally {
        if ($pidJob){
            Stop-Job $pidJob -Force | Out-Null
            Remove-Job $pidJob -Force | Out-Null
        }
        if (-not $proc.HasExited){ $proc.Kill() | Out-Null }
        Show-KeywordSummary -File $currentFile -Keywords $Kw
        Write-Info "Captura continua detenida."
    }
}

# ===================== MAIN =====================

if (-not ($Continuous -or $EventDump)){
    Write-Warn "Debes indicar -Continuous o -EventDump."
    Write-Host "Ejemplos:"
    Write-Host "  .\AndroidLogs.ps1 -Package com.zubale.app -Continuous"
    Write-Host "  .\AndroidLogs.ps1 -Package zubale -EventDump"
    exit 1
}

# Resolver adb
try {
    $ADB = Resolve-Adb -Explicit $AdbPath
    Write-Host "[INFO] Usando adb: $ADB" -ForegroundColor Cyan
    & $ADB version
} catch {
    Write-Err $_
    exit 1
}

Assert-Device -Adb $ADB

$resolvedPackage = Resolve-Package -Pattern $Package -Adb $ADB

if (-not (Test-Path $OutputDir)){
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

Write-Info "Package objetivo: $resolvedPackage"
Write-Info "OutputDir: $OutputDir"
if ($Raw){ Write-Warn "Modo Raw: sin filtro de keywords." }
elseif ($Keywords -and $Keywords.Count -gt 0){
    Write-Info "Keywords: $($Keywords -join ', ')"
}

# PIDs iniciales (solo para info y EventDump)
$pidsInitial = Get-Pids -Pkg $resolvedPackage -All:$IncludeAllProcesses -Adb $ADB
if ($pidsInitial.Count -eq 0){
    Write-Warn "No se encontraron PIDs. Abre la app ahora si no está abierta."
    if (-not $EventDump){
        # En continuo dejamos que el job los detecte luego
        Start-Sleep -Seconds 2
    } else {
        Read-Host "Presiona Enter tras abrir la app"
        $pidsInitial = Get-Pids -Pkg $resolvedPackage -All:$IncludeAllProcesses -Adb $ADB
    }
}

if ($pidsInitial.Count -gt 0){
    Write-Info "PID(s) inicial(es): $($pidsInitial -join ', ')"
} else {
    Write-Warn "Sigue sin PID. Se intentará filtrar dinámicamente (si aplica)."
}

# Hook Ctrl+C para resumen final
$script:TerminateRequested = $false
$handler = {
    $script:TerminateRequested = $true
    Write-Warn "Ctrl+C recibido, cerrando de forma limpia..."
}
$null = Register-EngineEvent PowerShell.Exiting -Action {} # placeholder
$ctrlC = Register-EngineEvent ConsoleCancelEvent -SourceIdentifier CtrlC -Action {
    $eventArgs.Cancel = $true
    & $handler
}

try {
    if ($EventDump){
        Start-EventDump -Pkg $resolvedPackage -Dir $OutputDir -Pids $pidsInitial -Kw $Keywords `
            -RawMode:$Raw -TagFilterSpec $TagFilter -Adb $ADB
    }
    if ($Continuous -and -not $script:TerminateRequested){
        Start-ContinuousCapture -Pkg $resolvedPackage -Dir $OutputDir -Kw $Keywords -RawMode:$Raw `
            -TagFilterSpec $TagFilter -RotateSizeMB $RotateSizeMB -RotateMinutes $RotateMinutes `
            -AllProcesses:$IncludeAllProcesses -RefreshPidSeconds $RefreshPidSeconds -Adb $ADB
    }
}
finally {
    if ($ctrlC){ Unregister-Event -SourceIdentifier CtrlC -ErrorAction SilentlyContinue }
    Write-Info "Fin."
}