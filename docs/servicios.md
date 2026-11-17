# Inventario de microservicios

Valores tomados del código (`application.yml`, `@KafkaListener`, `@RabbitListener`, `ConfiguracionKafka`, `ConfiguracionRabbit`).
Infraestructura (`docker-compose.yml`, configuración AWS actual): Kafka (`kafka:9092`), RabbitMQ (`rabbitmq:5672`) y PostgreSQL (`postgres-db:5432`) solo son accesibles dentro de la red Docker `ecommerce-net`; al host se publican únicamente 8080, 8081 y la consola de RabbitMQ en `127.0.0.1:15672`. Ver `docs/despliegue-aws.md`.
En la etapa 7 (commit `a0b8dbb`) también se publicaban Kafka en `localhost:29092`, AMQP en `5672` y PostgreSQL en `localhost:5433` (el 5432 del host lo ocupa un PostgreSQL nativo de Windows); los valores por defecto de los `application.yml` siguen apuntando a esas direcciones del host, que en desarrollo local publica `docker-compose.dev.yml` (solo en 127.0.0.1).

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
- **Variables de entorno con valor por defecto:** `KAFKA_BOOTSTRAP` (localhost:29092), `RABBITMQ_HOST/PORT/USER/PASSWORD` (localhost/5672/usuario `ecommerce`; en Docker se usan `SPRING_RABBITMQ_*`, que Spring enlaza por *relaxed binding* y tienen prioridad sobre el `application.yml`), `DB_URL` (order-api; si no se define se arma con `DB_HOST/PORT/NAME`), `DB_USER/PASSWORD`, `CORS_ORIGINS` (order-api, `*`), `SERVER_PORT` (dashboard), `ARCHIVO_ORDENES` (analytics-service, `./datos/ordenes.log`; en Docker `/app/datos/ordenes.log`). En Docker las contraseñas llegan desde `.env` (`POSTGRES_PASSWORD`, `RABBITMQ_PASSWORD`; plantilla en `.env.example`); los `application.yml` no tienen contraseña por defecto: en desarrollo local se pasan `DB_PASSWORD` y `RABBITMQ_PASSWORD` (`ops\arrancar.ps1` las toma del `.env`).
- **Perfil `sin-kafka` (order-api):** sustituye el publicador de Kafka por uno que solo escribe en el log.

## Orden de arranque

Kafka tiene `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`: cada topic lo crea solo su productor, así que nadie puede crear `new_orders` con una sola partición por accidente. Con un `docker compose up` directo, el orden lo imponen los `depends_on` de `docker-compose.yml` (en AWS se usa `ops/aws/arrancar-aws.sh`, que arranca por tandas y deja order-processor al final; ver `docs/despliegue-aws.md` §10):

1. postgres-db, kafka y rabbitmq (healthcheck: `pg_isready`, `kafka-topics.sh --list`, `rabbitmq-diagnostics -q ping`).
2. Workers (billing, inventory, notification) y dashboard-aggregator. Los workers declaran sus colas y crean sus topics de resultados. El dashboard refresca metadatos cada 10 s (`metadata.max.age.ms`), por lo que detecta esos topics aunque arranque antes que los workers.
3. order-api, cuando PostgreSQL y Kafka están sanos. Crea `new_orders` con 3 particiones y queda sano cuando su puerto 8080 responde.
4. order-processor y analytics-service, solo cuando order-api está sano, con `new_orders` ya creado. Leen desde `earliest`, así que una orden enviada mientras todavía arrancan no se pierde.

Desde jar en Windows, `ops\arrancar.ps1` sigue el mismo orden en tres tandas.

## Etapa 7: contenedores

Un único `Dockerfile` en la raíz, parametrizado con `--build-arg MODULO=<módulo>`:

- **Etapa builder:** `maven:3.9-eclipse-temurin-21`. No declara `MODULO`, así que es idéntica para los siete servicios y se construye una sola vez.
- **Etapa runtime:** `eclipse-temurin:21-jre-alpine`, con el usuario sin privilegios `ecommerce` (uid 100) y solo el jar del módulo.

La primera construcción completa (`docker compose build`) tardó 59 s.

Tamaños medidos con `docker images` en Docker Desktop. *Disco* es lo que ocupa la imagen descomprimida (incluye la base `eclipse-temurin:21-jre-alpine`, que ya ocupa 287 MB). *Contenido* es el tamaño comprimido que se descarga.

| Servicio | Imagen | Disco | Contenido | Jar | Límite de memoria | `-Xmx` | CPU | Puertos al host |
|---|---|---|---|---|---|---|---|---|
| kafka | `apache/kafka:3.7.0` | 612 MB | 210 MB | — | 1200M | 768m (`KAFKA_HEAP_OPTS`) | 1.0 | 9092, 29092 |
| rabbitmq | `rabbitmq:3.13-management-alpine` | 277 MB | 88 MB | — | 512M | — | 0.5 | 5672, 15672 |
| postgres-db | `postgres:16-alpine` | 420 MB | 117 MB | — | 384M | — | 0.5 | 5433→5432 |
| order-api | `ecommerce-hibrido-order-api` | 423 MB | 140 MB | 69 MB | 384M | 224m | 0.5 | 8080 |
| order-processor | `ecommerce-hibrido-order-processor` | 356 MB | 108 MB | 34 MB | 320M | 192m | 0.25 | ninguno |
| billing-worker | `ecommerce-hibrido-billing-worker` | 356 MB | 108 MB | 34 MB | 288M | 160m | 0.25 | ninguno |
| inventory-worker | `ecommerce-hibrido-inventory-worker` | 356 MB | 108 MB | 34 MB | 288M | 160m | 0.25 | ninguno |
| notification-worker | `ecommerce-hibrido-notification-worker` | 356 MB | 108 MB | 34 MB | 288M | 160m | 0.25 | ninguno |
| analytics-service | `ecommerce-hibrido-analytics-service` | 353 MB | 106 MB | 33 MB | 320M | 192m | 0.5 | ninguno |
| dashboard-aggregator | `ecommerce-hibrido-dashboard-aggregator` | 364 MB | 111 MB | 38 MB | 384M | 224m | 0.5 | 8081 |
| **Total** | | | | | **4368M** | | **4.5** | |

- **Sin `-Xmx` el contenedor muere con exit code 137.** La JVM reclamaría como heap hasta la cuarta parte de la memoria que ve, superaría el límite y Docker la mataría por exceso de memoria (OOM kill).
- **Consumo real en reposo** (`docker stats`, evidencias/e7/d-stats-limites.txt): los microservicios usan entre 114 y 215 MiB, el 35-56 % de su límite. Kafka usa unos 300 MiB de 1.17 GiB.
- **Volumen `analytics-datos`** montado en `/app/datos` de analytics-service. El registro del módulo de Sistemas Operativos sobrevive a `docker compose down`. Se inspecciona con `docker exec analytics-service tail /app/datos/ordenes.log`.
- **Firewall:** order-processor, los workers y analytics-service no publican ningún puerto. Solo son alcanzables dentro de la red `ecommerce-net`.
