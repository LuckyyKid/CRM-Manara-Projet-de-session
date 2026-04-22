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
    private static final int MAX_TOKENS = 3200;

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
                                                boolean reviewMode,
                                                GeneratedHomework draftHomework) {
        if (apiKey.isBlank()) {
            LOGGER.warn("Anthropic homework generation non disponible. Utilisation du fallback local.");
            return Optional.empty();
        }

        try {
            LOGGER.info("Appel Anthropic pour devoir. model={}, reviewMode={}, activityName={}, weakAxisCount={}, draftExerciseCount={}",
                    model, reviewMode, activityName, weakAxes.size(), draftHomework.exercises().size());
            String response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(activityName, childName, weakAxes, reviewMode, draftHomework))
                    .retrieve()
                    .body(String.class);
            LOGGER.info("Reponse Anthropic recue. responseLength={}", response == null ? 0 : response.length());
            GeneratedHomework generatedHomework = parseResponse(response);
            LOGGER.info("Devoir genere par Anthropic avec {} exercices.", generatedHomework.exercises().size());
            return Optional.of(generatedHomework);
        } catch (RestClientException | JacksonException | IllegalArgumentException exception) {
            LOGGER.warn("Anthropic homework generation echouee. activityName={}, reviewMode={}, weakAxisCount={}, draftExerciseCount={}. Utilisation du fallback local.",
                    activityName, reviewMode, weakAxes.size(), draftHomework.exercises().size(), exception);
            return Optional.empty();
        }
    }

    private Map<String, Object> buildRequest(String activityName,
                                             String childName,
                                             List<HomeworkService.AxisNeed> weakAxes,
                                             boolean reviewMode,
                                             GeneratedHomework draftHomework) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("max_tokens", MAX_TOKENS);
        request.put("temperature", 0.2);
        request.put("system", """
                Tu es un tuteur pedagogique.
                Tu generes des devoirs personnalises en francais.
                Renvoie uniquement un objet JSON valide, sans Markdown.
                Les exercices doivent progresser de facile vers difficile et cibler les erreurs observees.
                Sois concis: titre court, summary en une phrase, enonces courts, expectedAnswer courte mais concrete.
                """);
        request.put("messages", List.of(Map.of(
                "role", "user",
                "content", buildPrompt(activityName, childName, weakAxes, reviewMode, draftHomework)
        )));
        return request;
    }

    private String buildPrompt(String activityName,
                               String childName,
                               List<HomeworkService.AxisNeed> weakAxes,
                               boolean reviewMode,
                               GeneratedHomework draftHomework) {
        StringBuilder axesText = new StringBuilder();
        for (HomeworkService.AxisNeed axis : weakAxes) {
            axesText.append("- Axe: ").append(axis.axisTitle()).append("\n")
                    .append("  Score recent: ").append(Math.round(axis.averageScore())).append("%\n")
                    .append("  Difficulte cible: ").append(axis.targetDifficulty()).append("\n")
                    .append("  Nombre d'exercices: ").append(axis.exerciseCount()).append("\n")
                    .append("  Erreurs observees: ").append(axis.mistakes().isEmpty() ? "aucune detaillee" : String.join(" | ", axis.mistakes())).append("\n");
        }
        StringBuilder draftText = new StringBuilder();
        for (int i = 0; i < draftHomework.exercises().size(); i++) {
            GeneratedExercise exercise = draftHomework.exercises().get(i);
            draftText.append("- Exercice ").append(i + 1)
                    .append(" | axe=").append(exercise.axisTitle())
                    .append(" | difficulte=").append(exercise.difficulty())
                    .append(" | question=").append(exercise.questionText())
                    .append(" | attendu=").append(exercise.expectedAnswer())
                    .append(" | erreur=").append(blankToDefault(exercise.targetMistake(), "aucune"))
                    .append("\n");
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
                6. Reste tres concis pour que le JSON tienne en entier dans la reponse.
                7. Tu pars du brouillon fourni ci-dessous. Tu peux ameliorer la formulation, mais tu dois garder le meme nombre d'exercices, le meme axe et la meme difficulte pour chaque position.

                Brouillon de devoir a respecter :
                %s

                Format JSON obligatoire :
                {
                  "title": "Titre du devoir",
                  "summary": "Resume court",
                  "exercises": [
                    {
                      "axisTitle": "Titre axe",
                      "type": "OPEN" ou "CHOICE",
                      "difficulty": "FACILE",
                      "questionText": "Enonce",
                      "expectedAnswer": "Reponse attendue",
                      "targetMistake": "Erreur visee ou vide",
                      "options": ["Choix 1", "Choix 2", "Choix 3", "Choix 4"]
                    }
                  ]
                }
                """.formatted(
                blankToDefault(activityName, "Activite"),
                blankToDefault(childName, "Eleve"),
                reviewMode ? "REVISION ESPACEE" : "REMEDIATION",
                axesText,
                draftText
        );
    }

    private GeneratedHomework parseResponse(String response) throws JacksonException {
        JsonNode root = objectMapper.readTree(response);
        AnthropicContentResult contentResult = extractContent(root);
        if ("max_tokens".equalsIgnoreCase(contentResult.stopReason())) {
            LOGGER.warn("Anthropic homework generation stopped because max_tokens={} was reached. Attempting recovery from partial JSON.", MAX_TOKENS);
        }
        JsonNode homeworkNode = parseHomeworkNode(contentResult.text());
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
                    normalizeType(node.path("type").asText("OPEN")),
                    requiredText(node, "difficulty"),
                    requiredText(node, "questionText"),
                    requiredText(node, "expectedAnswer"),
                    node.path("targetMistake").asText("").trim(),
                    parseOptions(node.path("options"))
            ));
        }
        LOGGER.info("JSON devoir Anthropic parse. title='{}', exerciseCount={}", title, exercises.size());
        return new GeneratedHomework(title, summary, exercises);
    }

    private JsonNode parseHomeworkNode(String generatedText) throws JacksonException {
        String rawJson = extractJsonCandidate(generatedText);
        try {
            return objectMapper.readTree(extractJson(rawJson));
        } catch (JacksonException | IllegalArgumentException exception) {
            LOGGER.warn("JSON Anthropic invalide detecte. Tentative de reparation. rawLength={}", rawJson.length());
            String repairedJson = recoverHomeworkJson(rawJson);
            LOGGER.info("JSON devoir repare. repairedLength={}", repairedJson.length());
            return objectMapper.readTree(repairedJson);
        }
    }

    private AnthropicContentResult extractContent(JsonNode root) {
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
        return new AnthropicContentResult(generatedText.toString(), root.path("stop_reason").asText(""));
    }

    private String extractJsonCandidate(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "").trim();
        }
        int start = cleaned.indexOf('{');
        if (start < 0) {
            throw new IllegalArgumentException("Anthropic response does not contain JSON.");
        }
        return cleaned.substring(start);
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

    private String recoverHomeworkJson(String rawJson) {
        String title = extractStringField(rawJson, "title");
        String summary = extractStringField(rawJson, "summary");
        List<String> exerciseObjects = extractExerciseObjects(rawJson);
        if (exerciseObjects.isEmpty()) {
            throw new IllegalArgumentException("Anthropic response does not contain any complete homework exercise.");
        }

        StringBuilder recovered = new StringBuilder();
        recovered.append("{");
        recovered.append("\"title\":").append(toJsonString(blankToDefault(title, "Devoir personnalise"))).append(",");
        recovered.append("\"summary\":").append(toJsonString(blankToDefault(summary, "Serie d'exercices cibles sur les axes a consolider."))).append(",");
        recovered.append("\"exercises\":[");
        for (int i = 0; i < exerciseObjects.size(); i++) {
            if (i > 0) {
                recovered.append(",");
            }
            recovered.append(exerciseObjects.get(i));
        }
        recovered.append("]}");
        return recovered.toString();
    }

    private String extractStringField(String text, String fieldName) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + java.util.regex.Pattern.quote(fieldName) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                .matcher(text);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private List<String> extractExerciseObjects(String text) {
        List<String> objects = new ArrayList<>();
        int exercisesIndex = text.indexOf("\"exercises\"");
        if (exercisesIndex < 0) {
            return objects;
        }
        int arrayStart = text.indexOf('[', exercisesIndex);
        if (arrayStart < 0) {
            return objects;
        }

        boolean inString = false;
        boolean escaping = false;
        int depth = 0;
        int objectStart = -1;
        for (int i = arrayStart + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '{') {
                if (depth == 0) {
                    objectStart = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objectStart >= 0) {
                    objects.add(text.substring(objectStart, i + 1));
                    objectStart = -1;
                }
            } else if (c == ']' && depth == 0) {
                break;
            }
        }
        return objects;
    }

    private String toJsonString(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de serialiser le JSON de devoir.", exception);
        }
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText("").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Anthropic returned a blank " + fieldName + ".");
        }
        return value;
    }

    private String normalizeType(String value) {
        return "CHOICE".equalsIgnoreCase(value) ? "CHOICE" : "OPEN";
    }

    private List<String> parseOptions(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        for (JsonNode optionNode : node) {
            String option = optionNode.asText("").trim();
            if (!option.isBlank()) {
                options.add(option);
            }
        }
        return options;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record GeneratedHomework(String title, String summary, List<GeneratedExercise> exercises) {
    }

    public record GeneratedExercise(String axisTitle,
                                    String type,
                                    String difficulty,
                                    String questionText,
                                    String expectedAnswer,
                                    String targetMistake,
                                    List<String> options) {
    }

    private record AnthropicContentResult(String text, String stopReason) {
    }
}
