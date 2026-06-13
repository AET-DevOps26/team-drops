FROM node:20-alpine AS build

WORKDIR /app

ARG VITE_AUTH_ENABLED=false
ARG VITE_KEYCLOAK_URL=http://localhost:8090
ARG VITE_KEYCLOAK_REALM=team-drops
ARG VITE_KEYCLOAK_CLIENT_ID=team-drops
ENV VITE_AUTH_ENABLED=$VITE_AUTH_ENABLED
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM
ENV VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID

COPY package*.json ./
RUN npm ci

COPY . .

RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
COPY docker-entrypoint.d/30-write-config.sh /docker-entrypoint.d/30-write-config.sh
RUN sed -i 's/\r$//' /docker-entrypoint.d/30-write-config.sh \
    && chmod +x /docker-entrypoint.d/30-write-config.sh

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
