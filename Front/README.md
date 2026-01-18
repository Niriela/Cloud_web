# Cloud Web Frontend

Application React pour Cloud Web avec système d'authentification complet.

## Configuration requise

- Node.js 16+
- npm ou yarn

## Installation

```bash
npm install
```

## Démarrage

```bash
npm start
```

L'application sera disponible sur `http://localhost:3000`

## Structure du projet

```
src/
├── components/       # Composants réutilisables
├── pages/           # Pages principales
├── api/             # Appels API
├── index.js         # Point d'entrée
└── App.js           # Composant principal
```

## Pages

- `/login` - Page de connexion
- `/register` - Page d'inscription
- `/dashboard` - Tableau de bord (protégé)

## Authentification

- JWT (JSON Web Token) pour l'authentification
- Stockage du token en localStorage
- Redirection automatique si non authentifié
