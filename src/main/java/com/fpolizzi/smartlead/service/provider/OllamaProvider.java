package com.fpolizzi.smartlead.service.provider;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.Map;

/**
 * AI Provider implementation using local Ollama
 */
@Component
public class OllamaProvider implements AiProvider {

    private final RestTemplate restTemplate;
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL = "llama3.2:latest";

    public OllamaProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String generateText(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Ollama API request format
            Map<String, Object> request = new HashMap<>();
            request.put("model", OLLAMA_MODEL);
            request.put("prompt", prompt);
            request.put("stream", false);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // Make the request
            Map<String, Object> response = restTemplate.postForObject(
                OLLAMA_URL,
                entity,
                Map.class
            );

            if (response != null && response.containsKey("response")) {
                return response.get("response").toString();
            }

            return null;
        } catch (Exception e) {
            System.err.println("❌ Ollama error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Try a quick health check
            restTemplate.getForObject(OLLAMA_URL, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getName() {
        return "Ollama (" + OLLAMA_MODEL + ")";
    }
}
