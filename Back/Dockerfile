# Back/Dockerfile CORRIGÉ
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Copier le fichier Firebase (depuis la racine du contexte)
COPY cloud-web-mobile-firebase-adminsdk-fbsvc-25a331b444.json ./

# 2. Copier les fichiers Maven
COPY Back/pom.xml .
RUN mvn dependency:go-offline

# 3. Copier le code source
COPY Back/src ./src

# 4. Builder
RUN mvn clean package -DskipTests -Pdocker

# Étape runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Variables d'environnement - DÉSACTIVEZ FIREBASE POUR TESTER
ENV SPRING_PROFILES_ACTIVE=docker
ENV FIREBASE_ENABLED=false
ENV DB_HOST=postgres
ENV DB_PORT=5432

RUN addgroup -S spring && adduser -S spring -G spring

# Copier le JAR
COPY --from=builder /app/target/*.jar app.jar

# Copier le fichier Firebase AU BON ENDROIT
COPY --from=builder /app/cloud-web-mobile-firebase-adminsdk-fbsvc-25a331b444.json ./

RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]