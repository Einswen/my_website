package com.einswen.blogapi.controller;

import com.einswen.blogapi.dto.PetActionRequest;
import com.einswen.blogapi.dto.PetChatRequest;
import com.einswen.blogapi.dto.PetChatResponse;
import com.einswen.blogapi.dto.PetStateResponse;
import com.einswen.blogapi.service.PetService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/pet")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public PetStateResponse getPetState() {
        return petService.getPetState();
    }

    @GetMapping(value = "/sprite", produces = MediaType.IMAGE_GIF_VALUE)
    public ResponseEntity<Resource> getPetSprite() {
        Resource sprite = new ClassPathResource("pet-assets/oneko.gif");
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
            .body(sprite);
    }

    @PostMapping("/pat")
    public PetStateResponse patPet() {
        return petService.patPet();
    }

    @PostMapping("/feed")
    public PetStateResponse feedPet(@RequestBody PetActionRequest request) {
        return petService.feedPet(request.optionId());
    }

    @PostMapping("/outfit")
    public PetStateResponse changeOutfit(@RequestBody PetActionRequest request) {
        return petService.changeOutfit(request.optionId());
    }

    @PostMapping("/chat")
    public PetChatResponse chat(@RequestBody PetChatRequest request) {
        return petService.chat(request.message());
    }
}
