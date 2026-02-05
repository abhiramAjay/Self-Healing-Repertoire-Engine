package com.selfhealing.repertoire.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private long totalRecordings;
    private long orphanedCount;
    private long healedCount;
    // Estimated Revenue Recovery ($0.05 per healed record for this demo)
    private double estimatedRecovery;
}
