package com.fpolizzi.smartlead.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound contact form message. senderEmail is optional, so clients that only send "message" still work.
 */
public record InboundMessageRequest(
        @NotBlank @Size(max = InboundMessageRequest.MAX_MESSAGE_LENGTH) String message,
        @Email String senderEmail) {

    /** Maximum message length. Must match the length of Comment.content. */
    public static final int MAX_MESSAGE_LENGTH = 2000;

    public InboundMessageRequest(String message) {
        this(message, null);
    }
}
