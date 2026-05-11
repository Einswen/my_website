package com.einswen.blogapi.controller;

import com.einswen.blogapi.dto.MessageCreateRequest;
import com.einswen.blogapi.dto.MessageCreateResponse;
import com.einswen.blogapi.model.Message;
import com.einswen.blogapi.service.GuestbookService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final GuestbookService guestbookService;

    public MessageController(GuestbookService guestbookService) {
        this.guestbookService = guestbookService;
    }

    @GetMapping
    public List<Message> listMessages() {
        return guestbookService.listMessages();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageCreateResponse createMessage(
        @RequestBody MessageCreateRequest payload,
        HttpServletRequest request
    ) {
        return guestbookService.createMessage(payload, request);
    }
}
