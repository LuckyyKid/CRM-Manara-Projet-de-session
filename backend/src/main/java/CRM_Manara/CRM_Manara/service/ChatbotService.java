package CRM_Manara.CRM_Manara.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {
    private static final Pattern AGE_PATTERN = Pattern.compile("\\b(\\d{1,2})\\s*ans?\\b");

    private final ActivityRecommendationService activityRecommendationService;
    private final List<FaqEntry> faqEntries = new ArrayList<>();
    private final List<String> suggestions = new ArrayList<>();

    public ChatbotService(ActivityRecommendationService activityRecommendationService) {
        this.activityRecommendationService = activityRecommendationService;
        initialiserFAQ();
        initialiserSuggestions();
    }

    private void initialiserFAQ() {
        faqEntries.add(new FaqEntry(
                Arrays.asList("bonjour", "salut", "allo", "hello", "hi", "hey", "bonsoir"),
                "Bonjour. Je suis l'assistant virtuel du Centre Manara.\n\nComment puis-je vous aider aujourd'hui ?",
                Arrays.asList("Trouver une activite pour mon enfant", "Comment inscrire mon enfant ?", "Quels sont les horaires ?")
        ));

        faqEntries.add(new FaqEntry(
                Arrays.asList("inscription", "inscrire", "s'inscrire", "comment inscrire", "enregistrer"),
                "Pour inscrire votre enfant a une activite :\n\n"
                        + "1. Connectez-vous a votre compte parent\n"
                        + "2. Ajoutez votre enfant dans la section Enfants\n"
                        + "3. Ouvrez Activites ou Planning\n"
                        + "4. Choisissez une activite puis cliquez sur S'inscrire",
                Arrays.asList("Creer un compte", "Se connecter", "Trouver une activite pour mon enfant")
        ));

        faqEntries.add(new FaqEntry(
                Arrays.asList("horaire", "horaires", "heure", "planning", "calendrier", "ouverture"),
                "Horaires du Centre Manara :\n\n"
                        + "Lundi a Vendredi : 8h00 - 18h00\n"
                        + "Samedi : 9h00 - 17h00\n"
                        + "Dimanche : Ferme",
                Arrays.asList("Voir les activites", "Comment inscrire mon enfant ?", "Nous contacter")
        ));

        faqEntries.add(new FaqEntry(
                Arrays.asList("compte", "connexion", "connecter", "login", "creer compte", "nouveau compte"),
                "Pour acceder au portail :\n\n"
                        + "Creer un compte : utilisez le bouton Inscription\n"
                        + "Se connecter : utilisez le bouton Connexion\n\n"
                        + "Votre email sert d'identifiant.",
                Arrays.asList("Creer un compte", "Se connecter", "Mot de passe oublie")
        ));

        faqEntries.add(new FaqEntry(
                Arrays.asList("mot de passe", "password", "oublie", "perdu", "reinitialiser"),
                "Pour recuperer votre mot de passe, contactez l'administration du centre.\n\n"
                        + "Email : info@centremanara.ca\n"
                        + "Telephone : (514) 555-1234",
                Arrays.asList("Nous contacter", "Se connecter", "Creer un compte")
        ));

        faqEntries.add(new FaqEntry(
                Arrays.asList("contact", "telephone", "email", "adresse", "joindre", "appeler", "contacter"),
                "Coordonnees du Centre Manara :\n\n"
                        + "Email : info@centremanara.ca\n"
                        + "Telephone : (514) 555-1234\n"
                        + "Adresse : 123 rue Exemple, Montreal",
                Arrays.asList("Quels sont les horaires ?", "Voir les activites", "Trouver une activite pour mon enfant")
        ));

        faqEntries.add(new FaqEntry(
                Arrays.asList("merci", "parfait", "super", "excellent", "genial", "cool"),
                "Avec plaisir. Si vous voulez, je peux aussi vous aider a trouver une activite adaptee a votre enfant.",
                Arrays.asList("Trouver une activite pour mon enfant", "Comment inscrire mon enfant ?")
        ));
    }

    private void initialiserSuggestions() {
        suggestions.add("Trouver une activite pour mon enfant");
        suggestions.add("Comment inscrire mon enfant ?");
        suggestions.add("Quelles activites proposez-vous ?");
        suggestions.add("Quels sont les horaires ?");
    }

    public ReponseChat trouverReponse(String messageUtilisateur) {
        if (messageUtilisateur == null || messageUtilisateur.trim().isEmpty()) {
            return new ReponseChat(getReponseParDefaut(), getSuggestionsDefaut());
        }

        String messageNormalise = normalize(messageUtilisateur);

        if (looksLikeActivityDiscoveryRequest(messageNormalise) && extractAge(messageNormalise) == null) {
            return new ReponseChat(
                    "Pour vous recommander une activite pertinente, j'ai d'abord besoin de l'age de votre enfant.\n\n"
                            + "Ensuite, vous pouvez aussi me decrire sa personnalite, ses interets et la difficulte que vous souhaitez travailler.",
                    Arrays.asList(
                            "Mon enfant a 8 ans et a besoin d'aide aux devoirs",
                            "Mon enfant a 12 ans, il aime bouger et depenser son energie",
                            "Mon enfant a 14 ans, il aime la musique et la creativite"
                    )
            );
        }

        if (looksLikeActivityDiscoveryRequest(messageNormalise) && !hasEnoughDiscoveryDetails(messageNormalise)) {
            return new ReponseChat(
                    "Je peux vous aider a trouver une activite adaptee.\n\n"
                            + "Decrivez-moi votre enfant avec un peu de detail :\n"
                            + "- son age\n"
                            + "- sa personnalite ou ses interets\n"
                            + "- la difficulte ou le besoin que vous souhaitez travailler\n\n"
                            + "Exemple : Mon enfant a 10 ans, il est reserve, il a besoin de reprendre confiance et il aime dessiner.",
                    Arrays.asList(
                            "Mon enfant a 8 ans et a besoin d'aide aux devoirs",
                            "Mon enfant a 12 ans, il aime bouger et depenser son energie",
                            "Mon enfant a 14 ans, il aime la musique et la creativite"
                    )
            );
        }

        RecommendationResult recommendation = buildRecommendation(messageNormalise);
        if (recommendation != null) {
            return new ReponseChat(recommendation.message(), recommendation.suggestions());
        }

        FaqEntry bestMatch = null;
        int bestScore = 0;
        for (FaqEntry entry : faqEntries) {
            int score = calculerScore(messageNormalise, entry.keywords());
            if (score > bestScore) {
                bestScore = score;
                bestMatch = entry;
            }
        }

        if (bestMatch != null && bestScore > 0) {
            return new ReponseChat(bestMatch.response(), bestMatch.followUpSuggestions());
        }

        return new ReponseChat(getReponseParDefaut(), getSuggestionsDefaut());
    }

    private RecommendationResult buildRecommendation(String message) {
        if (!shouldTryRecommendation(message)) {
            return null;
        }

        Integer age = extractAge(message);
        ActivityRecommendationService.RecommendationResponse response =
                activityRecommendationService.recommend(age, message, message);

        if (response.recommendations().isEmpty()) {
            if (age != null) {
                return new RecommendationResult(
                        "Je n'ai pas trouve de programme parfaitement adapte avec ces details pour le moment.\n\n"
                                + "Essayez de me donner un peu plus d'information sur les interets de votre enfant ou le besoin a travailler.",
                        Arrays.asList(
                                "Mon enfant a " + age + " ans et aime bouger",
                                "Mon enfant a " + age + " ans et a besoin de soutien scolaire",
                                "Mon enfant a " + age + " ans et aime creer"
                        )
                );
            }
            return null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Voici les programmes qui semblent le mieux correspondre :\n\n");
        for (ActivityRecommendationService.RecommendationItem activity : response.recommendations()) {
            builder.append("- ")
                    .append(activity.activityName())
                    .append(" (")
                    .append(activity.ageMin())
                    .append(" a ")
                    .append(activity.ageMax())
                    .append(" ans) : ")
                    .append(activity.reason());
            builder.append("\n");
        }
        builder.append("\nVous pouvez ensuite ouvrir la section Activites pour consulter les details et vous inscrire.");

        List<String> followUps = response.recommendations().stream()
                .map(ActivityRecommendationService.RecommendationItem::activityName)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        followUps.add("Comment inscrire mon enfant ?");
        return new RecommendationResult(builder.toString(), followUps);
    }

    private boolean shouldTryRecommendation(String message) {
        return looksLikeActivityDiscoveryRequest(message)
                || containsAny(message, "mon enfant", "ma fille", "mon fils", "il aime", "elle aime", "probleme", "difficulte")
                || extractAge(message) != null;
    }

    private boolean looksLikeActivityDiscoveryRequest(String message) {
        return containsAny(message,
                "trouver une activite",
                "quelle activite",
                "quelles activites",
                "recommande",
                "recommendation",
                "programme pour",
                "programme adapte",
                "quelle serait la meilleure activite",
                "je cherche une activite");
    }

    private boolean hasEnoughDiscoveryDetails(String message) {
        return extractAge(message) != null
                || containsAny(message, "timide", "reserve", "energie", "creatif", "creativite", "devoir", "concentration", "anxieux", "sport", "musique", "dessin", "lecture");
    }

    private Integer extractAge(String message) {
        Matcher matcher = AGE_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int calculerScore(String message, List<String> keywords) {
        int score = 0;
        for (String keyword : keywords) {
            if (message.contains(normalize(keyword))) {
                score += keyword.length();
            }
        }
        return score;
    }

    public String getReponseParDefaut() {
        return "Je peux vous aider pour les inscriptions, les horaires, le contact du centre et aussi pour trouver une activite adaptee a votre enfant.\n\n"
                + "Essayez par exemple : Mon enfant a 11 ans, il a besoin d'aide en mathematiques et il manque de confiance.";
    }

    public String getMessageBienvenue() {
        return "Bonjour. Je suis l'assistant du Centre Manara.\n\n"
                + "Je peux repondre a vos questions et vous aider a trouver une activite adaptee a votre enfant.";
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    private List<String> getSuggestionsDefaut() {
        return Arrays.asList(
                "Trouver une activite pour mon enfant",
                "Comment inscrire mon enfant ?",
                "Quels sont les horaires ?",
                "Comment vous contacter ?"
        );
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private record FaqEntry(List<String> keywords, String response, List<String> followUpSuggestions) {}

    private record RecommendationResult(String message, List<String> suggestions) {}
    public static class ReponseChat {
        public String message;
        public List<String> suggestions;

        public ReponseChat(String message, List<String> suggestions) {
            this.message = message;
            this.suggestions = suggestions;
        }
    }
}
