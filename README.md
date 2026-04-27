# CRM Manara

Application de gestion pour centre de loisirs jeunesse. Backend Spring Boot, frontend Angular, paiements Stripe.

---

## Demarrage rapide

### Prerequis

- Java 21+
- Node.js 20+
- PostgreSQL 15+ avec une base `manara`
- (optionnel) Stripe CLI pour tester les paiements

### 1 — Backend

```bash
cd backend
./mvnw spring-boot:run
```

Disponible sur `http://localhost:8080`.

### 2 — Frontend

```bash
cd frontend
npm install
npm start
```

Disponible sur `http://localhost:4200`. Le proxy Angular redirige `/api` vers `http://localhost:8080`.

### 3 — Stripe (paiements)

```bash
stripe listen --forward-to localhost:8080/api/stripe/webhook
```

Laisser ce terminal ouvert pendant les tests de paiement.

---

## Configuration locale

Creer le fichier `backend/src/main/resources/application-secret.properties` (jamais commite) :

```properties
# Base de donnees (si differente des defaults)
spring.datasource.url=jdbc:postgresql://localhost:5432/manara
spring.datasource.username=postgres
spring.datasource.password=TON_MOT_DE_PASSE

# Email (optionnel — les emails echouent silencieusement sans ca)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=ton@gmail.com
spring.mail.password=app_password_gmail

# Stripe
stripe.secret-key=sk_test_...
stripe.webhook-secret=whsec_...
stripe.first-child-price-id=price_...
stripe.additional-child-price-id=price_...
stripe.first-child-monthly-amount-cents=6000
stripe.additional-child-monthly-amount-cents=4000

# Google OAuth (optionnel)
spring.security.oauth2.client.registration.google.client-id=...
spring.security.oauth2.client.registration.google.client-secret=...
```

### Obtenir le webhook-secret Stripe

```bash
stripe listen --forward-to localhost:8080/api/stripe/webhook
```

La ligne `> Ready! Your webhook signing secret is whsec_...` donne la valeur a mettre dans `stripe.webhook-secret`.

---

## Donnees de test

Executer le script seed pour creer des comptes de test (idempotent, peut etre relance) :

```bash
psql -h localhost -U postgres -d manara -f backend/SQL/seed_test_data.sql
```

Comptes crees :

| Role | Email | Mot de passe |
|---|---|---|
| Admin | `admin@manara.test` | `root` |
| Parent (abonnement actif, 2 enfants) | `sarah.martin@test.com` | `root` |
| Parent (sans abonnement) | `pierre.dupont@test.com` | `root` |
| Animateur | `karim.benali@test.com` | `root` |
| Animateur | `fatima.zahra@test.com` | `root` |

---

## Tests

```bash
cd backend
./mvnw test
```

51 tests, 0 echecs.

---

## Structure du repo

```
backend/   Spring Boot — API REST, securite, services, tests Maven
frontend/  Angular — SPA, proxy vers backend
docs/      Diagrammes, maquettes
backend/SQL/
  schema_sprint1.sql          Schema initial
  subscription_billing_upgrade.sql  Tables Stripe (si Hibernate ne les cree pas)
  seed_test_data.sql          Donnees de test idempotentes
```

---

## API REST

| Prefixe | Role requis | Description |
|---|---|---|
| `/api/login` | public | Connexion email/mdp |
| `/api/me` | authentifie | Profil utilisateur courant |
| `/api/parent/**` | ROLE_PARENT | Espace parent |
| `/api/admin/**` | ROLE_ADMIN | Espace admin |
| `/api/animateur/**` | ROLE_ANIMATEUR | Espace animateur |
| `/api/stripe/webhook` | public (signe) | Webhooks Stripe |

---

## Fonctionnalites

### Roles

- **Admin** : gestion des parents, animateurs, activites, animations, inscriptions, abonnements
- **Parent** : tableau de bord, enfants, activites, rendez-vous, messagerie, abonnement Stripe
- **Animateur** : planning, inscriptions, presences, devoirs, messagerie

### Abonnement Stripe

- Forfait mensuel : 60 $/mois pour le premier enfant, +40 $/mois par enfant additionnel
- Flux : Stripe Checkout → webhook `invoice.paid` active l'abonnement (le retour `success_url` ne suffit pas)
- Evenements geres : `invoice.paid`, `invoice.payment_failed`, `customer.subscription.updated`, `customer.subscription.deleted`
- Cartes de test : `4242 4242 4242 4242` (succes), `4000 0000 0000 9995` (echec)
- Une inscription est bloquee si le parent n'a pas d'abonnement actif ou si l'enfant n'est pas couvert

### Messagerie

- Chat en temps reel via SSE entre parents, animateurs et admins
- Notification email avec cooldown de 15 min par destinataire

### Notifications

- Notifications en temps reel (SSE) pour parents et admins
- Types : inscriptions, rendez-vous, messages, abonnement, compte

---

## Variables d'environnement (production)

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/manara
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=...
FRONTEND_BASE_URL=https://ton-domaine.com
CORS_ALLOWED_ORIGINS=https://ton-domaine.com
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_FIRST_CHILD_PRICE_ID=price_...
STRIPE_ADDITIONAL_CHILD_PRICE_ID=price_...
STRIPE_FIRST_CHILD_MONTHLY_AMOUNT_CENTS=6000
STRIPE_ADDITIONAL_CHILD_MONTHLY_AMOUNT_CENTS=4000
SPRING_MAIL_USERNAME=...
SPRING_MAIL_PASSWORD=...
ANTHROPIC_API_KEY=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
```
