package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.dto.InboundMessageRequest;
import com.fpolizzi.smartlead.dto.LeadAnalysisResponse;
import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;
import org.springframework.stereotype.Service;

/**
 * Orchestrates one inbound message: store it, qualify it with the AI, and open a ticket when it is a lead.
 * Deliberately not @Transactional: the AI call can take up to the read timeout and should not hold a
 * database transaction open. Each save runs in its own transaction.
 */
@Service
public class LeadIntakeService {

    private final LeadQualificationService qualificationService;
    private final CommentService commentService;
    private final TicketService ticketService;

    public LeadIntakeService(LeadQualificationService qualificationService,
                             CommentService commentService,
                             TicketService ticketService) {
        this.qualificationService = qualificationService;
        this.commentService = commentService;
        this.ticketService = ticketService;
    }

    public IntakeResult process(InboundMessageRequest request) {
        // Saved first so the message is kept even if the AI call fails.
        Comment comment = commentService.record(request.senderEmail(), request.message());

        LeadAnalysisResponse analysis = qualificationService.qualifyMessage(request.message());
        Ticket ticket = analysis.isLead() ? ticketService.createFromAnalysis(analysis, comment) : null;
        return new IntakeResult(comment, analysis, ticket);
    }
}
