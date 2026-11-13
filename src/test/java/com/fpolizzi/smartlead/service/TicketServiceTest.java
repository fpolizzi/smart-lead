package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.dto.LeadAnalysisResponse;
import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.entity.enums.Category;
import com.fpolizzi.smartlead.entity.enums.LeadType;
import com.fpolizzi.smartlead.entity.enums.Priority;
import com.fpolizzi.smartlead.exception.LeadNotFoundException;
import com.fpolizzi.smartlead.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TicketServiceTest {

    private final TicketRepository repository = mock(TicketRepository.class);
    private final TicketService service = new TicketService(repository, new LeadAnalysisMapper());

    @Test
    void createsTicketFromAnalysisAndLinksComment() {
        when(repository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Comment comment = new Comment("bob@example.com", "I was charged twice.");
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            true, "Urgent Billing Issue - Double Charge", "support", "high",
            "Customer reporting double billing charge.", "BILLING");

        Ticket ticket = service.createFromAnalysis(analysis, comment);

        assertThat(ticket.getTitle()).isEqualTo("Urgent Billing Issue - Double Charge");
        assertThat(ticket.getLeadType()).isEqualTo(LeadType.SUPPORT);
        assertThat(ticket.getCategory()).isEqualTo(Category.BILLING);
        assertThat(ticket.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(ticket.getComment()).isSameAs(comment);
        verify(repository).save(ticket);
    }

    @Test
    void findAllDelegatesPagingToRepository() {
        PageRequest pageable = PageRequest.of(0, 10);
        Comment comment = new Comment("bob@example.com", "I was charged twice.");
        Ticket ticket = new Ticket("Double charge", LeadType.SUPPORT, Category.BILLING, Priority.HIGH,
            "Customer charged twice.", comment);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(ticket), pageable, 1));

        var page = service.findAll(pageable);

        assertThat(page.getContent()).containsExactly(ticket);
    }

    @Test
    void getByIdReturnsTicket() {
        Ticket ticket = new Ticket("Demo", LeadType.DEMO_REQUEST, null, Priority.MEDIUM, "Demo",
            new Comment("carol@example.com", "Demo please"));
        when(repository.findById(5L)).thenReturn(Optional.of(ticket));

        assertThat(service.getById(5L)).isSameAs(ticket);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(5L))
            .isInstanceOf(LeadNotFoundException.class)
            .hasMessage("Lead 5 not found");
    }
}
