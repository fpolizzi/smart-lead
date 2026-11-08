package com.fpolizzi.smartlead.service;

import com.fpolizzi.smartlead.entity.Comment;
import com.fpolizzi.smartlead.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    /** Stores the raw inbound message. The sender email may be null when the form does not provide one. */
    @Transactional
    public Comment record(String senderEmail, String content) {
        return commentRepository.save(new Comment(senderEmail, content));
    }
}
