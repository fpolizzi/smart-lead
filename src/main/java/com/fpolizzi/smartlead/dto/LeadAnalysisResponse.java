package com.fpolizzi.smartlead.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LeadAnalysisResponse(
    @JsonProperty("isLead") boolean isLead,
    String title,
    String type,
    String urgency,
    String summary,
    String supportCategory
) {}
