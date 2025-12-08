package com.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TourneeStatsResponse {
    private long totalTours;
    private long plannedCount;
    private long inProgressCount;
    private long completedCount;
    private double averageFillRate;
}
