# Front/Dockerfile - DEBUG
FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json ./
RUN npm install --legacy-peer-deps

COPY . .

# Variables minimales
RUN echo "VITE_API_URL=http://backend:8083/api" > .env.local
RUN echo "VITE_FIREBASE_API_KEY=test" >> .env.local

# Build
RUN npm run build

# DEBUG: Chercher où est le build
RUN echo "=== RECHERCHE DU BUILD ==="
RUN find /app -type f -name "index.html" 2>/dev/null
RUN echo "=== ARBORESCENCE ==="
RUN find /app -type d -name "build" -o -name "dist" 2>/dev/null | xargs -I {} sh -c 'echo "=== {} ===" && ls -la {}'
RUN echo "=== TOUT LE DOSSIER /app ==="
RUN ls -la /app/

FROM nginx:alpine

RUN rm -rf /usr/share/nginx/html/*

COPY nginx.conf /etc/nginx/nginx.conf

# Essayer plusieurs chemins
COPY --from=builder /app/build/client/. /usr/share/nginx/html/ 2>/dev/null || \
  (echo "=== build/client non trouvé ===" && \
   COPY --from=builder /app/build/. /usr/share/nginx/html/)

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]