# CRM Manara

## Version 1

La version 1 du projet est l'application web Spring Boot MVC complète livrée pour les sprints 1 et 2.

Cette version est maintenant figée dans la branche:

- `version-1`

Elle contient:

- authentification sécurisée avec Spring Security
- rôles `ADMIN`, `PARENT`, `ANIMATEUR`
- validation serveur des formulaires
- interactions Ajax sur plusieurs écrans
- demandes d'inscription parent avec approbation admin
- comptes parent en attente d'approbation admin
- profils enfant en attente d'approbation admin
- suivi des présences côté animateur
- notifications admin, parent et animateur
- courriels métier via Resend
- suivi du remplissage et de la liste d'attente
- tableaux de bord admin, parent et animateur
- paramètres de compte avec avatar persistant
- page d'erreur utilisateur dédiée

## Version 2

La V2 avance sur des branches dédiées de migration Angular.

En ce moment:

- `main` reste la base d'intégration de la V2
- `ahmed-angular` porte le chantier Angular + API en cours

Direction retenue pour la V2:

- backend Spring Boot conservé comme base métier
- frontend Angular pour remplacer progressivement les vues Thymeleaf
- endpoints REST `/api` exposés pour Angular
- migration progressive écran par écran

## Structure actuelle

- `src/main/java/...` : backend Spring Boot
- `src/main/resources/...` : vues Thymeleaf V1 encore présentes
- `frontend/` : nouveau frontend Angular V2

## Lancement local backend

```bash
./mvnw spring-boot:run
```

Application locale:

- `http://localhost:8080`

## Lancement local frontend Angular

```bash
cd frontend
npm install
npm start
```

Application locale Angular:

- `http://localhost:4200`

Le proxy Angular redirige `/api` vers le backend Spring sur `http://localhost:8080`.

## Tests backend

```bash
./mvnw test
```

## Parcours actuels disponibles côté backend V1

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

## Base de données

- MariaDB/MySQL pour l'environnement local
- H2 pour les tests

Le script SQL de référence est dans [SQL/schema_sprint1.sql](/home/ahmed/Desktop/Web/Projet-Session/projet-de-session/SQL/schema_sprint1.sql).
