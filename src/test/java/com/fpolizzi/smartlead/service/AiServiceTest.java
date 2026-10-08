package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.exception.AiProviderException;
import com.fpolizzi.smartlead.exception.AiUnavailableException;
import com.fpolizzi.smartlead.service.provider.AiProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiServiceTest {

    @Test
    void firstProviderWinsAndSecondNeverCalled() {
        AiProvider first = mock(AiProvider.class);
        AiProvider second = mock(AiProvider.class);
        when(first.generateText("test")).thenReturn("First answer");
        when(first.getName()).thenReturn("First");

        AiService service = new AiService(List.of(first, second));
        String result = service.generateText("test");

        assertEquals("First answer", result);
        verify(first, times(1)).generateText("test");
        verify(second, never()).generateText(any());
    }

    @Test
    void firstThrowsThenSecondUsed() {
        AiProvider first = mock(AiProvider.class);
        AiProvider second = mock(AiProvider.class);
        when(first.generateText("test")).thenThrow(new AiProviderException("First failed"));
        when(first.getName()).thenReturn("First");
        when(second.generateText("test")).thenReturn("Second answer");
        when(second.getName()).thenReturn("Second");

        AiService service = new AiService(List.of(first, second));
        String result = service.generateText("test");

        assertEquals("Second answer", result);
        verify(first, times(1)).generateText("test");
        verify(second, times(1)).generateText("test");
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
        when(first.generateText("test")).thenThrow(new AiProviderException("First failed"));
        when(first.getName()).thenReturn("First");
        when(second.generateText("test")).thenThrow(new AiProviderException("Second failed"));
        when(second.getName()).thenReturn("Second");

        AiService service = new AiService(List.of(first, second));

        assertThrows(AiUnavailableException.class, () -> service.generateText("test"));
    }
}
