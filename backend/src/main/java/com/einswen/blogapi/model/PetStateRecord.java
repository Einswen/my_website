package com.einswen.blogapi.model;

public record PetStateRecord(
    long id,
    String name,
    String color,
    String outfitId,
    int satiety,
    String lastSatietyUpdate,
    String lastFedAt,
    String lastInteractedAt,
    String updatedAt
) {
}
