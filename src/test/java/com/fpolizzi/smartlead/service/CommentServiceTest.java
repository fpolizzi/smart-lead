package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommentServiceTest {

    private final CommentRepository repository = mock(CommentRepository.class);
    private final CommentService service = new CommentService(repository);

    @Test
    void recordsCommentWithSenderAndContent() {
        when(repository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment saved = service.record("alice@example.com", "We need a demo.");

        assertThat(saved.getSenderEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getContent()).isEqualTo("We need a demo.");
        verify(repository).save(saved);
    }

    @Test
    void allowsMissingSenderEmail() {
        when(repository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment saved = service.record(null, "Anonymous message");

        assertThat(saved.getSenderEmail()).isNull();
        assertThat(saved.getContent()).isEqualTo("Anonymous message");
    }

    @Test
    void findAllDelegatesPagingToRepository() {
        PageRequest pageable = PageRequest.of(1, 5);
        Comment comment = new Comment("alice@example.com", "We need a demo.");
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(comment), pageable, 6));

        var page = service.findAll(pageable);

        assertThat(page.getContent()).containsExactly(comment);
        assertThat(page.getTotalElements()).isEqualTo(6);
    }
}
