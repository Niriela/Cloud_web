# Front/Dockerfile - Version simplifiée et efficace
FROM node:20-alpine AS builder

WORKDIR /app

# 1. Copier package.json pour le cache
COPY package.json package-lock.json ./

# 2. Installer toutes les dépendances (production + dev)
RUN npm install --frozen-lockfile

# 3. Copier tout le code source
COPY . .

# 4. Définir les variables d'environnement pour Vite
ARG VITE_API_URL=http://backend:8080/api
ARG VITE_APP_NAME=CloudWeb
ARG VITE_APP_VERSION=1.0.0

# 5. Builder avec ces variables
RUN VITE_API_URL=${VITE_API_URL} \
    VITE_APP_NAME=${VITE_APP_NAME} \
    VITE_APP_VERSION=${VITE_APP_VERSION} \
    npm run build

# Vérification
RUN ls -la /app/build/client/ && \
    echo "Fichiers générés:" && \
    find /app/build/client/ -name "*.js" | head -5

# Production
FROM nginx:alpine

RUN rm -rf /usr/share/nginx/html/*

COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=builder /app/build/client/. /usr/share/nginx/html/

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]