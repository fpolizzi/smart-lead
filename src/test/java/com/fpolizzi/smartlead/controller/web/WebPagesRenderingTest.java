package com.fpolizzi.smartlead.controller.web;

import com.fpolizzi.smartlead.dto.LeadAnalysisResponse;
import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.service.CommentService;
import com.fpolizzi.smartlead.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renders the real Thymeleaf templates against the H2 database. Catches template and expression errors
 * that the standalone controller tests cannot see.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebPagesRenderingTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CommentService commentService;

    @Autowired
    private TicketService ticketService;

    @Test
    void newMessageFormRenders() throws Exception {
        mvc.perform(get("/ui/messages/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<form")))
            .andExpect(content().string(containsString("bootstrap")));
    }

    @Test
    void messageListRendersStoredMessages() throws Exception {
        commentService.record("render-check@example.com", "Rendering check message");

        mvc.perform(get("/ui/messages"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Rendering check message")));
    }

    @Test
    void leadDetailRendersLead() throws Exception {
        Comment comment = commentService.record(null, "Please call me about pricing");
        Ticket ticket = ticketService.createFromAnalysis(new LeadAnalysisResponse(
            true, "Rendering check lead", "pricing-inquiry", "medium", "Asks about pricing", null), comment);

        mvc.perform(get("/ui/leads/{id}", ticket.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Rendering check lead")));

        mvc.perform(get("/ui/leads"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Rendering check lead")));
    }

    @Test
    void leadTitleSortReversesRowOrderAndMarksColumn() throws Exception {
        // Each ticket needs its own comment: Ticket.comment is unique.
        ticketService.createFromAnalysis(new LeadAnalysisResponse(
            true, "Sort check Zebra", "partnership", "low", "Zebra", null),
            commentService.record(null, "Sort check zebra message"));
        ticketService.createFromAnalysis(new LeadAnalysisResponse(
            true, "Sort check Alpha", "partnership", "low", "Alpha", null),
            commentService.record(null, "Sort check alpha message"));

        String ascending = mvc.perform(get("/ui/leads").param("sort", "title,asc"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("aria-sort=\"ascending\"")))
            .andReturn().getResponse().getContentAsString();
        assertThat(ascending.indexOf("Sort check Alpha")).isLessThan(ascending.indexOf("Sort check Zebra"));

        String descending = mvc.perform(get("/ui/leads").param("sort", "title,desc"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("aria-sort=\"descending\"")))
            .andReturn().getResponse().getContentAsString();
        assertThat(descending.indexOf("Sort check Zebra")).isLessThan(descending.indexOf("Sort check Alpha"));
    }

    @Test
    void unknownSortParameterStillRendersLeadList() throws Exception {
        mvc.perform(get("/ui/leads").param("sort", "bogus,asc"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Leads")));
    }

    @Test
    void unknownLeadRendersNotFoundPage() throws Exception {
        mvc.perform(get("/ui/leads/{id}", 987654321L))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("not found")));
    }
}
