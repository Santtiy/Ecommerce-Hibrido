<#
.SYNOPSIS
    Detiene SOLO los servicios lanzados por ops\arrancar.ps1, usando los PID de ops\pids.txt.

.DESCRIPTION
    No usa "Get-Process java | Stop-Process", porque eso cerraria tambien IntelliJ y cualquier otra
    aplicacion Java abierta. Antes de cerrar cada PID comprueba que siga siendo el mismo tipo de
    proceso (java o powershell): Windows reutiliza los numeros de PID, asi que un PID antiguo podria
    pertenecer ahora a otro programa.

    Uso:
        powershell -ExecutionPolicy Bypass -File ops\detener.ps1              # solo los servicios
        powershell -ExecutionPolicy Bypass -File ops\detener.ps1 -ConInfra    # y ademas docker compose down

    docker compose down NO borra el volumen de PostgreSQL: las ordenes guardadas se conservan.
#>

# -ConInfra es un interruptor: si se escribe, vale $true; si no, $false.
param(
    [switch]$ConInfra
)

$raiz        = Split-Path -Parent $PSScriptRoot
$archivoPids = Join-Path $PSScriptRoot 'pids.txt'
$compose     = Join-Path $raiz 'docker-compose.yml'

# ---------------------------------------------------------------------------------------------
# Cierra un PID solo si existe y su nombre de proceso es el esperado. Devuelve un texto con lo ocurrido.
# ---------------------------------------------------------------------------------------------
function Stop-PidSeguro([string]$pidTexto, [string]$nombreEsperado) {
    if (-not $pidTexto) { return 'sin PID registrado' }

    $proceso = Get-Process -Id ([int]$pidTexto) -ErrorAction SilentlyContinue
    if (-not $proceso) {
        return "PID $pidTexto ya no existe"
    }
    if ($proceso.ProcessName -ne $nombreEsperado) {
        # El PID ahora es de otro programa: no se toca.
        return "PID $pidTexto ahora es '$($proceso.ProcessName)', no '$nombreEsperado': NO se toca"
    }
    Stop-Process -Id $proceso.Id -Force
    return "PID $pidTexto detenido"
}

# ---------------------------------------------------------------------------------------------
# 1. Servicios registrados en pids.txt
# ---------------------------------------------------------------------------------------------

if (-not (Test-Path $archivoPids)) {
    Write-Host "No existe $archivoPids : no hay servicios registrados que detener." -ForegroundColor Yellow
} else {
    Write-Host "== Deteniendo servicios registrados en pids.txt" -ForegroundColor Cyan

    foreach ($linea in Get-Content $archivoPids) {
        if (-not $linea.Trim()) { continue }              # salta lineas vacias

        # Formato de cada linea: nombre;PID_de_java
        $partes  = $linea.Split(';')
        $nombre  = $partes[0]
        $pidJava = if ($partes.Count -gt 1) { $partes[1] } else { '' }

        $resultado = Stop-PidSeguro $pidJava 'java'
        $color = if ($resultado -like '*detenido') { 'Green' } else { 'Yellow' }
        Write-Host ("  {0,-22} {1}" -f $nombre, $resultado) -ForegroundColor $color
    }

    # Los PID ya no sirven: se borra el archivo para que arrancar.ps1 empiece limpio.
    Remove-Item $archivoPids
}

# ---------------------------------------------------------------------------------------------
# 2. Infraestructura (solo con -ConInfra)
# ---------------------------------------------------------------------------------------------

if ($ConInfra) {
    Write-Host "== docker compose down" -ForegroundColor Cyan
    docker compose -f $compose down
    if ($LASTEXITCODE -ne 0) {
        Write-Host "docker compose down devolvio un error (codigo $LASTEXITCODE)." -ForegroundColor Red
        exit 1
    }
}

Write-Host "Listo." -ForegroundColor Green
