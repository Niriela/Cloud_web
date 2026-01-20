# Front/Dockerfile (version finale corrigée)
FROM node:20-alpine AS builder

WORKDIR /app

# Copier package.json d'abord (optimisation cache)
COPY package*.json ./

# Installer dépendances
RUN npm ci

# Copier le reste
COPY . .

# Builder
RUN npm run build

# Vérifier
RUN echo "Build réussi. Fichiers:" && \
    ls -la /app/build/client/

# Étape production
FROM nginx:alpine

# SUPPRIMER COMPLÈTEMENT les fichiers par défaut
RUN rm -rf /usr/share/nginx/html/* && \
    rm -f /etc/nginx/conf.d/default.conf

# Copier VOTRE configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Copier VOS fichiers
COPY --from=builder /app/build/client/. /usr/share/nginx/html/

# Vérification
RUN echo "Fichiers après copie:" && \
    ls -la /usr/share/nginx/html/ && \
    echo "Pas de 50x.html:" $(test ! -f /usr/share/nginx/html/50x.html && echo "✅" || echo "❌")

EXPOSE 80

# Commande correcte
CMD ["nginx", "-g", "daemon off;"]