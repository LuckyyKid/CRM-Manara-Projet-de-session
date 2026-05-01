# Documentation technique

## Vue d'ensemble

Manara CRM est compose de trois blocs :

```text
Frontend Angular
  -> API REST Spring Boot
  -> Supabase/PostgreSQL
```

Le frontend est une SPA Angular. Le backend expose une API REST securisee par JWT et un endpoint WebSocket pour les notifications et compteurs temps reel. La base de donnees est geree via JPA/Hibernate et PostgreSQL.

## Structure frontend reelle

```text
frontend/src/app/
├── app.config.ts
├── app.routes.ts
├── app.html
├── app.ts
├── core/
│   ├── auth/
│   ├── http/
│   ├── models/
│   ├── services/
│   └── utils/
├── pages/
│   ├── about/
│   ├── admin/
│   ├── animateur/
│   ├── dashboard/
│   ├── home/
│   ├── login/
│   ├── messages/
│   ├── oauth-success/
│   ├── parent/
│   ├── settings/
│   └── signup/
└── shared/
    ├── availability-calendar/
    ├── chatbot/
    ├── list-page/
    └── pagination/
```

### Routing Angular

Les routes sont definies dans `frontend/src/app/app.routes.ts`.

Routes publiques :

- `/home`
- `/activity-finder`
- `/activities/:id`
- `/about`
- `/signup`
- `/login`
- `/oauth-success`

Routes protegees :

- `/me/dashboard`
- `/settings`
- `/admin/**`
- `/parent/**`
- `/animateur/**`

Les guards sont :

- `authGuard`
- `guestGuard`
- `adminGuard`
- `parentGuard`
- `animateurGuard`

### Services Angular principaux

- `AuthService` : login, OAuth, restauration de session, token JWT, logout.
- `AdminService` : activites, animations, animateurs, parents, enfants, demandes, notifications, abonnements.
- `ParentService` : enfants, inscriptions, activites, notifications, quiz, devoirs, plans sport.
- `AnimateurService` : animations, inscriptions, presences, notifications, quiz, devoirs, plans sport.
- `CommunicationService` : contacts, conversations, messages, compteurs sidebar, WebSocket, rendez-vous.
- `BillingService` : abonnement parent, checkout Stripe, enfants couverts.
- `SignupService` : inscription et disponibilite email.
- `ChatbotService` : widget chatbot.
- `ActivityRecommendationsService` : recommandations publiques d'activites.
- `OnboardingService` : parcours Intro.js.
- `SettingsService` : formulaire settings et avatar.

### Interceptors Angular

- `apiBaseInterceptor` ajoute `environment.apiUrl` aux URLs commencant par `/api`, `/oauth2`, `/login/oauth2` et `/logout`.
- `authInterceptor` ajoute `Authorization: Bearer <token>` si `auth_token` existe.
- `apiDebugInterceptor` journalise les requetes/reponses API.

### Environnements Angular

- `environment.ts` : API relative en local, WebSocket `/ws/realtime`.
- `environment.prod.ts` : backend Render et WebSocket Render.

## Structure backend reelle

```text
backend/src/main/java/CRM_Manara/CRM_Manara/
├── Controller/
├── Model/Entity/
│   └── Enum/
├── Repository/
├── dto/
├── service/
└── config/
```

### Controllers

Controllers REST principaux :

- `ApiAuthController`
- `ApiSignUpController`
- `ApiMeController`
- `ApiAdminController`
- `ApiParentController`
- `ApiAnimateurController`
- `ApiCommunicationController`
- `ApiParentBillingController`
- `ApiQuizController`
- `ApiAnimateurHomeworkController`
- `ApiAnimateurSportPracticePlanController`
- `ApiPublicController`
- `ChatbotController`
- `StripeWebhookController`

Controllers Thymeleaf ou legacy :

- `adminController`
- `parentController`
- `animateurController`
- `authController`
- `SignUpController`
- `SettingsController` sous profil `thymeleaf`
- `indexController`
- `AngularRedirectController`
- `AvatarController`
- `AppErrorController`

Le frontend Angular utilise principalement les controllers REST.

### Services backend principaux

- `AdminService`
- `parentService`
- `AnimateurService`
- `CurrentUserService`
- `JwtService`
- `BillingService`
- `ChatService`
- `AppointmentSlotService`
- `SidebarCountsService`
- `RealtimeService`
- `ParentQuizService`
- `QuizService`
- `HomeworkService`
- `SportPracticePlanService`
- `ActivityRecommendationService`
- `ChatbotService`
- `EmailService`
- `AvatarService`
- `AccountSettingsService`
- Services Anthropic : generation et scoring de quiz, devoirs, plans sport.

### Repositories

Repositories Spring Data JPA :

- `ActivityRepo`
- `AdminRepo`
- `AdminNotificationRepo`
- `AnimateurRepo`
- `AnimateurNotificationRepo`
- `AnimationRepo`
- `AppointmentSlotRepo`
- `BookingRepo`
- `ChatConversationRepo`
- `ChatMessageRepo`
- `EnfantRepo`
- `HomeworkAssignmentRepo`
- `HomeworkAttemptRepo`
- `InscriptionRepo`
- `ParentRepo`
- `ParentNotificationRepo`
- `ParentSubscriptionRepo`
- `ParentSubscriptionChildRepo`
- `QuizRepo`
- `QuizAttemptRepo`
- `SportPracticePlanRepo`
- `UserRepo`
- `VerificationTokenRepository`

### DTOs

Le dossier `dto/` contient des records et classes pour isoler les payloads API : `ActivityDto`, `AnimationDto`, `CurrentUserDto`, `QuizDto`, `HomeworkDto`, `SubscriptionDto`, `ChatMessageDto`, etc. Le frontend reprend une grande partie de ces contrats dans `frontend/src/app/core/models/api.models.ts`.

## Securite et authentification

La securite est configuree dans `SecurityConfig`.

### API stateless

Le chain `apiSecurityFilterChain` s'applique a `/api/**` :

- session `STATELESS`;
- CSRF desactive pour l'API;
- JWT via `JwtAuthenticationFilter`;
- `/api/auth/**`, `/api/signUp/**`, `/api/chatbot/**`, `/api/public/**` et `/api/stripe/webhook` sont publics;
- `/api/me` demande une authentification;
- `/api/admin/**` demande `ROLE_ADMIN`;
- `/api/parent/**` demande `ROLE_PARENT`;
- `/api/animateur/**` demande `ROLE_ANIMATEUR`;
- les autres routes `/api/**` demandent une authentification.

### JWT

Le login `/api/auth/login` retourne :

- `token`;
- `currentUser`.

Le frontend stocke le token dans `localStorage` sous `auth_token`.

### OAuth Google

Le backend configure OAuth2 Google. Le success handler `CustomAuthenticationSuccessHandler` redirige vers le frontend avec un token sur `/oauth-success?token=...`.

### Cookies

La configuration contient aussi des parametres de cookie session :

- `SameSite=None`;
- `Secure=true`;
- `HttpOnly=true`;
- `Partitioned=true`.

L'API Angular repose surtout sur JWT, mais le logout appelle aussi `/api/logout`.

## Communication frontend/backend

### REST

Les services Angular appellent les endpoints `/api/**`. En local, le proxy Angular pointe vers `http://localhost:8080`.

### WebSocket

Le backend expose `/ws/realtime`. Le frontend se connecte via `CommunicationService`.

Evenements connus :

- `sidebar-counts`;
- messages/notifications selon les payloads envoyes par `RealtimeService`, `ChatService` et les services de notifications.

### Paiement Stripe

Le parent lance un checkout via `/api/parent/billing/checkout`. Stripe appelle ensuite `/api/stripe/webhook`. Le webhook est responsable de mettre a jour les abonnements.

### IA Anthropic

Services concernes :

- `ActivityRecommendationService`;
- `AnthropicQuizGenerationService`;
- `AnthropicQuizScoringService`;
- `AnthropicHomeworkGenerationService`;
- `AnthropicHomeworkScoringService`;
- `AnthropicSportPracticePlanGenerationService`.

Si la cle est absente ou si l'appel echoue, certains services prevoient un fallback local ou une journalisation d'indisponibilite. Le comportement exact varie selon le service.

## Gestion des erreurs

Le backend contient `GlobalExceptionHandler`, qui traite notamment :

- erreurs d'integrite de donnees;
- `IllegalArgumentException`;
- `IllegalStateException`;
- `ResponseStatusException`;
- exceptions generales.

Le frontend utilise des signaux Angular pour afficher `loading`, `error`, `message` selon les pages. Les services utilisent aussi `shareReplay(1)` pour mettre en cache certaines listes et exposent des methodes `clearCache`.

## Points techniques importants

- `spring.thymeleaf.enabled=false` dans `application.properties`, mais les templates et controllers Thymeleaf existent encore.
- Le projet contient des controllers web historiques et une SPA Angular moderne.
- Le WebSocket n'est pas STOMP : il utilise un `TextWebSocketHandler` et des messages JSON.
- Les routes Angular et backend ne sont pas toutes 1:1; le frontend consomme surtout `/api/**`.
- Les donnees sensibles enfants/parents imposent une attention forte en production.
- Les modules tutorat/sport sont conditionnels selon le type d'activite et les droits calcules.
