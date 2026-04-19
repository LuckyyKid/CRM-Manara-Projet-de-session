package CRM_Manara.CRM_Manara.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnthropicService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${gemini.fallback-models:gemini-2.0-flash,gemini-1.5-flash}")
    private String geminiFallbackModels;

    @Value("${gemini.max-output-tokens:12000}")
    private int geminiMaxOutputTokens;

    @Value("${gemini.max-retries:2}")
    private int geminiMaxRetries;

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    @Value("${anthropic.max-output-tokens:8000}")
    private int anthropicMaxOutputTokens;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Appelle Gemini en priorite, Anthropic en fallback si une cle Anthropic est configuree.
     */
    public String callClaude(String systemPrompt, String userMessage) {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                return callGemini(systemPrompt, userMessage);
            } catch (RuntimeException geminiException) {
                if (anthropicApiKey != null && !anthropicApiKey.isBlank()) {
                    return callAnthropic(systemPrompt, userMessage);
                }
                throw geminiException;
            }
        }
        if (anthropicApiKey != null && !anthropicApiKey.isBlank()) {
            return callAnthropic(systemPrompt, userMessage);
        }
        throw new IllegalStateException(
            "Aucune cle API IA configuree. Ajoutez 'gemini.api.key' ou 'anthropic.api.key' dans application-secret.properties."
        );
    }

    private String callGemini(String systemPrompt, String userMessage) {
        RuntimeException lastException = null;
        for (String model : geminiModelsToTry()) {
            try {
                return callGeminiModel(model, systemPrompt, userMessage);
            } catch (RuntimeException e) {
                lastException = e;
            }
        }
        throw new RuntimeException(
            "Tous les modeles Gemini configures sont indisponibles ou ont echoue. Derniere erreur : "
                + (lastException != null ? lastException.getMessage() : "inconnue"),
            lastException
        );
    }

    private String callGeminiModel(String model, String systemPrompt, String userMessage) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";

        Map<String, Object> systemInstruction = Map.of(
            "parts", List.of(Map.of("text", systemPrompt))
        );
        Map<String, Object> userContent = Map.of(
            "role", "user",
            "parts", List.of(Map.of("text", userMessage))
        );
        Map<String, Object> body = new HashMap<>();
        body.put("system_instruction", systemInstruction);
        body.put("contents", List.of(userContent));
        body.put("generationConfig", Map.of(
            "temperature", 0.4,
            "maxOutputTokens", geminiMaxOutputTokens,
            "responseMimeType", "application/json"
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        HttpStatusCodeException lastHttpException = null;
        for (int attempt = 0; attempt <= geminiMaxRetries; attempt++) {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                return extractGeminiText(model, response);
            } catch (HttpStatusCodeException e) {
                lastHttpException = e;
                int status = e.getStatusCode().value();
                if (status != 429 && status != 503) {
                    throw new RuntimeException(
                        "Erreur Gemini API (" + model + ", " + e.getStatusCode() + "): " + e.getResponseBodyAsString(),
                        e
                    );
                }
                sleepBeforeRetry(attempt);
            }
        }

        throw new RuntimeException(
            "Gemini API (" + model + ") est temporairement indisponible apres "
                + (geminiMaxRetries + 1) + " tentative(s): "
                + (lastHttpException != null ? lastHttpException.getResponseBodyAsString() : ""),
            lastHttpException
        );
    }

    private String extractGeminiText(String model, ResponseEntity<Map> response) {
        Map body = response.getBody();
        if (body == null) {
            throw new RuntimeException("Gemini (" + model + ") a retourne une reponse vide.");
        }
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("Gemini (" + model + ") n'a retourne aucun candidat de reponse.");
        }

        String finishReason = String.valueOf(candidates.get(0).getOrDefault("finishReason", ""));
        if ("MAX_TOKENS".equals(finishReason)) {
            throw new RuntimeException(
                "Reponse Gemini (" + model + ") tronquee avant la fin du JSON. Augmentez gemini.max-output-tokens ou demandez moins d'axes/questions."
            );
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) {
            throw new RuntimeException("Gemini (" + model + ") n'a retourne aucun contenu. Raison: " + finishReason);
        }
        List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
        if (parts == null || parts.isEmpty() || parts.get(0).get("text") == null) {
            throw new RuntimeException("Gemini (" + model + ") a retourne une reponse texte vide. Raison: " + finishReason);
        }
        return parts.get(0).get("text");
    }

    private List<String> geminiModelsToTry() {
        List<String> models = new ArrayList<>();
        if (geminiModel != null && !geminiModel.isBlank()) {
            models.add(geminiModel.trim());
        }
        if (geminiFallbackModels != null && !geminiFallbackModels.isBlank()) {
            for (String model : geminiFallbackModels.split(",")) {
                String trimmed = model.trim();
                if (!trimmed.isEmpty() && !models.contains(trimmed)) {
                    models.add(trimmed);
                }
            }
        }
        return models;
    }

    private void sleepBeforeRetry(int attempt) {
        if (attempt >= geminiMaxRetries) {
            return;
        }
        try {
            Thread.sleep(750L * (attempt + 1));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private String callAnthropic(String systemPrompt, String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", anthropicApiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "claude-3-5-sonnet-20241022");
        body.put("max_tokens", anthropicMaxOutputTokens);
        body.put("system", systemPrompt);
        body.put("messages", List.of(Map.of("role", "user", "content", userMessage)));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.anthropic.com/v1/messages", request, Map.class);
            if ("max_tokens".equals(response.getBody().get("stop_reason"))) {
                throw new RuntimeException(
                    "Reponse Anthropic tronquee avant la fin du JSON. Augmentez anthropic.max-output-tokens ou demandez moins d'axes/questions."
                );
            }
            List<Map<String, String>> content = (List<Map<String, String>>) response.getBody().get("content");
            return content.get(0).get("text");
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Erreur Anthropic API (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        }
    }
}
