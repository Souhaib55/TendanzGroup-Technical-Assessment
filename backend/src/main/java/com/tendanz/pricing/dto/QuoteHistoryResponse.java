package com.tendanz.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for quote history API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteHistoryResponse {

    private Long id;
    private String eventType;
    private String details;
    private String changedBy;
    private LocalDateTime changedAt;
}
