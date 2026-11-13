package com.fpolizzi.smartlead.controller;

import com.fpolizzi.smartlead.dto.InboundMessageRequest;
import com.fpolizzi.smartlead.dto.LeadAnalysisResponse;
import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.entity.enums.LeadType;
import com.fpolizzi.smartlead.entity.enums.Priority;
import com.fpolizzi.smartlead.exception.AiResponseParseException;
import com.fpolizzi.smartlead.exception.AiUnavailableException;
import com.fpolizzi.smartlead.exception.GlobalExceptionHandler;
import com.fpolizzi.smartlead.service.CommentService;
import com.fpolizzi.smartlead.service.IntakeResult;
import com.fpolizzi.smartlead.service.LeadIntakeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MessageControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsLeadAndReturns201WithLocation() throws Exception {
        LeadIntakeService intake = mock(LeadIntakeService.class);
        Comment comment = comment(7L, "alice@example.com", "Need a demo for 100 users");
        Ticket ticket = ticket(42L, comment);
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            true, "Enterprise Demo Request", "demo-request", "high", "Inquiry for 100 users", null);
        InboundMessageRequest request = new InboundMessageRequest("Need a demo for 100 users", "alice@example.com");
        when(intake.process(request)).thenReturn(new IntakeResult(comment, analysis, ticket));

        mvc(intake, mock(CommentService.class)).perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/leads/42")))
            .andExpect(jsonPath("$.message.id").value(7))
            .andExpect(jsonPath("$.message.senderEmail").value("alice@example.com"))
            .andExpect(jsonPath("$.analysis.isLead").value(true))
            .andExpect(jsonPath("$.analysis.type").value("demo-request"))
            .andExpect(jsonPath("$.leadId").value(42));
    }

    @Test
    void storesNonLeadWithout201LocationAndWithoutLeadId() throws Exception {
        LeadIntakeService intake = mock(LeadIntakeService.class);
        Comment comment = comment(8L, null, "Great product!");
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            false, "Positive Feedback", "other", "low", "Praise for the product.", null);
        when(intake.process(new InboundMessageRequest("Great product!")))
            .thenReturn(new IntakeResult(comment, analysis, null));

        mvc(intake, mock(CommentService.class)).perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new InboundMessageRequest("Great product!"))))
            .andExpect(status().isCreated())
            .andExpect(header().doesNotExist("Location"))
            .andExpect(jsonPath("$.message.id").value(8))
            .andExpect(jsonPath("$.analysis.isLead").value(false))
            .andExpect(jsonPath("$.leadId").isEmpty());
    }

    @Test
    void returns400WhenMessageIsBlankOrNull() throws Exception {
        LeadIntakeService intake = mock(LeadIntakeService.class);
        MockMvc mvc = mvc(intake, mock(CommentService.class));

        mvc.perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new InboundMessageRequest(""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("message")));

        mvc.perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new InboundMessageRequest(null))))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(intake);
    }

    @Test
    void returns400WhenMessageExceedsMaxLength() throws Exception {
        LeadIntakeService intake = mock(LeadIntakeService.class);
        String tooLong = "x".repeat(InboundMessageRequest.MAX_MESSAGE_LENGTH + 1);

        mvc(intake, mock(CommentService.class)).perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new InboundMessageRequest(tooLong))))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(intake);
    }

    @Test
    void returns400WhenSenderEmailIsInvalid() throws Exception {
        LeadIntakeService intake = mock(LeadIntakeService.class);

        mvc(intake, mock(CommentService.class)).perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new InboundMessageRequest("Hi", "not-an-email"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("senderEmail")));

        verifyNoInteractions(intake);
    }

    @Test
    void returns400WhenBodyIsMalformed() throws Exception {
        MockMvc mvc = mvc(mock(LeadIntakeService.class), mock(CommentService.class));

        mvc.perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Request body is missing or malformed"));
    }

    @Test
    void returns503WhenAiServiceUnavailable() throws Exception {
        LeadIntakeService intake = mock(LeadIntakeService.class);
        when(intake.process(new InboundMessageRequest("Test"))).thenThrow(new AiUnavailableException());

        mvc(intake, mock(CommentService.class)).perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new InboundMessageRequest("Test"))))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void returns502WhenAiResponseCannotBeParsed() throws Exception {
        LeadIntakeService intake = mock(LeadIntakeService.class);
        when(intake.process(new InboundMessageRequest("Test")))
            .thenThrow(new AiResponseParseException("bad output", new RuntimeException()));

        mvc(intake, mock(CommentService.class)).perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new InboundMessageRequest("Test"))))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.detail").value("AI provider returned an invalid response"));
    }

    @Test
    void returns500WithGenericBodyOnUnexpectedError() throws Exception {
        LeadIntakeService intake = mock(LeadIntakeService.class);
        when(intake.process(new InboundMessageRequest("Test")))
            .thenThrow(new IllegalStateException("secret internal detail"));

        mvc(intake, mock(CommentService.class)).perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new InboundMessageRequest("Test"))))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    @Test
    void listsMessagesNewestFirstWithDefaultPaging() throws Exception {
        CommentService comments = mock(CommentService.class);
        Comment comment = comment(7L, "alice@example.com", "Need a demo");
        when(comments.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(comment), PageRequest.of(0, 20), 1));

        mvc(mock(LeadIntakeService.class), comments).perform(get("/messages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(7))
            .andExpect(jsonPath("$.content[0].content").value("Need a demo"))
            .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(comments).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private MockMvc mvc(LeadIntakeService intake, CommentService comments) {
        return MockMvcBuilders.standaloneSetup(new MessageController(intake, comments))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    private static Comment comment(Long id, String senderEmail, String content) {
        Comment comment = new Comment(senderEmail, content);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private static Ticket ticket(Long id, Comment comment) {
        Ticket ticket = new Ticket("Enterprise Demo Request", LeadType.DEMO_REQUEST, null, Priority.HIGH,
            "Inquiry for 100 users", comment);
        ReflectionTestUtils.setField(ticket, "id", id);
        return ticket;
    }
}
