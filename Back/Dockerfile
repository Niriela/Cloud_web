# Back/Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Copier les fichiers de configuration
COPY pom.xml .
# 2. Télécharger les dépendances
RUN mvn dependency:go-offline

# 3. Copier le code source
COPY src ./src

# 4. Copier le fichier Firebase (AJOUT IMPORTANT)
COPY ../cloud-web-mobile-firebase-adminsdk-fbsvc-25a331b444.json ./cloud-web-mobile-firebase-adminsdk-fbsvc-25a331b444.json

# 5. Builder l'application
RUN mvn clean package -DskipTests -Pdocker

# Étape 2 : Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Variables d'environnement
ENV SPRING_PROFILES_ACTIVE=docker
ENV DB_HOST=postgres-db
ENV DB_PORT=5432
ENV FIREBASE_ENABLED=true

# Créer un utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring

# Copier le JAR depuis l'étape de build
COPY --from=builder /app/target/*.jar app.jar

# Copier le fichier Firebase (IMPORTANT)
COPY --from=builder /app/cloud-web-mobile-firebase-adminsdk-fbsvc-25a331b444.json ./cloud-web-mobile-firebase-adminsdk-fbsvc-25a331b444.json

# Changer les permissions
RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]