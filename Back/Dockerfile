# Back/Dockerfile
# Étape 1 : Build avec Maven
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Copier les fichiers de configuration
COPY pom.xml .
# 2. Télécharger les dépendances (cache layer)
RUN mvn dependency:go-offline

# 3. Copier le code source
COPY src ./src

# 4. Builder l'application (avec profil docker)
RUN mvn clean package -DskipTests -Pdocker

# Étape 2 : Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Variables d'environnement pour Docker
ENV SPRING_PROFILES_ACTIVE=docker
ENV DB_HOST=postgres-db
ENV DB_PORT=5432
ENV FIREBASE_ENABLED=true

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S spring && adduser -S spring -G spring

# Copier le JAR depuis l'étape de build
COPY --from=builder /app/target/*.jar app.jar

# Changer les permissions
RUN chown -R spring:spring /app
USER spring:spring

# Exposer le port Spring Boot
EXPOSE 8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]