package com.fpolizzi.smartlead.service.provider;

/**
 * Interface for AI inference providers
 */
public interface AiProvider {
    /**
     * Generate text based on the input prompt
     * @param prompt the input text
     * @return generated text or null if the provider is unavailable
     */
    String generateText(String prompt);

    /**
     * Check if this provider is available
     * @return true if the provider can be used
     */
    boolean isAvailable();

    /**
     * Get the name of this provider for logging
     * @return provider name
     */
    String getName();
}
