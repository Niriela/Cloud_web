# Cloud Web - Application Full Stack

Projet complet d'application web cloud avec authentification.

## 📦 Architecture

- **Backend**: Spring Boot 3.2.1 (Java 21)
- **Frontend**: React 18
- **Base de données**: H2 (développement)
- **Authentification**: JWT (JSON Web Token)

## 🚀 Démarrage rapide

### Backend (Spring Boot)

```bash
cd Back
mvn clean install
mvn spring-boot:run
```

L'API sera disponible sur `http://localhost:8083/api`

### Frontend (React)

```bash
cd Front
npm install
npm start
```

L'application sera disponible sur `http://localhost:3000`

## 📋 Fonctionnalités

- ✅ Inscription d'utilisateur
- ✅ Connexion avec JWT
- ✅ Dashboard protégé
- ✅ Gestion de session
- ✅ Déconnexion

## 🔒 Sécurité

- Authentification JWT
- Mot de passes hashés avec BCrypt
- CORS configuré
- Routes protégées côté frontend et backend

## 📁 Structure

```
Cloud_web/
├── Back/              # Spring Boot API
│   ├── src/
│   │   ├── main/java/com/cloudweb/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── dto/
│   │   └── resources/
│   └── pom.xml
├── Front/             # React Application
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── api/
│   │   ├── App.js
│   │   └── index.js
│   └── package.json
└── README.md
```

## 🔑 Variables d'environnement

### Backend (application.yml)

- `jwt.secret` - Clé secrète JWT (à modifier en production)
- `jwt.expiration` - Durée d'expiration du token (24h par défaut)
- `spring.datasource.url` - URL de la base de données

### Frontend (.env)

```
REACT_APP_API_URL=http://localhost:8083/api
```

## 📚 API Endpoints

### Authentication

- `POST /api/auth/login` - Connexion
  - Body: `{ email, password }`
  - Response: `{ token, id, email, firstName, lastName }`

- `POST /api/auth/register` - Inscription
  - Body: `{ email, password, firstName, lastName }`
  - Response: `{ token, id, email, firstName, lastName }`

- `GET /api/auth/health` - Vérifier le statut

## 💻 Développement

Le projet utilise :
- **Maven** pour la gestion des dépendances Java
- **npm** pour la gestion des dépendances React
- **H2 Console** accessible sur `http://localhost:8083/api/h2-console`

## 🔄 Flux d'authentification

1. L'utilisateur remplit le formulaire de connexion/inscription
2. Les données sont envoyées au backend via une requête REST
3. Le backend valide les informations et génère un JWT
4. Le token est stocké en localStorage côté frontend
5. Tous les appels API incluent le token dans le header Authorization
6. Le backend valide le token pour chaque requête protégée

## ⚠️ À faire pour la production

1. Modifier la clé secrète JWT
2. Configurer une vraie base de données (PostgreSQL, MySQL)
3. Ajouter la validation des formulaires côté serveur
4. Configurer HTTPS
5. Ajouter des logs appropriés
6. Implémenter le refresh token
7. Ajouter la vérification des emails
8. Implémenter 2FA

---

Développement: January 2026
