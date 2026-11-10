package com.fpolizzi.smartlead.exception;

/**
 * Thrown when a lead (ticket) id does not exist. The handler maps it to 404.
 */
public class LeadNotFoundException extends RuntimeException {
    public LeadNotFoundException(Long leadId) {
        super("Lead " + leadId + " not found");
    }
}
