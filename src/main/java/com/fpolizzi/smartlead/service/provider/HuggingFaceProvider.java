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
public class HuggingFaceProvider implements AiProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final String url;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    public HuggingFaceProvider(RestClient restClient,
                              @Value("${huggingface.api-key}") String apiKey,
                              @Value("${huggingface.url}") String url,
                              @Value("${huggingface.model}") String model,
                              @Value("${huggingface.max-tokens:256}") int maxTokens,
                              @Value("${huggingface.temperature:0.7}") double temperature) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.url = url;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    @Override
    public String generateText(String prompt) {
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
            throw new AiProviderException(getName() + " request failed", e);
        }

        String content = response == null ? null : response.firstContent();
        if (content == null || content.isBlank()) {
            throw new AiProviderException(getName() + " returned no content");
        }

        return content;
    }

    @Override
    public String getName() {
        return "HuggingFace (" + model + ")";
    }

    private record ChatResponse(List<Choice> choices) {
        String firstContent() {
            if (choices == null || choices.isEmpty() || choices.get(0).message() == null) {
                return null;
            }
            return choices.get(0).message().content();
        }
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}
