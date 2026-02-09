# Guide Docker Compose - Cloud Web

## 📦 Configuration complète du projet

Ce docker-compose configure :
- **PostgreSQL** : Base de données (port 5433)
- **Backend Spring Boot** : API REST (port 8080)
- **Frontend React + Vite** : Interface utilisateur (port 5173)
- **OSM Tile Server** : Serveur de tuiles OpenStreetMap (port 8090)

## 🚀 Démarrage rapide

### 1. Copier le fichier de configuration
```bash
cp .env.example .env
```

### 2. Démarrer tous les services
```bash
docker-compose up -d
```

### 3. Voir les logs
```bash
# Tous les services
docker-compose logs -f

# Un service spécifique
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f osm-server
```

## 📋 Ordre de démarrage

Les services démarrent automatiquement dans cet ordre :
1. **db** - Base de données PostgreSQL
2. **osm-import** - Import des données OSM (une seule fois)
3. **backend** - Une fois que la DB est prête
4. **frontend** - Une fois que le backend est démarré
5. **osm-server** - Une fois l'import terminé

## 🔧 Commandes utiles

### Arrêter tous les services
```bash
docker-compose down
```

### Arrêter et supprimer les volumes (ATTENTION : supprime les données)
```bash
docker-compose down -v
```

### Reconstruire les images
```bash
docker-compose up -d --build
```

### Redémarrer un service spécifique
```bash
docker-compose restart backend
docker-compose restart frontend
```

### Accéder à un conteneur
```bash
docker-compose exec backend sh
docker-compose exec frontend sh
docker-compose exec db psql -U postgres -d cloud_db
```

## 🌐 URLs d'accès

- **Frontend** : http://localhost:5173
- **Backend API** : http://localhost:8080
- **Base de données** : localhost:5433
- **OSM Tiles** : http://localhost:8090

## 🗺️ Configuration OSM

Le serveur OSM utilise les données de Madagascar situés dans `./Osm-server/data/madagascar.osm.pbf`.

### Première exécution
L'import des données OSM peut prendre du temps (selon la taille du fichier .osm.pbf).

### Utiliser d'autres données OSM
1. Télécharger un fichier .osm.pbf depuis [Geofabrik](http://download.geofabrik.de/)
2. Placer le fichier dans `./Osm-server/data/`
3. Renommer en `madagascar.osm.pbf` ou modifier le path dans docker-compose.yml

## 🔄 Hot Reload

### Backend (Spring Boot)
Le code Java est rechargé automatiquement grâce à Maven et Spring DevTools.

### Frontend (React + Vite)
Les modifications sont reflétées instantanément grâce à Vite HMR.

## 🗄️ Base de données

### Accès direct
```bash
docker-compose exec db psql -U postgres -d cloud_db
```

### Variables d'environnement (modifiables dans .env)
- **DB_NAME** : cloud_db
- **DB_USERNAME** : postgres
- **DB_PASSWORD** : postgres
- **DB_HOST_PORT** : 5433 (port sur la machine hôte)

### Scripts d'initialisation
Les scripts SQL dans `./init-db/` sont exécutés automatiquement au premier démarrage.

## 🛠️ Dépannage

### Le backend ne démarre pas
```bash
# Vérifier les logs
docker-compose logs backend

# Reconstruire l'image
docker-compose up -d --build backend
```

### Le frontend affiche des erreurs de dépendances
```bash
# Supprimer node_modules et réinstaller
docker-compose exec frontend rm -rf node_modules
docker-compose restart frontend
```

### L'import OSM prend trop de temps
C'est normal pour les gros fichiers. Vous pouvez suivre la progression :
```bash
docker-compose logs -f osm-import
```

### Nettoyer complètement Docker
```bash
# Arrêter et supprimer tout
docker-compose down -v

# Supprimer les images
docker-compose down --rmi all

# Nettoyer Docker
docker system prune -a
```

## 📦 Volumes persistants

Les données suivantes sont persistées :
- **postgres_data** : Données de la base de données
- **maven_repo** : Cache Maven (dépendances Java)
- **npm_cache** : Cache npm (dépendances Node)
- **osm_data** : Base de données OSM (une fois importée)

## 🎯 Production

Pour le déploiement en production, pensez à :
1. Changer les mots de passe dans `.env`
2. Modifier `JWT_SECRET` avec une valeur sécurisée
3. Configurer `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` au lieu de `update`
4. Utiliser des builds de production pour le frontend
5. Ajouter un reverse proxy (nginx) pour gérer HTTPS
