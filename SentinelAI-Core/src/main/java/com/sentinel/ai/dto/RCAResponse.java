package com.sentinel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RCAResponse {
    private String issue;
    private String rootCause;
    private String impactedService;
    private String recommendedFix;
    private String provider;
    private List<String> errors;
    private EngineeringContextResponse engineeringContext;

    public RCAResponse(String issue, String rootCause, String impactedService, String recommendedFix, String provider, List<String> errors) {
        this.issue = issue;
        this.rootCause = rootCause;
        this.impactedService = impactedService;
        this.recommendedFix = recommendedFix;
        this.provider = provider;
        this.errors = errors;
    }
}
