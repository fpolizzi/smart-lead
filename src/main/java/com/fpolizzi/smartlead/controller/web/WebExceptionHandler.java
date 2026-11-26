package com.fpolizzi.smartlead.controller.web;

import com.fpolizzi.smartlead.exception.LeadNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Renders web UI errors as HTML. The REST API keeps its JSON handling in GlobalExceptionHandler.
 * AI failures on the message form are handled in MessageWebController, so they are not repeated here.
 */
@ControllerAdvice(basePackages = "com.fpolizzi.smartlead.controller.web")
public class WebExceptionHandler {

    @ExceptionHandler(LeadNotFoundException.class)
    public ModelAndView handleLeadNotFound(LeadNotFoundException e) {
        ModelAndView view = new ModelAndView("error", HttpStatus.NOT_FOUND);
        view.addObject("status", HttpStatus.NOT_FOUND.value());
        view.addObject("message", e.getMessage());
        return view;
    }
}
