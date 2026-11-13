package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.dto.InboundMessageRequest;
import com.fpolizzi.smartlead.dto.LeadAnalysisResponse;
import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.entity.enums.LeadType;
import com.fpolizzi.smartlead.entity.enums.Priority;
import com.fpolizzi.smartlead.exception.AiUnavailableException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LeadIntakeServiceTest {

    private final LeadQualificationService qualificationService = mock(LeadQualificationService.class);
    private final CommentService commentService = mock(CommentService.class);
    private final TicketService ticketService = mock(TicketService.class);
    private final LeadIntakeService service = new LeadIntakeService(qualificationService, commentService, ticketService);

    @Test
    void storesCommentAndCreatesTicketForLead() {
        Comment comment = new Comment("alice@example.com", "Need a demo for 200 users");
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            true, "Enterprise Demo Request", "demo-request", "high", "Demo for 200 users.", null);
        Ticket ticket = new Ticket("Enterprise Demo Request", LeadType.DEMO_REQUEST, null, Priority.HIGH,
            "Demo for 200 users.", comment);
        when(commentService.record("alice@example.com", "Need a demo for 200 users")).thenReturn(comment);
        when(qualificationService.qualifyMessage("Need a demo for 200 users")).thenReturn(analysis);
        when(ticketService.createFromAnalysis(analysis, comment)).thenReturn(ticket);

        IntakeResult result = service.process(
            new InboundMessageRequest("Need a demo for 200 users", "alice@example.com"));

        assertThat(result.comment()).isSameAs(comment);
        assertThat(result.analysis()).isEqualTo(analysis);
        assertThat(result.ticket()).isSameAs(ticket);
        verify(ticketService).createFromAnalysis(analysis, comment);
    }

    @Test
    void storesCommentButCreatesNoTicketForNonLead() {
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            false, "Positive Feedback", "other", "low", "Praise for the product.", null);
        when(qualificationService.qualifyMessage(anyString())).thenReturn(analysis);

        IntakeResult result = service.process(new InboundMessageRequest("Great product!"));

        assertThat(result.analysis()).isEqualTo(analysis);
        assertThat(result.ticket()).isNull();
        verify(commentService).record(null, "Great product!");
        verifyNoInteractions(ticketService);
    }

    @Test
    void keepsCommentWhenAiCallFails() {
        when(qualificationService.qualifyMessage(anyString())).thenThrow(new AiUnavailableException());

        assertThatThrownBy(() -> service.process(new InboundMessageRequest("Hi", "carol@example.com")))
            .isInstanceOf(AiUnavailableException.class);

        verify(commentService).record("carol@example.com", "Hi");
        verifyNoInteractions(ticketService);
    }
}
