package com.einswen.blogapi.service;

import com.einswen.blogapi.config.AppProperties;
import com.einswen.blogapi.dto.MessageCreateRequest;
import com.einswen.blogapi.dto.MessageCreateResponse;
import com.einswen.blogapi.exception.ApiException;
import com.einswen.blogapi.model.Message;
import com.einswen.blogapi.repository.MessageRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GuestbookService {

    private final MessageRepository messageRepository;
    private final AppProperties appProperties;

    public GuestbookService(MessageRepository messageRepository, AppProperties appProperties) {
        this.messageRepository = messageRepository;
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        messageRepository.initSchema();
    }

    public List<Message> listMessages() {
        return messageRepository.findAllOrderByCreatedAtDesc();
    }

    public MessageCreateResponse createMessage(MessageCreateRequest payload, HttpServletRequest request) {
        String name = payload.name() == null ? "" : payload.name().trim();
        String content = payload.content() == null ? "" : payload.content().trim();

        if (name.isEmpty() || content.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请填写名字和留言。");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        long cooldownSeconds = appProperties.getGuestbook().getCooldownSeconds();
        String ipHash = hashIp(resolveClientIp(request));

        messageRepository.findLatestCreatedAtByIpHash(ipHash).ifPresent(lastCreatedAt -> {
            OffsetDateTime availableAt = lastCreatedAt.plusSeconds(cooldownSeconds);

            if (now.isBefore(availableAt)) {
                long retryAfterSeconds = Math.max(1, availableAt.toEpochSecond() - now.toEpochSecond() + 1);
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, Map.of(
                    "message", "一小时只能留言一次。",
                    "retryAfterSeconds", retryAfterSeconds
                ));
            }
        });

        Message message = messageRepository.insert(name, content, now.toString(), ipHash);
        return new MessageCreateResponse(message, cooldownSeconds);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private String hashIp(String ipAddress) {
        String rawValue = appProperties.getGuestbook().getIpHashSalt() + ":" + ipAddress;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
