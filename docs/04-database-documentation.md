# Documentation base de donnees

Cette documentation est deduite des entites JPA dans `backend/src/main/java/CRM_Manara/CRM_Manara/Model/Entity` et des scripts SQL dans `backend/SQL/`.

Le projet cible PostgreSQL en execution actuelle. Le fichier `schema_sprint1.sql` indique un ancien schema MySQL/MariaDB; il doit etre considere comme historique ou a valider avant usage. Le schema exact de production doit etre confirme dans Supabase.

## Configuration JPA

Dans `application.properties` :

```properties
spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:update}
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.dialect=${SPRING_JPA_DATABASE_PLATFORM:org.hibernate.dialect.PostgreSQLDialect}
```

En production, il est recommande de remplacer `ddl-auto=update` par une strategie de migration controlee avant une exploitation reelle.

## Entites principales

### User

Table : `users`

Champs importants :

- `id`
- `email`, unique et obligatoire
- `password`
- `role` (`SecurityRole`)
- `dateCreation`
- `enabled`
- `avatarUrl`

Relations :

- lie a `Parent`, `Animateur` ou `Administrateurs` via relation one-to-one.

Donnees sensibles :

- email;
- mot de passe hashe;
- avatar.

### Parent

Table : `Parent`

Champs :

- `id`
- `nom`
- `prenom`
- `adresse`
- `user`

Relations :

- one-to-one vers `User`;
- one-to-many vers `Enfant`;
- one-to-many vers `ParentSubscription`.

### Enfant

Table : nom implicite JPA, probablement `enfant` selon le script SQL.

Champs :

- `id`
- `nom`
- `prenom`
- `dateDeNaissance`
- `active`
- `parent`

Relations :

- many-to-one vers `Parent`;
- one-to-many vers `Inscription`;
- one-to-many vers `ParentSubscriptionChild`.

Donnees sensibles :

- identite et date de naissance d'un enfant.

### Administrateurs

Table : `Administrateurs`

Champs :

- `id`
- `nom`
- `prenom`
- `user`
- `role`
- `status`
- `dateCreation`
- `lastLogin`

Relations :

- one-to-one vers `User`.

### Animateur

Table : `Animateurs`

Champs :

- `id`
- `nom`
- `prenom`
- `user`

Relations :

- one-to-one vers `User`;
- one-to-many vers `Animation`.

### Activity

Table : `Activity`

Champs :

- `id`
- `activyName`
- `description`
- `imageUrl`
- `ageMin`
- `ageMax`
- `capacity`
- `dateCreation`
- `status`
- `type`

Enums :

- `status` : `OUVERTE`, `COMPLETE`, `ANNULEE` dans le code avec accents encodes.
- `typeActivity` : `SPORT`, `MUSIQUE`, `ART`, `LECTURE`, `TUTORAT`.

### Animation

Table : `Animation`

Champs :

- `id`
- `animateur`
- `activity`
- `role`
- `statusAnimation`
- `startTime`
- `endTime`

Relations :

- many-to-one vers `Animateur`;
- many-to-one vers `Activity`;
- one-to-many vers `Inscription`.

Enums :

- `AnimationRole` : `PRINCIPAL`, `ASSISTANT`, `TUTEUR`, `COACH`.
- `animationStatus` : `ACTIF`, `ANNULE`, `REMPLACE` dans le code avec accents encodes.

### Inscription

Table : `inscription`

Champs :

- `id`
- `statusInscription`
- `presenceStatus`
- `incidentNote`
- `enfant`
- `animation`

Relations :

- many-to-one vers `Enfant`;
- many-to-one vers `Animation`.

Contrainte :

- unicite enfant + animation dans le script SQL et l'entite.

Enums :

- `statusInscription` : `EN_ATTENTE`, `APPROUVEE`, `REFUSEE`, `ACTIF`, `ANNULEE` dans le code avec accents encodes.
- `PresenceStatus` : `NON_SIGNEE`, `PRESENT`, `ABSENT`.

### ParentNotification, AnimateurNotification, AdminNotification

Tables :

- `parent_notifications`
- `animateur_notifications`
- `admin_notification`

Champs communs :

- categorie/source/type selon l'entite;
- titre;
- message;
- date de creation;
- statut lu/archive pour parent et animateur.

Relations :

- notifications parent liees a `Parent`;
- notifications animateur liees a `Animateur`;
- notifications admin non liees explicitement a un admin dans l'entite actuelle.

### ChatConversation

Table : definie par `@Table`, nom a valider dans le fichier source exact.

Champs :

- `id`
- `participantOne`
- `participantTwo`
- `createdAt`
- `updatedAt`
- `lastMessagePreview`
- `lastMessageAt`

Relations :

- many-to-one vers `User` pour les deux participants;
- one-to-many vers `ChatMessage`.

Contrainte :

- l'entite declare une contrainte d'unicite sur les participants.

### ChatMessage

Table : `chat_messages`

Champs :

- `id`
- `conversation`
- `sender`
- `recipient`
- `body`
- `createdAt`
- `readStatus`

Relations :

- many-to-one vers `ChatConversation`;
- many-to-one vers `User` expediteur;
- many-to-one vers `User` destinataire.

### AppointmentSlot

Table : `appointment_slots`

Champs :

- `id`
- `animateur`
- `parent`
- `startTime`
- `endTime`
- `status`
- `createdAt`
- `bookedAt`

Relations :

- many-to-one vers `Animateur`;
- many-to-one optionnel vers `Parent`.

### Booking

Table : `bookings`

Champs :

- `id`
- `slot`
- `animateur`
- `parent`
- `date`
- `startTime`
- `endTime`
- `status`
- `createdAt`
- `cancelledAt`
- `updatedAt`

Relations :

- many-to-one vers `AppointmentSlot`;
- many-to-one vers `Animateur`;
- many-to-one vers `Parent`.

### ParentSubscription

Table : `parent_subscription`

Champs :

- `id`
- `parent`
- `user`
- `children`
- `status`
- `provider`
- `stripeCustomerId`
- `stripeSubscriptionId`
- `stripeCheckoutSessionId`
- `stripePriceId`
- `stripeAdditionalPriceId`
- `coveredChildrenCount`
- `pendingCoveredChildrenCount`
- `firstChildMonthlyAmountCents`
- `additionalChildMonthlyAmountCents`
- `currentPeriodStart`
- `currentPeriodEnd`
- `cancelAtPeriodEnd`
- `createdAt`
- `updatedAt`

Enums :

- `SubscriptionStatus` : `INACTIVE`, `CHECKOUT_PENDING`, `ACTIVE`, `PAST_DUE`, `CANCELED`.
- `BillingProvider` : `STRIPE`.

### ParentSubscriptionChild

Table : `parent_subscription_child`

Champs :

- `id`
- `subscription`
- `enfant`
- `createdAt`

Contrainte :

- unicite `subscription_id` + `enfant_id`.

### Quiz

Table : `Quiz`

Champs :

- `id`
- `animateur`
- `animation`
- `title`
- `sourceNotes`
- `createdAt`
- `axes`
- `attempts`

Relations :

- many-to-one vers `Animateur`;
- many-to-one vers `Animation`;
- one-to-many vers `QuizAxis`;
- one-to-many vers `QuizAttempt`.

### QuizAxis

Table : `QuizAxis`

Champs :

- `id`
- `quiz`
- `title`
- `summary`
- `position`
- `questions`

### QuizQuestion

Table : `QuizQuestion`

Champs :

- `id`
- `axis`
- `angle`
- `type`
- `questionText`
- `expectedAnswer`
- `optionsJson`
- `position`

### QuizAttempt

Table : `QuizAttempt`

Champs :

- `id`
- `quiz`
- `enfant`
- `submittedAt`
- `elapsedSeconds`
- `scorePercent`
- `status`
- `answers`

### QuizAnswer

Table : `QuizAnswer`

Champs :

- `id`
- `attempt`
- `question`
- `answerText`

### HomeworkAssignment

Table : `HomeworkAssignment`

Champs :

- `id`
- `enfant`
- `animateur`
- `animation`
- `quizAttempt`
- `quiz`
- `title`
- `summary`
- `status`
- `createdAt`
- `dueDate`
- `exercises`
- `attempts`

### HomeworkExercise

Table : `HomeworkExercise`

Champs :

- `id`
- `assignment`
- `axisTitle`
- `type`
- `difficulty`
- `questionText`
- `expectedAnswer`
- `targetMistake`
- `optionsJson`
- `position`

### HomeworkAttempt

Table : `HomeworkAttempt`

Champs principaux deduits :

- `id`
- assignation;
- enfant;
- date de soumission;
- temps ecoule;
- score;
- statut;
- reponses.

### HomeworkAnswer

Table : `HomeworkAnswer`

Champs :

- `id`
- `attempt`
- `exercise`
- `answerText`

### SportPracticePlan

Table : `SportPracticePlan`

Champs :

- `id`
- `animateur`
- `animation`
- `title`
- `summary`
- `sourceNotes`
- `createdAt`
- `items`

### SportPracticePlanItem

Table : `SportPracticePlanItem`

Champs :

- `id`
- `plan`
- `title`
- `instructions`
- `purpose`
- `durationLabel`
- `safetyTip`
- `position`

### VerificationToken

Table : `verification_tokens`

Champs :

- `id`
- `token`
- `user`
- `expirationDate`

## Relations metier importantes

- Un `User` porte le role de securite.
- Un parent possede des enfants.
- Une animation associe une activite et un animateur.
- Une inscription associe un enfant et une animation.
- Un abonnement parent couvre un ou plusieurs enfants via `ParentSubscriptionChild`.
- Un quiz appartient a un animateur et peut etre lie a une animation.
- Les tentatives de quiz et devoirs appartiennent a un enfant.
- Les devoirs peuvent etre generes depuis une tentative de quiz.
- Les plans de pratique sport appartiennent a une animation et un animateur.
- Les conversations et messages sont rattaches aux utilisateurs.

## Donnees critiques

- Donnees personnelles parents et enfants.
- Dates de naissance enfants.
- Emails utilisateurs.
- Hashs de mot de passe.
- Identifiants Stripe client, abonnement, session checkout.
- Messages de conversation.
- Notes d'incident de presence.
- Reponses de quiz/devoirs.

## Confidentialite et consentement pour les donnees d'enfants

L'application peut contenir des donnees associees a des enfants, par exemple les informations de profil, les inscriptions, les activites, les quiz, les devoirs, les plans de pratique maison, les messages ou les notifications lies au suivi de l'enfant.

Comme ces donnees peuvent etre sensibles, une politique officielle de confidentialite et de consentement parental devra etre redigee et validee avant toute utilisation reelle dans un centre. Le projet doit actuellement etre considere comme un prototype scolaire / prototype fonctionnel, et non comme une solution legalement prete pour une utilisation en production avec des donnees reelles d'enfants.

Avant une vraie production :

- le consentement parental devrait etre requis avant la collecte ou l'utilisation de donnees d'enfants;
- une procedure de suppression ou de modification des donnees devrait etre prevue sur demande;
- les acces aux donnees doivent etre limites selon les roles : parent, admin, animateur/intervenant;
- les obligations legales applicables au contexte du centre doivent etre validees.

## Politique de sauvegarde Supabase

Aucune politique de sauvegarde Supabase officielle n'a encore ete definie pour ce prototype. Avant une utilisation reelle dans un centre, il faudra definir une strategie de sauvegarde reguliere, verifier les options de backup disponibles dans Supabase, determiner une frequence de sauvegarde et designer une personne responsable de la restauration en cas de probleme.

## Recommandations de maintenance

- Valider le schema exact Supabase avant livraison.
- Mettre en place des migrations versionnees avant production reelle.
- Sauvegarder regulierement la base Supabase.
- Ne jamais stocker de cle API en base.
- Surveiller les contraintes d'unicite : email utilisateur, inscription enfant-animation, enfant couvert par abonnement.
- Prevoir une politique de retention pour messages, notifications et donnees enfants.
- Verifier les encodages d'enums contenant des accents avant migration ou comparaison manuelle.
