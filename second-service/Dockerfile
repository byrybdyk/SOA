FROM quay.io/wildfly/wildfly:latest

USER root

COPY keystore.jks /opt/jboss/wildfly/standalone/configuration/
COPY standalone.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml
COPY build/libs/second-service.war /opt/jboss/wildfly/standalone/deployments/

USER jboss

EXPOSE 8081 8443

ENTRYPOINT ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
