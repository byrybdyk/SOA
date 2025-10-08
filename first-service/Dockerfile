FROM payara/server-full:latest

USER root

# Копируем war в стандартную папку автодеплоя
COPY build/libs/first-service.war /opt/payara/deployments/

# Меняем владельца (иначе Payara не сможет прочитать файл)
RUN chown payara:payara /opt/payara/deployments/first-service.war

USER payara

# Порты: 8080 — HTTP, 4848 — admin console
EXPOSE 8080 4848

CMD ["bin/startInForeground.sh"]
