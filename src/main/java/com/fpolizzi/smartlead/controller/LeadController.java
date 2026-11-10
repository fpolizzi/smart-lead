package com.fpolizzi.smartlead.controller;

import com.fpolizzi.smartlead.dto.LeadResponse;
import com.fpolizzi.smartlead.service.TicketService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leads")
public class LeadController {

    private final TicketService ticketService;

    public LeadController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public PagedModel<LeadResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(ticketService.findAll(pageable).map(LeadResponse::from));
    }

    @GetMapping("/{leadId}")
    public LeadResponse get(@PathVariable Long leadId) {
        return LeadResponse.from(ticketService.getById(leadId));
    }
}
