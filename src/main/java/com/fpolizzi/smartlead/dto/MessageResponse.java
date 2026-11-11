package com.fpolizzi.smartlead.dto;

import com.fpolizzi.smartlead.entity.Comment;

import java.time.LocalDateTime;

public record MessageResponse(Long id, String senderEmail, String content, LocalDateTime createdAt) {

    public static MessageResponse from(Comment comment) {
        return new MessageResponse(
                comment.getId(),
                comment.getSenderEmail(),
                comment.getContent(),
                comment.getCreatedAt());
    }
}
