package com.fpolizzi.smartlead.service.provider;

import com.fpolizzi.smartlead.exception.AiProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaProviderTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void parsesSuccessResponse() {
        RestClient restClient = builder.build();
        OllamaProvider provider = new OllamaProvider(restClient, "http://localhost:11434/api/generate", "llama2");

        String responseBody = "{\"response\":\"This is the answer\"}";
        server.expect(requestTo("http://localhost:11434/api/generate"))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        String result = provider.generateText("test prompt");
        assertEquals("This is the answer", result);
    }

    @Test
    void throwsOnMissingResponse() {
        RestClient restClient = builder.build();
        OllamaProvider provider = new OllamaProvider(restClient, "http://localhost:11434/api/generate", "llama2");

        String responseBody = "{}";
        server.expect(requestTo("http://localhost:11434/api/generate"))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        assertThrows(AiProviderException.class, () -> provider.generateText("test prompt"));
    }

    @Test
    void throwsOnEmptyResponse() {
        RestClient restClient = builder.build();
        OllamaProvider provider = new OllamaProvider(restClient, "http://localhost:11434/api/generate", "llama2");

        String responseBody = "{\"response\":\"   \"}";
        server.expect(requestTo("http://localhost:11434/api/generate"))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        assertThrows(AiProviderException.class, () -> provider.generateText("test prompt"));
    }
}
