package com.fpolizzi.smartlead.controller.web;

import com.fpolizzi.smartlead.dto.LeadResponse;
import com.fpolizzi.smartlead.service.TicketService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Set;

@Controller
@RequestMapping("/ui/leads")
public class LeadWebController {

    private static final Set<String> SORTABLE = Set.of("id", "title", "leadType", "priority", "createdAt");

    private final TicketService ticketService;

    public LeadWebController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public String list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        Pageable sorted = SortSupport.sanitize(pageable, SORTABLE);
        model.addAttribute("leads", ticketService.findAll(sorted).map(LeadResponse::from));
        SortSupport.addSortAttributes(model, sorted.getSort());
        return "leads/list";
    }

    /** A missing lead is rendered as a 404 page by WebExceptionHandler. */
    @GetMapping("/{leadId}")
    public String detail(@PathVariable Long leadId, Model model) {
        model.addAttribute("lead", LeadResponse.from(ticketService.getById(leadId)));
        return "leads/detail";
    }
}
