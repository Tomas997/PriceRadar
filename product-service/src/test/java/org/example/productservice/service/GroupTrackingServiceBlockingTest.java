package org.example.productservice.service;

import org.example.productservice.model.CatalogItem;
import org.example.productservice.model.TrackedGroup;
import org.example.productservice.model.TrackedItem;
import org.example.productservice.repository.CatalogItemRepository;
import org.example.productservice.repository.GroupPriceEntryRepository;
import org.example.productservice.repository.TrackedGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupTrackingServiceBlockingTest {

    @Mock private TrackedGroupRepository groupRepo;
    @Mock private CatalogItemRepository catalogItemRepo;
    @Mock private GroupPriceEntryRepository priceEntryRepo;
    @Mock private TelegramNotificationService telegramService;

    @InjectMocks private GroupTrackingService service;

    private static final String OWNER = "owner@example.com";

    private TrackedGroup groupWithItem(boolean telegramBlocked) {
        CatalogItem ci = new CatalogItem("Citrus", "iPhone 15", "https://citrus.ua/1", 42000L);
        ci.setId(1L);

        TrackedItem ti = new TrackedItem();
        ti.setCatalogItem(ci);

        TrackedGroup group = new TrackedGroup();
        group.setId(1L);
        group.setUserEmail(OWNER);
        group.setTelegramChatId("chat-1");
        group.setLastMinPrice(42000L);
        group.setTelegramBlocked(telegramBlocked);
        group.getItems().add(ti);
        ti.setGroup(group);

        return group;
    }

    @Test
    void sendTestNotification_setsBlockedFlag_andThrows502() {
        TrackedGroup group = groupWithItem(false);
        when(groupRepo.findById(1L)).thenReturn(Optional.of(group));
        when(telegramService.send(anyString(), anyString())).thenReturn(TelegramSendResult.USER_BLOCKED_BOT);

        assertThatThrownBy(() -> service.sendTestNotification(1L, null, OWNER))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_GATEWAY));

        assertThat(group.isTelegramBlocked()).isTrue();
        verify(groupRepo, atLeastOnce()).save(group);
    }

    @Test
    void sendTestNotification_clearsBlockedFlag_onSuccessAfterPreviousBlock() {
        TrackedGroup group = groupWithItem(true);
        when(groupRepo.findById(1L)).thenReturn(Optional.of(group));
        when(telegramService.send(anyString(), anyString())).thenReturn(TelegramSendResult.OK);

        service.sendTestNotification(1L, null, OWNER);

        assertThat(group.isTelegramBlocked()).isFalse();
        verify(groupRepo, atLeastOnce()).save(group);
    }

    @Test
    void sendTestNotification_doesNotChangeFlag_onTransientError() {
        TrackedGroup group = groupWithItem(false);
        when(groupRepo.findById(1L)).thenReturn(Optional.of(group));
        when(telegramService.send(anyString(), anyString())).thenReturn(TelegramSendResult.ERROR);

        service.sendTestNotification(1L, null, OWNER);

        assertThat(group.isTelegramBlocked()).isFalse();
    }

    @Test
    void sendTestNotification_usesOverrideChatId_whenProvided() {
        TrackedGroup group = groupWithItem(false);
        when(groupRepo.findById(1L)).thenReturn(Optional.of(group));
        when(telegramService.send(eq("override-chat"), anyString())).thenReturn(TelegramSendResult.OK);

        service.sendTestNotification(1L, "override-chat", OWNER);

        verify(telegramService).send(eq("override-chat"), anyString());
    }

    @Test
    void sendTestNotification_usesChatIdFromGroup_whenOverrideIsNull() {
        TrackedGroup group = groupWithItem(false);
        when(groupRepo.findById(1L)).thenReturn(Optional.of(group));
        when(telegramService.send(eq("chat-1"), anyString())).thenReturn(TelegramSendResult.OK);

        service.sendTestNotification(1L, null, OWNER);

        verify(telegramService).send(eq("chat-1"), anyString());
    }
}
