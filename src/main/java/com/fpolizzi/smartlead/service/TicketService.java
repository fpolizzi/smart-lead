package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.dto.LeadAnalysisResponse;
import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final LeadAnalysisMapper mapper;

    public TicketService(TicketRepository ticketRepository, LeadAnalysisMapper mapper) {
        this.ticketRepository = ticketRepository;
        this.mapper = mapper;
    }

    @Transactional
    public Ticket createFromAnalysis(LeadAnalysisResponse analysis, Comment comment) {
        return ticketRepository.save(mapper.toTicket(analysis, comment));
    }
}
