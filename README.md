# Ecommerce-Hibrido

Plataforma de órdenes de comercio electrónico con **arquitectura de microservicios orientada a
eventos e híbrida**:
- **Kafka** transporta los eventos (lo que ya ocurrió).
- **RabbitMQ** reparte los comandos de trabajo (lo que hay que hacer) entre workers.
- **PostgreSQL** persiste las órdenes.
- Un **dashboard** web muestra en tiempo real el estado de cada orden.

Proyecto académico (proyecto integrador). Está desplegado y verificado en **AWS EC2 t3.small**
(2 GiB de RAM, Ubuntu Server 24.04).

## Arquitectura

```
                    HTTP :8080                       HTTP/WS :8081
 navegador ──► order-api ──(new_orders, 3 part.)──► Kafka ◄── dashboard-aggregator ◄── navegador
                  │                                  │  ▲        (billing/inventory/notification_events)
              PostgreSQL                             │  │
                                   ┌─────────────────┤  └──── billing / inventory / notification-worker
                                   ▼                 ▼                     ▲
                          analytics-service    order-processor             │
                                                     │                     │
                                                     ▼                     │
                                      RabbitMQ: exchange fanout order_tasks ──► billing_queue
                                                                            ├─► inventory_queue
                                                                            └─► notification_queue
                                                     (DLX order_tasks_dlx ──► failed_tasks)
```

1. `POST /order` valida la orden, calcula descuento e IVA, la guarda en PostgreSQL y publica
   `OrdenCreada` en el topic `new_orders`. Responde **202 Accepted**: la orden se aceptó, pero el
   procesamiento sigue de forma asíncrona.
2. `order-processor` traduce cada evento en un comando `ProcesarOrden` y lo publica en el exchange
   fanout `order_tasks`. El exchange lo copia a las tres colas.
3. Los tres workers procesan su parte (pago, stock, correo) y publican su resultado en Kafka. Usan
   2 hilos y prefetch 1; tras 3 intentos fallidos, el mensaje va a `failed_tasks`.
4. `dashboard-aggregator` combina los tres resultados y los envía por WebSocket (`/ws/ordenes`).
5. `analytics-service` acumula las ventas y registra cada orden en un archivo (módulo de Sistemas
   Operativos).

## Servicios (los diez contenedores de `docker-compose.yml`)

| Servicio | Tipo | Función | Puerto en el host |
|---|---|---|---|
| `postgres-db` | PostgreSQL 16 Alpine | Persistencia de órdenes | — (solo red interna) |
| `kafka` | Apache Kafka 3.7.0 (KRaft) | Eventos: `new_orders` y resultados | — (solo red interna) |
| `rabbitmq` | RabbitMQ 3.13 management | Comandos: `order_tasks`, colas y DLX | `127.0.0.1:15672` (consola) |
| `order-api` | Spring Boot | `POST /order` | `8080` |
| `order-processor` | Spring Boot | Puente Kafka → RabbitMQ | — |
| `billing-worker` | Spring Boot | Pago (`billing_events`) | — |
| `inventory-worker` | Spring Boot | Stock (`inventory_events`) | — |
| `notification-worker` | Spring Boot | Correo (`notification_events`) | — |
| `dashboard-aggregator` | Spring Boot | Página web y WebSocket | `8081` |
| `analytics-service` | Spring Boot | Acumulado de ventas y registro de órdenes | — |

Detalle de topics, grupos, colas y jar: [`docs/servicios.md`](docs/servicios.md).

## Tecnologías

Java 21 · Spring Boot 3.3.5 (Web, Validation, Data JPA, Kafka, AMQP, WebSocket) · Maven multimódulo
(`common` y siete servicios) · Apache Kafka 3.7 · RabbitMQ 3.13 · PostgreSQL 16 · Docker y Docker
Compose v2 · JUnit 5 y Mockito.

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 21 (probado con Eclipse Temurin 21) |
| Maven | 3.9.x (no hay `mvnw`) |
| Docker | Docker Desktop (Windows) o Docker Engine (Ubuntu), con **Compose v2** (`docker compose`) |

## Credenciales (`.env`)

Las contraseñas **no están en el código**. `docker-compose.yml` exige dos variables y no arranca
sin ellas:

```powershell
Copy-Item .env.example .env      # Windows   (Ubuntu: cp .env.example .env && chmod 600 .env)
```

Rellena `POSTGRES_PASSWORD` y `RABBITMQ_PASSWORD` en `.env`:
- Usa solo letras, números, `-` y `_`.
- Guarda el archivo con finales de línea LF.
- `.env` está ignorado por Git y por Docker. **No lo subas nunca.**
- Si el volumen de PostgreSQL ya existe, su contraseña es la que tenía al crearse. Cambiar `.env`
  no la cambia (ver [`docs/despliegue-aws.md`](docs/despliegue-aws.md) §7.1).

No ejecutes `docker compose config` sin `--quiet`: imprime las contraseñas resueltas.

## Compilación y pruebas

```powershell
mvn -B verify                      # compila los 8 módulos y ejecuta las pruebas unitarias
mvn -B clean package -DskipTests   # solo los jar (<modulo>/target/<modulo>-1.0.0.jar)
```

Las pruebas son unitarias (dominio, servicios y mapeos) y no necesitan Docker.

## Modos de ejecución

Hay dos modos. Ambos usan los mismos diez servicios, las mismas imágenes de infraestructura y las
mismas credenciales de `.env`.

| | Despliegue (AWS / Docker completo) | Desarrollo local (Windows) |
|---|---|---|
| Archivos Compose | `docker-compose.yml` | `docker-compose.yml` + `docker-compose.dev.yml` |
| Dónde corren los 7 microservicios | Contenedores | Jar desde IntelliJ u `ops\arrancar.ps1` |
| Puertos de infraestructura | No se publican | `127.0.0.1:5433` (PostgreSQL), `127.0.0.1:29092` (Kafka), `127.0.0.1:5672` (AMQP) |
| Puertos de aplicación | `8080`, `8081`; consola `127.0.0.1:15672` | `8080`, `8081` (los jar); consola `127.0.0.1:15672` |
| Límites de CPU y RAM | Ajustados a 2 GiB | Los mismos, en la infraestructura |

`docker-compose.dev.yml` **solo se aplica si se pasa con `-f`**. Por eso no se llama
`docker-compose.override.yml`, que compose cargaría solo y cambiaría el despliegue.

Los dos modos comparten los nombres de contenedor:
- Detén uno antes de usar el otro.
- Al cambiar de modo, compose recrea `postgres-db`, `kafka` y `rabbitmq`.
- Los datos de PostgreSQL se conservan; los topics y las colas los recrean los servicios.

### Desarrollo local en Windows

```powershell
cd C:\Projects\Ecommerce-Hibrido
powershell -ExecutionPolicy Bypass -File ops\arrancar.ps1          # infraestructura + 7 jar, en orden
powershell -ExecutionPolicy Bypass -File ops\enviar-ordenes.ps1    # órdenes de prueba
powershell -ExecutionPolicy Bypass -File ops\detener.ps1 -ConInfra # detiene todo sin borrar datos
```

`arrancar.ps1` hace lo siguiente:
- Lee `.env` y pasa las contraseñas a los jar sin mostrarlas.
- Levanta solo `postgres-db`, `kafka` y `rabbitmq` con la capa de desarrollo.
- Empaqueta los jar si faltan y los arranca en el orden correcto. Los logs quedan en `logs\`.

Si los contenedores de los microservicios están en marcha, `arrancar.ps1` se detiene y muestra el
comando para pararlos.

Desde IntelliJ, define en cada configuración de ejecución:
- `DB_PASSWORD` en order-api.
- `RABBITMQ_PASSWORD` en order-processor y en los tres workers.

El orden de arranque está en [`docs/pruebas-e4-e6.md`](docs/pruebas-e4-e6.md).

### Docker completo (local) y AWS EC2

```bash
docker compose build                 # en Windows, construye las 7 imágenes
docker compose up -d --wait          # local: los diez contenedores
```

En EC2 no se compila:
- Las imágenes se construyen en Windows y se transfieren con `ops/aws/exportar-imagenes.ps1`
  (`docker save` y `docker load`).
- Se arranca por tandas con `bash ops/aws/arrancar-aws.sh` y se detiene con
  `bash ops/aws/detener-aws.sh`. Ninguno de los dos borra volúmenes.

Guía completa: [`docs/despliegue-aws.md`](docs/despliegue-aws.md).

## Acceso

| Qué | URL |
|---|---|
| API de órdenes | `POST http://localhost:8080/order` (en EC2: `http://<IP_PUBLICA_EC2>:8080/order`) |
| Dashboard | `http://localhost:8081` (en EC2: `http://<IP_PUBLICA_EC2>:8081`) |
| Consola de RabbitMQ | `http://localhost:15672`, usuario `ecommerce` y contraseña `RABBITMQ_PASSWORD`. En EC2, solo por túnel SSH. |

Ejemplo (PowerShell):

```powershell
$orden = @{ cliente = "Ana Perez"; items = @(@{ sku = "SKU-1"; nombre = "Monitor"; cantidad = 1; precioUnitario = 150000 }) } | ConvertTo-Json -Depth 5
(Invoke-WebRequest -UseBasicParsing -Method Post -Uri http://localhost:8080/order -ContentType "application/json" -Body $orden).StatusCode   # 202
```

El 202 confirma solo que la orden se aceptó. El procesamiento terminó cuando el dashboard muestra
pago, stock y correo para esa orden.

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/servicios.md`](docs/servicios.md) | Inventario de módulos, topics, colas, orden de arranque y contenedores |
| [`docs/despliegue-aws.md`](docs/despliegue-aws.md) | Despliegue en EC2 t3.small: límites, JVM, seguridad, `.env`, transferencia, arranque y monitoreo |
| [`docs/pruebas-e2-e3.md`](docs/pruebas-e2-e3.md) | Pruebas de la API, PostgreSQL y Kafka |
| [`docs/pruebas-e4-e6.md`](docs/pruebas-e4-e6.md) | Pruebas de RabbitMQ, workers, dashboard y analytics |
| [`docs/revision-solid.md`](docs/revision-solid.md) | Revisión de principios SOLID |
| `evidencias/` | Salidas registradas de las pruebas por etapa |

## Estructura

```
common/                 contratos de mensajería compartidos (Java puro)
order-api/              API REST, dominio, JPA y productor de Kafka
order-processor/        Kafka → RabbitMQ
billing-worker/  inventory-worker/  notification-worker/
dashboard-aggregator/   página estática y WebSocket
analytics-service/      consumidor de Kafka y registro en archivo
Dockerfile              imagen única parametrizada (--build-arg MODULO=...)
docker-compose.yml      configuración principal (AWS / Docker completo)
docker-compose.dev.yml  capa opcional de desarrollo local
ops/                    scripts de Windows; ops/aws/ scripts de EC2
docs/  evidencias/
```
