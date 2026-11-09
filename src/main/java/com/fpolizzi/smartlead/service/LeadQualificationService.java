package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.dto.LeadAnalysisResponse;
import com.fpolizzi.smartlead.exception.AiResponseParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class LeadQualificationService {

    /** Token in smart-lead-prompt.txt that is replaced with the inbound message. */
    static final String MESSAGE_PLACEHOLDER = "{{message}}";
    /** Keeps parse-error messages from carrying a whole model response. */
    private static final int RAW_RESPONSE_ERROR_LIMIT = 500;

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final String promptTemplate;

    public LeadQualificationService(
            AiService aiService,
            ObjectMapper objectMapper,
            @Value("classpath:prompt/smart-lead-prompt.txt") Resource promptResource) throws IOException {
        this.aiService = aiService;
        this.objectMapper = objectMapper;
        this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public LeadAnalysisResponse qualifyMessage(String inboundMessage) {
        // replace() rather than String.format: a '%' in the customer's message must not break the prompt.
        String message = inboundMessage != null ? inboundMessage : "";
        String finalPrompt = promptTemplate.replace(MESSAGE_PLACEHOLDER, message);
        String rawResponse = aiService.generateText(finalPrompt);
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new AiResponseParseException("AI returned an empty response");
        }
        String cleanedJson = cleanJsonResponse(rawResponse);

        try {
            return objectMapper.readValue(cleanedJson, LeadAnalysisResponse.class);
        } catch (JacksonException e) {
            throw new AiResponseParseException(
                    "Failed to parse AI response into LeadAnalysisResponse: " + truncateForError(rawResponse), e);
        }
    }

    private static String truncateForError(String rawResponse) {
        return rawResponse.length() <= RAW_RESPONSE_ERROR_LIMIT
                ? rawResponse
                : rawResponse.substring(0, RAW_RESPONSE_ERROR_LIMIT) + "...";
    }

    static String cleanJsonResponse(String rawResponse) {
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace >= firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }
        return cleaned.trim();
    }
}
