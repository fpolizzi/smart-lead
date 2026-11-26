package com.fpolizzi.smartlead.controller.web;

import com.fpolizzi.smartlead.dto.MessageIntakeResponse;
import com.fpolizzi.smartlead.dto.MessageResponse;
import com.fpolizzi.smartlead.exception.AiResponseParseException;
import com.fpolizzi.smartlead.exception.AiUnavailableException;
import com.fpolizzi.smartlead.service.CommentService;
import com.fpolizzi.smartlead.service.IntakeResult;
import com.fpolizzi.smartlead.service.LeadIntakeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/ui/messages")
public class MessageWebController {

    private static final Set<String> SORTABLE = Set.of("id", "senderEmail", "content", "createdAt");

    private final LeadIntakeService leadIntakeService;
    private final CommentService commentService;

    public MessageWebController(LeadIntakeService leadIntakeService, CommentService commentService) {
        this.leadIntakeService = leadIntakeService;
        this.commentService = commentService;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("messageForm", new MessageForm());
        return "messages/form";
    }

    /** Submits the form. Renders the result page, or the form again when validation fails. */
    @PostMapping
    public String submit(@Valid @ModelAttribute("messageForm") MessageForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return "messages/form";
        }

        IntakeResult result;
        try {
            result = leadIntakeService.process(form.toRequest());
        } catch (AiUnavailableException | AiResponseParseException e) {
            // The message is stored before the AI call, so it is kept even though no lead was created.
            redirect.addFlashAttribute("warning",
                    "Your message was saved, but the AI could not analyse it, so no lead was created.");
            return "redirect:/ui/messages";
        }

        model.addAttribute("intake", new MessageIntakeResponse(
                MessageResponse.from(result.comment()),
                result.analysis(),
                result.ticket() == null ? null : result.ticket().getId()));
        return "messages/result";
    }

    @GetMapping
    public String list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        Pageable sorted = SortSupport.sanitize(pageable, SORTABLE);
        model.addAttribute("messages", commentService.findAll(sorted).map(MessageResponse::from));
        SortSupport.addSortAttributes(model, sorted.getSort());
        return "messages/list";
    }
}
