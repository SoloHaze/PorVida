<#
.SYNOPSIS
  Automatiza la captura de logs (adb logcat) para una app Android (reservas / calendario).

.DESCRIPTION
  Permite:
    - Verificar adb y dispositivo.
    - Detectar package (búsqueda parcial o exacta).
    - Capturar logs continuos filtrados por palabras clave típicas (order, booking, reserva, slot...).
    - Capturar ventana puntual tras reproducir un evento (dump).
    - Rotar archivos por tamaño o intervalo.
    - Filtrar por PID(s) para reducir ruido.
    - Generar resumen rápido de ocurrencias.

.PARAMETER Package
  Nombre completo del package (ej: com.zubale.app) o fragmento (ej: zubale). Si es fragmento, se selecciona interactivo si hay múltiples.

.PARAMETER OutputDir
  Carpeta de salida (se crea si no existe). Por defecto: ./logs

.PARAMETER Keywords
  Palabras clave (findstr /i). Por defecto: order,booking,reserva,calendar,slot,agenda

.PARAMETER Continuous
  Modo captura continua (hasta Ctrl+C) con filtro de palabras clave y PID(s).

.PARAMETER EventDump
  Limpia buffer, espera a que reproduzcas el flujo en la app y luego hace un dump único (-d).

.PARAMETER Raw
  No aplica findstr de Keywords (salvo que tú piping manual). Útil para inspección inicial.

.PARAMETER TagFilter
  Especificación de tags y niveles estilo logcat (ej: "BookingFlow:D CalendarGen:I *:S"). Ignorado si vacío.

.PARAMETER RotateSizeMB
  Si se indica (>0), rota el archivo cuando supera ese tamaño (aprox, chequeo periódico).

.PARAMETER RotateMinutes
  Si se indica (>0), rota cada N minutos.

.PARAMETER IncludeAllProcesses
  Intenta incluir todos los procesos que coinciden con el package (no solo el pid principal).

.PARAMETER RefreshPidSeconds
  Intervalo en segundos para re-evaluar PID(s) en modo continuo (por reinicios). Default 15.

.EXAMPLE
  .\android_log_capture.ps1 -Package com.zubale.app -Continuous

.EXAMPLE
  .\android_log_capture.ps1 -Package zubale -EventDump

.EXAMPLE
  .\android_log_capture.ps1 -Package com.zubale.app -Continuous -Raw -TagFilter "BookingFlow:D *:S"

.NOTES
  Requiere adb en PATH y dispositivo autorizado. Presiona Ctrl + C para detener modo continuo.
#>

[CmdletBinding()]
param(
    [string] $Package,
    [string] $OutputDir = "./logs",
    [string[]] $Keywords = @("order","booking","reserva","calendar","slot","agenda"),
    [switch] $Continuous,
    [switch] $EventDump,
    [switch] $Raw,
    [string] $TagFilter,
    [int] $RotateSizeMB = 0,
    [int] $RotateMinutes = 0,
    [switch] $IncludeAllProcesses,
    [int] $RefreshPidSeconds = 15
)

# ===================== Helpers =====================

function Write-Info($msg){ Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Warn($msg){ Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Write-Err($msg){  Write-Host "[ERR ] $msg" -ForegroundColor Red }

function Assert-Adb {
    if (-not (Get-Command adb -ErrorAction SilentlyContinue)){
        Write-Err "adb no encontrado en PATH. Instala platform-tools y agrega a PATH."
        exit 1
    }
}

function Assert-Device {
    $devices = (& adb devices) -split "`n" | Where-Object {$_ -match "`tdevice`r?$"}
    if (-not $devices){
        Write-Err "No hay dispositivo en estado 'device'. Revisa: adb devices"
        exit 1
    }
}

function Resolve-Package {
    param([string]$Pattern)
    if (-not $Pattern){
        Write-Err "Debes proporcionar -Package (nombre o fragmento)."
        exit 1
    }

    # Si parece nombre completo (contiene 2+ puntos y no espacios) intentamos validar directo
    $isFull = ($Pattern -match '^[a-zA-Z0-9_\.]+\.[a-zA-Z0-9_\.]+$')
    $all = & adb shell pm list packages 2>$null | ForEach-Object {
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
        [switch]$All
    )
    # pidof (single) + fallback ps
    $pidof = (& adb shell pidof -s $Pkg 2>$null).Trim()
    $list = @()
    if ($pidof) { $list += $pidof }

    if ($All){
        # Incluir otros procesos con el package en ps
        $psLines = & adb shell ps 2>$null | Select-String -Pattern $Pkg
        foreach($l in $psLines){
            $parts = ($l.ToString() -split "\s+")
            # Formato tradicional / Android 8+: PID suele estar en la segunda o tercera posición.
            $candidate = ($parts | Where-Object {$_ -match '^\d+$'} | Select-Object -First 1)
            if ($candidate -and -not ($list -contains $candidate)){
                $list += $candidate
            }
        }
    }
    $list = $list | Where-Object { $_ -match '^\d+$'} | Sort-Object -Unique
    return $list
}

function New-LogFileName {
    param([string]$BaseDir,[string]$Pkg,[string]$Suffix)
    $ts = Get-Date -Format "yyyyMMdd_HHmmss"
    $name = "${Pkg}_$Suffix_$ts.log"
    return (Join-Path $BaseDir $name)
}

function Start-EventDump {
    param(
        [string]$Pkg,
        [string]$Dir,
        [string[]]$Pids,
        [string[]]$Kw,
        [switch]$RawMode,
        [string]$TagFilterSpec
    )
    Write-Info "Limpiando buffer..."
    & adb logcat -c

    Write-Host ""
    Write-Host "Realiza ahora la acción en la app (abrir pedido / generar slots)."
    Read-Host "Presiona Enter cuando hayas terminado"

    $file = New-LogFileName -BaseDir $Dir -Pkg $Pkg -Suffix "EVENT"
    Write-Info "Volcando logs (-d) a: $file"

    $baseArgs = @("logcat","-d","-v","time")
    if ($TagFilterSpec){
        $baseArgs += ($TagFilterSpec -split ' ')
    }
    elseif ($Pids.Count -gt 0){
        foreach($p in $Pids){ $baseArgs += @("--pid",$p) }
    }

    $rawOut = & adb @baseArgs 2>&1

    if (-not $RawMode){
        # Aplicar filtro de keywords (findstr es case-insensitive con /i)
        $pattern = $Kw -join "|"
        $filtered = $rawOut | Select-String -Pattern $pattern -SimpleMatch -CaseSensitive:$false | ForEach-Object { $_.Line }
        if ($filtered.Count -eq 0){
            Write-Warn "Sin coincidencias de keywords. Guardando salida completa."
            $rawOut | Out-File -Encoding UTF8 $file
        } else {
            $filtered | Out-File -Encoding UTF8 $file
            # Guardar full también opcional
            $fullFile = $file -replace '\.log$', '.full.log'
            $rawOut | Out-File -Encoding UTF8 $fullFile
            Write-Info "Archivo filtrado: $file"
            Write-Info "Archivo completo:  $fullFile"
        }
    } else {
        $rawOut | Out-File -Encoding UTF8 $file
    }

    Show-KeywordSummary -File $file -Keywords $Kw
}

function Show-KeywordSummary {
    param(
        [string]$File,
        [string[]]$Keywords
    )
    if (-not (Test-Path $File)) { return }
    Write-Host ""
    Write-Host "=== Resumen por palabra clave ===" -ForegroundColor Green
    $content = Get-Content $File
    foreach($k in $Keywords){
        $count = ($content | Select-String -Pattern $k -SimpleMatch -CaseSensitive:$false).Count
        "{0,-12} : {1}" -f $k, $count | Write-Host
    }
    Write-Host "================================="
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
        [int]$RefreshPidSeconds
    )

    $currentFile = New-LogFileName -BaseDir $Dir -Pkg $Pkg -Suffix "LIVE"
    Write-Info "Escribiendo en: $currentFile"
    Write-Info "Ctrl + C para detener."

    $stopWatch = [System.Diagnostics.Stopwatch]::StartNew()
    $lastRotateTime = Get-Date

    $baseArgs = @("logcat","-v","time")
    $pidSnapshot = @()

    $rotateNeeded = {
        param($file)
        $rotate = $false
        if ($RotateSizeMB -gt 0 -and (Test-Path $file)){
            $sizeMB = ((Get-Item $file).Length / 1MB)
            if ($sizeMB -ge $RotateSizeMB){ $rotate = $true }
        }
        if ($RotateMinutes -gt 0){
            $elapsed = (New-TimeSpan -Start $lastRotateTime -End (Get-Date)).TotalMinutes
            if ($elapsed -ge $RotateMinutes){ $rotate = $true }
        }
        return $rotate
    }

    # Lanzamos un hilo (job) para refrescar PIDs si no usamos TagFilter
    $pidJob = $null
    if (-not $TagFilterSpec){
        $pidJob = Start-Job -ScriptBlock {
            param($Pkg,$AllProcesses,$RefreshSeconds)
            while($true){
                $psOut = & adb shell ps 2>$null | Select-String -Pattern $Pkg
                $pids = @()
                $pidof = (& adb shell pidof -s $Pkg 2>$null).Trim()
                if ($pidof){ $pids += $pidof }
                foreach($line in $psOut){
                    $parts = ($line.ToString() -split "\s+")
                    $candidate = ($parts | Where-Object {$_ -match '^\d+$'} | Select-Object -First 1)
                    if ($candidate -and -not ($pids -contains $candidate)){ $pids += $candidate }
                }
                $pids = $pids | Sort-Object -Unique
                [pscustomobject]@{ Timestamp=(Get-Date); Pids=$pids }
                Start-Sleep -Seconds $RefreshSeconds
            }
        } -ArgumentList $Pkg,$AllProcesses,$RefreshPidSeconds
    }

    try {
        # Abrimos logcat sin PID (si TagFilter) o con PIDs cambiantes (los añadiremos manualmente con re-lanzamiento si cambian)
        # Estrategia simple: si no TagFilter -> no usamos --pid directo; filtramos manualmente post-captura por regex PID.
        # (Porque re-lanzar adb cada vez que cambia PID produce cortes.)
        $logArgs = $baseArgs.Clone()
        if ($TagFilterSpec){
            $logArgs += ($TagFilterSpec -split ' ')
        }
        Write-Info "Iniciando adb logcat (flujo)..."
        $proc = Start-Process -FilePath "adb" -ArgumentList $logArgs -NoNewWindow -RedirectStandardOutput Pipe -PassThru

        $reader = $proc.StandardOutput
        while(-not $proc.HasExited){
            $line = $reader.ReadLine()
            if ($null -eq $line){ Start-Sleep -Milliseconds 50; continue }

            # Actualizar pids desde job
            if ($pidJob){
                $latest = Receive-Job -Job $pidJob -Keep | Select-Object -Last 1
                if ($latest){
                    $pidSnapshot = $latest.Pids
                }
            }

            $accept = $true
            if (-not $TagFilterSpec){
                if ($pidSnapshot.Count -gt 0){
                    # Intento de extracción de PID en formato threadtime/time: "MM-DD HH:MM:SS.mmm PID TID ..."
                    $pidInLine = ($line -split "\s+" | Where-Object {$_ -match '^\d+$'} | Select-Object -First 1)
                    if ($pidInLine -and -not ($pidSnapshot -contains $pidInLine)){
                        $accept = $false
                    }
                }
            }

            if ($accept){
                if (-not $RawMode -and $Kw.Count -gt 0){
                    $match = $false
                    foreach($k in $Kw){
                        if ($line -imatch [Regex]::Escape($k)){ $match = $true; break }
                    }
                    if (-not $match){ $accept = $false }
                }
            }

            if ($accept){
                Add-Content -Path $currentFile -Value $line
            }

            if (& $rotateNeeded $currentFile){
                Write-Info "Rotando archivo..."
                Show-KeywordSummary -File $currentFile -Keywords $Kw
                $currentFile = New-LogFileName -BaseDir $Dir -Pkg $Pkg -Suffix "LIVE"
                Write-Info "Nuevo archivo: $currentFile"
                $lastRotateTime = Get-Date
            }
        }
    }
    finally {
        if ($pidJob){ Stop-Job $pidJob -Force | Out-Null; Remove-Job $pidJob -Force | Out-Null }
        Show-KeywordSummary -File $currentFile -Keywords $Kw
        Write-Info "Captura continua detenida."
    }
}
function Resolve-Adb {
    param([string]$Explicit)

    if ($Explicit -and (Test-Path $Explicit)) { return $Explicit }

    $candidates = @(
        $Explicit
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
        "$env:ANDROID_HOME\platform-tools\adb.exe"
        "C:\Android\Sdk\platform-tools\adb.exe"
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    ) | Where-Object { $_ -and (Test-Path $_) }

    if ($candidates.Count -gt 0) { return $candidates[0] }

    $fromPath = (Get-Command adb -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source)
    if ($fromPath) { return $fromPath }

    throw "[ERR ] adb no encontrado. Agrega platform-tools al PATH o define ANDROID_SDK_ROOT."
}

# Uso:
try {
    $adb = Resolve-Adb
    Write-Host "[INFO] Usando adb: $adb" -ForegroundColor Cyan
} catch {
    Write-Host $_ -ForegroundColor Red
    exit 1
}

# Ejemplo:
& $adb version
# ===================== Main Flow =====================

if (-not ($Continuous -or $EventDump)){
    Write-Warn "No especificaste -Continuous ni -EventDump. Ejemplos:"
    Write-Host "  .\android_log_capture.ps1 -Package com.zubale.app -Continuous"
    Write-Host "  .\android_log_capture.ps1 -Package zubale -EventDump"
    exit 1
}

Assert-Adb
Assert-Device

$resolvedPackage = Resolve-Package -Pattern $Package

if (-not (Test-Path $OutputDir)){
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

Write-Info "Package objetivo: $resolvedPackage"
Write-Info "OutputDir: $OutputDir"

# Obtener PIDs iniciales (para modos que lo requieren)
$pidsInitial = Get-Pids -Pkg $resolvedPackage -All:$IncludeAllProcesses
if ($pidsInitial.Count -eq 0){
    Write-Warn "No se encontró PID activo. Abre la app ahora."
    Read-Host "Presiona Enter tras abrir la app"
    $pidsInitial = Get-Pids -Pkg $resolvedPackage -All:$IncludeAllProcesses
}

if ($pidsInitial.Count -gt 0){
    Write-Info "PID(s) inicial(es): $($pidsInitial -join ', ')"
} else {
    Write-Warn "Persisten sin PID. Continuaré (modo TagFilter o esperando aparición)."
}

if ($EventDump){
    Start-EventDump -Pkg $resolvedPackage -Dir $OutputDir -Pids $pidsInitial -Kw $Keywords -RawMode:$Raw -TagFilterSpec $TagFilter
}

if ($Continuous){
    Start-ContinuousCapture -Pkg $resolvedPackage -Dir $OutputDir -Kw $Keywords -RawMode:$Raw -TagFilterSpec $TagFilter `
        -RotateSizeMB $RotateSizeMB -RotateMinutes $RotateMinutes -AllProcesses:$IncludeAllProcesses -RefreshPidSeconds $RefreshPidSeconds
}


