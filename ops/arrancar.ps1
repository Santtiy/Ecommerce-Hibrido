<#
.SYNOPSIS
    Arranca la infraestructura (si hace falta) y los siete microservicios en segundo plano,
    esperando de verdad a que cada uno este listo antes de seguir.

.DESCRIPTION
    Pensado para Windows 11 y Windows PowerShell 5.1:

        powershell -ExecutionPolicy Bypass -File ops\arrancar.ps1             # arranque normal
        powershell -ExecutionPolicy Bypass -File ops\arrancar.ps1 -Reiniciar  # detiene lo anterior y arranca

    - La salida de cada servicio va a logs\<servicio>.log (para seguirlo en vivo:
      Get-Content logs\order-api.log -Wait -Tail 20).
    - ops\pids.txt guarda una linea por servicio:  nombre;PID_de_java
      ops\detener.ps1 usa ese archivo para detener SOLO esos procesos.
    - Si un servicio no arranca, el script se detiene, muestra las ultimas 30 lineas de su log
      y NO lanza los siguientes.

    - MODO DESARROLLO: la infraestructura (postgres-db, kafka, rabbitmq) se levanta con
      docker-compose.yml + docker-compose.dev.yml, que publica 5433, 29092 y 5672 SOLO en
      127.0.0.1. Los siete microservicios corren como jar, no en contenedor.
    - Requiere .env en la raiz (plantilla: .env.example). Sus contrasenas se pasan a los jar como
      DB_PASSWORD y RABBITMQ_PASSWORD, solo en este proceso; nunca se muestran.
    - Si los contenedores de los microservicios (modo AWS) estan en marcha, el script se detiene
      sin tocarlos y muestra el comando para pararlos.

    Nota: el archivo esta escrito sin tildes a proposito. PowerShell 5.1 lee los .ps1 sin BOM
    con la codificacion ANSI de Windows, y las tildes se verian corruptas.
#>

# -Reiniciar es un interruptor: si se escribe, primero se ejecuta detener.ps1.
param(
    [switch]$Reiniciar
)

# ---------------------------------------------------------------------------------------------
# 1. Rutas y configuracion
# ---------------------------------------------------------------------------------------------

# $PSScriptRoot es la carpeta de este script (ops\). La raiz del proyecto es la de arriba.
$raiz        = Split-Path -Parent $PSScriptRoot
$archivoPids = Join-Path $PSScriptRoot 'pids.txt'
$compose     = Join-Path $raiz 'docker-compose.yml'
$composeDev  = Join-Path $raiz 'docker-compose.dev.yml'     # capa de desarrollo (puertos en 127.0.0.1)
$archivoEnv  = Join-Path $raiz '.env'
$carpetaLogs = Join-Path $raiz 'logs'

$contenedores   = @('postgres-db', 'kafka', 'rabbitmq')
# Puertos que publica docker-compose.dev.yml y que usan por defecto los application.yml.
$puertosInfra   = @{ 'postgres-db' = 5433; 'kafka' = 29092; 'rabbitmq' = 5672 }
# Contenedores de los microservicios (modo AWS): no pueden convivir con los jar (8080/8081, grupos).
$contenedoresApp = @('order-api', 'order-processor', 'billing-worker', 'inventory-worker',
                     'notification-worker', 'dashboard-aggregator', 'analytics-service')
$segundosMaximo = 90     # tiempo maximo que se espera a cada servicio

# Servicios con su puerto HTTP. Los que no tienen servidor web llevan $null y se considera que
# estan listos cuando su log contiene la linea de Spring "Started ... in ... seconds".
$puertos = @{
    'order-api'            = 8080
    'dashboard-aggregator' = 8081
}

# Orden de arranque en tres tandas. Los de una tanda se lanzan juntos y se espera a que TODOS
# esten listos antes de pasar a la siguiente.
#  - Tanda 1: los workers crean sus topics de resultados y declaran sus colas; el dashboard se suscribe.
#  - Tanda 2: order-api SOLO, porque es quien crea new_orders con 3 particiones.
#  - Tanda 3: los dos consumidores de new_orders (analytics-service y order-processor).
# IMPORTANTE: si un consumidor se suscribe a new_orders ANTES de que exista, Kafka lo crea solo con
# 1 particion; order-api lo amplia luego a 3, pero ese consumidor se queda con la particion 0 hasta
# su siguiente refresco de metadatos (5 minutos) y no ve las ordenes de las particiones 1 y 2.
$tandas = @(
    @('billing-worker', 'inventory-worker', 'notification-worker', 'dashboard-aggregator'),
    @('order-api'),
    @('analytics-service', 'order-processor')
)

function Get-RutaJar([string]$modulo) { Join-Path $raiz "$modulo\target\$modulo-1.0.0.jar" }
function Get-RutaLog([string]$modulo) { Join-Path $carpetaLogs "$modulo.log" }

# Detiene el script con un mensaje en rojo.
function Stop-ConError([string]$mensaje) {
    Write-Host ""
    Write-Host "ERROR: $mensaje" -ForegroundColor Red
    exit 1
}

# Muestra las ultimas lineas del log de un servicio (los logs se escriben en UTF-8).
function Show-FinalLog([string]$modulo, [int]$lineas = 30) {
    $log = Get-RutaLog $modulo
    Write-Host ""
    Write-Host "----- Ultimas $lineas lineas de $log -----" -ForegroundColor Yellow
    if (Test-Path $log) { Get-Content $log -Tail $lineas -Encoding UTF8 } else { Write-Host "(el log no existe)" }
    # La salida de error va a un archivo aparte; se muestra solo si tiene algo.
    $err = $log -replace '\.log$', '.err.log'
    if ((Test-Path $err) -and (Get-Item $err).Length -gt 0) {
        Write-Host "----- $err -----" -ForegroundColor Yellow
        Get-Content $err -Tail $lineas -Encoding UTF8
    }
    Write-Host "------------------------------------------" -ForegroundColor Yellow
}

# ---------------------------------------------------------------------------------------------
# 2. Comprobaciones previas
# ---------------------------------------------------------------------------------------------

foreach ($herramienta in @('docker', 'java', 'mvn')) {
    if (-not (Get-Command $herramienta -ErrorAction SilentlyContinue)) {
        Stop-ConError "No se encontro '$herramienta' en el PATH."
    }
}
# Ruta completa de java.exe (la que usa esta consola), para lanzar los servicios con la misma JVM.
$java = (Get-Command java).Source

# Con -Reiniciar se detiene primero lo que haya registrado en pids.txt (sin tocar los contenedores).
if ($Reiniciar) {
    Write-Host "== Reiniciando: ejecutando detener.ps1" -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot 'detener.ps1')
}

# Si pids.txt tiene procesos java vivos, no se arranca otra vez (habria duplicados y puertos ocupados).
# Si los PID registrados ya estan todos muertos, el archivo es un residuo y simplemente se sobrescribe.
if (Test-Path $archivoPids) {
    $vivos = @()
    foreach ($linea in Get-Content $archivoPids) {
        $partes = $linea.Split(';')
        if ($partes.Count -lt 2 -or -not $partes[1]) { continue }
        $proceso = Get-Process -Id ([int]$partes[1]) -ErrorAction SilentlyContinue
        if ($proceso -and $proceso.ProcessName -eq 'java') { $vivos += $partes[0] }
    }
    if ($vivos.Count -gt 0) {
        Stop-ConError ("Ya hay servicios en marcha: " + ($vivos -join ', ') +
                       ". Usa -Reiniciar o ejecuta antes ops\detener.ps1.")
    }
    Write-Host "pids.txt anterior sin procesos vivos: se sobrescribe."
}

# Credenciales: .env es obligatorio (docker compose lo exige y los jar necesitan las mismas claves).
# Se leen las dos variables SIN mostrarlas y se exportan solo en este proceso: Start-Process hereda
# el entorno, asi que los jar las reciben como DB_PASSWORD y RABBITMQ_PASSWORD (application.yml).
if (-not (Test-Path $archivoEnv)) {
    Stop-ConError "No existe .env. Crealo con: Copy-Item .env.example .env  (y rellena POSTGRES_PASSWORD y RABBITMQ_PASSWORD)."
}
$valoresEnv = @{}
foreach ($linea in Get-Content $archivoEnv) {
    if ($linea -match '^\s*(POSTGRES_PASSWORD|RABBITMQ_PASSWORD)=(.*)$') {
        $valoresEnv[$Matches[1]] = $Matches[2].TrimEnd("`r").Trim()
    }
}
foreach ($v in @('POSTGRES_PASSWORD', 'RABBITMQ_PASSWORD')) {
    if (-not $valoresEnv[$v]) { Stop-ConError "$v no tiene valor en .env (plantilla: .env.example)." }
}
$env:DB_PASSWORD       = $valoresEnv['POSTGRES_PASSWORD']
$env:RABBITMQ_PASSWORD = $valoresEnv['RABBITMQ_PASSWORD']
Write-Host "  .env leido (POSTGRES_PASSWORD y RABBITMQ_PASSWORD con valor; no se muestran)."

# Los contenedores de los microservicios (modo AWS) no se detienen desde aqui: se pide hacerlo a mano.
$appEnMarcha = @($contenedoresApp | Where-Object { (docker inspect --format '{{.State.Running}}' $_ 2>$null) -eq 'true' })
if ($appEnMarcha.Count -gt 0) {
    Stop-ConError ("Hay contenedores de microservicios en marcha (modo AWS): " + ($appEnMarcha -join ', ') +
                   ". Detenlos (no borra datos) con: docker compose stop " + ($appEnMarcha -join ' '))
}

# Si 8080 u 8081 ya estan ocupados (por ejemplo, una ejecucion desde IntelliJ) se aborta ya.
foreach ($puerto in $puertos.Values) {
    if (Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue) {
        Stop-ConError "El puerto $puerto ya esta en uso. Cierra lo que lo ocupa (netstat -ano | findstr :$puerto)."
    }
}

# ---------------------------------------------------------------------------------------------
# 3. Infraestructura: postgres-db, kafka y rabbitmq deben estar "healthy"
# ---------------------------------------------------------------------------------------------

function Test-InfraSana {
    foreach ($c in $contenedores) {
        # Lee el estado del healthcheck definido en docker-compose.yml.
        $estado = docker inspect --format '{{.State.Health.Status}}' $c 2>$null
        if ($LASTEXITCODE -ne 0 -or $estado -ne 'healthy') {
            Write-Host "  $c -> $(if ($estado) { $estado } else { 'no existe o no esta corriendo' })"
            return $false
        }
        # Sano no basta: en modo AWS el contenedor esta sano pero su puerto no llega al host.
        $puerto = $puertosInfra[$c]
        $cliente = New-Object System.Net.Sockets.TcpClient
        try   { $abierto = $cliente.ConnectAsync('127.0.0.1', $puerto).Wait(2000) -and $cliente.Connected }
        catch { $abierto = $false }
        finally { $cliente.Dispose() }
        if (-not $abierto) {
            Write-Host "  $c -> healthy, pero 127.0.0.1:$puerto no responde (arrancado sin docker-compose.dev.yml)"
            return $false
        }
        Write-Host "  $c -> healthy (127.0.0.1:$puerto)"
    }
    return $true
}

Write-Host "== 1/4 Infraestructura" -ForegroundColor Cyan
docker info *> $null
if ($LASTEXITCODE -ne 0) { Stop-ConError "Docker no responde. Abre Docker Desktop y vuelve a intentarlo." }

if (-not (Test-InfraSana)) {
    Write-Host "Levantando la infraestructura (modo desarrollo) con docker compose (puede tardar un par de minutos)..."
    Write-Host "  Si estaba arrancada en modo AWS, compose recrea esos tres contenedores para publicar los puertos:"
    Write-Host "  PostgreSQL conserva sus datos (volumen); topics y colas los recrean los servicios al arrancar."
    # Solo los tres servicios de infraestructura. --wait no devuelve el control hasta que los
    # healthchecks pasan. docker compose lee .env de la raiz del proyecto.
    docker compose -f $compose -f $composeDev up -d --wait @contenedores
    if ($LASTEXITCODE -ne 0) { Stop-ConError "docker compose up fallo. Revisa: docker compose -f `"$compose`" -f `"$composeDev`" logs" }
    if (-not (Test-InfraSana)) { Stop-ConError "La infraestructura sigue sin estar sana." }
}

# ---------------------------------------------------------------------------------------------
# 4. Jar ejecutables: si falta alguno, se empaqueta todo
# ---------------------------------------------------------------------------------------------

Write-Host "== 2/4 Jar" -ForegroundColor Cyan
$todos  = $tandas | ForEach-Object { $_ }        # aplana las tandas en una lista
$faltan = @($todos | Where-Object { -not (Test-Path (Get-RutaJar $_)) })
if ($faltan.Count -gt 0) {
    Write-Host "Faltan jar de: $($faltan -join ', '). Ejecutando mvn clean package -DskipTests..."
    mvn -f (Join-Path $raiz 'pom.xml') clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { Stop-ConError "Maven fallo al empaquetar." }
} else {
    Write-Host "  Los siete jar existen."
}

# ---------------------------------------------------------------------------------------------
# 5. Lanzamiento en segundo plano y espera activa
# ---------------------------------------------------------------------------------------------

# Lanza "java -jar" directamente, sin ventana, con la salida redirigida:
#   logs\<servicio>.log      -> salida normal (aqui escribe Spring sus logs)
#   logs\<servicio>.err.log  -> salida de error (normalmente vacia)
# Start-Process exige dos archivos distintos para las dos salidas.
# -PassThru devuelve el proceso, asi que su .Id ES el PID real de java.
function Start-Servicio([string]$modulo) {
    $jar = Get-RutaJar $modulo
    $log = Get-RutaLog $modulo
    $err = $log -replace '\.log$', '.err.log'

    # stdout.encoding=UTF-8: sin esto, al redirigir, Java escribe las tildes en otra codificacion.
    # La ruta del jar va entre comillas por si la carpeta del proyecto tuviera espacios.
    $proceso = Start-Process -FilePath $java `
                             -ArgumentList "-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar `"$jar`"" `
                             -WorkingDirectory $raiz `
                             -RedirectStandardOutput $log `
                             -RedirectStandardError $err `
                             -WindowStyle Hidden `
                             -PassThru
    $pidJava = $proceso.Id

    # Se registra en cuanto se lanza: si algo falla despues, detener.ps1 igual lo encuentra.
    Add-Content -Path $archivoPids -Value "$modulo;$pidJava" -Encoding ASCII
    Write-Host ("  lanzado {0,-22} java PID {1,-7} log {2}" -f $modulo, $pidJava, $log)
    return $pidJava
}

# Sondea un puerto cada segundo hasta que acepte conexiones o se agote el tiempo.
# Ademas vigila que el proceso siga vivo: si java murio, no tiene sentido seguir esperando.
function Esperar-Puerto([string]$modulo, [int]$puerto, [int]$pidJava) {
    $limite = (Get-Date).AddSeconds($segundosMaximo)
    while ((Get-Date) -lt $limite) {
        if (-not (Get-Process -Id $pidJava -ErrorAction SilentlyContinue)) {
            Show-FinalLog $modulo
            Stop-ConError "$modulo termino antes de abrir el puerto $puerto."
        }
        if (Test-NetConnection -ComputerName localhost -Port $puerto -InformationLevel Quiet `
                               -WarningAction SilentlyContinue) {
            return
        }
        Start-Sleep -Seconds 1
    }
    Show-FinalLog $modulo
    Stop-ConError "$modulo no abrio el puerto $puerto en $segundosMaximo segundos."
}

# Espera a que el log contenga la linea de Spring "Started <Clase> in X seconds".
function Esperar-Log([string]$modulo, [int]$pidJava) {
    $log    = Get-RutaLog $modulo
    $limite = (Get-Date).AddSeconds($segundosMaximo)
    while ((Get-Date) -lt $limite) {
        if ((Test-Path $log) -and (Select-String -Path $log -Pattern 'Started \S+ in [\d.,]+ seconds' -Quiet)) {
            return
        }
        if (-not (Get-Process -Id $pidJava -ErrorAction SilentlyContinue)) {
            Show-FinalLog $modulo
            Stop-ConError "$modulo termino antes de completar el arranque."
        }
        Start-Sleep -Seconds 1
    }
    Show-FinalLog $modulo
    Stop-ConError "$modulo no escribio 'Started ...' en $segundosMaximo segundos."
}

Write-Host "== 3/4 Servicios" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $carpetaLogs | Out-Null
[System.IO.File]::WriteAllText($archivoPids, '')     # crea o vacia pids.txt

for ($t = 0; $t -lt $tandas.Count; $t++) {
    Write-Host "Tanda $($t + 1): $($tandas[$t] -join ', ')"

    # Se lanzan todos los de la tanda y se guarda el PID de java de cada uno.
    $lanzados = @{}
    foreach ($modulo in $tandas[$t]) { $lanzados[$modulo] = Start-Servicio $modulo }

    # Se espera a cada uno: primero el log "Started", y si tiene puerto, tambien el puerto.
    foreach ($modulo in $tandas[$t]) {
        $cronometro = [System.Diagnostics.Stopwatch]::StartNew()
        Esperar-Log $modulo $lanzados[$modulo]
        if ($puertos.ContainsKey($modulo)) { Esperar-Puerto $modulo $puertos[$modulo] $lanzados[$modulo] }
        Write-Host ("  listo    {0,-22} ({1:N0} s)" -f $modulo, $cronometro.Elapsed.TotalSeconds) -ForegroundColor Green
    }
}

# ---------------------------------------------------------------------------------------------
# 6. Resumen
# ---------------------------------------------------------------------------------------------

Write-Host ""
Write-Host "== 4/4 Los siete servicios estan listos" -ForegroundColor Green
Write-Host "PIDs en $archivoPids  |  logs en $carpetaLogs"
Write-Host ""
Write-Host "  Dashboard ............ http://localhost:8081"
Write-Host "  API de ordenes ....... http://localhost:8080/order   (POST; prueba con ops\enviar-ordenes.ps1)"
Write-Host "  RabbitMQ (consola) ... http://localhost:15672        usuario ecommerce / clave: RABBITMQ_PASSWORD de .env"
Write-Host ""
Write-Host "Para detener: ops\detener.ps1   (con -ConInfra detiene tambien postgres-db, kafka y rabbitmq)"
