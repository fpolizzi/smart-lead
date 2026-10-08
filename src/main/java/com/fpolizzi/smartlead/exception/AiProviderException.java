package com.fpolizzi.smartlead.exception;

/**
 * Thrown by an AI provider when it cannot produce text.
 * AiService treats this as a fallback signal; other runtime exceptions are bugs and propagate.
 */
public class AiProviderException extends RuntimeException {
    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
