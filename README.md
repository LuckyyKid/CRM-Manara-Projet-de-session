# CRM Manara

## Vue d'ensemble

CRM Manara est maintenant organisé en mono-repo avec une V1 figée et une V2 en cours de migration.

- `version-1` : version Spring MVC / Thymeleaf figée pour les sprints 1 et 2
- `main` : branche d'intégration de la V2
- V2 : backend Spring Boot conservé, frontend Angular ajouté progressivement

## Structure du repo

- `backend/` : application Spring Boot, logique métier, sécurité, API REST, vues Thymeleaf héritées de la V1, tests Maven
- `frontend/` : frontend Angular V2
- `docs/` : diagrammes, maquettes et guide du chatbot

## Ce qui est déjà fait en V2

### Architecture

- repo restructuré en mono-repo clair `backend/ + frontend/ + docs/`
- couche DTO ajoutée côté Spring
- premiers endpoints REST `/api` ajoutés pour Angular
- coexistence temporaire API + écrans Thymeleaf hérités

### Backend Spring

- backend déplacé dans `backend/`
- sécurité adaptée pour Angular sur `http://localhost:4200`
- CORS actif sur `/api/**`
- gestion de session Spring conservée
- `/api/me` disponible pour récupérer l'utilisateur connecté
- endpoints API déjà disponibles pour:
  - `/api/me`
  - `/api/parent/**`
  - `/api/admin/**`
  - `/api/animateur/**`

### Frontend Angular

- projet Angular créé dans `frontend/`
- proxy Angular configuré vers `http://localhost:8080`
- auth/session Angular branchée avec session Spring
- garde d'auth pour les routes protégées
- garde invité pour la page de login Angular
- page de base `me/dashboard` branchée sur `/api/me`

### Validation déjà faite

- `cd backend && ./mvnw test` : OK
- `cd frontend && npm run build` : OK
- smoke test validé avec:
  - backend sur `8080`
  - frontend sur `4200`
  - `/api/me` en `401` sans session
  - login Spring redirige bien vers `http://localhost:4200/me/dashboard`
  - `/api/me` retourne bien le profil après authentification

## Ce qu'il reste à faire en V2

### Priorité immédiate

- construire le layout Angular principal
- remplacer progressivement les dashboards Thymeleaf par des pages Angular
- créer les services Angular par domaine:
  - auth
  - parent
  - admin
  - animateur
- brancher les écrans Angular sur les endpoints `/api`

### Parent

- dashboard Angular parent
- liste des enfants
- activités et planning
- notifications
- paramètres

### Admin

- dashboard Angular admin
- gestion des activités
- gestion des animations
- gestion des animateurs
- gestion des parents / enfants
- page des demandes
- notifications admin

### Animateur

- dashboard Angular animateur
- planning / inscriptions
- gestion des présences
- notifications animateur

### Backend à compléter pour Angular

- standardiser davantage les réponses JSON
- ajouter les endpoints manquants pour les formulaires complets CRUD
- vérifier la cohérence finale DTO / contrôleurs / services
- réduire progressivement la dépendance aux templates Thymeleaf côté V2

## Lancer le backend

```bash
cd backend
./mvnw spring-boot:run
```

- backend local: `http://localhost:8080`

## Tester le backend

```bash
cd backend
./mvnw test
```

## Lancer le frontend Angular

```bash
cd frontend
npm install
npm start
```

- frontend local: `http://localhost:4200`
- proxy Angular vers Spring: `/api` -> `http://localhost:8080`

## Parcours encore disponibles côté Thymeleaf

Ces écrans existent encore pendant la migration et servent de référence fonctionnelle.

### Parent

- `/parent/dashboard`
- `/parent/enfants`
- `/parent/activities`
- `/parent/planning`
- `/parent/notifications`
- `/settings`

### Admin

- `/admin/adminDashboard`
- `/admin/activities`
- `/admin/animations`
- `/admin/animateurs`
- `/admin/parents`
- `/admin/demandes`
- `/admin/notifications`

### Animateur

- `/animateur/dashboard`
- `/animateur/notifications`
- `/animateur/inscriptions`

## Références

- schéma SQL backend : [backend/SQL/schema_sprint1.sql](/home/ahmed/Desktop/Web/Projet-Session/projet-de-session/backend/SQL/schema_sprint1.sql)
- documentation projet : [docs](/home/ahmed/Desktop/Web/Projet-Session/projet-de-session/docs)
