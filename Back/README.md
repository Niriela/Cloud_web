# Cloud Web Backend API

Backend API pour l'application Cloud Web utilisant Spring Boot 3.2.1 et Java 21.

## Configuration requise

- Java 21
- Maven 3.9+

## Structure du projet

```
src/main/java/com/cloudweb/
├── controller/       # Contrôleurs REST
├── service/         # Services métier
├── entity/          # Entités JPA
├── dto/             # Data Transfer Objects
├── repository/      # Repositories JPA
├── security/        # Configuration de sécurité et JWT
└── CloudWebApplication.java
```

## Démarrage

```bash
mvn clean install
mvn spring-boot:run
```

L'API sera disponible sur `http://localhost:8083/api`

## Endpoints disponibles

### Authentication
- `POST /api/auth/login` - Connexion
- `POST /api/auth/register` - Inscription
- `GET /api/auth/health` - Vérifier le statut de l'API

## Configuration

Les paramètres sont définis dans `application.yml`:
- Port: 8083
- Base de données: H2 (en mémoire)
- Secret JWT: À modifier en production
