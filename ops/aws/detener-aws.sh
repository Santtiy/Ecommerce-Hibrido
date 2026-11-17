#!/usr/bin/env bash
# =============================================================================================
# detener-aws.sh - Detiene los diez contenedores de docker-compose.yml SIN borrar nada.
#
#   bash ops/aws/detener-aws.sh            # docker compose stop (por defecto, recomendado)
#   bash ops/aws/detener-aws.sh --down     # docker compose down (SIN -v)
#
# stop  : detiene los contenedores y los CONSERVA. Se mantienen los volumenes (PostgreSQL,
#         analytics-datos) y tambien los datos internos de Kafka (topics, offsets) y RabbitMQ
#         (colas durables), que en este proyecto viven en el sistema de archivos del contenedor.
#         Con restart: unless-stopped, un contenedor detenido a mano NO se relanza al reiniciar EC2.
# --down: elimina contenedores y red, pero NUNCA volumenes (no se usa -v). Se pierden los topics
#         de Kafka y las colas de RabbitMQ; los productores/consumidores los recrean al arrancar.
#
# Orden inverso al de arranque: primero el que publica comandos (order-processor), luego los
# consumidores y order-api, y al final la infraestructura, para no dejar mensajes a medias.
# =============================================================================================

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ARCHIVO_COMPOSE="$RAIZ/docker-compose.yml"
COMPOSE=(docker compose -f "$ARCHIVO_COMPOSE")

MODO="stop"
case "${1:-}" in
    "")      ;;
    --down)  MODO="down" ;;
    -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
    *)       echo "Opcion desconocida: $1 (usa --down o nada)" >&2; exit 2 ;;
esac

[ -f "$ARCHIVO_COMPOSE" ] || { echo "No existe $ARCHIVO_COMPOSE" >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo "El demonio de Docker no responde." >&2; exit 1; }

# docker-compose.yml exige POSTGRES_PASSWORD y RABBITMQ_PASSWORD para cargar el modelo, incluso
# para "stop". Detener debe funcionar aunque falte .env (p. ej. en una emergencia): en ese caso
# se usan valores de relleno. Es seguro porque stop y down (sin -v) solo detienen o eliminan
# contenedores existentes; nunca crean ni recrean uno, asi que esos valores no llegan a ningun
# servicio. Si .env existe, compose lo usa y no se toca nada.
if [ ! -f "$RAIZ/.env" ]; then
    export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-no-usada-al-detener}"
    export RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-no-usada-al-detener}"
    echo "(sin .env: se usan valores de relleno solo para poder detener)"
fi

paso() { printf '\n== %s\n' "$*"; }

paso "Deteniendo order-processor"
"${COMPOSE[@]}" stop order-processor

paso "Deteniendo workers, analytics-service y dashboard-aggregator"
"${COMPOSE[@]}" stop billing-worker inventory-worker notification-worker analytics-service dashboard-aggregator

paso "Deteniendo order-api"
"${COMPOSE[@]}" stop order-api

paso "Deteniendo infraestructura"
"${COMPOSE[@]}" stop rabbitmq kafka postgres-db

if [ "$MODO" = "down" ]; then
    paso "docker compose down (SIN -v: los volumenes se conservan)"
    "${COMPOSE[@]}" down
fi

paso "Estado final"
"${COMPOSE[@]}" ps -a --format 'table {{.Name}}\t{{.Status}}' || true
echo "--- Volumenes conservados"
docker volume ls --format '{{.Name}}' | grep -E '^(ecommerce-hibrido_postgres-data|analytics-datos)$' || echo "(ninguno encontrado)"
echo "--- free -h"
free -h
echo "Listo."
