# Dockerfile UNICO y parametrizado para los siete microservicios.

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml ./
COPY common/pom.xml               common/
COPY order-api/pom.xml            order-api/
COPY analytics-service/pom.xml    analytics-service/
COPY order-processor/pom.xml      order-processor/
COPY billing-worker/pom.xml       billing-worker/
COPY inventory-worker/pom.xml     inventory-worker/
COPY notification-worker/pom.xml  notification-worker/
COPY dashboard-aggregator/pom.xml dashboard-aggregator/

RUN mvn -B -q org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline \
        -DexcludeGroupIds=co.edu.proyecto

COPY . .
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

ARG MODULO

RUN addgroup -S ecommerce && adduser -S -G ecommerce -H -s /sbin/nologin ecommerce

WORKDIR /app

COPY --from=builder --chown=ecommerce:ecommerce /build/${MODULO}/target/${MODULO}-1.0.0.jar app.jar

RUN mkdir -p /app/datos && chown ecommerce:ecommerce /app/datos

USER ecommerce

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
