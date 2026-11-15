<#
.SYNOPSIS
    Envia cinco ordenes de prueba a order-api y compara lo esperado con lo obtenido.

.DESCRIPTION
    Requiere que los servicios esten arrancados (ops\arrancar.ps1).

        powershell -ExecutionPolicy Bypass -File ops\enviar-ordenes.ps1

    Que se puede comprobar aqui y que no:
      - El codigo HTTP (202 o 400) y el total calculado SI: los devuelve order-api en la respuesta.
      - PAYMENT_* y STOCK_* NO: los producen los workers despues, de forma asincrona, y no viajan en
        la respuesta HTTP. El script los muestra como "esperado" y se comprueban en el dashboard
        (http://localhost:8081) o en las ventanas de los workers, buscando el orderId.

    Por que Invoke-WebRequest y no Invoke-RestMethod: en PowerShell 5.1, Invoke-RestMethod no expone
    el codigo HTTP de una respuesta correcta (no distingue 200 de 202). Invoke-WebRequest si
    (.StatusCode), y su cuerpo se convierte con ConvertFrom-Json igual que lo haria Invoke-RestMethod.
#>

$url = 'http://localhost:8080/order'
$segundosEntrePeticiones = 2

# ---------------------------------------------------------------------------------------------
# Las cinco ordenes. Cada una lleva el JSON que se envia y lo que se espera recibir.
# Total esperado = subtotal - 10 % (descuento si el subtotal supera 100.000) + 19 % de IVA.
#   150.000 -> 135.000 -> 160.650 | 1.500.000 -> 1.350.000 -> 1.606.500 | 300.000 -> 270.000 -> 321.300
# ---------------------------------------------------------------------------------------------
$casos = @(
    @{
        Nombre      = '1. Libro x1 a 150.000'
        Json        = '{"cliente":"Cliente Demo","items":[{"sku":"LIB-001","nombre":"Libro","cantidad":1,"precioUnitario":150000}]}'
        Codigo      = 202
        Total       = 160650
        Asincrono   = 'PAYMENT_SUCCESS, STOCK_RESERVED, EMAIL_SENT'
    },
    @{
        Nombre      = '2. Televisor x1 a 1.500.000'
        Json        = '{"cliente":"Cliente Demo","items":[{"sku":"TV-001","nombre":"Televisor","cantidad":1,"precioUnitario":1500000}]}'
        Codigo      = 202
        Total       = 1606500
        Asincrono   = 'PAYMENT_FAILED (supera 1.000.000, RN-11), STOCK_RESERVED, EMAIL_SENT'
    },
    @{
        Nombre      = '3. Memoria USB x15 a 20.000'
        Json        = '{"cliente":"Cliente Demo","items":[{"sku":"USB-001","nombre":"Memoria USB","cantidad":15,"precioUnitario":20000}]}'
        Codigo      = 202
        Total       = 321300
        Asincrono   = 'PAYMENT_SUCCESS, STOCK_OUT_OF_STOCK (mas de 10 unidades, RN-12), EMAIL_SENT'
    },
    @{
        Nombre      = '4. Lista de items vacia'
        Json        = '{"cliente":"Cliente Demo","items":[]}'
        Codigo      = 400
        Total       = $null
        Asincrono   = $null
    },
    @{
        Nombre      = '5. Cantidad 0'
        Json        = '{"cliente":"Cliente Demo","items":[{"sku":"LIB-001","nombre":"Libro","cantidad":0,"precioUnitario":150000}]}'
        Codigo      = 400
        Total       = $null
        Asincrono   = $null
    }
)

# ---------------------------------------------------------------------------------------------
# Envia un JSON por POST y devuelve un objeto con el codigo HTTP y el cuerpo de la respuesta.
# Si el servidor responde 4xx/5xx, PowerShell lanza una excepcion: el try/catch la captura y
# extrae igualmente el codigo y el cuerpo del error.
# ---------------------------------------------------------------------------------------------
function Send-Orden([string]$json) {
    # Se envian los bytes en UTF-8 de forma explicita: PowerShell 5.1 podria usar otra codificacion.
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    try {
        $respuesta = Invoke-WebRequest -Uri $url -Method Post -Body $bytes `
                                       -ContentType 'application/json; charset=utf-8' `
                                       -UseBasicParsing -ErrorAction Stop
        return @{ Codigo = [int]$respuesta.StatusCode; Cuerpo = $respuesta.Content }
    } catch {
        $http = $_.Exception.Response
        if (-not $http) {
            # No hubo respuesta: order-api no esta arrancado o el puerto es otro.
            return @{ Codigo = 0; Cuerpo = "Sin respuesta del servidor: $($_.Exception.Message)" }
        }
        # PowerShell 5.1 suele dejar el cuerpo del error en ErrorDetails.Message; si no, se lee del stream.
        $cuerpo = $_.ErrorDetails.Message
        if (-not $cuerpo) {
            $lector = New-Object System.IO.StreamReader($http.GetResponseStream())
            $cuerpo = $lector.ReadToEnd()
            $lector.Close()
        }
        return @{ Codigo = [int]$http.StatusCode; Cuerpo = $cuerpo }
    }
}

# ---------------------------------------------------------------------------------------------
# Bucle principal: envia cada caso, compara y muestra el resultado.
# ---------------------------------------------------------------------------------------------
$fallos = 0

for ($i = 0; $i -lt $casos.Count; $i++) {
    $caso = $casos[$i]
    Write-Host ""
    Write-Host "== $($caso.Nombre)" -ForegroundColor Cyan

    $r = Send-Orden $caso.Json

    # Comparacion del codigo HTTP.
    $okCodigo = ($r.Codigo -eq $caso.Codigo)
    Write-Host ("  HTTP   esperado {0} | obtenido {1}" -f $caso.Codigo, $r.Codigo) `
               -ForegroundColor $(if ($okCodigo) { 'Green' } else { 'Red' })
    if (-not $okCodigo) { $fallos++ }

    if ($caso.Codigo -eq 202 -and $okCodigo) {
        # Respuesta correcta: {"orderId": "...", "estado": "...", "total": ..., "moneda": "COP"}
        $orden = $r.Cuerpo | ConvertFrom-Json
        $okTotal = ([decimal]$orden.total -eq [decimal]$caso.Total)
        Write-Host ("  Total  esperado {0} | obtenido {1} {2}" -f $caso.Total, $orden.total, $orden.moneda) `
                   -ForegroundColor $(if ($okTotal) { 'Green' } else { 'Red' })
        if (-not $okTotal) { $fallos++ }
        Write-Host "  orderId: $($orden.orderId)   estado: $($orden.estado)"
        Write-Host "  Asincrono esperado (verlo en el dashboard): $($caso.Asincrono)" -ForegroundColor DarkGray
    } else {
        # Error esperado (400) o algo inesperado: se muestra el cuerpo tal cual llego.
        Write-Host "  Cuerpo: $($r.Cuerpo)"
    }

    # Pausa entre peticiones para que los logs de cada orden queden separados.
    if ($i -lt $casos.Count - 1) { Start-Sleep -Seconds $segundosEntrePeticiones }
}

# ---------------------------------------------------------------------------------------------
# Resumen final. El codigo de salida es 1 si algo no coincidio (util si se automatiza).
# ---------------------------------------------------------------------------------------------
Write-Host ""
if ($fallos -eq 0) {
    Write-Host "Todo lo comprobable desde HTTP coincide. Revisa ahora el dashboard: http://localhost:8081" -ForegroundColor Green
    exit 0
} else {
    Write-Host "$fallos comprobacion(es) no coincidieron. Revisa las ventanas de los servicios." -ForegroundColor Red
    exit 1
}
