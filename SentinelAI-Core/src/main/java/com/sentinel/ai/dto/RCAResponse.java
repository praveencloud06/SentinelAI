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
}
