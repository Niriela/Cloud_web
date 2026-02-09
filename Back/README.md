# Cloud Web Backend API

Backend API pour l'application Cloud Web utilisant Spring Boot 3.2.1 et Java 21.

## Configuration requise

- Docker + Docker Compose

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

## Démarrage (Docker)

```bash
docker compose up -d
```

L'API sera disponible sur `http://localhost:8080/api`

## Endpoints disponibles

### Authentication
- `POST /api/auth/login` - Connexion
- `POST /api/auth/register` - Inscription
- `GET /api/auth/health` - Vérifier le statut de l'API

## Configuration

Les paramètres sont définis dans `application.yml` et `application-docker.yml`:
- Port: 8080
- Base de données: PostgreSQL (conteneur Docker)
- Secret JWT: À modifier en production
