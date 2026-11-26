package com.fpolizzi.smartlead.controller.web;

import com.fpolizzi.smartlead.dto.InboundMessageRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form backing object for the message form. A plain bean rather than the InboundMessageRequest record,
 * because Thymeleaf's th:field needs getters and setters. The constraints mirror InboundMessageRequest.
 */
public class MessageForm {

    @NotBlank
    @Size(max = InboundMessageRequest.MAX_MESSAGE_LENGTH)
    private String message;

    @Email
    private String senderEmail;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    /** A blank email field means no email. Normalised here so @Email does not reject whitespace. */
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail == null || senderEmail.isBlank() ? null : senderEmail.trim();
    }

    public InboundMessageRequest toRequest() {
        return new InboundMessageRequest(message, senderEmail);
    }
}
