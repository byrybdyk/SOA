# ---------- СТАДИЯ 1: сборка приложения ----------
FROM gradle:8.10-jdk21-alpine AS build

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем gradle файлы и зависимости (чтобы кэшировать их между сборками)
COPY build.gradle settings.gradle ./
COPY gradle gradle

# Кэшируем зависимости
RUN gradle dependencies --no-daemon || return 0

# Копируем исходники
COPY src src

# Собираем jar
RUN gradle bootJar --no-daemon

# ---------- СТАДИЯ 2: минимальный runtime ----------
FROM eclipse-temurin:21-jdk-alpine

# Рабочая директория для приложения
WORKDIR /app

# Копируем jar из предыдущего этапа
COPY --from=build /app/build/libs/*.jar first-service.jar

# Указываем порт (Spring Boot по умолчанию — 8080)
EXPOSE 8080

# Переменные окружения (для подключения к Postgres из docker-compose)
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/soa
ENV SPRING_DATASOURCE_USERNAME=user
ENV SPRING_DATASOURCE_PASSWORD=password
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update

# Команда запуска
ENTRYPOINT ["java", "-jar", "first-service.jar"]
