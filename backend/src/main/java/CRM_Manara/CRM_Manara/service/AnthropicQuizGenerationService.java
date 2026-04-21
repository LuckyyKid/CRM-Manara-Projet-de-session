package CRM_Manara.CRM_Manara.service;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AnthropicQuizGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicQuizGenerationService.class);
    private static final int MIN_AXES = 3;
    private static final int MAX_AXES = 7;
    private static final List<String> REQUIRED_ANGLES = List.of(
            "Reconnaissance",
            "Application",
            "Piege",
            "Transfert",
            "Justification"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AnthropicQuizGenerationService(@Value("${anthropic.api.key:${ANTHROPIC_API_KEY:}}") String apiKey,
                                          @Value("${anthropic.model:claude-sonnet-4-6}") String model) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = blankToDefault(model, "claude-sonnet-4-6");
    }

    public Optional<GeneratedQuiz> generateQuiz(String title, String sourceNotes, String activityName) {
        if (apiKey.isBlank()) {
            LOGGER.warn("Anthropic API non disponible ou echouee. Utilisation du fallback local.");
            return Optional.empty();
        }

        try {
            String response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(title, sourceNotes, activityName))
                    .retrieve()
                    .body(String.class);

            GeneratedQuiz quiz = parseResponse(response);
            LOGGER.info("Quiz genere par Anthropic avec {} axes.", quiz.axes().size());
            return Optional.of(quiz);
        } catch (RestClientException | JacksonException | IllegalArgumentException exception) {
            LOGGER.warn("Anthropic API non disponible ou echouee. Utilisation du fallback local.", exception);
            return Optional.empty();
        }
    }

    private Map<String, Object> buildRequest(String title, String sourceNotes, String activityName) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("max_tokens", 8192);
        request.put("temperature", 0.3);
        request.put("system", """
                Tu es un tuteur pedagogique. Genere un micro-diagnostic fiable en francais.
                Renvoie uniquement un objet JSON valide, sans Markdown, sans commentaire.
                Les axes doivent venir des notes de seance et couvrir les apprentissages importants.
                """);
        request.put("messages", List.of(Map.of(
                "role", "user",
                "content", buildPrompt(title, sourceNotes, activityName)
        )));
        return request;
    }

    private String buildPrompt(String title, String sourceNotes, String activityName) {
        return """
                Tu es un expert pedagogique en %s.

                Titre souhaite du quiz : %s

                Voici les notes exactes de la seance d'aujourd'hui :
                %s

                A partir de ces notes UNIQUEMENT :

                1. Extrais entre 3 et 7 axes pedagogiques SPECIFIQUES au contenu couvert.
                   Exemples de bons axes pour du calcul differentiel :
                   - "Regles de derivation de base"
                   - "Signe de la derivee et croissance"
                   - "Points critiques et extremums"

                   Exemples de MAUVAIS axes (trop generiques) :
                   - "Concepts principaux"
                   - "Methodes et etapes"
                   - Le nom de l'activite comme axe

                2. Pour chaque axe, genere exactement 5 questions basees sur le contenu reel :
                   - Reconnaissance : l'etudiant doit identifier un concept VU dans les notes
                   - Application : l'etudiant doit resoudre un exercice SIMILAIRE a ceux vus en classe
                   - Piege : une erreur FREQUENTE liee a ce concept specifique
                   - Transfert : appliquer le concept dans un NOUVEAU contexte
                   - Justification : expliquer POURQUOI la methode fonctionne

                3. Chaque question doit etre SPECIFIQUE au contenu.
                   Exemple BON : "Calcule la derivee de f(x) = 4x^2 - 3x + 1"
                   Exemple MAUVAIS : "Identifie l'idee principale reliee a Calcul Differentiel"

                4. Chaque expectedAnswer doit contenir la VRAIE reponse, pas juste
                   "L'etudiant nomme le concept central"
                   Exemple BON : "f'(x) = 8x - 3, on applique la regle de puissance"
                   Exemple MAUVAIS : "La reponse applique correctement la methode"

                Format JSON obligatoire :
                {
                  "axes": [
                    {
                      "title": "Titre specifique",
                      "summary": "Resume court base sur les notes",
                      "questions": [
                        {
                          "angle": "Reconnaissance",
                          "type": "OPEN",
                          "questionText": "Question precise",
                          "expectedAnswer": "Vraie reponse attendue"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(
                blankToDefault(activityName, "Non precisee"),
                blankToDefault(title, "Quiz de micro-diagnostic"),
                sourceNotes
        );
    }

    private GeneratedQuiz parseResponse(String response) throws JacksonException {
        JsonNode root = objectMapper.readTree(response);
        String generatedText = extractText(root);
        if (generatedText.isBlank()) {
            throw new IllegalArgumentException("Anthropic returned an empty response.");
        }

        JsonNode quizNode = objectMapper.readTree(extractJson(generatedText));
        JsonNode axesNode = quizNode.path("axes");
        if (!axesNode.isArray() || axesNode.size() < MIN_AXES || axesNode.size() > MAX_AXES) {
            throw new IllegalArgumentException("Anthropic returned an invalid axis count.");
        }

        List<GeneratedAxis> axes = new ArrayList<>();
        for (JsonNode axisNode : axesNode) {
            axes.add(parseAxis(axisNode));
        }
        return new GeneratedQuiz(axes);
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

    private GeneratedAxis parseAxis(JsonNode axisNode) {
        String title = requiredText(axisNode, "title");
        String summary = requiredText(axisNode, "summary");
        JsonNode questionsNode = axisNode.path("questions");
        if (!questionsNode.isArray() || questionsNode.size() != REQUIRED_ANGLES.size()) {
            throw new IllegalArgumentException("Anthropic returned an invalid question count.");
        }

        List<GeneratedQuestion> questions = new ArrayList<>();
        for (String requiredAngle : REQUIRED_ANGLES) {
            JsonNode matchingQuestion = findQuestionForAngle(questionsNode, requiredAngle);
            questions.add(new GeneratedQuestion(
                    requiredAngle,
                    "OPEN",
                    requiredText(matchingQuestion, "questionText"),
                    requiredText(matchingQuestion, "expectedAnswer")
            ));
        }
        return new GeneratedAxis(title, summary, questions);
    }

    private JsonNode findQuestionForAngle(JsonNode questionsNode, String requiredAngle) {
        for (JsonNode questionNode : questionsNode) {
            if (requiredAngle.equalsIgnoreCase(questionNode.path("angle").asText())) {
                return questionNode;
            }
        }
        throw new IllegalArgumentException("Anthropic omitted angle: " + requiredAngle);
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

    public record GeneratedQuiz(List<GeneratedAxis> axes) {
    }

    public record GeneratedAxis(String title, String summary, List<GeneratedQuestion> questions) {
    }

    public record GeneratedQuestion(String angle, String type, String questionText, String expectedAnswer) {
    }
}
