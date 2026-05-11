package com.einswen.blogapi.dto;

public record PetOptionResponse(
    String id,
    String label,
    String description,
    int satietyDelta
) {
}
