package com.fpolizzi.smartlead.dto;

/**
 * Result of POST /messages. leadId is null when the AI judged the message not to be a lead.
 */
public record MessageIntakeResponse(MessageResponse message, LeadAnalysisResponse analysis, Long leadId) {
}
