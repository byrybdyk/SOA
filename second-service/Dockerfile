FROM quay.io/wildfly/wildfly:latest

USER root

# Копируем keystore и standalone.xml
COPY keystore.jks /opt/jboss/wildfly/standalone/configuration/
COPY standalone.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml

# Копируем WAR
COPY build/libs/second-service.war /opt/jboss/wildfly/standalone/deployments/

USER jboss

ENTRYPOINT ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
