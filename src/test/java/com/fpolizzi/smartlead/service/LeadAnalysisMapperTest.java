package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.dto.LeadAnalysisResponse;
import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.entity.enums.Category;
import com.fpolizzi.smartlead.entity.enums.LeadType;
import com.fpolizzi.smartlead.entity.enums.Priority;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LeadAnalysisMapperTest {

    private final LeadAnalysisMapper mapper = new LeadAnalysisMapper();

    @ParameterizedTest
    @CsvSource({
        "demo-request,DEMO_REQUEST",
        "pricing-inquiry,PRICING_INQUIRY",
        "partnership,PARTNERSHIP",
        "support,SUPPORT",
        "other,OTHER",
        "  Demo-Request  ,DEMO_REQUEST"
    })
    void mapsLeadTypeFromPromptValues(String input, LeadType expected) {
        assertThat(mapper.toLeadType(input)).isEqualTo(expected);
    }

    @Test
    void unknownOrMissingLeadTypeFallsBackToOther() {
        assertThat(mapper.toLeadType("bogus")).isEqualTo(LeadType.OTHER);
        assertThat(mapper.toLeadType("")).isEqualTo(LeadType.OTHER);
        assertThat(mapper.toLeadType(null)).isEqualTo(LeadType.OTHER);
    }

    @ParameterizedTest
    @CsvSource({"low,LOW", "medium,MEDIUM", "high,HIGH", "HIGH,HIGH"})
    void mapsUrgencyToPriority(String input, Priority expected) {
        assertThat(mapper.toPriority(input)).isEqualTo(expected);
    }

    @Test
    void unknownOrMissingUrgencyFallsBackToLow() {
        assertThat(mapper.toPriority("asap-ish")).isEqualTo(Priority.LOW);
        assertThat(mapper.toPriority(null)).isEqualTo(Priority.LOW);
    }

    @Test
    void categoryIsNullForNonSupportLeads() {
        assertThat(mapper.toCategory(LeadType.DEMO_REQUEST, "BILLING")).isNull();
        assertThat(mapper.toCategory(LeadType.PRICING_INQUIRY, null)).isNull();
        assertThat(mapper.toCategory(LeadType.PARTNERSHIP, "BUG")).isNull();
        assertThat(mapper.toCategory(LeadType.OTHER, "ACCOUNT")).isNull();
    }

    @Test
    void mapsSupportCategoryForSupportLeads() {
        assertThat(mapper.toCategory(LeadType.SUPPORT, "BILLING")).isEqualTo(Category.BILLING);
        assertThat(mapper.toCategory(LeadType.SUPPORT, "billing")).isEqualTo(Category.BILLING);
        assertThat(mapper.toCategory(LeadType.SUPPORT, "FAILURE")).isEqualTo(Category.FAILURE);
    }

    @Test
    void unknownOrMissingSupportCategoryFallsBackToOther() {
        assertThat(mapper.toCategory(LeadType.SUPPORT, null)).isEqualTo(Category.OTHER);
        assertThat(mapper.toCategory(LeadType.SUPPORT, "garbage")).isEqualTo(Category.OTHER);
    }

    @Test
    void toTicketCopiesFieldsAndComment() {
        Comment comment = new Comment("bob@example.com", "I was charged twice.");
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            true, "Urgent Billing Issue - Double Charge", "support", "high",
            "Customer reporting double billing charge.", "BILLING");

        Ticket ticket = mapper.toTicket(analysis, comment);

        assertThat(ticket.getTitle()).isEqualTo("Urgent Billing Issue - Double Charge");
        assertThat(ticket.getLeadType()).isEqualTo(LeadType.SUPPORT);
        assertThat(ticket.getCategory()).isEqualTo(Category.BILLING);
        assertThat(ticket.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(ticket.getSummary()).isEqualTo("Customer reporting double billing charge.");
        assertThat(ticket.getComment()).isSameAs(comment);
    }

    @Test
    void toTicketLeavesCategoryNullForDemoRequest() {
        Comment comment = new Comment("carol@example.com", "Can we get a demo?");
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            true, "Enterprise Demo Request", "demo-request", "medium",
            "Wants a demo.", null);

        Ticket ticket = mapper.toTicket(analysis, comment);

        assertThat(ticket.getLeadType()).isEqualTo(LeadType.DEMO_REQUEST);
        assertThat(ticket.getCategory()).isNull();
        assertThat(ticket.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void blankTitleFallsBackToFirstLineOfMessage() {
        Comment comment = new Comment("dan@example.com", "  Need pricing for 40 seats\nSecond line of the message.");
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            true, "   ", "pricing-inquiry", "medium", "Asks for pricing.", null);

        Ticket ticket = mapper.toTicket(analysis, comment);

        assertThat(ticket.getTitle()).isEqualTo("Need pricing for 40 seats");
    }

    @Test
    void oversizedTitleIsTruncatedToColumnLimit() {
        Comment comment = new Comment("dan@example.com", "Pricing question");
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            true, "T".repeat(300), "pricing-inquiry", "medium", "Asks for pricing.", null);

        Ticket ticket = mapper.toTicket(analysis, comment);

        assertThat(ticket.getTitle()).hasSize(LeadAnalysisMapper.TITLE_MAX_LENGTH);
    }

    @Test
    void oversizedSummaryIsTruncatedToColumnLimit() {
        Comment comment = new Comment("erin@example.com", "Question");
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            true, "Question", "other", "low", "S".repeat(1500), null);

        Ticket ticket = mapper.toTicket(analysis, comment);

        assertThat(ticket.getSummary()).hasSize(LeadAnalysisMapper.SUMMARY_MAX_LENGTH);
    }

    @Test
    void nullSummaryFallsBackToMessage() {
        Comment comment = new Comment("erin@example.com", "Do you ship to Canada?");
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            true, "Shipping Question", "other", "low", null, null);

        Ticket ticket = mapper.toTicket(analysis, comment);

        assertThat(ticket.getSummary()).isEqualTo("Do you ship to Canada?");
    }

    @Test
    void mappedTicketPassesValidationForBadModelOutput() {
        Comment comment = new Comment("frank@example.com", "Charged twice.\nPlease help.");
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            true, "  ", "support", "high", "S".repeat(1500), "BILLING");

        Ticket ticket = mapper.toTicket(analysis, comment);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(ticket)).isEmpty();
        }
    }
}
