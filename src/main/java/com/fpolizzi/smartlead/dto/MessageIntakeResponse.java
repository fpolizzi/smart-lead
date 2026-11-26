package com.fpolizzi.smartlead.dto;

import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.service.IntakeResult;

/**
 * Result of POST /messages. leadId is null when the AI judged the message not to be a lead.
 */
public record MessageIntakeResponse(MessageResponse message, LeadAnalysisResponse analysis, Long leadId) {

    public static MessageIntakeResponse from(IntakeResult result) {
        Ticket ticket = result.ticket();
        return new MessageIntakeResponse(
                MessageResponse.from(result.comment()),
                result.analysis(),
                ticket == null ? null : ticket.getId());
    }
}
