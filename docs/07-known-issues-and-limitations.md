# Problemes connus et limitations

Ce document liste les risques visibles dans le code ou importants pour la livraison.

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

## Limitations techniques actuelles

### Backend Render en plan gratuit

Render peut mettre le service en veille. La premiere requete peut etre lente, ce qui affecte :

- chargement initial;
- restauration `/api/me`;
- WebSocket;
- login ou OAuth.

Mitigation avant demo :

- reveiller le backend;
- tester login;
- tester refresh;
- tester logout.

### Schema SQL historique

`backend/SQL/schema_sprint1.sql` indique MySQL/MariaDB alors que l'application utilise PostgreSQL. Le schema reel doit etre valide dans Supabase.

Risque :

- confusion lors d'une restauration ou installation manuelle.

### Hibernate `ddl-auto=update`

La configuration par defaut utilise `update`. C'est pratique en prototype, mais insuffisant pour une production client.

Risque :

- migrations implicites non controlees;
- differences entre environnements.

### Controllers Thymeleaf encore presents

Le depot contient des controllers et templates Thymeleaf, mais `spring.thymeleaf.enabled=false`.

Risque :

- confusion entre l'ancienne interface serveur et la SPA Angular;
- endpoints legacy non utilises mais encore accessibles selon profil/configuration.

### Encodage de certains libelles

Certains fichiers affichent des accents mal encodes (`ANNULÃ‰E`, `COMPLÃˆTE`, etc.).

Risque :

- affichage incorrect;
- comparaison enum/texte delicate;
- documentation ou exports peu propres.

### WebSocket simple

Le temps reel utilise un WebSocket JSON custom, pas STOMP.

Risque :

- moins de conventions standard;
- authentification/identification du handshake a surveiller, avec JWT passe en query param `token`;
- reconnexion cote frontend a maintenir.

### Logs debug frontend

Plusieurs services/interceptors journalisent les requetes API et tokens ou headers en console.

Risque :

- bruit en production;
- exposition d'informations sensibles si les logs incluent des tokens.

À verifier avant production reelle.

## Risques fonctionnels

### Abonnement Stripe

La logique depend fortement du webhook Stripe. Un retour success depuis Checkout ne suffit pas a garantir l'etat en base.

Risques :

- webhook absent;
- secret incorrect;
- abonnement Stripe actif mais base non mise a jour;
- enfant non couvert alors que le parent pense avoir paye.

### IA Anthropic

Les fonctions quiz, devoirs, scoring, plans sport et recommandations peuvent utiliser Anthropic.

Risques :

- cle absente;
- quota depasse;
- latence;
- JSON IA invalide ou partiel;
- contenu pedagogique a relire avant usage reel.

### Donnees enfants

L'application manipule des informations d'enfants : nom, prenom, date de naissance, inscriptions, activites, quiz, devoirs, plans de pratique maison, resultats, messages et notifications lies au suivi de l'enfant.

Risques :

- exigences legales et consentement;
- acces non autorise;
- duree de conservation non definie.

Politique attendue avant usage reel :

- une politique officielle de confidentialite et de consentement parental devra etre redigee et validee avant toute utilisation reelle dans un centre;
- le projet doit actuellement etre considere comme un prototype scolaire / prototype fonctionnel, et non comme une solution legalement prete pour une utilisation en production avec des donnees reelles d'enfants;
- le consentement parental devrait etre requis avant la collecte ou l'utilisation de donnees d'enfants;
- une procedure de suppression ou de modification des donnees devrait etre prevue sur demande;
- les acces aux donnees doivent etre limites selon les roles : parent, admin, animateur/intervenant;
- les obligations legales applicables au contexte du centre doivent etre validees avant une vraie production.

### Statuts et workflow

Plusieurs statuts coexistent :

- compte utilisateur `enabled`;
- enfant `active`;
- inscription `EN_ATTENTE`, `APPROUVEE`, `REFUSEE`, `ACTIF`, `ANNULEE`;
- presence `NON_SIGNEE`, `PRESENT`, `ABSENT`;
- abonnement `INACTIVE`, `CHECKOUT_PENDING`, `ACTIVE`, `PAST_DUE`, `CANCELED`.

Risque :

- etat incoherent si une action echoue au milieu d'un workflow.

## Bugs ou points mentionnes

### Bouton de deconnexion masque dans certains cas

Bug observe : un utilisateur pouvait rester techniquement connecte mais ne plus voir le bouton `Deconnexion` si l'etat Angular `currentUser` n'etait pas restaure correctement.

Correction apportee dans le code :

- source d'etat `hasAuthSession`;
- sidebar visible si token local ou user charge;
- logout robuste meme si backend lent.

Test a refaire avant demo.

### Messagerie lente ou clignotement temps reel

Bug observe : dans la version deployee, la messagerie pouvait alterner entre `Connexion en cours` et `Temps reel actif`. Le clic sur une conversation pouvait aussi sembler lent, car l'interface attendait plusieurs appels reseau avant de stabiliser l'affichage.

Cause identifiee :

- le WebSocket etait ouvert sans JWT transmis explicitement au backend;
- le backend pouvait refuser la session temps reel en production;
- le frontend relancait ensuite la connexion periodiquement, ce qui provoquait un clignotement visible;
- l'ouverture d'une conversation declenchait aussi des rafraichissements de compteurs et de liste avant de rendre l'experience fluide.

Correction apportee dans le code :

- `CommunicationService` ajoute le token JWT a l'URL WebSocket via `?token=...`;
- `RealtimeWebSocketHandler` valide ce token avec `JwtService`;
- la reconnexion WebSocket utilise un delai progressif;
- la conversation selectionnee s'affiche immediatement pendant le chargement du detail;
- les compteurs et la liste des conversations se rafraichissent en arriere-plan;
- les messages temps reel sont ajoutes localement a la conversation active au lieu de recharger toute la conversation.

Risque restant :

- le cold start Render peut encore ralentir la premiere connexion REST ou WebSocket apres une periode d'inactivite.

Test a refaire avant demo :

- ouvrir la messagerie apres connexion;
- verifier que le statut devient `Temps reel actif`;
- cliquer entre plusieurs conversations;
- envoyer un message;
- rafraichir la page messagerie;
- refaire le test apres reveil du backend Render.

### Render cold start pendant presentation

Le backend peut mettre plusieurs secondes a repondre. Cela peut donner l'impression que le login ou les donnees sont bloques.

Mitigation :

- reveiller le service;
- garder une page backend ouverte;
- verifier les comptes de test 10 minutes avant la presentation.

## Points a ameliorer avant une vraie utilisation centre

- Ajouter une politique de confidentialite.
- Ajouter une politique de consentement parental.
- Ajouter des migrations SQL versionnees.
- Ajouter une sauvegarde Supabase planifiee.
- Reduire les logs sensibles en production.
- Ajouter des tests end-to-end pour les parcours critiques.
- Ajouter monitoring et alerting Render/Supabase.
- Verifier les permissions fines pour messagerie et rendez-vous.
- Ajouter un audit trail admin.
- Formaliser les conditions d'annulation et remboursement Stripe.
- Valider les contenus IA avant exposition automatique aux enfants.
- Ajouter une gestion de fichiers/avatar robuste hors filesystem local si Render ne persiste pas les uploads.

## Politique de sauvegarde Supabase

Aucune politique de sauvegarde Supabase officielle n'a encore ete definie pour ce prototype. Avant une utilisation reelle dans un centre, il faudra definir une strategie de sauvegarde reguliere, verifier les options de backup disponibles dans Supabase, determiner une frequence de sauvegarde et designer une personne responsable de la restauration en cas de probleme.

## À completer

- Liste officielle des bugs connus par l'equipe.
- Politique RGPD/Loi 25 ou equivalent selon le pays/province.
- Contrat de support et maintenance.
- Configuration definitive Netlify/Render/Supabase.
