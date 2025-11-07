# ---------- Stage 1: Build WAR ----------
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app

# Кэш зависимостей: только gradle wrapper и build-файлы из second-service/
COPY second-service/build.gradle second-service/settings.gradle /app/
COPY second-service/gradle /app/gradle
RUN gradle build --no-daemon -x test || true

# Копируем остальной код второго сервиса
COPY --chown=gradle:gradle second-service/ /app
RUN gradle clean war --no-daemon

# ---------- Stage 2: WildFly runtime ----------
FROM quay.io/wildfly/wildfly:latest

USER root
# Конфиг WildFly (путь от корня репо)
COPY second-service/standalone.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml

# Деплой приложения: возьмём любой *.war и положим с фиксированным именем
COPY --from=builder /app/build/libs/*.war /opt/jboss/wildfly/standalone/deployments/second-service.war

# TLS артефакты (пути от корня репо)
COPY tls/second-service/second-service.p12 /opt/jboss/wildfly/standalone/configuration/second-service.p12
COPY tls/shared/truststore.jks       /opt/jboss/wildfly/standalone/configuration/truststore.jks

# Права на файлы
RUN chown jboss:jboss \
    /opt/jboss/wildfly/standalone/configuration/second-service.p12 \
    /opt/jboss/wildfly/standalone/configuration/truststore.jks \
    /opt/jboss/wildfly/standalone/deployments/second-service.war

USER jboss
EXPOSE 8080 8443

# Включаем truststore для исходящих HTTPS-запросов из приложения
ENTRYPOINT exec /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 \
  -Djavax.net.ssl.trustStore=/opt/jboss/wildfly/standalone/configuration/truststore.jks \
  -Djavax.net.ssl.trustStorePassword=changeit
