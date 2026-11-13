package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.dto.LeadAnalysisResponse;
import com.fpolizzi.smartlead.exception.AiResponseParseException;
import com.fpolizzi.smartlead.exception.AiUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LeadQualificationServiceTest {

    private AiService aiService;
    private ObjectMapper objectMapper;
    private LeadQualificationService service;

    @BeforeEach
    void setUp() throws IOException {
        aiService = mock(AiService.class);
        objectMapper = new ObjectMapper();
        Resource promptResource = new ByteArrayResource("Analyze: {{message}}".getBytes(StandardCharsets.UTF_8));
        service = new LeadQualificationService(aiService, objectMapper, promptResource);
    }

    @Test
    void qualifiesMessageSuccessfullyWithRawJson() {
        String aiOutput = """
            {
              "isLead": true,
              "title": "Enterprise Demo Request",
              "type": "demo-request",
              "urgency": "high",
              "summary": "Enterprise customer requesting a demo for 200 seats."
            }
            """;
        when(aiService.generateText(anyString())).thenReturn(aiOutput);

        LeadAnalysisResponse response = service.qualifyMessage("Need demo for 200 users");

        assertThat(response).isNotNull();
        assertThat(response.isLead()).isTrue();
        assertThat(response.title()).isEqualTo("Enterprise Demo Request");
        assertThat(response.type()).isEqualTo("demo-request");
        assertThat(response.urgency()).isEqualTo("high");
        assertThat(response.summary()).isEqualTo("Enterprise customer requesting a demo for 200 seats.");
        verify(aiService).generateText("Analyze: Need demo for 200 users");
    }

    @Test
    void qualifiesMessageSuccessfullyWithMarkdownJsonFences() {
        String aiOutput = """
            ```json
            {
              "isLead": false,
              "title": "Spam / Solicitation",
              "type": "other",
              "urgency": "low",
              "summary": "SEO agency cold pitch."
            }
            ```
            """;
        when(aiService.generateText(anyString())).thenReturn(aiOutput);

        LeadAnalysisResponse response = service.qualifyMessage("Buy our SEO services");

        assertThat(response).isNotNull();
        assertThat(response.isLead()).isFalse();
        assertThat(response.title()).isEqualTo("Spam / Solicitation");
        assertThat(response.type()).isEqualTo("other");
        assertThat(response.urgency()).isEqualTo("low");
        assertThat(response.summary()).isEqualTo("SEO agency cold pitch.");
    }

    @Test
    void throwsAiResponseParseExceptionOnInvalidJson() {
        when(aiService.generateText(anyString())).thenReturn("Not a valid json response");

        assertThatThrownBy(() -> service.qualifyMessage("Test inquiry"))
            .isInstanceOf(AiResponseParseException.class)
            .hasMessageContaining("Failed to parse AI response into LeadAnalysisResponse");
    }

    @Test
    void throwsAiResponseParseExceptionOnBlankAiResponse() {
        when(aiService.generateText(anyString())).thenReturn("   ");

        assertThatThrownBy(() -> service.qualifyMessage("Test inquiry"))
            .isInstanceOf(AiResponseParseException.class)
            .hasMessageContaining("empty response");
    }

    @Test
    void messageContainingPercentSignIsNotTreatedAsFormatSpecifier() {
        String aiOutput = """
            {"isLead": false, "title": "Discount Question", "type": "pricing-inquiry", "urgency": "low", "summary": "Asks about a discount."}
            """;
        when(aiService.generateText(anyString())).thenReturn(aiOutput);

        LeadAnalysisResponse response = service.qualifyMessage("Is 50% off available %d %s?");

        assertThat(response.title()).isEqualTo("Discount Question");
        verify(aiService).generateText("Analyze: Is 50% off available %d %s?");
    }

    @Test
    void propagatesAiUnavailableException() {
        when(aiService.generateText(anyString())).thenThrow(new AiUnavailableException());

        assertThatThrownBy(() -> service.qualifyMessage("Test message"))
            .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void classpathPromptTemplateLoadsAndFormatsCorrectly() throws IOException {
        Resource promptResource = new ClassPathResource("prompt/smart-lead-prompt.txt");
        LeadQualificationService realPromptService = new LeadQualificationService(aiService, objectMapper, promptResource);

        String sampleAiResponse = """
            {
              "isLead": true,
              "title": "Enterprise Demo Request - 50 Engineers",
              "type": "demo-request",
              "urgency": "high",
              "summary": "Engineering team of 50 requesting an enterprise plan demo next week."
            }
            """;
        when(aiService.generateText(anyString())).thenReturn(sampleAiResponse);

        LeadAnalysisResponse response = realPromptService.qualifyMessage("We’re a team of 50 engineers and we’d like to schedule a demo of your enterprise plan next week.");

        assertThat(response.isLead()).isTrue();
        assertThat(response.type()).isEqualTo("demo-request");
        assertThat(response.urgency()).isEqualTo("high");
    }

    @Test
    void parsesSupportCategoryWhenPresent() {
        String aiOutput = """
            {
              "isLead": true,
              "title": "Urgent Billing Issue - Double Charge",
              "type": "support",
              "urgency": "high",
              "summary": "Customer reporting double billing charge requiring urgent resolution.",
              "supportCategory": "BILLING"
            }
            """;
        when(aiService.generateText(anyString())).thenReturn(aiOutput);

        LeadAnalysisResponse response = service.qualifyMessage("I was charged twice");

        assertThat(response.type()).isEqualTo("support");
        assertThat(response.supportCategory()).isEqualTo("BILLING");
    }
}
