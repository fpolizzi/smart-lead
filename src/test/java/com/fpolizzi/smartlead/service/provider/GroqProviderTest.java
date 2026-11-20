package com.fpolizzi.smartlead.service.provider;

import com.fpolizzi.smartlead.exception.AiProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GroqProviderTest {
    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private GroqProvider provider(String token) {
        return new GroqProvider(builder.build(), token, "https://api.groq.com/openai/v1/chat/completions",
            "qwen/qwen3.8-27b", 256, 0.1);
    }

    @Test
    void sendsChatRequestAndExtractsFirstContent() {
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andExpect(content().json("""
                {"model":"qwen/qwen3.8-27b","messages":[{"role":"user","content":"test prompt"}],"max_tokens":256,"temperature":0.1}
                """))
            .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"answer\"}}]}", MediaType.APPLICATION_JSON));

        assertEquals("answer", provider("test-key").generateText("test prompt"));
        server.verify();
    }

    @Test
    void missingTokenFailsWithoutRequest() {
        assertThrows(AiProviderException.class, () -> provider("  ").generateText("test"));
        server.verify();
    }

    @Test
    void httpAndAuthenticationErrorsAreSafe() {
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
            .andRespond(withUnauthorizedRequest());
        AiProviderException error = assertThrows(AiProviderException.class, () -> provider("test-key").generateText("test"));
        assertFalse(error.getMessage().contains("test-key"));
        server.verify();
    }

    @Test
    void serverErrorsFail() {
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
            .andRespond(withServerError());
        assertThrows(AiProviderException.class, () -> provider("test-key").generateText("test"));
        server.verify();
    }

    @Test
    void malformedAndMissingContentFail() {
        for (String body : new String[]{"not json", "{}", "{\"choices\":[]}", "{\"choices\":[null]}",
            "{\"choices\":[{}]}", "{\"choices\":[{\"message\":{}}]}",
            "{\"choices\":[{\"message\":{\"content\":\"   \"}}]}"}) {
            RestClient.Builder localBuilder = RestClient.builder();
            MockRestServiceServer localServer = MockRestServiceServer.bindTo(localBuilder).build();
            localServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            GroqProvider provider = new GroqProvider(localBuilder.build(), "key", "https://api.groq.com/openai/v1/chat/completions", "model", 256, 0.1);
            assertThrows(AiProviderException.class, () -> provider.generateText("test"), body);
            localServer.verify();
        }
    }
}