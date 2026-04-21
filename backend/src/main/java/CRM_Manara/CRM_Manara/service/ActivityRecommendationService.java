package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ActivityRecommendationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityRecommendationService.class);

    private final AdminService adminService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public ActivityRecommendationService(
            AdminService adminService,
            @Value("${anthropic.api.key:${ANTHROPIC_API_KEY:}}") String apiKey,
            @Value("${anthropic.model:claude-sonnet-4-6}") String model
    ) {
        this.adminService = adminService;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "claude-sonnet-4-6" : model.trim();
    }

    private static final List<RecommendationCandidate> MOCK_CANDIDATES = List.of(
            new RecommendationCandidate(
                    "mock-camp-ete",
                    null,
                    "Camp de jour ete",
                    "Une experience estivale avec jeux, sorties creatrices et activites sportives adaptees au rythme des plus jeunes.",
                    6,
                    12,
                    typeActivity.SPORT,
                    "mock"
            ),
            new RecommendationCandidate(
                    "mock-robotique",
                    null,
                    "Atelier robotique",
                    "Decouverte de la robotique, logique de programmation et creation de projets concrets en petit groupe.",
                    12,
                    17,
                    typeActivity.TUTORAT,
                    "mock"
            )
    );

    public RecommendationResponse recommend(Integer age, String profile, String goal) {
        List<RecommendationCandidate> eligibleActivities = loadEligibleActivities(age);
        if (eligibleActivities.isEmpty()) {
            return new RecommendationResponse(
                    "NONE",
                    "Aucune activite disponible ne correspond actuellement a cet age.",
                    List.of()
            );
        }

        if (apiKey.isBlank()) {
            LOGGER.warn("Anthropic activity recommendation non disponible. Utilisation du fallback local.");
            return fallbackRecommendation(age, profile, goal, eligibleActivities);
        }

        try {
            String response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(age, profile, goal, eligibleActivities))
                    .retrieve()
                    .body(String.class);

            RecommendationResponse recommendation = parseResponse(response, eligibleActivities);
            LOGGER.info("Recommandations d'activites generees par Anthropic avec {} resultats.", recommendation.recommendations().size());
            return recommendation;
        } catch (RestClientException | JacksonException | IllegalArgumentException exception) {
            LOGGER.warn("Anthropic activity recommendation echouee. Utilisation du fallback local.", exception);
            return fallbackRecommendation(age, profile, goal, eligibleActivities);
        }
    }

    private List<RecommendationCandidate> loadEligibleActivities(Integer age) {
        List<RecommendationCandidate> allCandidates = new ArrayList<>();
        allCandidates.addAll(adminService.getAllActivities().stream()
                .filter(activity -> activity.getStatus() == null || !activity.getStatus().name().startsWith("ANNUL"))
                .map(this::toCandidate)
                .toList());
        allCandidates.addAll(MOCK_CANDIDATES);

        return allCandidates.stream()
                .filter(activity -> age == null || (age >= activity.ageMin() && age <= activity.ageMax()))
                .sorted(Comparator.comparing(RecommendationCandidate::activityName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private Map<String, Object> buildRequest(Integer age, String profile, String goal, List<RecommendationCandidate> eligibleActivities) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("max_tokens", 1400);
        request.put("temperature", 0.2);
        request.put("system", """
                Tu es un conseiller pedagogique et parascolaire.
                Tu dois recommander uniquement parmi la liste exacte des activites fournie.
                Respecte strictement l'age fourni. Si l'age est connu, ne recommande jamais une activite hors tranche d'age.
                Ne cree aucune activite fictive.
                Renvoie uniquement un objet JSON valide, sans Markdown.
                """);
        request.put("messages", List.of(Map.of(
                "role", "user",
                "content", buildPrompt(age, profile, goal, eligibleActivities)
        )));
        return request;
    }

    private String buildPrompt(Integer age, String profile, String goal, List<RecommendationCandidate> eligibleActivities) {
        StringBuilder builder = new StringBuilder();
        builder.append("Age de l'enfant : ")
                .append(age == null ? "non precise" : age + " ans")
                .append("\n");
        builder.append("Personnalite et interets : ").append(blankToDefault(profile, "non precis")).append("\n");
        builder.append("Besoin ou problematique : ").append(blankToDefault(goal, "non precise")).append("\n\n");
        builder.append("Liste des activites disponibles :\n");

        for (RecommendationCandidate activity : eligibleActivities) {
            builder.append("- catalogId=")
                    .append(activity.catalogId())
                    .append(" | activityId=")
                    .append(activity.activityId() == null ? "null" : activity.activityId())
                    .append(" | nom=")
                    .append(blankToDefault(activity.activityName(), "Sans nom"))
                    .append(" | age=")
                    .append(activity.ageMin())
                    .append("-")
                    .append(activity.ageMax())
                    .append(" | type=")
                    .append(activity.type() == null ? "non precise" : activity.type().name())
                    .append(" | description=")
                    .append(blankToDefault(activity.description(), "Aucune description"))
                    .append("\n");
        }

        builder.append("""

                Choisis les 3 meilleures activites maximum.
                Base-toi sur la personnalite, les interets et le besoin.
                Tu ne cherches pas un match parfait. Tu dois aider le parent.
                S'il existe des activites compatibles avec l'age, classe toujours les meilleures options disponibles.
                Ne renvoie un tableau vide que s'il n'existe vraiment aucune activite dans la liste.

                Renvoie exactement ce JSON :
                {
                  "summary": "Resume bref de la recommandation",
                  "recommendations": [
                    {
                      "catalogId": "db-123",
                      "reason": "Pourquoi cette activite convient",
                      "matchScore": 87
                    }
                  ]
                }
                """);
        return builder.toString();
    }

    private RecommendationResponse parseResponse(String response, List<RecommendationCandidate> eligibleActivities) throws JacksonException {
        JsonNode root = objectMapper.readTree(response);
        String generatedText = extractText(root);
        JsonNode recommendationNode = objectMapper.readTree(extractJson(generatedText));

        String summary = recommendationNode.path("summary").asText("").trim();
        JsonNode recommendationsNode = recommendationNode.path("recommendations");
        if (!recommendationsNode.isArray()) {
            throw new IllegalArgumentException("Anthropic response does not contain a recommendations array.");
        }

        Map<String, RecommendationCandidate> activitiesById = new LinkedHashMap<>();
        for (RecommendationCandidate activity : eligibleActivities) {
            activitiesById.put(activity.catalogId(), activity);
        }

        List<RecommendationItem> recommendations = new ArrayList<>();
        for (JsonNode itemNode : recommendationsNode) {
            String catalogId = itemNode.path("catalogId").asText("").trim();
            RecommendationCandidate activity = activitiesById.get(catalogId);
            if (activity == null) {
                continue;
            }
            String reason = itemNode.path("reason").asText("").trim();
            int matchScore = clampMatchScore(itemNode.path("matchScore").asInt(0));
            recommendations.add(toRecommendationItem(activity, reason, matchScore));
        }

        if (recommendations.isEmpty() && !eligibleActivities.isEmpty()) {
            return fallbackRecommendation(null, null, null, eligibleActivities);
        }

        return new RecommendationResponse(
                "AI",
                summary.isBlank() ? "Voici les activites qui correspondent le mieux au profil fourni." : summary,
                recommendations.stream().limit(3).toList()
        );
    }

    private RecommendationResponse fallbackRecommendation(Integer age, String profile, String goal, List<RecommendationCandidate> eligibleActivities) {
        String message = normalize(blankToDefault(profile, "") + " " + blankToDefault(goal, ""));
        List<RecommendationItem> recommendations = eligibleActivities.stream()
                .map(activity -> new ScoredRecommendation(activity, scoreActivity(activity, message, age)))
                .sorted(Comparator.comparingInt(ScoredRecommendation::score).reversed())
                .limit(3)
                .map(scored -> toRecommendationItem(
                        scored.activity(),
                        buildFallbackReason(scored.activity(), message),
                        scored.score()
                ))
                .toList();

        return new RecommendationResponse(
                "FALLBACK",
                recommendations.isEmpty()
                        ? "Aucune activite ne ressort clairement avec les informations fournies."
                        : "Voici les activites qui semblent le mieux correspondre au profil fourni.",
                recommendations
        );
    }

    private int scoreActivity(RecommendationCandidate activity, String message, Integer age) {
        int score = 35;
        if (age != null) {
            if (age < activity.ageMin() || age > activity.ageMax()) {
                return 0;
            }
            score += 25;
        }

        String haystack = normalize(
                blankToDefault(activity.activityName(), "") + " "
                        + blankToDefault(activity.description(), "") + " "
                        + (activity.type() == null ? "" : activity.type().name())
        );

        for (String keyword : recommendationKeywords()) {
            if (message.contains(keyword) && haystack.contains(keyword)) {
                score += 8;
            }
        }

        if (activity.type() != null) {
            score += scoreByActivityType(activity.type(), message);
        }

        return clampMatchScore(score);
    }

    private int scoreByActivityType(typeActivity type, String message) {
        return switch (type) {
            case TUTORAT -> containsAny(message, "devoir", "ecole", "scolaire", "math", "francais", "calcul", "concentration", "tutorat") ? 20 : 0;
            case SPORT -> containsAny(message, "sport", "bouger", "energie", "depense", "equipe", "physique", "moteur") ? 20 : 0;
            case MUSIQUE -> containsAny(message, "musique", "rythme", "chanter", "instrument", "artistique") ? 20 : 0;
            case ART -> containsAny(message, "dessin", "creatif", "creativite", "art", "peinture", "manuel", "imaginer") ? 20 : 0;
            case LECTURE -> containsAny(message, "lecture", "lire", "langue", "vocabulaire", "expression") ? 20 : 0;
        };
    }

    private String buildFallbackReason(RecommendationCandidate activity, String message) {
        if (activity.type() != null) {
            return switch (activity.type()) {
                case TUTORAT -> "Cette activite soutient les apprentissages scolaires et le travail academique.";
                case SPORT -> "Cette activite aide a canaliser l'energie et favorise le mouvement.";
                case MUSIQUE -> "Cette activite convient bien a un enfant attire par le rythme et l'expression.";
                case ART -> "Cette activite valorise la creativite, l'expression et le travail manuel.";
                case LECTURE -> "Cette activite renforce le langage, la lecture et l'expression.";
            };
        }

        if (containsAny(message, "confiance", "social", "amis", "timide", "reserve")) {
            return "Cette activite peut aider a developper la confiance et l'engagement.";
        }

        return "Cette activite semble compatible avec les informations fournies.";
    }

    private RecommendationItem toRecommendationItem(RecommendationCandidate activity, String reason, int matchScore) {
        return new RecommendationItem(
                activity.catalogId(),
                activity.activityId(),
                blankToDefault(activity.activityName(), "Sans nom"),
                blankToDefault(activity.description(), "Description a venir."),
                activity.ageMin(),
                activity.ageMax(),
                activity.type() == null ? null : activity.type().name(),
                reason == null || reason.isBlank() ? "Cette activite semble compatible avec le profil fourni." : reason.trim(),
                clampMatchScore(matchScore)
        );
    }

    private RecommendationCandidate toCandidate(Activity activity) {
        return new RecommendationCandidate(
                "db-" + activity.getId(),
                activity.getId(),
                blankToDefault(activity.getActivyName(), "Sans nom"),
                blankToDefault(activity.getDescription(), "Description a venir."),
                activity.getAgeMin(),
                activity.getAgeMax(),
                activity.getType(),
                "db"
        );
    }

    private String extractText(JsonNode root) {
        StringBuilder generatedText = new StringBuilder();
        JsonNode content = root.path("content");
        if (!content.isArray()) {
            throw new IllegalArgumentException("Anthropic returned no content.");
        }
        for (JsonNode block : content) {
            String text = block.path("text").asText("");
            if (!text.isBlank()) {
                generatedText.append(text);
            }
        }
        if (generatedText.isEmpty()) {
            throw new IllegalArgumentException("Anthropic returned an empty response.");
        }
        return generatedText.toString();
    }

    private String extractJson(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Anthropic response does not contain JSON.");
        }
        return cleaned.substring(start, end + 1);
    }

    private List<String> recommendationKeywords() {
        Set<String> keywords = new LinkedHashSet<>();
        keywords.addAll(Arrays.asList(
                "devoir", "ecole", "math", "francais", "calcul", "concentration",
                "sport", "bouger", "energie", "equipe", "moteur",
                "musique", "rythme", "instrument", "artistique",
                "dessin", "art", "creatif", "creativite", "peinture",
                "lecture", "langue", "vocabulaire", "robot", "robotique", "technologie",
                "confiance", "social", "timide", "reserve"
        ));
        return new ArrayList<>(keywords);
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private int clampMatchScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record RecommendationCandidate(
            String catalogId,
            Long activityId,
            String activityName,
            String description,
            int ageMin,
            int ageMax,
            typeActivity type,
            String source
    ) {
    }

    private record ScoredRecommendation(RecommendationCandidate activity, int score) {
    }

    public record RecommendationResponse(String source, String summary, List<RecommendationItem> recommendations) {
    }

    public record RecommendationItem(
            String catalogId,
            Long activityId,
            String activityName,
            String description,
            int ageMin,
            int ageMax,
            String type,
            String reason,
            int matchScore
    ) {
    }
}
