package com.einswen.blogapi.dto;

import java.util.List;

public record PetStateResponse(
    String name,
    String color,
    String outfitId,
    int satiety,
    String hungerStage,
    String mood,
    String statusText,
    String reaction,
    String lastFedAt,
    String lastInteractedAt,
    String updatedAt,
    List<PetOptionResponse> foods,
    List<PetOptionResponse> outfits
) {
}
