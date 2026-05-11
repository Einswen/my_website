package com.einswen.blogapi.dto;

public record PetChatResponse(
    String reply,
    PetStateResponse pet
) {
}
