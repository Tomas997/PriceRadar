package org.example.productservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TelegramNotificationServiceTest {

    @Mock private RestClient restClient;
    @Mock(answer = Answers.RETURNS_SELF) private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private TelegramNotificationService service;

    @BeforeEach
    void setUp() {
        service = new TelegramNotificationService(restClient);
        ReflectionTestUtils.setField(service, "botToken", "test-bot-token");

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(responseSpec).when(requestBodyUriSpec).retrieve();
    }

    @Test
    void send_returnsOk_onSuccess() {
        doReturn(null).when(responseSpec).toBodilessEntity();

        TelegramSendResult result = service.send("123456", "Hello");

        assertThat(result).isEqualTo(TelegramSendResult.OK);
    }

    @Test
    void send_returnsUserBlockedBot_when403BotBlocked() {
        doThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden",
                null, "{\"description\":\"bot was blocked by the user\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8))
                .when(responseSpec).toBodilessEntity();

        TelegramSendResult result = service.send("123456", "Hello");

        assertThat(result).isEqualTo(TelegramSendResult.USER_BLOCKED_BOT);
    }

    @Test
    void send_returnsUserBlockedBot_when403UserDeactivated() {
        doThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden",
                null, "{\"description\":\"user is deactivated\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8))
                .when(responseSpec).toBodilessEntity();

        TelegramSendResult result = service.send("123456", "Hello");

        assertThat(result).isEqualTo(TelegramSendResult.USER_BLOCKED_BOT);
    }

    @Test
    void send_returnsUserBlockedBot_when400ChatNotFound() {
        doThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                null, "{\"description\":\"chat not found\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8))
                .when(responseSpec).toBodilessEntity();

        TelegramSendResult result = service.send("123456", "Hello");

        assertThat(result).isEqualTo(TelegramSendResult.USER_BLOCKED_BOT);
    }

    @Test
    void send_returnsError_on403OtherReason() {
        doThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden",
                null, "{\"description\":\"some other reason\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8))
                .when(responseSpec).toBodilessEntity();

        TelegramSendResult result = service.send("123456", "Hello");

        assertThat(result).isEqualTo(TelegramSendResult.ERROR);
    }

    @Test
    void send_returnsError_onNetworkException() {
        doThrow(new RuntimeException("Connection refused")).when(restClient).post();

        TelegramSendResult result = service.send("123456", "Hello");

        assertThat(result).isEqualTo(TelegramSendResult.ERROR);
    }

    @Test
    void send_returnsError_whenBotTokenBlank() {
        ReflectionTestUtils.setField(service, "botToken", "");

        TelegramSendResult result = service.send("123456", "Hello");

        assertThat(result).isEqualTo(TelegramSendResult.ERROR);
        verify(restClient, never()).post();
    }

    @Test
    void send_returnsError_whenChatIdNull() {
        TelegramSendResult result = service.send(null, "Hello");

        assertThat(result).isEqualTo(TelegramSendResult.ERROR);
        verify(restClient, never()).post();
    }

    @Test
    void send_returnsError_whenChatIdBlank() {
        TelegramSendResult result = service.send("  ", "Hello");

        assertThat(result).isEqualTo(TelegramSendResult.ERROR);
        verify(restClient, never()).post();
    }
}
