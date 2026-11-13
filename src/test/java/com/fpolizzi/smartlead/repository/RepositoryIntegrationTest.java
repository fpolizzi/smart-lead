package com.fpolizzi.smartlead.repository;

import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.entity.Ticket;
import com.fpolizzi.smartlead.entity.enums.Category;
import com.fpolizzi.smartlead.entity.enums.LeadType;
import com.fpolizzi.smartlead.entity.enums.Priority;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class RepositoryIntegrationTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReadsComment() {
        Comment saved = commentRepository.save(new Comment("alice@example.com", "We need a demo next week."));

        Comment found = commentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getSenderEmail()).isEqualTo("alice@example.com");
        assertThat(found.getContent()).isEqualTo("We need a demo next week.");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void savesTicketLinkedToComment() {
        Comment comment = commentRepository.save(new Comment("bob@example.com", "I was charged twice."));
        Ticket ticket = ticketRepository.save(
                new Ticket("Double charge", LeadType.SUPPORT, Category.BILLING, Priority.HIGH, "Customer charged twice.", comment));

        Ticket found = ticketRepository.findById(ticket.getId()).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("Double charge");
        assertThat(found.getLeadType()).isEqualTo(LeadType.SUPPORT);
        assertThat(found.getCategory()).isEqualTo(Category.BILLING);
        assertThat(found.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(found.getComment()).isNotNull();
        assertThat(found.getComment().getId()).isEqualTo(comment.getId());
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void pagesTicketsAndLoadsTheirComments() {
        Comment first = commentRepository.save(new Comment("a@example.com", "First"));
        Comment second = commentRepository.save(new Comment("b@example.com", "Second"));
        ticketRepository.save(new Ticket("First", LeadType.OTHER, null, Priority.LOW, "First", first));
        ticketRepository.save(new Ticket("Second", LeadType.OTHER, null, Priority.LOW, "Second", second));
        entityManager.flush();
        entityManager.clear();

        Page<Ticket> page = ticketRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getComment().getContent()).isNotBlank();
    }

    @Test
    void savesNonSupportTicketWithoutCategory() {
        Comment comment = commentRepository.save(new Comment("carol@example.com", "Can we get a demo?"));
        Ticket ticket = ticketRepository.save(
                new Ticket("Demo request", LeadType.DEMO_REQUEST, null, Priority.MEDIUM, "Wants a demo.", comment));

        Ticket found = ticketRepository.findById(ticket.getId()).orElseThrow();

        assertThat(found.getLeadType()).isEqualTo(LeadType.DEMO_REQUEST);
        assertThat(found.getCategory()).isNull();
    }

    @Test
    void rejectsTicketWithoutComment() {
        Ticket ticket = new Ticket("Demo", LeadType.DEMO_REQUEST, null, Priority.MEDIUM, "Demo requested", null);

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(ticket))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMultipleTicketsForOneComment() {
        Comment comment = commentRepository.save(new Comment("alice@example.com", "Demo"));
        ticketRepository.saveAndFlush(new Ticket("First", LeadType.DEMO_REQUEST, null, Priority.MEDIUM, "Demo", comment));

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(
                new Ticket("Second", LeadType.DEMO_REQUEST, null, Priority.MEDIUM, "Demo", comment)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingTicketKeepsComment() {
        Comment comment = commentRepository.save(new Comment("alice@example.com", "Demo"));
        Ticket ticket = ticketRepository.saveAndFlush(
                new Ticket("Demo", LeadType.DEMO_REQUEST, null, Priority.MEDIUM, "Demo", comment));

        ticketRepository.delete(ticket);
        ticketRepository.flush();

        assertThat(commentRepository.findById(comment.getId())).isPresent();
    }

    @Test
    void distinctNewCommentsAreNotEqual() {
        Comment comment = new Comment("alice@example.com", "Demo");
        Comment other = new Comment("alice@example.com", "Demo");
        assertThat(comment).isNotEqualTo(other);
    }

    @Test
    void commentKeepsSetMembershipAfterPersistence() {
        Comment comment = new Comment("alice@example.com", "Demo");
        Set<Comment> comments = new HashSet<>();
        comments.add(comment);

        commentRepository.saveAndFlush(comment);
        assertThat(comments.contains(comment)).isTrue();
    }

    @Test
    void commentKeepsSetMembershipAfterMutation() {
        Comment comment = new Comment("alice@example.com", "Demo");
        Set<Comment> comments = new HashSet<>();
        comments.add(comment);
        comment.setSenderEmail("bob@example.com");
        comment.setContent("Changed");
        assertThat(comments.contains(comment)).isTrue();
    }

    @Test
    void reloadedCommentIsNotEqualToOriginal() {
        Comment comment = commentRepository.saveAndFlush(new Comment("alice@example.com", "Demo"));
        Long id = comment.getId();
        entityManager.clear();
        Comment reloaded = commentRepository.findById(id).orElseThrow();
        assertThat(reloaded).isNotSameAs(comment).isNotEqualTo(comment);
    }

    @Test
    void distinctNewTicketsAreNotEqual() {
        Comment comment = new Comment("alice@example.com", "Demo");
        Ticket ticket = new Ticket("Demo", LeadType.DEMO_REQUEST, null, Priority.MEDIUM, "Demo", comment);
        Ticket other = new Ticket("Demo", LeadType.DEMO_REQUEST, null, Priority.MEDIUM, "Demo", comment);
        assertThat(ticket).isNotEqualTo(other);
    }

    @Test
    void ticketKeepsSetMembershipAfterPersistence() {
        Comment comment = commentRepository.saveAndFlush(new Comment("alice@example.com", "Demo"));
        Ticket ticket = new Ticket("Demo", LeadType.DEMO_REQUEST, null, Priority.MEDIUM, "Demo", comment);
        Set<Ticket> tickets = new HashSet<>();
        tickets.add(ticket);

        ticketRepository.saveAndFlush(ticket);
        assertThat(tickets.contains(ticket)).isTrue();
    }

    @Test
    void ticketKeepsSetMembershipAfterMutation() {
        Comment comment = new Comment("alice@example.com", "Demo");
        Ticket ticket = new Ticket("Demo", LeadType.DEMO_REQUEST, null, Priority.MEDIUM, "Demo", comment);
        Set<Ticket> tickets = new HashSet<>();
        tickets.add(ticket);
        ticket.setTitle("Changed");
        ticket.setSummary("Updated");
        comment.setContent("Updated comment");
        assertThat(tickets.contains(ticket)).isTrue();
    }

    @Test
    void reloadedTicketIsNotEqualToOriginal() {
        Ticket ticket = ticketRepository.saveAndFlush(validTicket());
        Long id = ticket.getId();
        entityManager.clear();
        Ticket reloaded = ticketRepository.findById(id).orElseThrow();
        assertThat(reloaded).isNotSameAs(ticket).isNotEqualTo(ticket);
    }

    @ParameterizedTest
    @EnumSource(value = LeadType.class, names = "SUPPORT", mode = EnumSource.Mode.EXCLUDE)
    void rejectsCategoryForNonSupport(LeadType leadType) {
        Ticket ticket = validTicket();
        ticket.setLeadType(leadType);
        ticket.setCategory(Category.BILLING);
        assertRejected(ticket);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectsNullOrBlankTitle(String title) {
        Ticket ticket = validTicket();
        ticket.setTitle(title);
        assertRejected(ticket);
    }

    @Test
    void rejectsNullLeadType() {
        Ticket ticket = validTicket();
        ticket.setLeadType(null);
        assertRejected(ticket);
    }

    @Test
    void rejectsNullPriority() {
        Ticket ticket = validTicket();
        ticket.setPriority(null);
        assertRejected(ticket);
    }

    @Test
    void rejectsNullSummary() {
        Ticket ticket = validTicket();
        ticket.setSummary(null);
        assertRejected(ticket);
    }

    @Test
    void rejectsOversizedSummary() {
        Ticket ticket = validTicket();
        ticket.setSummary("x".repeat(1001));
        assertRejected(ticket);
    }

    @Test
    void savesSupportTicketWithoutCategory() {
        Ticket ticket = validTicket();
        ticket.setLeadType(LeadType.SUPPORT);
        ticket.setSummary("x".repeat(1000));
        ticketRepository.saveAndFlush(ticket);
        Long id = ticket.getId();
        entityManager.clear();

        Ticket found = ticketRepository.findById(id).orElseThrow();
        assertThat(found.getCategory()).isNull();
        assertThat(found.getSummary()).hasSize(1000);
    }

    private Ticket validTicket() {
        Comment comment = commentRepository.saveAndFlush(new Comment("alice@example.com", "Demo"));
        return new Ticket("Demo", LeadType.DEMO_REQUEST, null, Priority.MEDIUM, "Demo", comment);
    }

    private void assertRejected(Ticket ticket) {
        assertThatThrownBy(() -> ticketRepository.saveAndFlush(ticket))
                .isInstanceOfAny(ConstraintViolationException.class, DataIntegrityViolationException.class);
    }
}
