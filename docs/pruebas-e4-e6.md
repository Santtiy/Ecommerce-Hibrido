# Pruebas manuales — Etapas 4, 5 y 6

## 0. Infraestructura

```powershell
cd C:\Projects\Ecommerce-Hibrido
docker compose up -d --wait     # levanta postgres-db, kafka y rabbitmq y espera a que estén sanos
docker compose ps               # los tres deben decir (healthy)
```

En IntelliJ: *Maven → Reload* (hay 5 módulos nuevos) y **JDK 21** en las configuraciones de ejecución.

## 1. Orden de arranque de los siete servicios

| # | Clase a ejecutar (IntelliJ) | Por qué en este orden |
|---|---|---|
| 1 | `OrdenApiApplication` (order-api) | Crea el topic `new_orders` con 3 particiones. |
| 2 | `AnalyticsApplication` (analytics-service) | Consumidor de eventos, independiente del resto. |
| 3 | `BillingWorkerApplication` (billing-worker) | Declaran `order_tasks`, el DLX, su cola y `failed_tasks`, y crean su topic de resultados. |
| 4 | `InventoryWorkerApplication` (inventory-worker) | ↑ |
| 5 | `NotificationWorkerApplication` (notification-worker) | ↑ |
| 6 | `DashboardApplication` (dashboard-aggregator) | Abre `http://localhost:8081` antes de enviar órdenes, para verlas llegar en vivo. |
| 7 | `OrderProcessorApplication` (order-processor) | **Último.** Un exchange fanout sin colas enlazadas **descarta los mensajes en silencio**: si order-processor publicara antes de que los workers declaren sus colas, esos comandos se perderían. |

> Al arrancar, order-processor y analytics-service leen `new_orders` desde el principio
> (`auto-offset-reset: earliest`) si su grupo aún no tiene posición guardada. Las órdenes que ya
> enviaste en la etapa 3 aparecerán también en el dashboard.

## 2. Interfaces web

| Qué | URL | Credenciales |
|---|---|---|
| Administración de RabbitMQ | http://localhost:15672 | usuario `ecommerce`, contraseña `ecommerce` |
| Dashboard de órdenes | http://localhost:8081 | — |

En RabbitMQ, comprueba:
- **Exchanges:** `order_tasks` y `order_tasks_dlx`, los dos de tipo `fanout`.
- **Queues:** `billing_queue`, `inventory_queue` y `notification_queue` muestran la etiqueta `DLX` en *Features* y tienen **2 consumidores** cada una (`concurrency: 2`). `failed_tasks` está vacía.

## 3. Órdenes de prueba (PowerShell)

```powershell
function Enviar-Orden($precio, $cantidad) {
  $cuerpo = @{ cliente = "Ana Perez"; items = @(@{ sku = "SKU-1"; nombre = "Monitor"; cantidad = $cantidad; precioUnitario = $precio }) } | ConvertTo-Json -Depth 5
  Invoke-RestMethod -Method Post -Uri http://localhost:8080/order -ContentType "application/json" -Body $cuerpo
}

Enviar-Orden 150000 1      # total 160.650  -> PAYMENT_SUCCESS, STOCK_RESERVED, EMAIL_SENT (todo verde)
Enviar-Orden 1000000 1     # total 1.071.000 -> PAYMENT_FAILED (rojo), STOCK_RESERVED, EMAIL_SENT
Enviar-Orden 1000 11       # 11 unidades     -> PAYMENT_SUCCESS, STOCK_OUT_OF_STOCK (rojo), EMAIL_SENT
```

Observa que la segunda orden recibe `EMAIL_SENT` aunque el pago falló: el fanout entrega el
comando a las tres colas a la vez y el correo no depende del pago. Es la limitación aceptada de RN-09.

## 4. Qué observar en cada log

| Servicio | Línea esperada por cada orden |
|---|---|
| order-api | `OrdenCreada <id> publicada en new_orders (partición N, offset M)` |
| analytics-service | `Orden <id> \| órdenes acumuladas: N \| total acumulado: X COP`, seguida de `[SO] ResultadoES[operacion=escritura, bytes=…, milisegundos=…] \| archivo=… bytes \| memoria usada=… MB, libre=…, total=…, máxima=…, procesadores=…` |
| order-processor | `Orden <id> recibida de Kafka y despachada a RabbitMQ (order_tasks)` |
| billing-worker | `[billing] orden <id> -> PAYMENT_SUCCESS en 3xx ms (hilo …-1)` |
| inventory-worker | `[inventory] orden <id> -> STOCK_RESERVED en 2xx ms (hilo …)` |
| notification-worker | `[notification] orden <id> -> EMAIL_SENT en 4xx ms (hilo …)` |
| dashboard-aggregator | Tres líneas `[dashboard] pago/stock/correo de <id> = … -> EstadoDashboard[…]` y, al abrir la página, `Navegador conectado (1 abiertos)` |

Para ver la concurrencia, envía varias órdenes seguidas (por ejemplo,
`1..6 | % { Enviar-Orden 150000 1 }`). En cada worker se alternan los dos hilos del listener, y
los tiempos de 200 a 500 ms se solapan.

**Flujo completo correcto:** cada orden produce exactamente una línea en cada uno de los siete
logs, y en el dashboard su fila pasa de gris (`pending`) a su color final en las tres columnas.

## 5. Etapa 6: el archivo de órdenes

Al arrancar, analytics-service registra la ruta absoluta en el log: `Registro de órdenes en …`.
Por defecto es `datos/ordenes.log`, relativa al directorio de trabajo, que en IntelliJ suele ser
la carpeta `analytics-service`.

```powershell
Get-Content .\analytics-service\datos\ordenes.log      # una línea "orderId;monto;instante" por orden
```

## 6. (Opcional) Demostrar la cola de fallos

Un resultado de negocio negativo (`PAYMENT_FAILED`) **no** es un fallo técnico: se publica
normalmente. Para ver un mensaje llegar a `failed_tasks`, envía un mensaje que el worker no pueda
procesar:

1. En RabbitMQ → *Queues* → `billing_queue` → *Publish message*, escribe en *Payload* `esto no es JSON` y pulsa *Publish message*.
2. En el log de billing-worker aparecen **3 intentos** separados por 1 s, y después el rechazo.
3. `failed_tasks` pasa a tener **1 mensaje**. Con *Get messages* puedes ver su contenido y la cabecera `x-death`, que indica de qué cola vino.

## 7. Apagar

```powershell
docker compose down        # detiene los contenedores y conserva los datos
```
