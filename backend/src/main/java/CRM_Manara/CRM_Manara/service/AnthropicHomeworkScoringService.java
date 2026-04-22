package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.HomeworkAnswer;
import CRM_Manara.CRM_Manara.Model.Entity.HomeworkAttempt;
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
public class AnthropicHomeworkScoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicHomeworkScoringService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AnthropicHomeworkScoringService(@Value("${anthropic.api.key:${ANTHROPIC_API_KEY:}}") String apiKey,
                                           @Value("${anthropic.model:claude-sonnet-4-6}") String model) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "claude-sonnet-4-6" : model.trim();
    }

    public Optional<ScoringResult> scoreAttempt(HomeworkAttempt attempt) {
        if (attempt == null || attempt.getAnswers().isEmpty()) {
            return Optional.of(new ScoringResult(0, "SCORED_AI"));
        }
        if (apiKey.isBlank()) {
            LOGGER.warn("Correction IA du devoir indisponible: cle Anthropic absente. attemptId={}", attempt.getId());
            return Optional.empty();
        }

        try {
            String response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(attempt))
                    .retrieve()
                    .body(String.class);
            ScoringResult result = parseResponse(response);
            LOGGER.info("Devoir {} corrige par Anthropic a {}%.", attempt.getId(), Math.round(result.scorePercent()));
            return Optional.of(result);
        } catch (RestClientException | JacksonException | IllegalArgumentException exception) {
            LOGGER.warn("Correction IA du devoir echouee. attemptId={}", attempt.getId(), exception);
            return Optional.empty();
        }
    }

    private Map<String, Object> buildRequest(HomeworkAttempt attempt) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("max_tokens", 1200);
        request.put("temperature", 0.1);
        request.put("system", """
                Tu es un correcteur pedagogique.
                Tu corriges un devoir personnalise et attribues un score global sur 100.
                Renvoie uniquement un objet JSON valide, sans Markdown.
                Une reponse vide, evasive, hors sujet ou du type "je ne sais pas" doit etre notee comme incorrecte.
                Les questions a choix doivent etre correctes seulement si le bon choix exact est selectionne.
                """);
        request.put("messages", List.of(Map.of(
                "role", "user",
                "content", buildPrompt(attempt)
        )));
        return request;
    }

    private String buildPrompt(HomeworkAttempt attempt) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Devoir: ").append(attempt.getAssignment().getTitle()).append("\n");
        prompt.append("Eleve: ").append(attempt.getEnfant().getPrenom()).append(" ").append(attempt.getEnfant().getNom()).append("\n\n");
        prompt.append("Corrige les reponses ci-dessous et donne un score global strict sur 100.\n\n");

        int index = 1;
        for (HomeworkAnswer answer : attempt.getAnswers()) {
            prompt.append("Exercice ").append(index++).append("\n");
            prompt.append("Axe: ").append(answer.getExercise().getAxisTitle()).append("\n");
            prompt.append("Type: ").append(answer.getExercise().getType()).append("\n");
            prompt.append("Difficulte: ").append(answer.getExercise().getDifficulty()).append("\n");
            prompt.append("Question: ").append(answer.getExercise().getQuestionText()).append("\n");
            prompt.append("Attendu: ").append(answer.getExercise().getExpectedAnswer()).append("\n");
            if (!answer.getExercise().getOptions().isEmpty()) {
                prompt.append("Choix possibles: ").append(String.join(" | ", answer.getExercise().getOptions())).append("\n");
            }
            prompt.append("Reponse eleve: ").append(answer.getAnswerText()).append("\n\n");
        }

        prompt.append("""
                Renvoie exactement ce JSON :
                {
                  "scorePercent": 0,
                  "status": "SCORED_AI"
                }
                """);
        return prompt.toString();
    }

    private ScoringResult parseResponse(String response) throws JacksonException {
        JsonNode root = objectMapper.readTree(response);
        String generatedText = extractText(root);
        JsonNode scoringNode = objectMapper.readTree(extractJson(generatedText));
        double score = clampScore(scoringNode.path("scorePercent").asDouble(Double.NaN));
        return new ScoringResult(score, "SCORED_AI");
    }

    private String extractText(JsonNode root) {
        StringBuilder generatedText = new StringBuilder();
        JsonNode content = root.path("content");
        if (!content.isArray()) {
            throw new IllegalArgumentException("Anthropic returned no scoring content.");
        }
        for (JsonNode block : content) {
            String text = block.path("text").asText("");
            if (!text.isBlank()) {
                generatedText.append(text);
            }
        }
        if (generatedText.isEmpty()) {
            throw new IllegalArgumentException("Anthropic returned an empty scoring response.");
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
            throw new IllegalArgumentException("Anthropic scoring response does not contain JSON.");
        }
        return cleaned.substring(start, end + 1);
    }

    private double clampScore(double score) {
        if (Double.isNaN(score)) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }

    public record ScoringResult(double scorePercent, String status) {
    }
}
