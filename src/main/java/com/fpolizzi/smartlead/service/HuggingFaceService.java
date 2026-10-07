package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.service.provider.AiProvider;
import com.fpolizzi.smartlead.service.provider.HuggingFaceProvider;
import com.fpolizzi.smartlead.service.provider.OllamaProvider;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Orchestrates multiple AI providers with a fallback strategy
 * Tries providers in order and falls back to mock if all fail
 */
@Service
public class HuggingFaceService {

    private final OllamaProvider ollamaProvider;
    private final HuggingFaceProvider huggingFaceProvider;

    public HuggingFaceService(OllamaProvider ollamaProvider, HuggingFaceProvider huggingFaceProvider) {
        this.ollamaProvider = ollamaProvider;
        this.huggingFaceProvider = huggingFaceProvider;
    }

    /**
     * Generate text using available AI providers
     * Strategy: Try Ollama first, then HuggingFace, then mock fallback
     *
     * @param prompt the input text
     * @return generated text from one of the available providers
     */
    public String generateText(String prompt) {
        // List of providers in priority order
        List<AiProvider> providers = List.of(
            ollamaProvider,
            huggingFaceProvider
        );

        // Try each provider
        for (AiProvider provider : providers) {
            try {
                String result = provider.generateText(prompt);
                if (result != null && !result.isEmpty()) {
                    System.out.println("✓ Generated response using: " + provider.getName());
                    return result;
                }
            } catch (Exception e) {
                System.err.println("⚠️  " + provider.getName() + " failed: " + e.getMessage());
            }
        }

        // Fallback to mock if all providers fail
        System.err.println("⚠️  All AI providers unavailable. Using mock response.");
        return generateMockResponse(prompt);
    }

    /**
     * Mock response for when all services are unavailable
     */
    private String generateMockResponse(String prompt) {
        return prompt + "\n\n[Mock Response]\n" +
               "All AI inference providers are currently unavailable.\n" +
               "Please ensure:\n" +
               "  • Ollama is running: ollama serve\n" +
               "  • Or internet access to HuggingFace API is available\n" +
               "  • And HF_TOKEN environment variable is set";
    }
}
