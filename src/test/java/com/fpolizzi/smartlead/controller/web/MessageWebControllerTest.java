package com.fpolizzi.smartlead.controller.web;

import com.fpolizzi.smartlead.dto.InboundMessageRequest;
import com.fpolizzi.smartlead.dto.LeadAnalysisResponse;
import com.fpolizzi.smartlead.dto.MessageIntakeResponse;
import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.entity.enums.LeadType;
import com.fpolizzi.smartlead.entity.enums.Priority;
import com.fpolizzi.smartlead.exception.AiUnavailableException;
import com.fpolizzi.smartlead.service.CommentService;
import com.fpolizzi.smartlead.service.IntakeResult;
import com.fpolizzi.smartlead.service.LeadIntakeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MessageWebControllerTest {

    @Test
    void newFormRendersEmptyForm() throws Exception {
        mvc(mock(LeadIntakeService.class), mock(CommentService.class)).perform(get("/ui/messages/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("messages/form"))
            .andExpect(model().attributeExists("messageForm"));
    }

    @Test
    void invalidSubmissionRendersFormWithErrorsAndDoesNotCallAi() throws Exception {
        LeadIntakeService intake = mock(LeadIntakeService.class);

        mvc(intake, mock(CommentService.class)).perform(post("/ui/messages")
                .param("message", "")
                .param("senderEmail", "not-an-email"))
            .andExpect(status().isOk())
            .andExpect(view().name("messages/form"))
            .andExpect(model().attributeHasFieldErrors("messageForm", "message", "senderEmail"));

        verifyNoInteractions(intake);
    }

    @Test
    void validSubmissionRendersResultWithLeadId() throws Exception {
        LeadIntakeService intake = mock(LeadIntakeService.class);
        Comment comment = comment(7L, "alice@example.com", "Need a demo for 100 users");
        Ticket ticket = ticket(42L, comment);
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            true, "Enterprise Demo Request", "demo-request", "high", "Inquiry for 100 users", null);
        when(intake.process(new InboundMessageRequest("Need a demo for 100 users", "alice@example.com")))
            .thenReturn(new IntakeResult(comment, analysis, ticket));

        MvcResult result = mvc(intake, mock(CommentService.class)).perform(post("/ui/messages")
                .param("message", "Need a demo for 100 users")
                .param("senderEmail", "alice@example.com"))
            .andExpect(status().isOk())
            .andExpect(view().name("messages/result"))
            .andReturn();

        MessageIntakeResponse body = (MessageIntakeResponse) result.getModelAndView().getModel().get("intake");
        assertThat(body.leadId()).isEqualTo(42L);
        assertThat(body.message().senderEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void blankEmailIsTreatedAsNoEmail() throws Exception {
        LeadIntakeService intake = mock(LeadIntakeService.class);
        Comment comment = comment(8L, null, "Great product!");
        LeadAnalysisResponse analysis = new LeadAnalysisResponse(
            false, "Positive Feedback", "other", "low", "Praise.", null);
        when(intake.process(new InboundMessageRequest("Great product!", null)))
            .thenReturn(new IntakeResult(comment, analysis, null));

        mvc(intake, mock(CommentService.class)).perform(post("/ui/messages")
                .param("message", "Great product!")
                .param("senderEmail", "   "))
            .andExpect(status().isOk())
            .andExpect(view().name("messages/result"));

        verify(intake).process(new InboundMessageRequest("Great product!", null));
    }

    @Test
    void aiFailureRedirectsToListWithWarningAfterMessageIsSaved() throws Exception {
        LeadIntakeService intake = mock(LeadIntakeService.class);
        when(intake.process(any())).thenThrow(new AiUnavailableException());

        mvc(intake, mock(CommentService.class)).perform(post("/ui/messages")
                .param("message", "Need pricing details"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/ui/messages"))
            .andExpect(flash().attributeExists("warning"));
    }

    @Test
    void listRendersPagedMessages() throws Exception {
        CommentService comments = mock(CommentService.class);
        when(comments.findAll(any())).thenReturn(new PageImpl<>(
            List.of(comment(1L, "bob@example.com", "Hello")), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mvc(mock(LeadIntakeService.class), comments).perform(get("/ui/messages"))
            .andExpect(status().isOk())
            .andExpect(view().name("messages/list"))
            .andExpect(model().attributeExists("messages"));
    }

    @Test
    void sortBySenderAscendingIsPassedToService() throws Exception {
        CommentService comments = mock(CommentService.class);
        when(comments.findAll(any())).thenReturn(new PageImpl<>(List.of(),
            org.springframework.data.domain.PageRequest.of(0, 20), 0));

        mvc(mock(LeadIntakeService.class), comments).perform(get("/ui/messages").param("sort", "senderEmail,asc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sortProperty", "senderEmail"))
            .andExpect(model().attribute("sortDirection", "asc"));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(comments).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("senderEmail").isAscending()).isTrue();
    }

    @Test
    void unknownSortColumnFallsBackToCreatedAtDescending() throws Exception {
        CommentService comments = mock(CommentService.class);
        when(comments.findAll(any())).thenReturn(new PageImpl<>(List.of(),
            org.springframework.data.domain.PageRequest.of(0, 20), 0));

        mvc(mock(LeadIntakeService.class), comments).perform(get("/ui/messages").param("sort", "password,asc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sortProperty", "createdAt"))
            .andExpect(model().attribute("sortDirection", "desc"));
    }

    private MockMvc mvc(LeadIntakeService intake, CommentService comments) {
        return MockMvcBuilders.standaloneSetup(new MessageWebController(intake, comments))
            .setControllerAdvice(new WebExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    private static Comment comment(Long id, String senderEmail, String content) {
        Comment comment = new Comment(senderEmail, content);
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
