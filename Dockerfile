# =============================================================================================
# Dockerfile UNICO y parametrizado para los siete microservicios.
#
#   docker build --build-arg MODULO=order-api -t ecommerce/order-api .
#   (docker compose lo hace solo: cada servicio pasa su MODULO en build.args)
#
# Dos etapas (multi-stage):
#   1. builder : Maven + JDK 21. Compila TODO el proyecto una sola vez.
#   2. runtime : solo JRE 21 sobre Alpine, usuario sin privilegios y el jar de UN modulo.
# =============================================================================================

# ---------------------------------------------------------------------------------------------
# ETAPA 1 - builder
# NO declara ARG MODULO: asi esta etapa es identica para los siete servicios y Docker la
# construye una vez y la reutiliza de la cache para los otros seis.
# ---------------------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# 1a. Primero SOLO los pom.xml. Mientras no cambien, la capa de dependencias queda en cache y
#     un cambio en el codigo no vuelve a descargar nada.
COPY pom.xml ./
COPY common/pom.xml               common/
COPY order-api/pom.xml            order-api/
COPY analytics-service/pom.xml    analytics-service/
COPY order-processor/pom.xml      order-processor/
COPY billing-worker/pom.xml       billing-worker/
COPY inventory-worker/pom.xml     inventory-worker/
COPY notification-worker/pom.xml  notification-worker/
COPY dashboard-aggregator/pom.xml dashboard-aggregator/

# excludeGroupIds: los modulos propios (p. ej. common) todavia no estan compilados en este punto
# y no existen en ningun repositorio remoto; sin la exclusion go-offline fallaria buscandolos.
RUN mvn -B -q org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline \
        -DexcludeGroupIds=co.edu.proyecto

# 1b. Despues el codigo, y se empaqueta todo (las pruebas ya se ejecutan con mvn verify fuera).
COPY . .
RUN mvn -B clean package -DskipTests

# ---------------------------------------------------------------------------------------------
# ETAPA 2 - runtime
# Parte de una imagen limpia: Maven, el JDK y el codigo fuente de la etapa 1 se descartan.
# ---------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# ARG MODULO va AQUI y no antes (ver comentario de la etapa 1).
ARG MODULO

# Usuario y grupo del sistema sin privilegios (-S): el proceso java no corre como root.
RUN addgroup -S ecommerce && adduser -S -G ecommerce -H -s /sbin/nologin ecommerce

WORKDIR /app

# Solo el jar ejecutable del modulo pedido, con el usuario sin privilegios como propietario.
COPY --from=builder --chown=ecommerce:ecommerce /build/${MODULO}/target/${MODULO}-1.0.0.jar app.jar

# /app/datos: directorio escribible por el usuario sin privilegios. analytics-service monta ahi
# su volumen (el registro del modulo de Sistemas Operativos); un volumen nombrado nuevo hereda
# este propietario, asi que el usuario ecommerce puede escribir en el.
RUN mkdir -p /app/datos && chown ecommerce:ecommerce /app/datos

USER ecommerce

# El limite de heap (-Xmx) se pasa desde docker-compose.yml, uno por servicio.
ENV JAVA_OPTS=""

# exec: java sustituye al shell y pasa a ser el PID 1, asi recibe el SIGTERM de "docker stop"
# y Spring se apaga de forma ordenada.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
