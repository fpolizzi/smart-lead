package com.fpolizzi.smartlead.exception;

/**
 * Thrown when an AI response cannot be parsed into the expected structure.
 * The response is an upstream failure, so the handler maps it to 502.
 */
public class AiResponseParseException extends RuntimeException {
    public AiResponseParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
