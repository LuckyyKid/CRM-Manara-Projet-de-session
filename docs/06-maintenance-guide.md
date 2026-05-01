# Guide de maintenance

Ce guide explique comment modifier le projet sans casser les flux existants.

## Principes generaux

- Garder les roles `ROLE_ADMIN`, `ROLE_PARENT`, `ROLE_ANIMATEUR` coherents entre backend, frontend et base.
- Ajouter les endpoints dans un controller REST existant lorsque le domaine existe deja.
- Ajouter ou mettre a jour les DTOs Java et les interfaces TypeScript correspondantes.
- Ne pas contourner `AuthService`, les guards ou les interceptors Angular.
- Ne pas stocker de secret dans le depot.
- Tester chaque modification avec au moins un compte par role impacte.

## Ajouter une page frontend

1. Creer un composant dans `frontend/src/app/pages/...`.
2. Ajouter le template HTML et le style si necessaire.
3. Ajouter la route dans `frontend/src/app/app.routes.ts`.
4. Choisir le guard adapte :
   - public : aucun guard;
   - connecte : `authGuard`;
   - admin : `adminGuard`;
   - parent : `parentGuard`;
   - animateur : `animateurGuard`.
5. Ajouter un lien dans `app.html` uniquement si la page doit apparaitre dans la navigation.
6. Si la page consomme l'API, passer par un service Angular dans `core/services`.
7. Tester le refresh direct de l'URL avec la redirection SPA.

## Ajouter un composant Angular

1. Placer le composant dans `shared/` s'il est reutilisable.
2. Placer le composant dans `pages/.../components/` s'il est specifique a une page.
3. Respecter les patterns Angular standalone deja utilises.
4. Declarer les imports Angular necessaires dans le composant.
5. Eviter les appels HTTP directs dans le composant si un service existe deja.

## Ajouter un service Angular

1. Creer le service dans `frontend/src/app/core/services`.
2. Injecter `HttpClient`.
3. Utiliser des DTOs dans `frontend/src/app/core/models/api.models.ts`.
4. Utiliser des URLs relatives `/api/...`; `apiBaseInterceptor` ajoutera l'URL backend.
5. Si les donnees sont cachees, utiliser `shareReplay(1)` et fournir une methode de reset si necessaire.

## Ajouter un endpoint backend

1. Identifier le domaine :
   - admin : `ApiAdminController`;
   - parent : `ApiParentController`;
   - animateur : `ApiAnimateurController`, `ApiQuizController`, `ApiAnimateurHomeworkController`, etc.;
   - communication : `ApiCommunicationController`;
   - public : `ApiPublicController`.
2. Creer ou reutiliser un DTO dans `dto/`.
3. Mettre la logique metier dans un service, pas directement dans le controller.
4. Utiliser les repositories uniquement dans les services.
5. Valider les entrees et retourner `ResponseStatusException` avec un statut clair si necessaire.
6. Verifier l'acces via `SecurityConfig`.
7. Ajouter l'appel cote Angular dans le service correspondant.
8. Tester avec un token du bon role.

## Ajouter une nouvelle entite

1. Creer une classe JPA dans `Model/Entity`.
2. Definir `@Entity` et `@Table` si le nom de table doit etre stable.
3. Ajouter les relations JPA avec attention aux cascades.
4. Creer un repository dans `Repository`.
5. Creer les DTOs necessaires.
6. Ajouter la logique metier dans `service`.
7. Ajouter un script SQL de migration si la production ne doit pas dependre de `ddl-auto=update`.
8. Verifier le schema Supabase.

## Modifier une fonctionnalite existante

Avant de modifier :

1. Lire le composant Angular.
2. Lire le service Angular appele.
3. Lire le controller REST.
4. Lire le service backend.
5. Lire les entites/repositories concernes.
6. Identifier les tests existants.

Apres modification :

1. Lancer le build frontend.
2. Lancer les tests backend.
3. Tester manuellement le role concerne.
4. Tester refresh et logout si la modification touche l'authentification.

## Tests utiles

Frontend :

```bash
cd frontend
npm run build
npm test
```

Backend :

```bash
cd backend
./mvnw test
```

Sur Windows :

```powershell
cd backend
.\mvnw.cmd test
```

## Zones sensibles

### Authentification

Fichiers :

- `AuthService`
- `auth.guard.ts`
- `guest.guard.ts`
- `auth.interceptor.ts`
- `SecurityConfig`
- `JwtAuthenticationFilter`
- `JwtService`
- `CurrentUserService`

Risques :

- masquer la sidebar alors qu'un token existe;
- rediriger vers un mauvais dashboard;
- perdre les roles apres OAuth;
- invalider tous les tokens en changeant `APP_JWT_SECRET`.

### Roles et permissions

Les roles sont controles a deux niveaux :

- Angular guards;
- Spring Security par prefixe `/api/admin/**`, `/api/parent/**`, `/api/animateur/**`.

Ne jamais se fier uniquement au frontend.

### Inscriptions

Entites/services :

- `Inscription`;
- `parentService`;
- `AdminService`;
- `BillingService`.

Risques :

- doublons enfant-animation;
- inscription d'un enfant non couvert par abonnement;
- capacite depassee;
- statut incoherent.

### Messagerie et temps reel

Fichiers :

- `CommunicationService`;
- `ApiCommunicationController`;
- `ChatService`;
- `RealtimeService`;
- `RealtimeWebSocketHandler`;
- `WebSocketConfig`.

Risques :

- WebSocket non autorise par CORS;
- conversations marquees non lues;
- compteurs sidebar non synchronises.

### Quiz, devoirs et IA

Fichiers :

- `QuizService`;
- `ParentQuizService`;
- `HomeworkService`;
- services Anthropic;
- entites `Quiz*` et `Homework*`.

Risques :

- cle Anthropic absente;
- reponse IA invalide;
- JSON partiel;
- devoir genere avec questions incoherentes;
- acces parent a un quiz non admissible.

### Abonnement Stripe

Fichiers :

- `BillingService`;
- `ApiParentBillingController`;
- `StripeWebhookController`;
- `ParentSubscription`;
- `ParentSubscriptionChild`.

Risques :

- webhook non configure;
- price IDs incorrects;
- abonnement actif dans Stripe mais non synchronise en base;
- enfant non couvert.

### Deploiement

Risques :

- Render en veille;
- CORS incomplet;
- URL backend changee sans rebuild frontend;
- secret JWT change;
- base Supabase inaccessible.

## Bonnes pratiques

- Ajouter des DTOs plutot que d'exposer directement les entites.
- Garder les messages d'erreur explicites cote backend.
- Eviter les changements globaux de structure sans tests.
- Ne pas supprimer les controllers Thymeleaf sans verifier s'ils servent encore en demo ou fallback.
- Documenter tout nouveau endpoint dans `docs/03-api-documentation.md`.
- Documenter toute nouvelle entite dans `docs/04-database-documentation.md`.
- Mettre a jour la checklist de deploiement si une variable d'environnement est ajoutee.
