package com.fpolizzi.smartlead.controller.web;

import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.entity.enums.LeadType;
import com.fpolizzi.smartlead.entity.enums.Priority;
import com.fpolizzi.smartlead.exception.LeadNotFoundException;
import com.fpolizzi.smartlead.service.TicketService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LeadWebControllerTest {

    @Test
    void listRendersPagedLeads() throws Exception {
        TicketService tickets = mock(TicketService.class);
        Ticket ticket = ticket(42L, comment(7L));
        when(tickets.findAll(any())).thenReturn(
            new PageImpl<>(List.of(ticket), PageRequest.of(0, 20), 1));

        mvc(tickets).perform(get("/ui/leads"))
            .andExpect(status().isOk())
            .andExpect(view().name("leads/list"))
            .andExpect(model().attributeExists("leads"));
    }

    @Test
    void detailRendersLead() throws Exception {
        TicketService tickets = mock(TicketService.class);
        when(tickets.getById(42L)).thenReturn(ticket(42L, comment(7L)));

        mvc(tickets).perform(get("/ui/leads/42"))
            .andExpect(status().isOk())
            .andExpect(view().name("leads/detail"))
            .andExpect(model().attributeExists("lead"));
    }

    @Test
    void unknownLeadRendersErrorPageWith404() throws Exception {
        TicketService tickets = mock(TicketService.class);
        when(tickets.getById(999L)).thenThrow(new LeadNotFoundException(999L));

        mvc(tickets).perform(get("/ui/leads/999"))
            .andExpect(status().isNotFound())
            .andExpect(view().name("error"))
            .andExpect(model().attribute("message", "Lead 999 not found"));
    }

    @Test
    void sortByTitleAscendingIsPassedToService() throws Exception {
        TicketService tickets = mock(TicketService.class);
        when(tickets.findAll(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc(tickets).perform(get("/ui/leads").param("sort", "title,asc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sortProperty", "title"))
            .andExpect(model().attribute("sortDirection", "asc"));

        Pageable sent = sentPageable(tickets);
        assertThat(sent.getSort().getOrderFor("title").isAscending()).isTrue();
    }

    @Test
    void unknownSortColumnFallsBackToCreatedAtDescending() throws Exception {
        TicketService tickets = mock(TicketService.class);
        when(tickets.findAll(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc(tickets).perform(get("/ui/leads").param("sort", "bogus,asc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sortProperty", "createdAt"))
            .andExpect(model().attribute("sortDirection", "desc"));

        assertThat(sentPageable(tickets).getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void defaultSortIsCreatedAtDescending() throws Exception {
        TicketService tickets = mock(TicketService.class);
        when(tickets.findAll(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc(tickets).perform(get("/ui/leads"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sortProperty", "createdAt"))
            .andExpect(model().attribute("sortDirection", "desc"));
    }

    private static Pageable sentPageable(TicketService tickets) {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tickets).findAll(captor.capture());
        return captor.getValue();
    }

    private MockMvc mvc(TicketService tickets) {
        return MockMvcBuilders.standaloneSetup(new LeadWebController(tickets))
            .setControllerAdvice(new WebExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    private static Comment comment(Long id) {
        Comment comment = new Comment("alice@example.com", "Need a demo for 100 users");
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
