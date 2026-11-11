package com.fpolizzi.smartlead.dto;

import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.entity.enums.Category;
import com.fpolizzi.smartlead.entity.enums.LeadType;
import com.fpolizzi.smartlead.entity.enums.Priority;

import java.time.LocalDateTime;

public record LeadResponse(
        Long id,
        String title,
        LeadType leadType,
        Category category,
        Priority priority,
        String summary,
        Long messageId,
        LocalDateTime createdAt) {

    public static LeadResponse from(Ticket ticket) {
        return new LeadResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getLeadType(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getSummary(),
                ticket.getComment().getId(),
                ticket.getCreatedAt());
    }
}
