package com.sentinel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogUploadRequest {
    private String logType; // "json", "csv", "text"
    private String content; // Raw log content
}
