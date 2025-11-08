FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app

COPY second-service/build.gradle second-service/settings.gradle /app/
COPY second-service/gradle /app/gradle
RUN gradle build --no-daemon -x test || true

COPY --chown=gradle:gradle second-service/ /app
RUN gradle clean war --no-daemon

FROM quay.io/wildfly/wildfly:latest

USER root

COPY second-service/standalone.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml

COPY --from=builder /app/build/libs/*.war \
     /opt/jboss/wildfly/standalone/deployments/second-service.war

COPY tls/second-service/keystore.p12 \
     /opt/jboss/wildfly/standalone/configuration/second-service.p12
COPY tls/truststore/truststore.p12 \
     /opt/jboss/wildfly/standalone/configuration/truststore.p12

RUN chown jboss:jboss \
    /opt/jboss/wildfly/standalone/configuration/second-service.p12 \
    /opt/jboss/wildfly/standalone/configuration/truststore.p12 \
    /opt/jboss/wildfly/standalone/deployments/second-service.war \
    /opt/jboss/wildfly/standalone/configuration/standalone.xml

USER jboss
EXPOSE 8080 8443

ENTRYPOINT ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", \
  "-Djavax.net.ssl.trustStore=/opt/jboss/wildfly/standalone/configuration/truststore.p12", \
  "-Djavax.net.ssl.trustStorePassword=changeit", \
  "-Djavax.net.ssl.trustStoreType=PKCS12" ]
