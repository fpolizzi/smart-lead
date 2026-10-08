package com.fpolizzi.smartlead.service.provider;

/**
 * Interface for AI inference providers
 */
public interface AiProvider {
    /**
     * Generate text based on the input prompt
     * @param prompt the input text
     * @return generated text (non-blank)
     * @throws RuntimeException if the provider cannot complete the request
     */
    String generateText(String prompt);

    /**
     * Get the name of this provider for logging
     * @return provider name
     */
    String getName();
}
