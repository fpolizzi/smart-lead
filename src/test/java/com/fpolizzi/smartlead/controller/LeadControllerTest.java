package com.fpolizzi.smartlead.controller;

import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.entity.enums.LeadType;
import com.fpolizzi.smartlead.entity.enums.Priority;
import com.fpolizzi.smartlead.exception.GlobalExceptionHandler;
import com.fpolizzi.smartlead.exception.LeadNotFoundException;
import com.fpolizzi.smartlead.service.TicketService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

class LeadControllerTest {

    @Test
    void listsLeadsNewestFirstWithDefaultPaging() throws Exception {
        TicketService tickets = mock(TicketService.class);
        Ticket ticket = ticket(42L, comment(7L));
        when(tickets.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(ticket), PageRequest.of(0, 20), 1));

        mvc(tickets).perform(get("/leads"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(42))
            .andExpect(jsonPath("$.content[0].messageId").value(7))
            .andExpect(jsonPath("$.content[0].leadType").value("DEMO_REQUEST"))
            .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tickets).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void returnsSingleLeadById() throws Exception {
        TicketService tickets = mock(TicketService.class);
        when(tickets.getById(42L)).thenReturn(ticket(42L, comment(7L)));

        mvc(tickets).perform(get("/leads/42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.title").value("Enterprise Demo Request"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.messageId").value(7));
    }

    @Test
    void returns404WhenLeadDoesNotExist() throws Exception {
        TicketService tickets = mock(TicketService.class);
        when(tickets.getById(999L)).thenThrow(new LeadNotFoundException(999L));

        mvc(tickets).perform(get("/leads/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Lead 999 not found"));
    }

    @Test
    void returns400WhenLeadIdIsNotNumeric() throws Exception {
        TicketService tickets = mock(TicketService.class);

        mvc(tickets).perform(get("/leads/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'leadId'"));
    }

    private MockMvc mvc(TicketService tickets) {
        return MockMvcBuilders.standaloneSetup(new LeadController(tickets))
            .setControllerAdvice(new GlobalExceptionHandler())
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
