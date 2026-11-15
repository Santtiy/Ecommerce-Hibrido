# Pruebas manuales — Etapas 2 y 3

Requisitos: `docker compose up -d --wait` ejecutado en la raíz, y order-api y analytics-service
arrancados. Los mismos casos están en `order-api/src/test/http/order.http` para IntelliJ.

## 1. Peticiones desde PowerShell

`Invoke-RestMethod` devuelve el cuerpo de la respuesta. En Windows PowerShell 5.1, cuando la
respuesta es un error (4xx/5xx) lanza una excepción, así que los casos de error van dentro de
`try/catch` para mostrar el código y el cuerpo.

**Orden válida de 150.000 COP → 202** (total esperado 160.650,00: −10 % y luego +19 % de IVA)

```powershell
$orden = @{
  cliente = "Ana Perez"
  items   = @(@{ sku = "SKU-1"; nombre = "Monitor"; cantidad = 1; precioUnitario = 150000 })
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method Post -Uri http://localhost:8080/order -ContentType "application/json" -Body $orden
```

Para ver el código 202 de forma explícita:

```powershell
(Invoke-WebRequest -UseBasicParsing -Method Post -Uri http://localhost:8080/order -ContentType "application/json" -Body $orden).StatusCode
```

**Orden sin ítems → 400**

```powershell
$sinItems = '{ "cliente": "Ana Perez", "items": [] }'
try {
  Invoke-RestMethod -Method Post -Uri http://localhost:8080/order -ContentType "application/json" -Body $sinItems
} catch {
  $_.Exception.Response.StatusCode.value__   # 400
  $_.ErrorDetails.Message                    # cuerpo RespuestaError con los campos inválidos
}
```

**Orden con cantidad 0 → 400**

```powershell
$cantidadCero = '{ "cliente": "Ana Perez", "items": [ { "sku": "SKU-1", "nombre": "Monitor", "cantidad": 0, "precioUnitario": 150000 } ] }'
try {
  Invoke-RestMethod -Method Post -Uri http://localhost:8080/order -ContentType "application/json" -Body $cantidadCero
} catch {
  $_.Exception.Response.StatusCode.value__   # 400
  $_.ErrorDetails.Message
}
```

## 2. Ver las filas en PostgreSQL

```powershell
docker exec -it postgres-db psql -U ecommerce -d ecommerce -c "SELECT * FROM ordenes;"
docker exec -it postgres-db psql -U ecommerce -d ecommerce -c "SELECT * FROM items_orden;"
```

## 3. Comprobar las particiones del topic

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic new_orders
```

Debe mostrar `PartitionCount: 3` y tres líneas `Partition: 0`, `1` y `2`.

## 4. Ver los eventos crudos

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic new_orders --from-beginning
```

Cada línea es un `OrdenCreada` en JSON con `ocurridoEn` en formato ISO-8601. Se sale con `Ctrl+C`.
Este consumidor de consola usa un grupo propio y temporal, así que no le quita mensajes a
analytics-service.

## Limitación conocida: la doble escritura

`OrdenService.registrar` primero **guarda** la orden en PostgreSQL y después **publica** el evento
en Kafka. Son dos sistemas distintos y no comparten transacción. Si la publicación fallara justo
después de confirmarse el guardado (Kafka caído, red cortada, la aplicación se detiene), la orden
quedaría en la base de datos pero el evento se perdería: analytics-service, y en etapas
posteriores el procesador, nunca se enterarían de ella.

Es el problema clásico de la **doble escritura**. Su solución estándar es el **patrón Outbox**:
el evento se guarda en una tabla `outbox` dentro de la *misma* transacción que la orden, y un
proceso aparte lee esa tabla y lo publica en Kafka, reintentando hasta lograrlo. Así, o se
guardan la orden y su evento, o no se guarda ninguno.

Está fuera del alcance de este proyecto y no se implementa: se deja documentado como material
de sustentación.
