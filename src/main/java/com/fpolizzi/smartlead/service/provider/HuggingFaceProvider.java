package com.fpolizzi.smartlead.service.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.Map;

/**
 * AI Provider implementation using HuggingFace inference API
 */
@Component
public class HuggingFaceProvider implements AiProvider {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private static final String MODEL = "meta-llama/Llama-3.3-70B-Instruct";
    private static final String[] ENDPOINTS = {
        "https://huggingface.co/api/models/meta-llama/Llama-3.3-70B-Instruct",
        "https://api-inference.huggingface.co/models/meta-llama/Llama-3.3-70B-Instruct"
    };

    public HuggingFaceProvider(RestTemplate restTemplate, @Value("${HF_TOKEN:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    @Override
    public String generateText(String prompt) {
        for (String endpoint : ENDPOINTS) {
            String result = tryEndpoint(prompt, endpoint);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private String tryEndpoint(String prompt, String endpoint) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.setBearerAuth(apiKey);
            }

            Map<String, Object> request = new HashMap<>();
            request.put("inputs", prompt);
            request.put("parameters", Map.of(
                "max_new_tokens", 256,
                "temperature", 0.7
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            Map<String, Object> response = restTemplate.postForObject(
                endpoint,
                entity,
                Map.class
            );

            if (response != null && response.containsKey("generated_text")) {
                return response.get("generated_text").toString();
            }

            // Handle array response format
            if (response != null && response.get("0") instanceof Map) {
                Map<String, Object> firstResult = (Map<String, Object>) response.get("0");
                if (firstResult.containsKey("generated_text")) {
                    return firstResult.get("generated_text").toString();
                }
            }

            return null;
        } catch (Exception e) {
            System.err.println("❌ HuggingFace endpoint failed: " + endpoint);
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        // HuggingFace API is typically available if we have an API key
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public String getName() {
        return "HuggingFace (" + MODEL + ")";
    }
}
