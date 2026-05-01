# Manara CRM

Manara CRM est une application de gestion pour un centre d'activites jeunesse. Le projet combine une application frontend Angular, une API Spring Boot et une base PostgreSQL, avec des modules pour les parents, les animateurs et l'administration du centre.

Cette documentation est basee sur les fichiers presents dans le depot. Les informations non confirmees dans le code sont marquees `A completer`.

---

## Objectif du projet

Le projet sert de prototype SaaS pour centraliser la gestion d'un centre jeunesse :

- catalogue d'activites et animations planifiees;
- inscription des enfants par les parents;
- validation administrative des comptes, enfants et inscriptions;
- suivi des presences par les animateurs;
- messagerie et notifications;
- rendez-vous parent-animateur;
- abonnement parent via Stripe;
- outils de tutorat : quiz, correction, devoirs personnalises;
- outils sport : plans de pratique maison;
- accompagnement utilisateur avec Intro.js.

## Contexte d'utilisation

Le projet peut etre utilise comme base pour un hackathon, un projet scolaire avance ou une reprise technique par une equipe chargee de l'adapter a un centre reel. Avant une utilisation en production reelle, il faut completer la validation metier, la securite operationnelle, les sauvegardes et la gouvernance des donnees.

## Technologies utilisees

### Frontend

- Angular 21
- TypeScript 5.9
- RxJS
- Intro.js 8.3 pour l'onboarding guide
- WebSocket natif pour les notifications et compteurs temps reel
- Netlify pour le deploiement indique par la configuration actuelle

### Backend

- Java 17 selon `backend/pom.xml`
- Spring Boot 4.0.2
- Spring Web MVC
- Spring Security
- JWT pour l'API Angular
- OAuth2 Google
- Spring Data JPA / Hibernate
- PostgreSQL
- WebSocket Spring
- Stripe Java SDK 29.5.0
- Resend et SMTP pour les emails
- Anthropic API pour les recommandations, quiz, devoirs et plans de pratique

### Base de donnees

- PostgreSQL en local ou heberge via Supabase/PostgreSQL
- Scripts SQL disponibles dans `backend/SQL/`
- Hibernate configure en `ddl-auto=update` par defaut

## Architecture generale

```text
Navigateur
  |
  | Angular SPA
  | Netlify
  v
Spring Boot API REST + WebSocket
  |
  | Render
  v
Supabase / PostgreSQL
```

Le frontend appelle les routes `/api/**`, `/oauth2/**`, `/logout`, `/avatars/**` et `/ws/realtime`. En production, `frontend/src/environments/environment.prod.ts` pointe vers :

- Frontend Netlify : `https://manaracrm.netlify.app`
- API : `https://crm-manara-projet-de-session.onrender.com`
- WebSocket : `wss://crm-manara-projet-de-session.onrender.com/ws/realtime`

Le backend autorise par defaut les origines :

- `https://manaracrm.netlify.app`
- `https://crm-manara-projet-de-session.vercel.app`
- `https://*.netlify.app`

## Fonctionnalites principales presentes

### Public

- Page d'accueil.
- FAQ.
- Catalogue public d'activites.
- Recherche/recommandation d'activites via `/api/public/activity-recommendations`.
- Chatbot FAQ via `/api/chatbot/init` et `/api/chatbot/message`.
- Inscription parent avec verification de disponibilite de courriel.
- Connexion email/mot de passe.
- Connexion Google OAuth.

### Parent

- Tableau de bord parent.
- Gestion des enfants.
- Consultation des activites et animations.
- Demande d'inscription d'un enfant a une animation.
- Planning.
- Notifications.
- Messagerie.
- Rendez-vous avec animateur.
- Gestion d'abonnement Stripe.
- Selection des enfants couverts par l'abonnement.
- Quiz disponibles selon les inscriptions.
- Soumission de quiz.
- Generation de devoirs depuis un resultat de quiz.
- Liste, reponse et consultation des resultats de devoirs.
- Plans de pratique maison pour les activites sportives.
- Guide d'onboarding Intro.js.

### Animateur

- Tableau de bord animateur.
- Consultation de ses animations.
- Consultation des inscriptions.
- Mise a jour des presences et notes d'incident.
- Notifications.
- Messagerie.
- Disponibilites et rendez-vous.
- Creation et consultation des quiz.
- Tableau de bord de progression tutorat.
- Consultation des soumissions de quiz.
- Suivi des devoirs par enfant.
- Generation de plans de pratique maison pour les activites sportives.

### Administrateur

- Tableau de bord admin.
- CRUD activites.
- CRUD animations.
- CRUD animateurs.
- Consultation parents et enfants.
- Activation/desactivation de comptes parents, enfants et animateurs.
- Validation ou refus des demandes d'inscription.
- Consultation des notifications administratives.
- Consultation des abonnements.
- Consultation des quiz lies a une animation.
- Messagerie.

## Roles utilisateurs

Les roles reels sont definis dans `SecurityRole` :

- `ROLE_ADMIN`
- `ROLE_PARENT`
- `ROLE_ANIMATEUR`

Les guards Angular correspondants sont dans `frontend/src/app/core/auth/auth.guard.ts`.

## Structure du projet

```text
.
├── backend/
│   ├── SQL/
│   ├── src/main/java/CRM_Manara/CRM_Manara/
│   │   ├── Controller/
│   │   ├── Model/Entity/
│   │   ├── Repository/
│   │   ├── dto/
│   │   ├── service/
│   │   └── config/
│   └── src/main/resources/
│       ├── application.properties
│       ├── application-secret.properties.example
│       ├── static/
│       └── templates/
├── frontend/
│   ├── src/app/
│   │   ├── core/
│   │   ├── pages/
│   │   └── shared/
│   ├── src/environments/
│   ├── public/
│   └── proxy.conf.json
└── docs/
    ├── 01-user-guide.md
    ├── 02-technical-documentation.md
    ├── 03-api-documentation.md
    ├── 04-database-documentation.md
    ├── 05-deployment-guide.md
    ├── 06-maintenance-guide.md
    └── 07-known-issues-and-limitations.md
```

## Demarrage rapide

### Prerequis

- Java 17+
- Node.js 20+
- PostgreSQL compatible Supabase ou local
- Maven Wrapper inclus dans `backend/`
- Compte Stripe pour tester les paiements si le module abonnement est utilise
- Cle Anthropic optionnelle pour les fonctions IA

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Sur Windows :

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend local : `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm start
```

Frontend local : `http://localhost:4200`

Le fichier `frontend/proxy.conf.json` redirige notamment `/api`, `/oauth2`, `/logout`, `/avatars`, `/images` et `/ws` vers le backend local.

## Configuration de la base de donnees

En local, creer une base PostgreSQL, par exemple `manara`, puis configurer :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/manara
spring.datasource.username=postgres
spring.datasource.password=TON_MOT_DE_PASSE
```

En production Supabase, utiliser l'URL JDBC fournie par Supabase dans `SPRING_DATASOURCE_URL`.

Scripts disponibles :

- `backend/SQL/schema_sprint1.sql` : ancien schema initial, note comme MySQL/MariaDB dans le fichier.
- `backend/SQL/seed_sprint1.sql` : donnees initiales sprint.
- `backend/SQL/seed_test_data.sql` : donnees de test PostgreSQL.
- `backend/SQL/subscription_billing_upgrade.sql` : tables d'abonnement Stripe.

Le schema exact de production doit etre valide directement dans Supabase.

## Variables d'environnement

Les valeurs sont lues dans `backend/src/main/resources/application.properties`.

### Backend principales

```bash
PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
SPRING_JPA_HIBERNATE_DDL_AUTO=update
FRONTEND_BASE_URL=https://manaracrm.netlify.app
CORS_ALLOWED_ORIGINS=https://manaracrm.netlify.app,https://*.netlify.app
APP_JWT_SECRET=...
APP_JWT_EXPIRATION_MS=86400000
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
ANTHROPIC_API_KEY=...
ANTHROPIC_MODEL=claude-sonnet-4-6
STRIPE_SECRET_KEY=...
STRIPE_WEBHOOK_SECRET=...
STRIPE_MONTHLY_PRICE_ID=...
STRIPE_FIRST_CHILD_PRICE_ID=...
STRIPE_ADDITIONAL_CHILD_PRICE_ID=...
STRIPE_FIRST_CHILD_MONTHLY_AMOUNT_CENTS=6000
STRIPE_ADDITIONAL_CHILD_MONTHLY_AMOUNT_CENTS=4000
RESEND_API_KEY=...
RESEND_FROM_EMAIL=...
DEMO_COPY_EMAIL=...
SPRING_MAIL_HOST=...
SPRING_MAIL_USERNAME=...
SPRING_MAIL_PASSWORD=...
SERVER_SERVLET_SESSION_COOKIE_SAME_SITE=None
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
SERVER_SERVLET_SESSION_COOKIE_HTTP_ONLY=true
SERVER_SERVLET_SESSION_COOKIE_PARTITIONED=true
```

### Frontend

Les URLs de production sont dans `frontend/src/environments/environment.prod.ts` :

```ts
apiUrl: 'https://crm-manara-projet-de-session.onrender.com'
wsUrl: 'wss://crm-manara-projet-de-session.onrender.com/ws/realtime'
```

## Donnees de test

Le fichier `backend/SQL/seed_test_data.sql` documente et cree les comptes suivants :

| Role | Email | Mot de passe |
|---|---|---|
| Admin | `admin@manara.test` | `root` |
| Parent actif | `sarah.martin@test.com` | `root` |
| Parent sans abonnement | `pierre.dupont@test.com` | `root` |
| Animateur | `karim.benali@test.com` | `root` |
| Animatrice | `fatima.zahra@test.com` | `root` |

Commande locale indicative :

```bash
psql -h localhost -U postgres -d manara -f backend/SQL/seed_test_data.sql
```

## Commandes utiles

### Frontend

```bash
cd frontend
npm install
npm start
npm run build
npm test
```

### Backend

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

Sur Windows :

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

### Stripe local

```bash
stripe listen --forward-to localhost:8080/api/stripe/webhook
```

## Tests

### Statut officiel

Tests manuels effectues :

- login;
- logout;
- inscription parent;
- validation admin;
- gestion des enfants;
- activites;
- inscriptions;
- messagerie;
- notifications;
- rendez-vous;
- abonnement Stripe;
- quiz;
- devoirs;
- plans de pratique maison;
- onboarding Intro.js.

Tests automatises :

- Oui, partiellement.
- Il existe des tests backend et un test frontend minimal.
- La suite backend complete ne passe pas actuellement a cause d'un probleme de configuration de test.

Tests manuels :

- Oui.
- Les principaux parcours utilisateurs ont ete testes manuellement pendant le developpement.

Bugs connus / points a corriger :

- Le backend Render peut etre lent au demarrage a cause du cold start du plan gratuit.
- Certains tests automatises backend doivent etre corriges.
- Quelques textes avec accents peuvent encore etre mal encodes.
- La configuration exacte Stripe, OAuth et IA doit etre verifiee avant une vraie mise en production.

## Documentation detaillee

- [Guide utilisateur](docs/01-user-guide.md)
- [Documentation technique](docs/02-technical-documentation.md)
- [Documentation API](docs/03-api-documentation.md)
- [Documentation base de donnees](docs/04-database-documentation.md)
- [Guide de deploiement](docs/05-deployment-guide.md)
- [Guide de maintenance](docs/06-maintenance-guide.md)
- [Problemes connus et limitations](docs/07-known-issues-and-limitations.md)
- [Guide chatbot existant](docs/GUIDE_CHATBOT.md)
- [Diagrammes existants](docs/Diagrammes/)
- [Maquettes existantes](docs/Maquettes/)

## Auteurs / contributeurs

- Belmeddah Ahmed
- Bafing Keita
- Francois Paul-Ryan
- Ariel Wilkins Saintil

## À completer

- URL Render finale si differente de `https://crm-manara-projet-de-session.onrender.com`.
- Procedure officielle de rotation des secrets.
