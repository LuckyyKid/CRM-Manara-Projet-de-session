# CRM Manara

Application Spring Boot MVC pour le module CRM du Centre Manara.

## Sprint 2

Le MVP Sprint 2 couvre:

- authentification sécurisée avec Spring Security
- rôles `ADMIN`, `PARENT`, `ANIMATEUR`
- validation serveur des formulaires
- interactions Ajax sur plusieurs écrans
- demandes d'inscription parent avec approbation admin
- suivi des présences côté animateur
- notifications parent et reçus email
- suivi du remplissage et de la liste d'attente
- tableaux de bord admin, parent et animateur

## Lancement

```bash
./mvnw spring-boot:run
```

Application locale:

- `http://localhost:8080`

## Vérification

```bash
./mvnw test
```

## Parcours principaux

- Parent:
  - `/parent/dashboard`
  - `/parent/enfants`
  - `/parent/activities`
  - `/parent/planning`
  - `/parent/notifications`
- Admin:
  - `/admin/adminDashboard`
  - `/admin/activities`
  - `/admin/animations`
  - `/admin/animateurs`
  - `/admin/parents`
  - `/admin/inscriptions`
- Animateur:
  - `/animateur/dashboard`
  - `/animateur/inscriptions`

## Base de données

- MariaDB/MySQL pour l'environnement local
- H2 pour les tests

Le script SQL de référence est dans [SQL/schema_sprint1.sql](/home/ahmed/Desktop/Web/Projet-Session/projet-de-session/SQL/schema_sprint1.sql), mis à jour pour refléter l'état Sprint 2 du schéma métier.
