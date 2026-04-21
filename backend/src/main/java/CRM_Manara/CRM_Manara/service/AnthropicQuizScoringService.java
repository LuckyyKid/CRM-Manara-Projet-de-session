package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.QuizAnswer;
import CRM_Manara.CRM_Manara.Model.Entity.QuizAttempt;
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

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnthropicQuizScoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicQuizScoringService.class);
    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}[\\p{L}'-]{2,}|\\d+(?:[.,]\\d+)?");
    private static final Set<String> STOP_WORDS = Set.of(
            "avec", "dans", "des", "les", "une", "pour", "que", "qui", "sur", "aux", "par",
            "plus", "moins", "comme", "cette", "cela", "donc", "mais", "car", "est", "sont",
            "reponse", "etudiant", "etudiante", "doit"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AnthropicQuizScoringService(@Value("${anthropic.api.key:${ANTHROPIC_API_KEY:}}") String apiKey,
                                       @Value("${anthropic.model:claude-sonnet-4-6}") String model) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "claude-sonnet-4-6" : model.trim();
    }

    public ScoringResult scoreAttempt(QuizAttempt attempt) {
        if (apiKey.isBlank()) {
            LOGGER.warn("Anthropic scoring non disponible. Utilisation du scoring local.");
            return localScore(attempt);
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
            LOGGER.info("Tentative {} scoree par Anthropic a {}%.", attempt.getId(), Math.round(result.scorePercent()));
            return result;
        } catch (RestClientException | JacksonException | IllegalArgumentException exception) {
            LOGGER.warn("Anthropic scoring echoue. Utilisation du scoring local.", exception);
            return localScore(attempt);
        }
    }

    private Map<String, Object> buildRequest(QuizAttempt attempt) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("max_tokens", 1024);
        request.put("temperature", 0.1);
        request.put("system", """
                Tu es un correcteur pedagogique. Evalue les reponses d'un micro-diagnostic.
                Renvoie uniquement un objet JSON valide, sans Markdown.
                Le score global doit refleter la proximite avec les attendus, la justesse et la precision.
                """);
        request.put("messages", List.of(Map.of(
                "role", "user",
                "content", buildPrompt(attempt)
        )));
        return request;
    }

    private String buildPrompt(QuizAttempt attempt) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Quiz: ").append(attempt.getQuiz().getTitle()).append("\n");
        prompt.append("Eleve: ").append(attempt.getEnfant().getPrenom()).append(" ").append(attempt.getEnfant().getNom()).append("\n\n");
        prompt.append("Corrige chaque reponse selon l'attendu. Score global sur 100.\n\n");

        int index = 1;
        for (QuizAnswer answer : attempt.getAnswers()) {
            prompt.append("Question ").append(index++).append("\n");
            prompt.append("Axe: ").append(answer.getQuestion().getAxis().getTitle()).append("\n");
            prompt.append("Angle: ").append(answer.getQuestion().getAngle()).append("\n");
            prompt.append("Question: ").append(answer.getQuestion().getQuestionText()).append("\n");
            prompt.append("Attendu: ").append(answer.getQuestion().getExpectedAnswer()).append("\n");
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

    private ScoringResult localScore(QuizAttempt attempt) {
        if (attempt.getAnswers().isEmpty()) {
            return new ScoringResult(0, "SCORED_LOCAL");
        }

        double total = 0;
        for (QuizAnswer answer : attempt.getAnswers()) {
            total += localAnswerScore(answer);
        }
        return new ScoringResult(total / attempt.getAnswers().size(), "SCORED_LOCAL");
    }

    private double localAnswerScore(QuizAnswer answer) {
        Set<String> expectedTokens = keywords(answer.getQuestion().getExpectedAnswer());
        Set<String> answerTokens = keywords(answer.getAnswerText());
        if (answerTokens.isEmpty()) {
            return 0;
        }
        if (expectedTokens.isEmpty()) {
            return answer.getAnswerText().trim().length() >= 20 ? 60 : 30;
        }

        long matches = expectedTokens.stream().filter(answerTokens::contains).count();
        double overlap = (double) matches / expectedTokens.size();
        double detailBonus = Math.min(20, answer.getAnswerText().trim().length() / 8.0);
        return clampScore(20 + (overlap * 70) + detailBonus);
    }

    private Set<String> keywords(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = WORD_PATTERN.matcher(value == null ? "" : value.toLowerCase());
        while (matcher.find()) {
            String token = stripAccents(matcher.group()).replace("'", "").replace("-", "");
            if (token.length() >= 2 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private double clampScore(double score) {
        if (Double.isNaN(score)) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String stripAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    public record ScoringResult(double scorePercent, String status) {
    }
}
