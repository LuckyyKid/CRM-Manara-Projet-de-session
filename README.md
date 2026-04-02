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

La suite du projet sera développée sur `main`.

Direction retenue pour la version 2:

- backend Spring Boot conservé comme base métier
- frontend Angular pour remplacer progressivement les vues Thymeleaf côté interface
- adaptations Spring nécessaires pour servir une API plus propre au frontend Angular
- évolution progressive sans perdre les règles métier déjà implantées

## Etat actuel de `main`

`main` sert maintenant de base de travail pour la version 2.

Le backend existant reste utilisable pour:

- l'authentification
- la gestion des rôles
- les parents, enfants, activités, animations, inscriptions et présences
- les notifications et les courriels
- les validations métier principales

## Lancement local

```bash
./mvnw spring-boot:run
```

Application locale:

- `http://localhost:8080`

## Tests

```bash
./mvnw test
```

## Parcours actuels disponibles

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
