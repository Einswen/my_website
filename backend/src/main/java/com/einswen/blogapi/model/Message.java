package com.einswen.blogapi.model;

public record Message(
    long id,
    String name,
    String content,
    String createdAt,
    boolean pinned
) {
}
