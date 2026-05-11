package com.einswen.blogapi.model;

public record PetChatMessage(
    String role,
    String content
) {
}
