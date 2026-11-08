package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.dto.LeadAnalysisResponse;
import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.entity.enums.Category;
import com.fpolizzi.smartlead.entity.enums.LeadType;
import com.fpolizzi.smartlead.entity.enums.Priority;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Maps the AI analysis output (prompt vocabulary) onto the persistence enums.
 */
@Component
public class LeadAnalysisMapper {

    /** Column limits on Ticket. */
    static final int TITLE_MAX_LENGTH = 255;
    static final int SUMMARY_MAX_LENGTH = 1000;

    /**
     * Builds a Ticket from the model output. Free text is sanitized so model output that is blank or too long
     * still produces a valid ticket: blank title/summary fall back to the inbound message, and both are truncated
     * to their column limits.
     */
    public Ticket toTicket(LeadAnalysisResponse analysis, Comment comment) {
        LeadType leadType = toLeadType(analysis.type());
        String message = comment.getContent() == null ? "" : comment.getContent().trim();

        String title = textOrFallback(analysis.title(), firstLine(message));
        String summary = textOrFallback(analysis.summary(), message);

        return new Ticket(
                truncate(title, TITLE_MAX_LENGTH),
                leadType,
                toCategory(leadType, analysis.supportCategory()),
                toPriority(analysis.urgency()),
                truncate(summary, SUMMARY_MAX_LENGTH),
                comment
        );
    }

    /** "demo-request" -> DEMO_REQUEST. Unknown or missing values fall back to OTHER. */
    public LeadType toLeadType(String type) {
        return parseEnum(type, LeadType.class, LeadType.OTHER);
    }

    /** "high" -> HIGH. Unknown or missing values fall back to LOW. */
    public Priority toPriority(String urgency) {
        return parseEnum(urgency, Priority.class, Priority.LOW);
    }

    /**
     * Support subcategory. Only meaningful for SUPPORT leads; every other lead type gets null.
     * Unknown or missing values for a SUPPORT lead fall back to Category.OTHER.
     */
    public Category toCategory(LeadType leadType, String supportCategory) {
        if (leadType != LeadType.SUPPORT) {
            return null;
        }
        return parseEnum(supportCategory, Category.class, Category.OTHER);
    }

    /** Returns the trimmed model text, or the fallback when the text is null or blank. */
    private static String textOrFallback(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static String firstLine(String text) {
        return text.lines().findFirst().map(String::trim).orElse("");
    }

    private static String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private static <E extends Enum<E>> E parseEnum(String raw, Class<E> enumType, E fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
