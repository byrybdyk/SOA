FROM node:20 AS build-stage
WORKDIR /app

COPY frontend/package*.json ./
RUN npm install

COPY frontend/ ./

RUN npm run build

FROM nginx:1.25-alpine AS production-stage

COPY --from=build-stage /app/dist /usr/share/nginx/html

COPY tls/frontend/privkey.key   /etc/nginx/certs/frontend.key
COPY tls/frontend/fullchain.crt /etc/nginx/certs/frontend.crt

COPY tls/ca/rootCA.crt          /etc/nginx/certs/rootCA.crt

COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 443
CMD ["nginx", "-g", "daemon off;"]
