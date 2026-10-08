package com.fpolizzi.smartlead.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException() {
        super("All AI providers are unavailable. Ensure Ollama is running (ollama serve) " +
              "or HuggingFace is reachable and HF_TOKEN is set.");
    }
}
