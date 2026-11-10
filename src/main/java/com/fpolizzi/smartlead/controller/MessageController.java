package com.fpolizzi.smartlead.controller;

import com.fpolizzi.smartlead.dto.InboundMessageRequest;
import com.fpolizzi.smartlead.dto.MessageIntakeResponse;
import com.fpolizzi.smartlead.dto.MessageResponse;
import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.service.CommentService;
import com.fpolizzi.smartlead.service.IntakeResult;
import com.fpolizzi.smartlead.service.LeadIntakeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final LeadIntakeService leadIntakeService;
    private final CommentService commentService;

    public MessageController(LeadIntakeService leadIntakeService, CommentService commentService) {
        this.leadIntakeService = leadIntakeService;
        this.commentService = commentService;
    }

    /** Stores the message, qualifies it with the AI, and opens a lead when it qualifies. */
    @PostMapping
    public ResponseEntity<MessageIntakeResponse> create(@Valid @RequestBody InboundMessageRequest request) {
        IntakeResult result = leadIntakeService.process(request);
        Ticket ticket = result.ticket();

        MessageIntakeResponse body = new MessageIntakeResponse(
                MessageResponse.from(result.comment()),
                result.analysis(),
                ticket == null ? null : ticket.getId());

        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CREATED);
        if (ticket != null) {
            response.location(ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/leads/{leadId}")
                    .buildAndExpand(ticket.getId())
                    .toUri());
        }
        return response.body(body);
    }

    @GetMapping
    public PagedModel<MessageResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(commentService.findAll(pageable).map(MessageResponse::from));
    }
}
