# Modes de démarrage - Cloud Web

## 🐳 Mode Docker (RECOMMANDÉ)

### Avantages
✅ Configuration automatique de tous les services
✅ Isolation complète (pas de conflits de ports)
✅ Base de données PostgreSQL intégrée
✅ Serveur OSM inclus
✅ Hot reload fonctionnel
✅ Pas besoin d'installer Java, Maven, Node.js, PostgreSQL

### Commandes
```bash
# Windows
start-docker.bat        # Démarrer
stop-docker.bat         # Arrêter
clean-docker.bat        # Nettoyer complètement

# Linux/Mac
./start-docker.sh       # Démarrer
./stop-docker.sh        # Arrêter
./clean-docker.sh       # Nettoyer complètement
```

### URLs
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Database: localhost:5433
- OSM Tiles: http://localhost:8090

---

## 💻 Mode Local (Sans Docker)

### Prérequis
- Java 21
- Maven 3.9+
- Node.js 20+
- PostgreSQL 16

### Commandes
```bash
# Windows
start-all.bat           # Démarrer backend + frontend
start-backend.bat       # Démarrer uniquement backend
start-frontend.bat      # Démarrer uniquement frontend

# Linux/Mac
./start-all.sh
./start-backend.sh
./start-frontend.sh
```

### URLs
- Frontend: http://localhost:5173 (ou 3000 selon config)
- Backend: http://localhost:8080

### Configuration manuelle requise
1. Installer et démarrer PostgreSQL
2. Créer la base de données `cloud_db`
3. Configurer les variables d'environnement
4. Installer les dépendances Maven et npm

---

## 🔄 Comparaison

| Fonctionnalité | Docker | Local |
|----------------|--------|-------|
| Setup initial | 1 commande | Multiple installations |
| Base de données | ✅ Incluse | ❌ À installer |
| OSM Server | ✅ Inclus | ❌ À configurer |
| Hot reload | ✅ Oui | ✅ Oui |
| Ressources | Plus élevé | Moins élevé |
| Portabilité | ✅ Parfaite | ⚠️ Dépend de l'OS |

---

## 🎯 Recommandation

**Utilisez le mode Docker** sauf si :
- Vous avez déjà tout installé localement
- Vous voulez économiser des ressources
- Vous ne voulez pas utiliser le serveur OSM
