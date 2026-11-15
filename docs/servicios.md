# Inventario de microservicios

Valores tomados del código (`application.yml`, `@KafkaListener`, `@RabbitListener`, `ConfiguracionKafka`, `ConfiguracionRabbit`).
Infraestructura (docker-compose): Kafka en `localhost:29092` desde el host (`kafka:9092` dentro de la red Docker), RabbitMQ en `5672` (consola web en `15672`), PostgreSQL en `localhost:5433` desde el host (`postgres-db:5432` dentro de la red Docker; el 5432 del host lo ocupa un PostgreSQL nativo de Windows).

| Módulo | Clase principal | Puerto | group-id de Kafka | Topics que consume | Topics que publica | Cola de RabbitMQ | Jar generado |
|---|---|---|---|---|---|---|---|
| order-api | `co.edu.proyecto.orden.OrdenApiApplication` | 8080 (`POST /order`) | — (solo produce) | — | `new_orders` (3 particiones, clave = orderId) | — | `order-api/target/order-api-1.0.0.jar` |
| analytics-service | `co.edu.proyecto.analytics.AnalyticsApplication` | — (sin servidor web) | `analytics-group` | `new_orders` | — | — | `analytics-service/target/analytics-service-1.0.0.jar` |
| order-processor | `co.edu.proyecto.processor.OrderProcessorApplication` | — (sin servidor web) | `processor-group` | `new_orders` | — | Publica en el exchange fanout `order_tasks` | `order-processor/target/order-processor-1.0.0.jar` |
| billing-worker | `co.edu.proyecto.billing.BillingWorkerApplication` | — (sin servidor web) | — (solo produce) | — | `billing_events` | Consume `billing_queue` | `billing-worker/target/billing-worker-1.0.0.jar` |
| inventory-worker | `co.edu.proyecto.inventory.InventoryWorkerApplication` | — (sin servidor web) | — (solo produce) | — | `inventory_events` | Consume `inventory_queue` | `inventory-worker/target/inventory-worker-1.0.0.jar` |
| notification-worker | `co.edu.proyecto.notification.NotificationWorkerApplication` | — (sin servidor web) | — (solo produce) | — | `notification_events` | Consume `notification_queue` | `notification-worker/target/notification-worker-1.0.0.jar` |
| dashboard-aggregator | `co.edu.proyecto.dashboard.DashboardApplication` | 8081 (`SERVER_PORT`); WebSocket `/ws/ordenes` | `dashboard-group` | `billing_events`, `inventory_events`, `notification_events` | — | — | `dashboard-aggregator/target/dashboard-aggregator-1.0.0.jar` |

## Notas

- **Los group-id son distintos** (`analytics-group`, `processor-group`, `dashboard-group`). Por eso cada uno de esos servicios recibe su propia copia de cada evento. Si dos coincidieran, Kafka repartiría los eventos entre ellos y cada uno vería solo una parte. El group-id aparece tanto en `application.yml` como en el atributo `groupId` del `@KafkaListener`; manda el de la anotación, y en los tres servicios ambos valores coinciden.
- **Topología de RabbitMQ:** `order_tasks` es un exchange *fanout*, así que cada comando `ProcesarOrden` se copia a las tres colas. Cada cola tiene como dead-letter exchange a `order_tasks_dlx` (también fanout), enlazado a la cola `failed_tasks`. Un mensaje llega ahí cuando fallan sus 3 intentos. Cada worker declara su propia cola, el exchange, el DLX y `failed_tasks`. Declararlos de nuevo con los mismos parámetros no cambia nada, así que el orden de arranque no importa.
- **Workers:** `prefetch: 1` y `concurrency: 2` hilos por instancia.
- **Consumidores de `new_orders`:** 3 hilos, uno por partición.
- **Variables de entorno con valor por defecto:** `KAFKA_BOOTSTRAP` (localhost:29092), `RABBITMQ_HOST/PORT/USER/PASSWORD` (localhost/5672/ecommerce/ecommerce), `DB_HOST/PORT/NAME/USER/PASSWORD` (order-api), `SERVER_PORT` (dashboard), `ARCHIVO_ORDENES` (analytics-service, `./datos/ordenes.log`).
- **Perfil `sin-kafka` (order-api):** sustituye el publicador de Kafka por uno que solo escribe en el log.

## Orden de arranque sugerido

1. `docker compose up -d --wait`: postgres-db, kafka, rabbitmq.
2. Workers: billing, inventory, notification. Declaran las colas y crean sus topics de resultados.
3. dashboard-aggregator, analytics-service, order-processor.
4. order-api. Crea `new_orders` si no existe.
