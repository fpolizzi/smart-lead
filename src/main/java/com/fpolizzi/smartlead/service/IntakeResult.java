package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.dto.LeadAnalysisResponse;
import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;

/**
 * Outcome of processing one inbound message. ticket is null when the message was not a lead.
 */
public record IntakeResult(Comment comment, LeadAnalysisResponse analysis, Ticket ticket) {
}
