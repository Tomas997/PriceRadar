package org.example.productservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Slf4j
@Service
public class TelegramNotificationService {

    @Value("${telegram.bot.token:}")
    private String botToken;

    private final RestClient restClient = RestClient.create();

    public void send(String chatId, String text) {
        if (botToken.isBlank()) {
            log.warn("TELEGRAM_BOT_TOKEN is not configured");
            return;
        }
        if (chatId == null || chatId.isBlank()) {
            log.warn("Telegram chatId is empty, skipping notification");
            return;
        }
        log.info("Sending Telegram message to chatId={}, token starts with={}",
                chatId, botToken.substring(0, Math.min(10, botToken.length())));
        try {
            restClient.post()
                    .uri("https://api.telegram.org/bot" + botToken + "/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML"))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Telegram notification sent successfully to chatId={}", chatId);
        } catch (RestClientResponseException e) {
            log.error("Telegram API error for chatId={}: status={} body={}",
                    chatId, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to send Telegram notification to chatId={}: {}", chatId, e.getMessage());
        }
    }
}
