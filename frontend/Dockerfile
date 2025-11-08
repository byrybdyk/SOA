# 1️⃣ Этап сборки фронтенда
FROM node:20 AS build-stage
WORKDIR /app

# Скопируем package.json / lock-файл и установим зависимости
COPY frontend/package*.json ./
RUN npm install

# Скопируем весь фронт и соберём
COPY frontend/ ./
RUN npm run build

# 2️⃣ Этап деплоя (Nginx)
FROM nginx:1.25-alpine AS production-stage

# Скопируем собранную статику
COPY --from=build-stage /app/dist /usr/share/nginx/html

# ── фронтовой TLS (сертификат для 144.31.193.121) ──
# frontend.crt = fullchain; frontend.key = privatе key
COPY tls/frontend/privkey.key    /etc/nginx/certs/frontend.key
COPY tls/frontend/fullchain.crt  /etc/nginx/certs/frontend.crt

# ── корневой CA (которым подписаны сертификаты сервисов) ──
COPY tls/ca/rootCA.crt           /etc/nginx/certs/rootCA.crt

# Конфиг nginx
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 443
CMD ["nginx", "-g", "daemon off;"]
