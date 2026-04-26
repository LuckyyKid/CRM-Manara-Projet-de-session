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

## Abonnement mensuel Stripe

Le paiement parent utilise Stripe Checkout en mode `subscription`. Un forfait couvre un enfant a 60$/mois; chaque enfant additionnel ajoute une place mensuelle a 40$/mois. Le backend reste la source de verite: le retour `success_url` ne valide pas le paiement, seul le webhook Stripe met l'abonnement en `ACTIVE`.

### Variables locales

Ajouter les valeurs en variables d'environnement ou dans `backend/src/main/resources/application-secret.properties`:

```properties
stripe.secret-key=sk_test_...
stripe.webhook-secret=whsec_...
stripe.first-child-price-id=price_...
stripe.additional-child-price-id=price_...
stripe.first-child-monthly-amount-cents=6000
stripe.additional-child-monthly-amount-cents=4000
```

Les equivalents environnement sont:

```bash
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_FIRST_CHILD_PRICE_ID=price_...
STRIPE_ADDITIONAL_CHILD_PRICE_ID=price_...
STRIPE_FIRST_CHILD_MONTHLY_AMOUNT_CENTS=6000
STRIPE_ADDITIONAL_CHILD_MONTHLY_AMOUNT_CENTS=4000
```

Ne jamais exposer `STRIPE_SECRET_KEY` cote frontend. Angular recoit seulement l'URL Stripe Checkout ou Stripe Customer Portal retournee par le backend.

### Creer les prix mensuels en mode test

1. Ouvrir Stripe Dashboard en mode test.
2. Creer un produit, par exemple `Abonnement mensuel Manara`.
3. Ajouter un prix recurring mensuel a 60$ pour le premier enfant.
4. Ajouter un deuxieme prix recurring mensuel a 40$ pour chaque enfant additionnel.
5. Copier les identifiants `price_...` dans `STRIPE_FIRST_CHILD_PRICE_ID` et `STRIPE_ADDITIONAL_CHILD_PRICE_ID`.

### Webhook local

Installer Stripe CLI, se connecter, puis forwarder les evenements vers le backend:

```bash
stripe login
stripe listen --forward-to localhost:8080/api/stripe/webhook
```

Copier la valeur `whsec_...` affichee par Stripe CLI dans `STRIPE_WEBHOOK_SECRET`, puis relancer le backend.

Evenements geres:

- `checkout.session.completed`
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`
- `invoice.paid`
- `invoice.payment_failed`

### Tester une carte

Dans Stripe Checkout en mode test:

- carte: `4242 4242 4242 4242`
- date: une date future
- CVC: n'importe quelles 3 chiffres

Apres paiement, le parent retourne vers `/parent/billing?success=true`. Si le webhook local tourne, le statut devient `ACTIVE`; sinon il peut rester `CHECKOUT_PENDING`.

### Gestion du forfait actif

Une fois l'abonnement deja actif, le bouton de gestion ouvre Stripe Customer Portal au lieu de recreer un nouveau Checkout. Cela evite les doubles abonnements. Le parent revient ensuite sur `/parent/billing` pour choisir explicitement quels enfants utilisent les places payees.

### Script SQL explicite

Si votre base locale n'applique pas automatiquement les changements JPA, le script suivant cree les tables de facturation:

- [backend/SQL/subscription_billing_upgrade.sql](/home/ahmed/Desktop/Web/Projet-Session/projet-de-session/backend/SQL/subscription_billing_upgrade.sql)

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
