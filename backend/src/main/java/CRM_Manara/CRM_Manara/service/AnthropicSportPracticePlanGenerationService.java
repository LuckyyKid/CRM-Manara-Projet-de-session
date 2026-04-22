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
public class AnthropicSportPracticePlanGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicSportPracticePlanGenerationService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AnthropicSportPracticePlanGenerationService(
            @Value("${anthropic.api.key:${ANTHROPIC_API_KEY:}}") String apiKey,
            @Value("${anthropic.model:claude-sonnet-4-6}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "claude-sonnet-4-6" : model.trim();
    }

    public Optional<GeneratedPracticePlan> generatePlan(String title, String sourceNotes, String activityName) {
        if (apiKey.isBlank()) {
            LOGGER.warn("Anthropic indisponible pour la pratique maison: cle API absente.");
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

            GeneratedPracticePlan generated = parseResponse(response, title);
            LOGGER.info("Pratique maison generee par Anthropic avec {} item(s).", generated.items().size());
            return Optional.of(generated);
        } catch (RestClientException | JacksonException | IllegalArgumentException exception) {
            LOGGER.warn("Generation Anthropic de la pratique maison echouee.", exception);
            return Optional.empty();
        }
    }

    private Map<String, Object> buildRequest(String title, String sourceNotes, String activityName) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("max_tokens", 1400);
        request.put("temperature", 0.2);
        request.put("system", """
                Tu es un coach sportif jeunesse.
                Tu transformes des notes de seance en pratique maison simple pour un parent et un enfant.
                Tu restes concret, securitaire et realiste pour une pratique au parc ou a la maison.
                Ne renvoie que du JSON valide, sans Markdown.
                """);
        request.put("messages", List.of(Map.of(
                "role", "user",
                "content", buildPrompt(title, sourceNotes, activityName)
        )));
        return request;
    }

    private String buildPrompt(String title, String sourceNotes, String activityName) {
        return """
                Activite: %s
                Titre souhaite: %s

                Notes de seance:
                %s

                Cree une fiche de pratique maison en francais.
                Le parent n'est pas un expert. Les consignes doivent etre simples.
                Donne 4 a 6 items maximum.
                Chaque item doit etre praticable en 5 a 15 minutes.
                Evite tout materiel specialise si possible.
                Ajoute un conseil securite seulement s'il est utile.

                Renvoie exactement ce JSON:
                {
                  "title": "string",
                  "summary": "string",
                  "items": [
                    {
                      "title": "string",
                      "instructions": "string",
                      "purpose": "string",
                      "durationLabel": "string",
                      "safetyTip": "string"
                    }
                  ]
                }
                """.formatted(
                activityName == null || activityName.isBlank() ? "Sport" : activityName.trim(),
                title == null || title.isBlank() ? "Pratique maison" : title.trim(),
                sourceNotes == null ? "" : sourceNotes.trim()
        );
    }

    private GeneratedPracticePlan parseResponse(String response, String fallbackTitle) throws JacksonException {
        JsonNode root = objectMapper.readTree(response);
        String text = extractText(root);
        JsonNode json = objectMapper.readTree(extractJson(text));

        String title = json.path("title").asText(fallbackTitle == null ? "Pratique maison" : fallbackTitle).trim();
        String summary = json.path("summary").asText("").trim();
        JsonNode itemsNode = json.path("items");
        if (!itemsNode.isArray()) {
            throw new IllegalArgumentException("La reponse ne contient pas d'items.");
        }

        List<GeneratedPracticePlanItem> items = new ArrayList<>();
        for (JsonNode itemNode : itemsNode) {
            String itemTitle = itemNode.path("title").asText("").trim();
            String instructions = itemNode.path("instructions").asText("").trim();
            String purpose = itemNode.path("purpose").asText("").trim();
            String durationLabel = itemNode.path("durationLabel").asText("").trim();
            String safetyTip = itemNode.path("safetyTip").asText("").trim();
            if (itemTitle.isBlank() || instructions.isBlank() || purpose.isBlank()) {
                continue;
            }
            items.add(new GeneratedPracticePlanItem(itemTitle, instructions, purpose, durationLabel, safetyTip));
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Aucun item exploitable dans la reponse Anthropic.");
        }

        return new GeneratedPracticePlan(title, summary, items);
    }

    private String extractText(JsonNode root) {
        StringBuilder builder = new StringBuilder();
        JsonNode content = root.path("content");
        if (!content.isArray()) {
            throw new IllegalArgumentException("Anthropic n'a renvoye aucun contenu.");
        }
        for (JsonNode block : content) {
            String text = block.path("text").asText("");
            if (!text.isBlank()) {
                builder.append(text);
            }
        }
        if (builder.isEmpty()) {
            throw new IllegalArgumentException("Anthropic a renvoye une reponse vide.");
        }
        return builder.toString();
    }

    private String extractJson(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("La reponse ne contient pas de JSON exploitable.");
        }
        return cleaned.substring(start, end + 1);
    }

    public record GeneratedPracticePlan(String title, String summary, List<GeneratedPracticePlanItem> items) {
    }

    public record GeneratedPracticePlanItem(String title,
                                            String instructions,
                                            String purpose,
                                            String durationLabel,
                                            String safetyTip) {
    }
}
