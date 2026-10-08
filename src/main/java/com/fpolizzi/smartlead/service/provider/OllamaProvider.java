package com.fpolizzi.smartlead.service.provider;

import com.fpolizzi.smartlead.exception.AiProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
@Order(2)
public class OllamaProvider implements AiProvider {

    private final RestClient restClient;
    private final String url;
    private final String model;

    public OllamaProvider(RestClient restClient,
                         @Value("${ollama.url}") String url,
                         @Value("${ollama.model}") String model) {
        this.restClient = restClient;
        this.url = url;
        this.model = model;
    }

    @Override
    public String generateText(String prompt) {
        Map<String, Object> request = Map.of(
            "model", model,
            "prompt", prompt,
            "stream", false
        );

        GenerateResponse response;
        try {
            response = restClient.post()
                .uri(url)
                .body(request)
                .retrieve()
                .body(GenerateResponse.class);
        } catch (RestClientException e) {
            throw new AiProviderException(getName() + " request failed", e);
        }

        String content = response == null ? null : response.response();
        if (content == null || content.isBlank()) {
            throw new AiProviderException(getName() + " returned no content");
        }

        return content;
    }

    @Override
    public String getName() {
        return "Ollama (" + model + ")";
    }

    private record GenerateResponse(String response) {
    }
}
