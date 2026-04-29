package com.sentinel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogSearchResult {
    private String logText;
    private String resolutionNotes;
    private double similarity;
}
