package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.exception.AiProviderException;
import com.fpolizzi.smartlead.exception.AiUnavailableException;
import com.fpolizzi.smartlead.service.provider.AiProvider;
import com.fpolizzi.smartlead.service.provider.GroqProvider;
import com.fpolizzi.smartlead.service.provider.HuggingFaceProvider;
import com.fpolizzi.smartlead.service.provider.OllamaProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiServiceTest {

    @Test
    void springInjectsProvidersInPriorityOrder() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "groq.url", "https://groq.test", "groq.model", "groq-model",
                "huggingface.api-key", "", "huggingface.url", "https://hf.test", "huggingface.model", "hf-model",
                "ollama.url", "http://ollama.test", "ollama.model", "ollama-model")));
            context.register(TestConfiguration.class, OllamaProvider.class, HuggingFaceProvider.class, GroqProvider.class);
            context.refresh();
            assertEquals(List.of(GroqProvider.class, HuggingFaceProvider.class, OllamaProvider.class),
                context.getBean(ProviderList.class).providers().stream().map(Object::getClass).toList());
        }
    }

    @Configuration
    static class TestConfiguration {
        @Bean
        RestClient restClient() {
            return RestClient.create();
        }

        @Bean
        ProviderList providerList(List<AiProvider> providers) {
            return new ProviderList(providers);
        }
    }

    record ProviderList(List<AiProvider> providers) {
    }

    @Test
    void fallsBackToThirdProviderAfterFirstTwoFail() {
        AiProvider first = mock(AiProvider.class);
        AiProvider second = mock(AiProvider.class);
        AiProvider third = mock(AiProvider.class);
        when(first.generateText("test")).thenThrow(new AiProviderException("First failed"));
        when(second.generateText("test")).thenThrow(new AiProviderException("Second failed"));
        when(third.generateText("test")).thenReturn("Third answer");

        assertEquals("Third answer", new AiService(List.of(first, second, third)).generateText("test"));
        var order = inOrder(first, second, third);
        order.verify(first).generateText("test");
        order.verify(second).generateText("test");
        order.verify(third).generateText("test");
    }

    @Test
    void firstProviderWinsAndSecondNeverCalled() {
        AiProvider first = mock(AiProvider.class);
        AiProvider second = mock(AiProvider.class);
        AiProvider third = mock(AiProvider.class);
        when(first.generateText("test")).thenReturn("First answer");
        when(first.getName()).thenReturn("First");

        AiService service = new AiService(List.of(first, second, third));
        String result = service.generateText("test");

        assertEquals("First answer", result);
        verify(first, times(1)).generateText("test");
        verify(second, never()).generateText(any());
        verify(third, never()).generateText(any());
    }

    @Test
    void firstThrowsThenSecondUsed() {
        AiProvider first = mock(AiProvider.class);
        AiProvider second = mock(AiProvider.class);
        AiProvider third = mock(AiProvider.class);
        when(first.generateText("test")).thenThrow(new AiProviderException("First failed"));
        when(first.getName()).thenReturn("First");
        when(second.generateText("test")).thenReturn("Second answer");
        when(second.getName()).thenReturn("Second");

        AiService service = new AiService(List.of(first, second, third));
        String result = service.generateText("test");

        assertEquals("Second answer", result);
        verify(first, times(1)).generateText("test");
        verify(second, times(1)).generateText("test");
        verify(third, never()).generateText(any());
    }

    @Test
    void firstReturnsBlankThenSecondUsed() {
        AiProvider first = mock(AiProvider.class);
        AiProvider second = mock(AiProvider.class);
        when(first.generateText("test")).thenThrow(new AiProviderException("Empty"));
        when(first.getName()).thenReturn("First");
        when(second.generateText("test")).thenReturn("Second answer");
        when(second.getName()).thenReturn("Second");

        AiService service = new AiService(List.of(first, second));
        String result = service.generateText("test");

        assertEquals("Second answer", result);
    }

    @Test
    void allFailThrowsAiUnavailable() {
        AiProvider first = mock(AiProvider.class);
        AiProvider second = mock(AiProvider.class);
        AiProvider third = mock(AiProvider.class);
        when(first.generateText("test")).thenThrow(new AiProviderException("First failed"));
        when(first.getName()).thenReturn("First");
        when(second.generateText("test")).thenThrow(new AiProviderException("Second failed"));
        when(second.getName()).thenReturn("Second");
        when(third.generateText("test")).thenThrow(new AiProviderException("Third failed"));

        AiService service = new AiService(List.of(first, second, third));

        assertThrows(AiUnavailableException.class, () -> service.generateText("test"));
        verify(third).generateText("test");
    }
}
