# 🤖 Guide du Chatbot FAQ - Centre Manara

## Description

Le chatbot FAQ est un assistant virtuel simple qui répond aux questions fréquentes des utilisateurs du Centre Manara. Il utilise une logique basée sur des mots-clés pour identifier les questions et fournir des réponses appropriées.

---

## 📁 Fichiers créés

| Fichier | Description |
|---------|-------------|
| `ChatbotService.java` | Service contenant la logique FAQ et les réponses |
| `ChatbotController.java` | Controller REST pour les endpoints API |
| `chatbot.html` | Fragment Thymeleaf avec l'interface du widget |
| `style.css` | Styles CSS ajoutés pour le chatbot |
| `footer.html` | Modifié pour inclure le chatbot |
| `SecurityConfig.java` | Modifié pour permettre l'accès à l'API |

---

## 🚀 Comment tester le chatbot

### Étape 1 : Démarrer l'application

```bash
# Dans le terminal, à la racine du projet
./mvnw spring-boot:run
```

Ou dans votre IDE (IntelliJ/Eclipse), lancez la classe `CrmManaraApplication.java`.

### Étape 2 : Accéder à l'application

Ouvrez votre navigateur et allez à : **http://localhost:8080**

### Étape 3 : Utiliser le chatbot

1. Vous verrez un **bouton bleu avec une icône de chat** en bas à droite de l'écran
2. Cliquez dessus pour ouvrir le chatbot
3. Un message de bienvenue s'affiche avec des suggestions
4. Vous pouvez :
   - Cliquer sur une suggestion rapide
   - Ou taper votre propre question

---

## 💬 Questions que le chatbot comprend

### Inscriptions
- "Comment inscrire mon enfant?"
- "Je veux inscrire mon enfant"
- "Comment s'inscrire?"

### Activités
- "Quelles activités sont disponibles?"
- "Quels programmes offrez-vous?"
- "Liste des activités"

### Horaires
- "Quels sont les horaires?"
- "Quand sont les activités?"
- "C'est à quelle heure?"

### Compte utilisateur
- "Comment créer un compte?"
- "Comment me connecter?"
- "J'ai oublié mon mot de passe"

### Enfants
- "Comment ajouter un enfant?"
- "Gérer mes enfants"

### Tarifs
- "Combien ça coûte?"
- "Quels sont les prix?"
- "C'est gratuit?"

### Contact
- "Comment vous contacter?"
- "Quel est le téléphone?"
- "Quelle est l'adresse?"

### Âge
- "Quel âge pour les activités?"
- "Mon enfant a 5 ans"

### Annulation
- "Comment annuler une inscription?"
- "Je veux désinscrire mon enfant"

### Salutations
- "Bonjour", "Salut", "Allo"
- "Merci", "Au revoir"

---

## 🧪 Scénarios de test pour la démonstration

### Test 1 : Premier contact
1. Ouvrir le chatbot
2. Observer le message de bienvenue
3. Cliquer sur "Comment inscrire mon enfant?"
4. Vérifier que la réponse détaillée s'affiche

### Test 2 : Question manuelle
1. Taper "activités" dans le champ de texte
2. Appuyer sur Entrée ou cliquer sur le bouton envoyer
3. Vérifier que le chatbot répond avec la liste des activités

### Test 3 : Question non reconnue
1. Taper quelque chose de random comme "asdfgh"
2. Vérifier que le chatbot affiche un message par défaut avec des suggestions

### Test 4 : Conversation complète
1. Dire "Bonjour"
2. Demander "Quelles activités pour un enfant de 8 ans?"
3. Demander "Comment s'inscrire?"
4. Dire "Merci"
5. Dire "Au revoir"

### Test 5 : Responsive
1. Ouvrir les DevTools (F12)
2. Activer le mode mobile
3. Vérifier que le chatbot prend tout l'écran sur mobile

---

## 🔧 Architecture technique

```
┌─────────────────┐     HTTP POST      ┌─────────────────────┐
│                 │  ─────────────────▶│                     │
│  chatbot.html   │   /api/chatbot/    │  ChatbotController  │
│  (JavaScript)   │   message          │                     │
│                 │◀───────────────────│                     │
└─────────────────┘     JSON réponse   └──────────┬──────────┘
                                                  │
                                                  │ appel
                                                  ▼
                                       ┌─────────────────────┐
                                       │                     │
                                       │   ChatbotService    │
                                       │                     │
                                       │  - FAQ Map          │
                                       │  - Mot-clés         │
                                       │  - Réponses         │
                                       │                     │
                                       └─────────────────────┘
```

---

## 📝 Comment ajouter de nouvelles questions FAQ

Dans le fichier `ChatbotService.java`, ajoutez une nouvelle entrée dans la méthode `initialiserFAQ()` :

```java
faqResponses.put(
    new String[]{"mot-clé1", "mot-clé2", "mot-clé3"},
    "La réponse à afficher quand ces mots-clés sont détectés."
);
```

**Exemple :**
```java
faqResponses.put(
    new String[]{"piscine", "natation", "nager"},
    "Nos cours de natation sont disponibles pour les enfants de 5 à 15 ans.\n" +
    "Les sessions ont lieu le samedi matin de 9h à 12h."
);
```

---

## ⚠️ Limitations

- Le chatbot est basé sur des mots-clés simples (pas d'IA)
- Il ne comprend pas le contexte des conversations précédentes
- Les réponses sont statiques (prédéfinies)
- Il ne peut pas accéder aux données de la base de données

---

## 🎯 Points à présenter au professeur

1. **Widget flottant** - Le bouton est toujours visible en bas à droite
2. **Interface responsive** - S'adapte aux écrans mobiles
3. **Suggestions rapides** - Facilite l'utilisation
4. **Animation fluide** - Messages qui apparaissent avec animation
5. **Architecture MVC** - Séparation Controller/Service
6. **API REST** - Utilisation de `@RestController` et JSON
7. **Intégration Thymeleaf** - Fragment réutilisable
8. **Sécurité** - Endpoint accessible sans authentification

---

## 📌 Améliorations possibles (pour discussion)

- Ajouter une persistance des conversations
- Intégrer une vraie IA (OpenAI, Claude, etc.)
- Ajouter des réponses contextuelles selon le rôle de l'utilisateur
- Permettre aux admins de modifier les FAQ depuis l'interface
- Ajouter des statistiques sur les questions les plus posées

---

**Auteur :** Équipe CRM Manara  
**Date :** Mars 2026  
**Version :** 1.0
