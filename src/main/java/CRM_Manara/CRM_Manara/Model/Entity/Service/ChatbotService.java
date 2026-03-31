package CRM_Manara.CRM_Manara.Model.Entity.Service;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Service pour le chatbot FAQ du Centre Manara
 * Gère les réponses aux questions fréquentes des utilisateurs
 */
@Service
public class ChatbotService {

    private List<FaqEntry> faqEntries;
    private List<String> suggestions;
    private String derniereCategorie = null;

    public ChatbotService() {
        initialiserFAQ();
        initialiserSuggestions();
    }

    private void initialiserFAQ() {
        faqEntries = new ArrayList<>();

        // Salutations - priorité haute
        faqEntries.add(new FaqEntry(
            Arrays.asList("bonjour", "salut", "allo", "hello", "hi", "hey", "bonsoir"),
            "Bonjour! 👋 Je suis l'assistant virtuel du Centre Manara.\n\n" +
            "Comment puis-je vous aider aujourd'hui?",
            "salutation",
            Arrays.asList("Comment inscrire mon enfant?", "Voir les activités", "Horaires du centre")
        ));

        // Inscriptions
        faqEntries.add(new FaqEntry(
            Arrays.asList("inscription", "inscrire", "s'inscrire", "comment inscrire", "enregistrer", "inscription enfant"),
            "Pour inscrire votre enfant à une activité :\n\n" +
            "1️⃣ Connectez-vous à votre compte parent\n" +
            "2️⃣ Allez dans 'Mes enfants' et ajoutez votre enfant\n" +
            "3️⃣ Consultez le 'Planning' ou 'Activités'\n" +
            "4️⃣ Cliquez sur 'Inscrire' pour l'activité souhaitée\n\n" +
            "Avez-vous déjà un compte?",
            "inscription",
            Arrays.asList("Créer un compte", "Voir les activités", "Tarifs")
        ));

        // Activités
        faqEntries.add(new FaqEntry(
            Arrays.asList("activité", "activites", "programme", "programmes", "quelles activités", "liste activités", "offrez"),
            "Le Centre Manara offre plusieurs types d'activités :\n\n" +
            "⚽ Sports : soccer, basketball, natation\n" +
            "🎨 Culture : art, musique, danse\n" +
            "📚 Éducatif : aide aux devoirs, langues\n" +
            "🏕️ Camps et sorties spéciales\n\n" +
            "Quel type d'activité vous intéresse?",
            "activite",
            Arrays.asList("Activités sportives", "Activités culturelles", "Voir les horaires")
        ));

        // Activités sportives (suivi)
        faqEntries.add(new FaqEntry(
            Arrays.asList("sport", "sportif", "sportive", "soccer", "basketball", "natation", "foot", "basket"),
            "Nos activités sportives :\n\n" +
            "⚽ Soccer : Mardi et Jeudi 16h-18h (6-12 ans)\n" +
            "🏀 Basketball : Mercredi 14h-16h (8-15 ans)\n" +
            "🏊 Natation : Samedi 9h-12h (5-14 ans)\n\n" +
            "Les inscriptions sont ouvertes! Voulez-vous en savoir plus?",
            "sport",
            Arrays.asList("Comment s'inscrire?", "Tarifs", "Équipement nécessaire")
        ));

        // Activités culturelles (suivi)
        faqEntries.add(new FaqEntry(
            Arrays.asList("culturel", "culturelle", "art", "musique", "danse", "dessin", "peinture"),
            "Nos activités culturelles :\n\n" +
            "🎨 Arts plastiques : Lundi 15h-17h (tous âges)\n" +
            "🎵 Musique : Mercredi 10h-12h (7+ ans)\n" +
            "💃 Danse : Vendredi 16h-18h (5-16 ans)\n\n" +
            "Ces activités stimulent la créativité!",
            "culture",
            Arrays.asList("Comment s'inscrire?", "Tarifs", "Matériel fourni?")
        ));

        // Horaires
        faqEntries.add(new FaqEntry(
            Arrays.asList("horaire", "horaires", "heure", "quand", "planning", "calendrier", "schedule", "ouvert", "ouverture"),
            "📅 Horaires du Centre Manara :\n\n" +
            "Lundi - Vendredi : 8h00 - 18h00\n" +
            "Samedi : 9h00 - 17h00\n" +
            "Dimanche : Fermé\n\n" +
            "Pour voir le planning des activités, connectez-vous à votre espace parent.",
            "horaire",
            Arrays.asList("Voir les activités", "Comment s'inscrire?", "Nous contacter")
        ));

        // Compte
        faqEntries.add(new FaqEntry(
            Arrays.asList("compte", "connexion", "connecter", "login", "créer compte", "nouveau compte"),
            "Pour votre compte :\n\n" +
            "📝 Créer un compte : Cliquez sur 'Inscription' sur la page d'accueil\n" +
            "🔑 Se connecter : Cliquez sur 'Connexion'\n\n" +
            "Votre email sert d'identifiant.",
            "compte",
            Arrays.asList("J'ai oublié mon mot de passe", "Créer un compte maintenant")
        ));

        // Mot de passe oublié
        faqEntries.add(new FaqEntry(
            Arrays.asList("mot de passe", "password", "oublié", "perdu", "réinitialiser"),
            "Pour récupérer votre mot de passe :\n\n" +
            "Contactez l'administration par email ou téléphone.\n" +
            "📧 info@centremanara.ca\n" +
            "📞 (514) 555-1234\n\n" +
            "Un administrateur vous aidera à réinitialiser votre accès.",
            "password",
            Arrays.asList("Nous contacter", "Retour à l'accueil")
        ));

        // Enfants
        faqEntries.add(new FaqEntry(
            Arrays.asList("enfant", "enfants", "ajouter enfant", "mes enfants", "gérer enfants", "mon enfant"),
            "Pour gérer vos enfants :\n\n" +
            "1️⃣ Connectez-vous à votre espace parent\n" +
            "2️⃣ Allez dans 'Mes enfants'\n" +
            "3️⃣ Cliquez sur '+ Ajouter un enfant'\n" +
            "4️⃣ Remplissez les informations (nom, date de naissance)\n\n" +
            "Chaque enfant doit être enregistré avant de pouvoir l'inscrire aux activités.",
            "enfant",
            Arrays.asList("Inscrire à une activité", "Modifier les infos")
        ));

        // Tarifs
        faqEntries.add(new FaqEntry(
            Arrays.asList("prix", "tarif", "coût", "combien", "payer", "paiement", "gratuit", "argent", "cher"),
            "💰 Informations sur les tarifs :\n\n" +
            "• Les prix varient selon l'activité (10$ - 50$/session)\n" +
            "• Certaines activités sont gratuites\n" +
            "• Réductions pour familles nombreuses (-15%)\n" +
            "• Paiement en ligne ou sur place\n\n" +
            "Contactez-nous pour un devis personnalisé!",
            "tarif",
            Arrays.asList("Nous contacter", "Voir les activités")
        ));

        // Contact
        faqEntries.add(new FaqEntry(
            Arrays.asList("contact", "téléphone", "email", "adresse", "joindre", "appeler", "contacter"),
            "📍 Coordonnées du Centre Manara :\n\n" +
            "📧 Email : info@centremanara.ca\n" +
            "📞 Téléphone : (514) 555-1234\n" +
            "📍 Adresse : 123 rue Exemple, Montréal\n\n" +
            "Heures de bureau : Lun-Ven 8h-18h",
            "contact",
            Arrays.asList("Horaires d'ouverture", "Retour à l'accueil")
        ));

        // Âge
        faqEntries.add(new FaqEntry(
            Arrays.asList("âge", "age", "ans", "limite d'âge", "quel âge", "trop jeune", "trop vieux"),
            "👶 Les activités sont organisées par tranche d'âge :\n\n" +
            "• Petits : 3-5 ans\n" +
            "• Enfants : 6-12 ans\n" +
            "• Adolescents : 13-17 ans\n\n" +
            "Chaque activité indique l'âge minimum et maximum. Quel âge a votre enfant?",
            "age",
            Arrays.asList("Activités 3-5 ans", "Activités 6-12 ans", "Activités ados")
        ));

        // Annulation
        faqEntries.add(new FaqEntry(
            Arrays.asList("annuler", "annulation", "désinscrire", "retirer", "rembourser", "remboursement"),
            "Pour annuler une inscription :\n\n" +
            "1️⃣ Connectez-vous à votre espace parent\n" +
            "2️⃣ Allez dans 'Planning' ou 'Mes inscriptions'\n" +
            "3️⃣ Trouvez l'inscription concernée\n" +
            "4️⃣ Cliquez sur 'Annuler'\n\n" +
            "⚠️ Remboursement possible si annulation 48h avant.",
            "annulation",
            Arrays.asList("Nous contacter", "Politique de remboursement")
        ));

        // Équipement
        faqEntries.add(new FaqEntry(
            Arrays.asList("équipement", "materiel", "matériel", "apporter", "fourni", "vêtement"),
            "🎒 Équipement selon l'activité :\n\n" +
            "⚽ Sports : Vêtements de sport, chaussures adaptées\n" +
            "🏊 Natation : Maillot, bonnet, serviette\n" +
            "🎨 Arts : Matériel fourni par le centre\n" +
            "💃 Danse : Tenue confortable\n\n" +
            "Des précisions sont envoyées par email avant chaque session.",
            "equipement",
            Arrays.asList("Voir les activités", "Nous contacter")
        ));

        // Remerciements
        faqEntries.add(new FaqEntry(
            Arrays.asList("merci", "thanks", "parfait", "super", "excellent", "genial", "génial", "cool"),
            "Je vous en prie! 😊\n\n" +
            "N'hésitez pas si vous avez d'autres questions.\n" +
            "Bonne journée et à bientôt au Centre Manara!",
            "merci",
            Arrays.asList("Autre question", "Retour à l'accueil")
        ));

        // Au revoir
        faqEntries.add(new FaqEntry(
            Arrays.asList("bye", "aurevoir", "au revoir", "à bientôt", "ciao", "bonne journée", "a+"),
            "Au revoir! 👋\n\n" +
            "Merci d'avoir utilisé notre assistant.\n" +
            "À bientôt au Centre Manara!",
            "bye",
            null
        ));

        // Aide
        faqEntries.add(new FaqEntry(
            Arrays.asList("aide", "help", "aidez", "besoin d'aide", "question"),
            "Je peux vous aider avec :\n\n" +
            "📝 Les inscriptions aux activités\n" +
            "📅 Les horaires et le planning\n" +
            "👶 La gestion des enfants\n" +
            "💰 Les tarifs\n" +
            "📞 Les coordonnées du centre\n\n" +
            "Que souhaitez-vous savoir?",
            "aide",
            Arrays.asList("Inscriptions", "Activités", "Horaires", "Contact")
        ));

        // Oui / confirmation
        faqEntries.add(new FaqEntry(
            Arrays.asList("oui", "yes", "ouais", "ok", "d'accord", "bien sûr"),
            "Parfait! 👍\n\nQue puis-je faire pour vous?",
            "oui",
            Arrays.asList("Voir les activités", "Comment s'inscrire?", "Nous contacter")
        ));

        // Non
        faqEntries.add(new FaqEntry(
            Arrays.asList("non", "no", "pas vraiment", "non merci"),
            "D'accord, pas de problème!\n\nY a-t-il autre chose que je peux faire pour vous?",
            "non",
            Arrays.asList("Autre question", "Retour à l'accueil")
        ));
    }

    private void initialiserSuggestions() {
        suggestions = new ArrayList<>();
        suggestions.add("Comment inscrire mon enfant?");
        suggestions.add("Quelles activités proposez-vous?");
        suggestions.add("Quels sont les horaires?");
        suggestions.add("Comment vous contacter?");
    }

    /**
     * Trouve une réponse basée sur le message de l'utilisateur
     */
    public ReponseChat trouverReponse(String messageUtilisateur) {
        if (messageUtilisateur == null || messageUtilisateur.trim().isEmpty()) {
            return new ReponseChat(getReponseParDefaut(), getSuggestionsDefaut());
        }

        String messageLower = messageUtilisateur.toLowerCase().trim();
        
        // Recherche la meilleure correspondance
        FaqEntry meilleurMatch = null;
        int meilleurScore = 0;

        for (FaqEntry entry : faqEntries) {
            int score = calculerScore(messageLower, entry.motsCles);
            if (score > meilleurScore) {
                meilleurScore = score;
                meilleurMatch = entry;
            }
        }

        // Si on a trouvé une correspondance
        if (meilleurMatch != null && meilleurScore > 0) {
            derniereCategorie = meilleurMatch.categorie;
            List<String> sugg = meilleurMatch.suggestionsRelancee != null ? 
                meilleurMatch.suggestionsRelancee : getSuggestionsDefaut();
            return new ReponseChat(meilleurMatch.reponse, sugg);
        }

        // Réponse par défaut
        return new ReponseChat(getReponseParDefaut(), getSuggestionsDefaut());
    }

    private int calculerScore(String message, List<String> motsCles) {
        int score = 0;
        for (String motCle : motsCles) {
            if (message.contains(motCle.toLowerCase())) {
                // Bonus si le mot-clé est plus long (plus spécifique)
                score += motCle.length();
            }
        }
        return score;
    }

    public String getReponseParDefaut() {
        return "Je n'ai pas bien compris votre question. 🤔\n\n" +
               "Essayez de me demander :\n" +
               "• Comment inscrire mon enfant?\n" +
               "• Quelles activités proposez-vous?\n" +
               "• Quels sont les horaires?\n\n" +
               "Ou choisissez une suggestion ci-dessous.";
    }

    public String getMessageBienvenue() {
        return "Bonjour! 👋 Je suis l'assistant du Centre Manara.\n\n" +
               "Comment puis-je vous aider?";
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    private List<String> getSuggestionsDefaut() {
        return Arrays.asList("Inscriptions", "Activités", "Horaires", "Contact");
    }

    // Classe interne pour les entrées FAQ
    private static class FaqEntry {
        List<String> motsCles;
        String reponse;
        String categorie;
        List<String> suggestionsRelancee;

        FaqEntry(List<String> motsCles, String reponse, String categorie, List<String> suggestionsRelancee) {
            this.motsCles = motsCles;
            this.reponse = reponse;
            this.categorie = categorie;
            this.suggestionsRelancee = suggestionsRelancee;
        }
    }

    // Classe pour la réponse avec suggestions
    public static class ReponseChat {
        public String message;
        public List<String> suggestions;

        public ReponseChat(String message, List<String> suggestions) {
            this.message = message;
            this.suggestions = suggestions;
        }
    }
}
