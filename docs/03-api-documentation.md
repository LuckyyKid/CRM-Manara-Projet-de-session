# Documentation API

Cette documentation liste les endpoints backend identifies dans les controllers Spring. Les types de reponse sont deduits des signatures Java et des DTOs.

## Regles d'acces globales

Selon `SecurityConfig` :

- Public : `/api/auth/**`, `/api/signUp/**`, `/api/chatbot/**`, `/api/public/**`, `/api/stripe/webhook`.
- Authentifie : `/api/me`, `/api/communication/**`, `/api/logout` et autres routes `/api/**` non publiques.
- Admin : `/api/admin/**`.
- Parent : `/api/parent/**`.
- Animateur : `/api/animateur/**`.

## Authentification

| Methode | URL | Controller | Acces | Description | Body | Reponse |
|---|---|---|---|---|---|---|
| POST | `/api/auth/login` | `ApiAuthController` | Public | Connexion email/mot de passe | `LoginRequestDto { email, password }` | `AuthResponseDto { token, currentUser }` |
| POST | `/api/logout` | `SecurityConfig` | Authentifie si token/cookie | Logout backend | Aucun body requis | JSON `{ "success": true }` si backend repond |
| GET | `/api/me` | `ApiMeController` | Authentifie | Profil utilisateur courant | Aucun | `CurrentUserDto` |
| POST | `/api/me/avatar` | `ApiMeController` | Authentifie | Mise a jour avatar | multipart `avatarFile` | Map `{ success, message, avatarUrl?, errors? }` |

## Inscription

| Methode | URL | Controller | Acces | Description | Params / Body | Reponse |
|---|---|---|---|---|---|---|
| GET | `/api/signUp/email-availability` | `ApiSignUpController` | Public | Verifie si un email est disponible | query `email` | Map `{ available, message }` |
| POST | `/api/signUp` | `ApiSignUpController` | Public | Cree un compte parent en attente | `{ nom, prenom, adresse, email, password }` | `ActionResponseDto` |

## Public

| Methode | URL | Controller | Acces | Description | Body | Reponse |
|---|---|---|---|---|---|
| GET | `/api/public/activities` | `ApiPublicController` | Public | Liste les activites | Aucun | `List<ActivityDto>` |
| POST | `/api/public/activity-recommendations` | `ApiPublicController` | Public | Recommande des activites selon age/profil/objectif | `ActivityRecommendationRequestDto` | `RecommendationResponse` |
| GET | `/api/chatbot/init` | `ChatbotController` | Public | Message de bienvenue et suggestions | Aucun | Map |
| POST | `/api/chatbot/message` | `ChatbotController` | Public | Reponse chatbot | `{ message }` | Map `{ reponse, suggestions, success }` |

## Admin

Prefixe : `/api/admin`. Acces : `ROLE_ADMIN`.

| Methode | URL | Description | Params / Body | Reponse |
|---|---|---|---|---|
| GET | `/activities` | Liste les activites | Aucun | `List<ActivityDto>` |
| GET | `/activities/{id}` | Detail activite | path `id` | `ActivityDto` |
| POST | `/activities` | Cree une activite | `ActivityRequestDto` | `ActivityDto` |
| PUT | `/activities/{id}` | Modifie une activite | path `id`, `ActivityRequestDto` | `ActivityDto` |
| DELETE | `/activities/{id}` | Supprime une activite | path `id` | `ActionResponseDto` |
| GET | `/animations` | Liste animations avec capacite | Aucun | `List<AdminAnimationRowDto>` |
| GET | `/animations/{id}` | Detail animation | path `id` | `AnimationDto` |
| GET | `/animations/{id}/quizzes` | Quiz lies a une animation | path `id` | `List<QuizDto>` |
| POST | `/animations` | Cree une animation | `AnimationRequestDto` | `AnimationDto` |
| PUT | `/animations/{id}` | Modifie une animation | path `id`, `AnimationRequestDto` | `AnimationDto` |
| DELETE | `/animations/{id}` | Supprime une animation | path `id` | `ActionResponseDto` |
| GET | `/animateurs` | Liste animateurs | Aucun | `List<AnimateurDto>` |
| GET | `/animateurs/{id}` | Detail animateur | path `id` | `AnimateurDto` |
| POST | `/animateurs` | Cree un animateur | `AnimateurRequestDto` | `AnimateurDto` |
| PUT | `/animateurs/{id}` | Modifie un animateur | path `id`, `AnimateurRequestDto` | `AnimateurDto` |
| DELETE | `/animateurs/{id}` | Supprime un animateur | path `id` | `ActionResponseDto` |
| GET | `/parents` | Liste parents | Aucun | `List<ParentDto>` |
| GET | `/enfants` | Liste enfants | Aucun | `List<EnfantDto>` |
| GET | `/subscriptions` | Liste abonnements parents | Aucun | `List<AdminSubscriptionRowDto>` |
| GET | `/options` | Valeurs enums pour formulaires | Aucun | `AdminOptionsDto` |
| GET | `/demandes` | Demandes en attente/traitees | Aucun | `AdminDemandesDto` |
| GET | `/inscriptions` | Recherche inscriptions | query `animateurId`, `activityId`, `animationId`, `parentId`, `enfantId`, `status`, `search` | `List<AdminInscriptionReviewDto>` |
| GET | `/notifications` | Notifications admin | Aucun | `List<AdminNotificationDto>` |
| POST | `/parents/{id}/status` | Active/desactive parent | path `id`, query `enabled` | `ActionResponseDto` |
| DELETE | `/parents/{id}` | Supprime parent | path `id` | `ActionResponseDto` |
| POST | `/enfants/{id}/status` | Active/desactive enfant | path `id`, query `active` | `ActionResponseDto` |
| DELETE | `/enfants/{id}` | Supprime enfant | path `id` | `ActionResponseDto` |
| POST | `/animateurs/{id}/status` | Active/desactive animateur | path `id`, query `enabled` | `ActionResponseDto` |
| POST | `/inscriptions/{id}/approve` | Approuve inscription | path `id` | `ActionResponseDto` |
| POST | `/inscriptions/{id}/reject` | Refuse inscription | path `id` | `ActionResponseDto` |

## Parent

Prefixe : `/api/parent`. Acces : `ROLE_PARENT`.

| Methode | URL | Description | Params / Body | Reponse |
|---|---|---|---|---|
| GET | `/enfants` | Liste enfants du parent | Aucun | `List<EnfantDto>` |
| GET | `/enfants/{id}` | Detail enfant du parent | path `id` | `EnfantDto` |
| POST | `/enfants` | Ajoute un enfant | Map `{ nom, prenom, dateDeNaissance }` | `ActionResponseDto` |
| PUT | `/enfants/{id}` | Modifie un enfant | path `id`, Map `{ nom, prenom, dateDeNaissance }` | `ActionResponseDto` |
| GET | `/inscriptions` | Liste inscriptions du parent | Aucun | `List<InscriptionDto>` |
| POST | `/inscriptions` | Demande inscription | `InscriptionRequestDto { enfantId, animationId }` | `ActionResponseDto` |
| GET | `/activities` | Activites, animations et enfants admissibles | Aucun | `ParentActivitiesResponseDto` |
| GET | `/notifications` | Notifications parent | Aucun | `List<ParentNotificationDto>` |
| POST/PUT | `/notifications/read-all` | Marque toutes les notifications lues | Aucun | `ActionResponseDto` |
| POST/PUT | `/notifications/{id}/read` | Marque une notification lue | path `id` | `ActionResponseDto` |
| GET | `/quizzes` | Quiz disponibles | Aucun | `List<ParentQuizDto>` |
| GET | `/quizzes/{id}` | Detail quiz disponible | path `id` | `ParentQuizDto` |
| POST | `/quizzes/{id}/attempts` | Soumet un quiz | path `id`, `QuizAttemptSubmitDto` | `QuizAttemptDto` |
| GET | `/quiz-attempts` | Liste tentatives quiz | Aucun | `List<QuizAttemptDto>` |
| GET | `/quiz-attempts/{id}` | Detail tentative quiz | path `id` | `ParentQuizAttemptDetailDto` |
| POST | `/quiz-attempts/{id}/generate-homework` | Genere devoir depuis tentative | path `id` | `HomeworkDto` |
| GET | `/homeworks` | Liste devoirs | Aucun | `List<HomeworkDto>` |
| GET | `/homeworks/{id}` | Detail devoir | path `id` | `HomeworkDto` |
| POST | `/homeworks/{id}/attempts` | Soumet devoir | path `id`, `HomeworkAttemptSubmitDto` | `HomeworkAttemptDto` |
| GET | `/homework-attempts` | Liste tentatives devoir | Aucun | `List<HomeworkAttemptDto>` |
| GET | `/homework-attempts/{id}` | Detail tentative devoir | path `id` | `HomeworkAttemptDto` |
| GET | `/sport-practice-plans` | Plans de pratique | Aucun | `List<SportPracticePlanDto>` |
| GET | `/sport-practice-plans/{id}` | Detail plan de pratique | path `id` | `SportPracticePlanDto` |

## Abonnement parent

Prefixe : `/api/parent/billing`. Acces : `ROLE_PARENT`.

| Methode | URL | Description | Body | Reponse |
|---|---|---|---|---|
| GET | `/subscription` | Etat abonnement | Aucun | `SubscriptionDto` |
| POST | `/checkout` | Cree une session Stripe Checkout | `CheckoutSessionRequestDto { coveredChildrenCount }` optionnel | `CheckoutSessionDto { url }` |
| GET | `/covered-children` | Liste couverture enfants | Aucun | `List<BillingChildCoverageDto>` |
| PUT | `/covered-children` | Met a jour enfants couverts | `UpdateCoveredChildrenRequestDto { enfantIds }` | `List<BillingChildCoverageDto>` |

## Animateur

Prefixe : `/api/animateur`. Acces : `ROLE_ANIMATEUR`.

| Methode | URL | Description | Params / Body | Reponse |
|---|---|---|---|---|
| GET | `/animations` | Animations de l'animateur | Aucun | `List<AnimationDto>` |
| GET | `/notifications` | Notifications animateur | Aucun | `List<AnimateurNotificationDto>` |
| POST/PUT | `/notifications/read-all` | Marque toutes les notifications lues | Aucun | `ActionResponseDto` |
| POST/PUT | `/notifications/{id}/read` | Marque une notification lue | path `id` | `ActionResponseDto` |
| GET | `/animations/{id}/inscriptions` | Inscriptions d'une animation | path `id` | `List<InscriptionDto>` |
| GET | `/inscriptions` | Inscriptions de l'animateur | query `animationId`, `search` | `List<InscriptionDto>` |
| POST | `/inscriptions/{id}/presence` | Met a jour presence | path `id`, `PresenceUpdateRequestDto` | `ActionResponseDto` |

## Quiz animateur

Prefixe : `/api/animateur/quizzes`. Acces : `ROLE_ANIMATEUR`.

| Methode | URL | Description | Body | Reponse |
|---|---|---|---|---|
| GET | `/` | Liste quiz de l'animateur | Aucun | `List<QuizDto>` |
| GET | `/dashboard` | Tableau de bord tutorat | Aucun | `TutorDashboardDto` |
| GET | `/submissions` | Soumissions de quiz | Aucun | `List<TutorQuizSubmissionDto>` |
| GET | `/{id}` | Detail quiz | Aucun | `QuizDto` |
| POST | `/` | Cree un quiz | `QuizCreateRequestDto` | `QuizDto` |
| POST | `/backfill-homeworks` | Cree les devoirs manquants | Aucun | `ActionResponseDto` |
| DELETE | `/{id}` | Supprime un quiz | Aucun | 204 |

## Devoirs animateur

Prefixe : `/api/animateur/homeworks`. Acces : `ROLE_ANIMATEUR`.

| Methode | URL | Description | Reponse |
|---|---|---|---|
| GET | `/` | Vue d'ensemble devoirs | `AnimateurHomeworkOverviewDto` |
| GET | `/students/{enfantId}` | Detail devoirs d'un enfant | `AnimateurHomeworkStudentDetailDto` |
| GET | `/{assignmentId}` | Detail d'une assignation | `HomeworkDto` |
| GET | `/{assignmentId}/latest-attempt` | Derniere tentative | `HomeworkAttemptDto` |

## Plans de pratique animateur

Prefixe : `/api/animateur/sport-practice-plans`. Acces : `ROLE_ANIMATEUR`.

| Methode | URL | Description | Body | Reponse |
|---|---|---|---|---|
| GET | `/` | Liste plans | Aucun | `List<SportPracticePlanDto>` |
| GET | `/{id}` | Detail plan | Aucun | `SportPracticePlanDto` |
| POST | `/` | Cree un plan | `SportPracticePlanCreateRequestDto` | `SportPracticePlanDto` |

## Communication, messagerie, rendez-vous

Prefixe : `/api/communication`. Acces : authentifie.

| Methode | URL | Description | Params / Body | Reponse |
|---|---|---|---|---|
| GET | `/contacts` | Contacts disponibles | Aucun | `List<ChatParticipantDto>` |
| GET | `/conversations` | Conversations | Aucun | `List<ChatConversationSummaryDto>` |
| GET | `/conversations/{id}` | Detail conversation | path `id` | `ChatConversationDetailDto` |
| POST | `/conversations/{id}/read` | Marque conversation lue | path `id` | void |
| POST | `/messages` | Envoie message | `SendChatMessageRequestDto` | `ChatMessageDto` |
| GET | `/sidebar-counts` | Compteurs sidebar | Aucun | `SidebarCountsDto` |
| GET | `/appointments/my-slots` | Creneaux propres | Aucun | `List<AppointmentSlotDto>` |
| POST | `/appointments/my-slots` | Cree creneau | `AppointmentSlotCreateDto` | `AppointmentSlotDto` |
| PUT | `/appointments/my-slots/{slotId}` | Modifie creneau | path `slotId`, body | `AppointmentSlotDto` |
| PUT | `/appointments/my-slots/{slotId}/reschedule` | Replanifie creneau reserve | path `slotId`, body | `AppointmentSlotDto` |
| DELETE | `/appointments/my-slots/{slotId}` | Supprime creneau | path `slotId` | void |
| GET | `/appointments/animateur/{animateurUserId}/slots` | Disponibilites animateur | path `animateurUserId` | `List<AppointmentSlotDto>` |
| POST | `/appointments/slots/{slotId}/reserve` | Reserve un creneau | path `slotId` | `AppointmentSlotDto` |
| GET | `/availability/{animateurId}` | Disponibilite vue generique | path `animateurId` | `List<AppointmentSlotDto>` |
| POST | `/availability` | Cree disponibilite | `AppointmentSlotCreateDto` | `AppointmentSlotDto` |
| PUT | `/availability/{id}` | Modifie disponibilite | path `id`, body | `AppointmentSlotDto` |
| DELETE | `/availability/{id}` | Supprime disponibilite | path `id` | void |
| POST | `/booking` | Reserve via `BookingRequestDto` | body | `AppointmentSlotDto` |
| GET | `/booking/animateur/{id}` | Rendez-vous animateur | path `id` | `List<BookingDto>` |
| GET | `/booking/parent/{id}` | Rendez-vous parent | path `id` | `List<BookingDto>` |
| DELETE | `/booking/{id}` | Annule un rendez-vous | path `id` | `BookingDto` |
| POST | `/booking/{id}/reschedule` | Replanifie rendez-vous | path `id`, body | `BookingDto` |
| PUT | `/booking/reschedule/{id}` | Replanifie rendez-vous | path `id`, body | `BookingDto` |

## Stripe

| Methode | URL | Controller | Acces | Description |
|---|---|---|---|---|
| POST | `/api/stripe/webhook` | `StripeWebhookController` | Public, signature Stripe requise | Recoit les evenements Stripe et met a jour les abonnements |

Header requis : `Stripe-Signature`.

## WebSocket

| URL | Classe | Acces | Description |
|---|---|---|---|
| `/ws/realtime` | `RealtimeWebSocketHandler` | Origine CORS autorisee, auth liee a la session/handshake selon implementation | Canal temps reel pour compteurs, notifications et messages |

Notes :

- Le frontend utilise `environment.wsUrl`.
- Le protocole est WebSocket JSON simple, pas STOMP.

## Endpoints web Thymeleaf / legacy

Des controllers MVC existent encore pour `/admin/**`, `/parent/**`, `/animateur/**`, `/login`, `/signUp`, `/settings`, `/about`, `/avatars/**`. Comme `spring.thymeleaf.enabled=false`, leur usage en production Angular est a valider. Ils ne sont pas la surface API principale.
