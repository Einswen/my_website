package com.einswen.blogapi.dto;

import java.util.Map;

public record OpticalState(
    int score,
    boolean suitable,
    String label,
    String description,
    Map<String, Double> factorScores,
    Map<String, Double> normalizedFactors
) {
}
