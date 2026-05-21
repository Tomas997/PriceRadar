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

    public TelegramSendResult send(String chatId, String text) {
        if (botToken.isBlank()) {
            log.warn("TELEGRAM_BOT_TOKEN is not configured");
            return TelegramSendResult.ERROR;
        }
        if (chatId == null || chatId.isBlank()) {
            log.warn("Telegram chatId is empty, skipping notification");
            return TelegramSendResult.ERROR;
        }
        try {
            restClient.post()
                    .uri("https://api.telegram.org/bot" + botToken + "/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML"))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Telegram notification sent to chatId={}", chatId);
            return TelegramSendResult.OK;
        } catch (RestClientResponseException e) {
            log.error("Telegram API error for chatId={}: status={} body={}",
                    chatId, e.getStatusCode(), e.getResponseBodyAsString());
            if (isUserBlockedError(e)) {
                log.warn("User chatId={} has blocked the bot or is unreachable permanently", chatId);
                return TelegramSendResult.USER_BLOCKED_BOT;
            }
            return TelegramSendResult.ERROR;
        } catch (Exception e) {
            log.error("Failed to send Telegram notification to chatId={}: {}", chatId, e.getMessage());
            return TelegramSendResult.ERROR;
        }
    }

    private boolean isUserBlockedError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String body = e.getResponseBodyAsString();
        if (status == 403) {
            return body.contains("bot was blocked by the user")
                    || body.contains("user is deactivated");
        }
        if (status == 400) {
            return body.contains("chat not found");
        }
        return false;
    }
}
