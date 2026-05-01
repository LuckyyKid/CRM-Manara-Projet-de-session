# Guide de deploiement

Ce guide documente le deploiement observe dans le projet : frontend Angular sur Netlify, backend Spring Boot sur Render, base PostgreSQL compatible Supabase.

## Frontend Netlify

URL frontend officielle :

```text
https://manaracrm.netlify.app
```

### Build

Depuis `frontend/` :

```bash
npm install
npm run build
```

Sortie Angular actuelle : `frontend/dist/frontend`.

### Configuration SPA

Le fichier `frontend/src/_redirects` contient :

```text
/* /index.html 200
```

Cette redirection est necessaire pour que les routes Angular fonctionnent lors d'un refresh.

### URLs backend

En production, `frontend/src/environments/environment.prod.ts` contient :

```ts
apiUrl: 'https://crm-manara-projet-de-session.onrender.com'
wsUrl: 'wss://crm-manara-projet-de-session.onrender.com/ws/realtime'
```

Si l'URL Render change, mettre a jour ce fichier puis redeployer Netlify.

### Parametres Netlify

À compléter selon le compte Netlify :

- site name;
- branche de production;
- commande de build;
- dossier publie.

Valeurs probables :

- build command : `npm run build`
- publish directory : `frontend/dist/frontend`

## Backend Render

URL backend utilisee par le frontend :

```text
https://crm-manara-projet-de-session.onrender.com
```

### Configuration Render

Configuration officielle actuelle :

| Parametre | Valeur |
|---|---|
| Nom du service | `CRM-Manara-Projet-de-session` |
| Region | Oregon (US West) |
| Instance type | Free, 0.1 CPU, 512 MB |
| Repository | `https://github.com/LuckyyKid/CRM-Manara-Projet-de-session` |
| Branche deployee | `main` |
| Root Directory | `backend` |
| Dockerfile Path | `backend/Dockerfile` |
| Docker Build Context Directory | `backend/` |
| Registry Credential | No credential |

Le backend est deploye sur le plan gratuit Render. Apres une periode d'inactivite, le service peut subir un cold start : la premiere requete peut donc etre lente pendant que l'instance redemarre. Pour une demonstration officielle, un plan payant ou un prechauffage manuel du backend avant la presentation peut ameliorer la stabilite.

### Build

Depuis `backend/` :

```bash
./mvnw package
```

Sur Windows :

```powershell
.\mvnw.cmd package
```

Le projet est package en `war`, mais Spring Boot peut etre lance avec le plugin Maven ou via artefact selon la configuration Render.

### Commande de demarrage

À compléter selon Render.

Exemples possibles :

```bash
./mvnw spring-boot:run
```

ou execution de l'artefact genere.

## Base Supabase/PostgreSQL

Configurer dans Render :

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:PORT/DATABASE
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
```

Supabase fournit souvent une URL PostgreSQL standard. Verifier :

- SSL requis ou non;
- pooler Supabase utilise ou non;
- limite de connexions du plan.

## Variables d'environnement backend

### Obligatoires pour production

```bash
SPRING_DATASOURCE_URL=...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
FRONTEND_BASE_URL=https://manaracrm.netlify.app
CORS_ALLOWED_ORIGINS=https://manaracrm.netlify.app,https://*.netlify.app
APP_JWT_SECRET=valeur-longue-et-secrete
```

### Paiement Stripe

```bash
STRIPE_SECRET_KEY=...
STRIPE_WEBHOOK_SECRET=...
STRIPE_FIRST_CHILD_PRICE_ID=...
STRIPE_ADDITIONAL_CHILD_PRICE_ID=...
STRIPE_FIRST_CHILD_MONTHLY_AMOUNT_CENTS=6000
STRIPE_ADDITIONAL_CHILD_MONTHLY_AMOUNT_CENTS=4000
```

### OAuth Google

```bash
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
```

Callback cote Google :

```text
https://crm-manara-projet-de-session.onrender.com/login/oauth2/code/google
```

À adapter si l'URL Render change.

### IA Anthropic

```bash
ANTHROPIC_API_KEY=...
ANTHROPIC_MODEL=claude-sonnet-4-6
```

### Email

Resend :

```bash
RESEND_API_KEY=...
RESEND_FROM_EMAIL=...
DEMO_COPY_EMAIL=...
```

SMTP :

```bash
SPRING_MAIL_HOST=...
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=...
SPRING_MAIL_PASSWORD=...
```

## Configuration CORS

Le backend lit :

```bash
FRONTEND_BASE_URL
CORS_ALLOWED_ORIGINS
```

Ces valeurs sont utilisees par :

- `SecurityConfig` pour `/api/**`, `/oauth2/**`, `/login/oauth2/**`, `/ws/**`;
- `WebSocketConfig` pour `/ws/realtime`.

Ajouter toute URL Netlify de preview ou domaine custom necessaire dans `CORS_ALLOWED_ORIGINS`.

## Authentification, cookies et tokens

L'API Angular utilise principalement JWT :

- token recu par `/api/auth/login` ou OAuth;
- stockage frontend dans `localStorage` sous `auth_token`;
- envoi via header `Authorization: Bearer ...`.

Le backend configure aussi les cookies de session :

```bash
SERVER_SERVLET_SESSION_COOKIE_SAME_SITE=None
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
SERVER_SERVLET_SESSION_COOKIE_HTTP_ONLY=true
SERVER_SERVLET_SESSION_COOKIE_PARTITIONED=true
```

Le logout frontend nettoie localement le token et appelle `/api/logout`.

## Stripe webhook

En local :

```bash
stripe listen --forward-to localhost:8080/api/stripe/webhook
```

En production, configurer dans Stripe :

```text
https://crm-manara-projet-de-session.onrender.com/api/stripe/webhook
```

Puis mettre le secret dans `STRIPE_WEBHOOK_SECRET`.

Evenements geres par `BillingService` : a valider dans le service avant configuration finale. Le README historique mentionne `invoice.paid`, `invoice.payment_failed`, `customer.subscription.updated`, `customer.subscription.deleted`.

## Probleme connu Render free plan

Le plan gratuit Render peut mettre le backend en veille et provoquer un cold start apres une periode d'inactivite. Consequences :

- premiere requete lente;
- `/api/me` peut etre lent au refresh;
- WebSocket peut se reconnecter;
- les fonctions de presentation peuvent paraitre bloquees pendant le reveil.

Avant une demonstration :

1. Ouvrir l'URL backend Render.
2. Tester `/api/public/activities`.
3. Se connecter au frontend.
4. Verifier que `/api/me` repond.
5. Tester WebSocket en envoyant un message ou en verifiant les compteurs.

## Checklist avant demonstration

- Backend Render reveille.
- Frontend Netlify charge en navigation privee.
- Connexion admin OK.
- Connexion parent OK.
- Connexion animateur OK.
- Bouton deconnexion visible apres refresh.
- Base Supabase accessible.
- Stripe en mode test configure si la demo inclut l'abonnement.
- `ANTHROPIC_API_KEY` configure si la demo inclut quiz/devoirs/plans IA.
- WebSocket `/ws/realtime` accessible.
- CORS correctement configure pour le domaine Netlify.
- Comptes de test disponibles.

## Politique de sauvegarde Supabase

Aucune politique de sauvegarde Supabase officielle n'a encore ete definie pour ce prototype. Avant une utilisation reelle dans un centre, il faudra definir une strategie de sauvegarde reguliere, verifier les options de backup disponibles dans Supabase, determiner une frequence de sauvegarde et designer une personne responsable de la restauration en cas de probleme.

## Confidentialite et consentement parental

L'application peut contenir des donnees associees a des enfants, par exemple les informations de profil, les inscriptions, les activites, les quiz, les devoirs, les plans de pratique maison, les messages ou les notifications lies au suivi de l'enfant.

Comme ces donnees peuvent etre sensibles, une politique officielle de confidentialite et de consentement parental devra etre redigee et validee avant toute utilisation reelle dans un centre. Le projet doit actuellement etre considere comme un prototype scolaire / prototype fonctionnel, et non comme une solution legalement prete pour une utilisation en production avec des donnees reelles d'enfants.

Avant une vraie production :

- le consentement parental devrait etre requis avant la collecte ou l'utilisation de donnees d'enfants;
- une procedure de suppression ou de modification des donnees devrait etre prevue sur demande;
- les acces aux donnees doivent etre limites selon les roles : parent, admin, animateur/intervenant;
- les obligations legales applicables au contexte du centre doivent etre validees.

## Verification production

1. Ouvrir `https://manaracrm.netlify.app`.
2. Ouvrir la console reseau du navigateur.
3. Verifier que les appels `/api/public/activities` repondent 200.
4. Se connecter avec un compte de test.
5. Verifier `/api/me`.
6. Naviguer dans le dashboard du role.
7. Tester logout.
8. Tester refresh apres connexion.
9. Tester une action metier simple : notification, inscription ou message.

## Depannage frequent

### Erreur CORS

Verifier `CORS_ALLOWED_ORIGINS` et `FRONTEND_BASE_URL` sur Render. Inclure le domaine Netlify exact.

### 401 apres connexion

Verifier :

- presence du token `auth_token`;
- `APP_JWT_SECRET` stable entre redemarrages;
- expiration `APP_JWT_EXPIRATION_MS`;
- role utilisateur en base.

### Google OAuth ne revient pas au frontend

Verifier :

- callback Google;
- `FRONTEND_BASE_URL`;
- `GOOGLE_CLIENT_ID`;
- `GOOGLE_CLIENT_SECRET`.

### Stripe ne met pas l'abonnement a jour

Verifier :

- endpoint webhook Stripe;
- `STRIPE_WEBHOOK_SECRET`;
- price IDs;
- logs Render de `BillingService`.

### WebSocket ne se connecte pas

Verifier :

- `wsUrl` frontend;
- CORS WebSocket;
- backend reveille;
- proxy local si developpement.

### IA indisponible

Verifier :

- `ANTHROPIC_API_KEY`;
- `ANTHROPIC_MODEL`;
- quotas Anthropic;
- logs des services Anthropic.
