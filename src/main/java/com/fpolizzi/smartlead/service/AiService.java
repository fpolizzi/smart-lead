package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.exception.AiProviderException;
import com.fpolizzi.smartlead.exception.AiUnavailableException;
import com.fpolizzi.smartlead.service.provider.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private final List<AiProvider> providers;

    public AiService(List<AiProvider> providers) {
        this.providers = providers;
    }

    /**
     * Generate text using available AI providers.
     * Tries each provider in priority order (@Order annotation).
     *
     * @param prompt the input text
     * @return generated text from the first successful provider
     * @throws AiUnavailableException if all providers fail
     */
    public String generateText(String prompt) {
        for (AiProvider provider : providers) {
            try {
                String result = provider.generateText(prompt);
                log.info("Generated response using: {}", provider.getName());
                return result;
            } catch (AiProviderException e) {
                log.warn("{} failed: {}", provider.getName(), e.getMessage());
            }
        }

        log.error("All AI providers unavailable");
        throw new AiUnavailableException();
    }
}
