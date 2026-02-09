# Back/Dockerfile - Mode développement avec hot reload
FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

# Copier le pom.xml pour télécharger les dépendances (cache Docker)
COPY pom.xml .

# Télécharger les dépendances (sera mis en cache si pom.xml ne change pas)
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Variables d'environnement
ENV SPRING_PROFILES_ACTIVE=docker
ENV DB_HOST=db
ENV DB_PORT=5432

EXPOSE 8080

# Le CMD sera écrasé par docker-compose pour permettre le hot reload
CMD ["mvn", "spring-boot:run"]