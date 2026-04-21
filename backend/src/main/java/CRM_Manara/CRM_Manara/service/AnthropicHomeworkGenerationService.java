package CRM_Manara.CRM_Manara.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class AnthropicHomeworkGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicHomeworkGenerationService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AnthropicHomeworkGenerationService(@Value("${anthropic.api.key:${ANTHROPIC_API_KEY:}}") String apiKey,
                                              @Value("${anthropic.model:claude-sonnet-4-6}") String model) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "claude-sonnet-4-6" : model.trim();
    }

    public Optional<GeneratedHomework> generate(String activityName,
                                                String childName,
                                                List<HomeworkService.AxisNeed> weakAxes,
                                                boolean reviewMode) {
        if (apiKey.isBlank()) {
            LOGGER.warn("Anthropic homework generation non disponible. Utilisation du fallback local.");
            return Optional.empty();
        }

        try {
            String response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(activityName, childName, weakAxes, reviewMode))
                    .retrieve()
                    .body(String.class);
            GeneratedHomework generatedHomework = parseResponse(response);
            LOGGER.info("Devoir genere par Anthropic avec {} exercices.", generatedHomework.exercises().size());
            return Optional.of(generatedHomework);
        } catch (RestClientException | JacksonException | IllegalArgumentException exception) {
            LOGGER.warn("Anthropic homework generation echouee. Utilisation du fallback local.", exception);
            return Optional.empty();
        }
    }

    private Map<String, Object> buildRequest(String activityName,
                                             String childName,
                                             List<HomeworkService.AxisNeed> weakAxes,
                                             boolean reviewMode) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("max_tokens", 2200);
        request.put("temperature", 0.3);
        request.put("system", """
                Tu es un tuteur pedagogique.
                Tu generes des devoirs personnalises en francais.
                Renvoie uniquement un objet JSON valide, sans Markdown.
                Les exercices doivent progresser de facile vers difficile et cibler les erreurs observees.
                """);
        request.put("messages", List.of(Map.of(
                "role", "user",
                "content", buildPrompt(activityName, childName, weakAxes, reviewMode)
        )));
        return request;
    }

    private String buildPrompt(String activityName,
                               String childName,
                               List<HomeworkService.AxisNeed> weakAxes,
                               boolean reviewMode) {
        StringBuilder axesText = new StringBuilder();
        for (HomeworkService.AxisNeed axis : weakAxes) {
            axesText.append("- Axe: ").append(axis.axisTitle()).append("\n")
                    .append("  Score recent: ").append(Math.round(axis.averageScore())).append("%\n")
                    .append("  Difficulte cible: ").append(axis.targetDifficulty()).append("\n")
                    .append("  Nombre d'exercices: ").append(axis.exerciseCount()).append("\n")
                    .append("  Erreurs observees: ").append(axis.mistakes().isEmpty() ? "aucune detaillee" : String.join(" | ", axis.mistakes())).append("\n");
        }

        return """
                Activite: %s
                Eleve: %s
                Mode: %s

                Axes a travailler :
                %s

                Consignes :
                1. Cree exactement le nombre d'exercices demande pour chaque axe.
                2. La progression doit aller de facile vers difficile.
                3. Les exercices doivent etre differents de ceux du quiz.
                4. Chaque expectedAnswer doit contenir une vraie reponse attendue.
                5. Chaque exercice doit inclure un targetMistake si une erreur typique est connue.

                Format JSON obligatoire :
                {
                  "title": "Titre du devoir",
                  "summary": "Resume court",
                  "exercises": [
                    {
                      "axisTitle": "Titre axe",
                      "difficulty": "FACILE",
                      "questionText": "Enonce",
                      "expectedAnswer": "Reponse attendue",
                      "targetMistake": "Erreur visee ou vide"
                    }
                  ]
                }
                """.formatted(
                blankToDefault(activityName, "Activite"),
                blankToDefault(childName, "Eleve"),
                reviewMode ? "REVISION ESPACEE" : "REMEDIATION",
                axesText
        );
    }

    private GeneratedHomework parseResponse(String response) throws JacksonException {
        JsonNode root = objectMapper.readTree(response);
        String generatedText = extractText(root);
        JsonNode homeworkNode = objectMapper.readTree(extractJson(generatedText));
        String title = requiredText(homeworkNode, "title");
        String summary = requiredText(homeworkNode, "summary");
        JsonNode exercisesNode = homeworkNode.path("exercises");
        if (!exercisesNode.isArray() || exercisesNode.isEmpty()) {
            throw new IllegalArgumentException("Anthropic returned no homework exercises.");
        }

        List<GeneratedExercise> exercises = new ArrayList<>();
        for (JsonNode node : exercisesNode) {
            exercises.add(new GeneratedExercise(
                    requiredText(node, "axisTitle"),
                    requiredText(node, "difficulty"),
                    requiredText(node, "questionText"),
                    requiredText(node, "expectedAnswer"),
                    node.path("targetMistake").asText("").trim()
            ));
        }
        return new GeneratedHomework(title, summary, exercises);
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

    private String requiredText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText("").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Anthropic returned a blank " + fieldName + ".");
        }
        return value;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record GeneratedHomework(String title, String summary, List<GeneratedExercise> exercises) {
    }

    public record GeneratedExercise(String axisTitle,
                                    String difficulty,
                                    String questionText,
                                    String expectedAnswer,
                                    String targetMistake) {
    }
}
