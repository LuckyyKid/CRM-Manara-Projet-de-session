# CRM Manara

## Structure du repo

- `backend/` : application Spring Boot, logique métier, API REST, vues Thymeleaf V1, tests Maven
- `frontend/` : nouveau frontend Angular V2
- `docs/` : diagrammes, maquettes et guide du chatbot

## Branches

- `version-1` : version Spring MVC figée pour les sprints 1 et 2
- `main` : branche d'intégration V2
- `ahmed-angular` : migration Angular + adaptation Spring API en cours

## V2 retenue

- backend Spring Boot conservé
- frontend Angular ajouté dans le repo
- migration progressive des écrans depuis Thymeleaf vers Angular
- endpoints REST `/api` exposés pour le frontend Angular

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

## Parcours métier encore disponibles côté backend

- Parent:
  - `/parent/dashboard`
  - `/parent/enfants`
  - `/parent/activities`
  - `/parent/planning`
  - `/parent/notifications`
  - `/settings`
- Admin:
  - `/admin/adminDashboard`
  - `/admin/activities`
  - `/admin/animations`
  - `/admin/animateurs`
  - `/admin/parents`
  - `/admin/demandes`
  - `/admin/notifications`
- Animateur:
  - `/animateur/dashboard`
  - `/animateur/notifications`
  - `/animateur/inscriptions`

## Références

- backend schema SQL: [backend/SQL/schema_sprint1.sql](/home/ahmed/Desktop/Web/Projet-Session/projet-de-session/backend/SQL/schema_sprint1.sql)
- docs: [docs](/home/ahmed/Desktop/Web/Projet-Session/projet-de-session/docs)
