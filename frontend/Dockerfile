# 1️⃣ Этап сборки фронтенда
FROM node:20 AS build-stage
WORKDIR /app

COPY frontend/package*.json ./
RUN npm install

COPY frontend/ ./
# ❌ Эти строки были причиной лишних путей в сборке — убираем:
# COPY tls/frontend/privkey.key /tls/frontend/frontend.key
# COPY tls/frontend/fullchain.crt /tls/frontend/frontend-fullchain.crt

RUN npm run build

# 2️⃣ Этап деплоя (Nginx)
FROM nginx:1.25-alpine AS production-stage

COPY --from=build-stage /app/dist /usr/share/nginx/html

# фронтовые серты для публичного 144.31.193.121:8445
COPY tls/frontend/privkey.key   /etc/nginx/certs/frontend.key
COPY tls/frontend/fullchain.crt /etc/nginx/certs/frontend.crt

# корневой CA для проверки TLS до сервисов
COPY tls/ca/rootCA.crt          /etc/nginx/certs/rootCA.crt

COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 443
CMD ["nginx", "-g", "daemon off;"]
