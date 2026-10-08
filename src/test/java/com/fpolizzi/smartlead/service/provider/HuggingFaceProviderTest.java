package com.fpolizzi.smartlead.service.provider;

import com.fpolizzi.smartlead.exception.AiProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HuggingFaceProviderTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private HuggingFaceProvider provider() {
        return new HuggingFaceProvider(builder.build(), "test-key", "https://api.test", "model-1", 256, 0.7);
    }

    @Test
    void parsesSuccessResponse() {
        HuggingFaceProvider provider = provider();

        String responseBody = "{\"choices\":[{\"message\":{\"content\":\"Hello, world!\"}}]}";
        server.expect(requestTo("https://api.test"))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        String result = provider.generateText("test prompt");
        assertEquals("Hello, world!", result);
    }

    @Test
    void throwsOnMissingChoices() {
        HuggingFaceProvider provider = provider();

        String responseBody = "{}";
        server.expect(requestTo("https://api.test"))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        assertThrows(AiProviderException.class, () -> provider.generateText("test prompt"));
    }

    @Test
    void throwsOnEmptyContent() {
        HuggingFaceProvider provider = provider();

        String responseBody = "{\"choices\":[{\"message\":{\"content\":\"   \"}}]}";
        server.expect(requestTo("https://api.test"))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        assertThrows(AiProviderException.class, () -> provider.generateText("test prompt"));
    }

    @Test
    void wrapsHttpErrorsInProviderException() {
        HuggingFaceProvider provider = provider();

        server.expect(requestTo("https://api.test"))
            .andRespond(withServerError());

        AiProviderException e = assertThrows(AiProviderException.class, () -> provider.generateText("test prompt"));
        assertTrue(e.getMessage().contains("HuggingFace"));
    }
}
