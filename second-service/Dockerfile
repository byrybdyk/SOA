# ---------- Stage 1: Build WAR ----------
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app

# Кэш зависимостей
COPY second-service/build.gradle second-service/settings.gradle /app/
COPY second-service/gradle /app/gradle
RUN gradle build --no-daemon -x test || true

# Остальной код и сборка
COPY --chown=gradle:gradle second-service/ /app
RUN gradle clean war --no-daemon

# ---------- Stage 2: WildFly runtime ----------
FROM quay.io/wildfly/wildfly:latest

USER root

# 1) Кладём подготовленный standalone.xml (в котором ты уже включил TLS-раздел с PKCS12)
COPY second-service/standalone.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml

# 2) Деплой приложения
COPY --from=builder /app/build/libs/*.war \
     /opt/jboss/wildfly/standalone/deployments/second-service.war

# 3) TLS артефакты (строго по нашей структуре tls/<role>)
#    keystore p12 для HTTPS-листенера и общий truststore p12 для исходящих TLS
COPY tls/second-service/keystore.p12 \
     /opt/jboss/wildfly/standalone/configuration/second-service.p12
COPY tls/truststore/truststore.p12 \
     /opt/jboss/wildfly/standalone/configuration/truststore.p12

# Права
RUN chown jboss:jboss \
    /opt/jboss/wildfly/standalone/configuration/second-service.p12 \
    /opt/jboss/wildfly/standalone/configuration/truststore.p12 \
    /opt/jboss/wildfly/standalone/deployments/second-service.war \
    /opt/jboss/wildfly/standalone/configuration/standalone.xml

USER jboss
EXPOSE 8080 8443

# 4) Указываем truststore для исходящих HTTPS-запросов приложения
#    (пароль и тип соответствуют тому, как мы его создали)
ENTRYPOINT ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", \
  "-Djavax.net.ssl.trustStore=/opt/jboss/wildfly/standalone/configuration/truststore.p12", \
  "-Djavax.net.ssl.trustStorePassword=changeit", \
  "-Djavax.net.ssl.trustStoreType=PKCS12" ]
