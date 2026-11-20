package com.fpolizzi.smartlead.service.provider;

import com.fpolizzi.smartlead.exception.AiProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class GroqProvider implements AiProvider {
    private final RestClient restClient;
    private final String apiKey;
    private final String url;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    public GroqProvider(RestClient restClient,
                        @Value("${groq.api-key:}") String apiKey,
                        @Value("${groq.url}") String url,
                        @Value("${groq.model}") String model,
                        @Value("${groq.max-tokens:256}") int maxTokens,
                        @Value("${groq.temperature:0.1}") double temperature) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.url = url;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    @Override
    public String generateText(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Groq token is missing");
        }

        Map<String, Object> request = Map.of(
            "model", model,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "max_tokens", maxTokens,
            "temperature", temperature
        );

        ChatResponse response;
        try {
            response = restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .body(request)
                .retrieve()
                .body(ChatResponse.class);
        } catch (RestClientException e) {
            throw new AiProviderException("Groq request failed", e);
        }

        String content = response == null ? null : response.firstContent();
        if (content == null || content.isBlank()) {
            throw new AiProviderException("Groq returned no content");
        }
        return content;
    }

    @Override
    public String getName() {
        return "Groq (" + model + ")";
    }

    private record ChatResponse(List<Choice> choices) {
        String firstContent() {
            if (choices == null || choices.isEmpty() || choices.getFirst() == null
                || choices.getFirst().message() == null) {
                return null;
            }
            return choices.getFirst().message().content();
        }
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}