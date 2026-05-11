package com.einswen.blogapi.dto;

import com.einswen.blogapi.model.Message;

public record MessageCreateResponse(Message message, long cooldownSeconds) {
}
