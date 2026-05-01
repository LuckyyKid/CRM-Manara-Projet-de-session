# Guide utilisateur

Ce guide explique l'utilisation fonctionnelle de Manara CRM sans connaissance du code.

## Roles utilisateurs

L'application gere trois roles :

- `ROLE_PARENT` : parent responsable d'un ou plusieurs enfants.
- `ROLE_ANIMATEUR` : animateur, tuteur ou intervenant du centre.
- `ROLE_ADMIN` : administrateur charge de la configuration et de la validation.

Les pages visibles dependent du role connecte. Les outils tutorat et sport peuvent aussi etre affiches ou masques selon les droits calcules par le backend (`canAccessTutoringTools`, `canAccessSportPracticeTools`).

## Connexion et inscription

### Inscription parent

1. Ouvrir `/signup`.
2. Remplir nom, prenom, adresse, email et mot de passe.
3. L'application verifie la disponibilite de l'email.
4. Le compte parent est cree en attente d'approbation.
5. Un administrateur doit activer le compte avant l'utilisation complete.

### Connexion

1. Ouvrir `/login`.
2. Utiliser un email et un mot de passe.
3. L'application stocke un token JWT dans le navigateur.
4. L'utilisateur est redirige vers son tableau de bord selon son role.

La connexion Google OAuth existe aussi via le bouton Google. Le backend redirige ensuite vers `/oauth-success` avec un token.

## Guide parent

### Tableau de bord

Le parent accede a `/parent/dashboard`. Cette page donne une vue d'ensemble des enfants, inscriptions, notifications et raccourcis vers les principaux modules.

### Enfants

Dans `/parent/enfants`, le parent peut :

- voir ses enfants;
- ajouter un enfant;
- modifier un enfant;
- consulter le detail d'un enfant.

Un enfant ajoute peut etre en attente d'approbation administrative.

### Activites et inscriptions

Dans `/parent/activities`, le parent peut :

- consulter les activites et animations disponibles;
- choisir un enfant;
- envoyer une demande d'inscription a une animation.

L'inscription n'est pas automatiquement finale : elle passe par un statut gere par l'administration. Le backend verifie aussi les contraintes d'abonnement et de couverture enfant lorsque le module Stripe est actif.

### Planning

Dans `/parent/planning`, le parent consulte un calendrier ou une vue hebdomadaire des activites/animations de ses enfants.

### Notifications

Dans `/parent/notifications`, le parent peut :

- consulter les notifications;
- voir les notifications non lues;
- marquer une notification comme lue;
- marquer toutes les notifications comme lues.

### Messagerie

Dans `/parent/messages`, le parent peut discuter avec les contacts disponibles. La messagerie utilise les conversations et messages persistants, avec mise a jour temps reel par WebSocket.

### Rendez-vous

Dans `/parent/appointments`, le parent consulte les disponibilites d'un animateur et reserve un creneau. Dans `/parent/bookings`, il consulte ou gere ses rendez-vous.

### Abonnement

Dans `/parent/billing`, le parent peut :

- consulter l'etat de son abonnement;
- lancer un paiement Stripe Checkout;
- voir les enfants couverts;
- modifier la selection des enfants couverts.

Le tarif indique dans la configuration est de 60 $/mois pour le premier enfant et 40 $/mois pour chaque enfant additionnel.

### Quiz et devoirs

Si l'utilisateur a acces aux outils de tutorat :

- `/parent/quizzes` liste les quiz disponibles.
- `/parent/quizzes/:id/respond` permet de repondre a un quiz.
- `/parent/quizzes/attempts/:attemptId` affiche le resultat.
- Une action permet de generer un devoir personnalise depuis une tentative de quiz.
- `/parent/homeworks` liste les devoirs.
- `/parent/homeworks/:id/respond` permet de repondre a un devoir.
- `/parent/homeworks/attempts/:attemptId` affiche le resultat d'un devoir.

Les quiz et devoirs sont rattaches aux enfants et aux animations.

### Pratique maison

Si l'utilisateur a acces aux outils sport :

- `/parent/sport-practice-plans` liste les plans de pratique maison.
- `/parent/sport-practice-plans/:id` affiche le detail.

Ces plans sont generes cote animateur pour les activites sportives.

### Onboarding Intro.js

Le parent dispose d'un onboarding guide par Intro.js :

- parcours global;
- parcours tutorat;
- parcours sport.

Les etapes utilisent des identifiants HTML comme `tour-parent-sidebar-quizzes`, `tour-parent-planning-calendar` et `tour-parent-practice-list`. Les preferences de progression sont stockees dans `localStorage`.

## Guide animateur / tuteur / intervenant

### Tableau de bord

L'animateur accede a `/animateur/dashboard`.

### Animations

Dans `/animateur/animations`, il consulte les animations qui lui sont associees. Des actions rapides donnent acces aux quiz ou aux plans de pratique selon le type d'activite et les droits.

### Inscriptions et presences

Dans `/animateur/inscriptions`, l'animateur voit les enfants inscrits a ses animations. Il peut filtrer par animation et rechercher un enfant ou un parent.

Dans `/animateur/presence/:id`, il peut mettre a jour :

- `NON_SIGNEE`;
- `PRESENT`;
- `ABSENT`;
- une note d'incident.

### Notifications et messagerie

L'animateur dispose de :

- `/animateur/notifications`;
- `/animateur/messages`.

### Disponibilites et rendez-vous

Dans `/animateur/appointments`, l'animateur gere ses creneaux disponibles. Dans `/animateur/bookings`, il consulte ses rendez-vous reserves.

### Quiz tutorat

Si l'animateur a acces aux outils tutorat :

- `/animateur/quizzes` permet de creer un quiz.
- `/animateur/quizzes/history` liste les quiz generes.
- `/animateur/quizzes/:id/detail` affiche un quiz.
- `/quiz/:quizId/submission/:studentId` affiche une soumission eleve.

La creation s'appuie sur `QuizService` et peut utiliser Anthropic via `AnthropicQuizGenerationService`. Les corrections passent par `AnthropicQuizScoringService` avec fallback ou gestion d'erreur selon le service.

### Devoirs

Dans `/animateur/homeworks`, l'animateur consulte un tableau de suivi :

- devoirs assignes;
- devoirs soumis;
- eleves en difficulte;
- axes faibles;
- questions les plus echouees.

Il peut consulter le detail par enfant et le detail d'une assignation.

### Plans de pratique sport

Si l'animateur a acces aux outils sport :

- `/animateur/sport-practice-plans` permet de generer un plan.
- `/animateur/sport-practice-plans/history` liste les plans.
- `/animateur/sport-practice-plans/:id` affiche le detail.

La generation peut utiliser Anthropic via `AnthropicSportPracticePlanGenerationService`.

## Guide administrateur

### Tableau de bord

L'administrateur accede a `/admin/dashboard`, avec raccourcis vers catalogue, demandes, notifications et creation d'animation.

### Activites

Dans `/admin/activities`, l'administrateur peut :

- lister les activites;
- creer une activite;
- modifier une activite;
- supprimer une activite.

Champs principaux : nom, description, image, age minimum, age maximum, capacite, statut, type.

### Animations

Dans `/admin/animations`, l'administrateur peut :

- planifier une animation;
- choisir une activite;
- choisir un animateur;
- definir role, statut, debut et fin;
- consulter le detail et les quiz associes;
- supprimer une animation.

### Animateurs

Dans `/admin/animateurs`, l'administrateur peut :

- creer un animateur;
- modifier nom/prenom;
- activer ou desactiver un compte;
- supprimer un animateur.

### Parents / enfants

Dans `/admin/parents`, l'administrateur consulte les parents et enfants. Il peut activer/desactiver ou supprimer selon les actions exposees par l'API.

### Demandes

Dans `/admin/demandes`, l'administrateur traite :

- comptes parents en attente;
- enfants en attente;
- inscriptions en attente;
- inscriptions deja traitees.

Il peut approuver ou refuser une inscription.

### Notifications et abonnements

L'administrateur consulte :

- `/admin/notifications`;
- `/admin/messages`;
- les abonnements via l'API `/api/admin/subscriptions`, utilisee par les services admin.

## Parcours utilisateur typiques

### Parent inscrit un enfant

1. Le parent cree son compte.
2. L'admin approuve le compte.
3. Le parent ajoute un enfant.
4. L'admin approuve l'enfant si necessaire.
5. Le parent active ou configure son abonnement.
6. Le parent choisit une activite et envoie une demande d'inscription.
7. L'admin approuve l'inscription.
8. L'animateur voit l'enfant dans sa liste.

### Animateur fait le suivi d'une seance

1. L'animateur ouvre ses inscriptions.
2. Il filtre par animation.
3. Il marque les presences.
4. Il ajoute une note d'incident si necessaire.
5. Les informations restent disponibles pour le suivi.

### Tutorat avec quiz et devoir

1. L'animateur cree un quiz pour une animation de tutorat.
2. Le parent voit le quiz si son enfant est admissible.
3. L'enfant repond au quiz.
4. Le resultat est consulte par le parent et l'animateur.
5. Un devoir personnalise peut etre genere a partir des erreurs.
6. L'enfant soumet le devoir.
7. L'animateur consulte le suivi des devoirs.

## Limitations utilisateur

- Certaines fonctionnalites dependent d'un token JWT valide.
- Les outils tutorat et sport dependent de droits calcules par le backend.
- Les paiements Stripe necessitent une configuration complete des cles et price IDs.
- Les fonctions IA necessitent `ANTHROPIC_API_KEY`; des fallbacks existent dans certains services, mais la qualite depend de la configuration.
- Les comptes de test ne doivent pas etre utilises en production reelle.
- À compléter : politique exacte d'approbation d'un enfant selon les besoins du centre.

## Confidentialite et consentement parental

L'application peut contenir des donnees associees a des enfants, par exemple les informations de profil, les inscriptions, les activites, les quiz, les devoirs, les plans de pratique maison, les messages ou les notifications lies au suivi de l'enfant.

Comme ces donnees peuvent etre sensibles, une politique officielle de confidentialite et de consentement parental devra etre redigee et validee avant toute utilisation reelle dans un centre. Le projet doit actuellement etre considere comme un prototype scolaire / prototype fonctionnel, et non comme une solution legalement prete pour une utilisation en production avec des donnees reelles d'enfants.

Points a formaliser avant une utilisation reelle :

- le consentement parental devrait etre requis avant la collecte ou l'utilisation de donnees d'enfants;
- une procedure de suppression ou de modification des donnees devrait etre prevue sur demande;
- les acces aux donnees doivent etre limites selon les roles : parent, admin, animateur/intervenant;
- les obligations legales applicables au contexte du centre doivent etre validees avant une vraie production.
