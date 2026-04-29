package com.sentinel.ai.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Log {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String rawLog;

    @Column(columnDefinition = "TEXT")
    private String processedLog;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
