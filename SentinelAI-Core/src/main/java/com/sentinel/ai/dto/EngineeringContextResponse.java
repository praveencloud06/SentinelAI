package com.sentinel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineeringContextResponse {
    private boolean enabled;
    private boolean available;
    private String message;

    @Builder.Default
    private List<Map<String, Object>> timeline = new ArrayList<>();

    @Builder.Default
    private List<Map<String, Object>> deployments = new ArrayList<>();

    @Builder.Default
    private List<Map<String, Object>> releases = new ArrayList<>();

    @Builder.Default
    private List<Map<String, Object>> jiraIssues = new ArrayList<>();
}
