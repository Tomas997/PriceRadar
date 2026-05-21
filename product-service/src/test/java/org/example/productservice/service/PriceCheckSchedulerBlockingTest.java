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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceCheckSchedulerBlockingTest {

    @Mock private CatalogItemRepository catalogItemRepo;
    @Mock private TrackedGroupRepository groupRepo;
    @Mock private GroupPriceEntryRepository priceEntryRepo;
    @Mock private TelegramNotificationService telegramService;

    @InjectMocks private PriceCheckScheduler scheduler;

    private CatalogItem catalogItem(Long id, Long price) {
        CatalogItem ci = new CatalogItem("Citrus", "iPhone 15", "https://citrus.ua/" + id, price);
        ci.setId(id);
        return ci;
    }

    private TrackedGroup group(Long id, String email, Long minPrice, boolean blocked, CatalogItem... items) {
        TrackedGroup g = new TrackedGroup();
        g.setId(id);
        g.setUserEmail(email);
        g.setTelegramChatId("chat-" + id);
        g.setLastMinPrice(minPrice);
        g.setTelegramBlocked(blocked);
        for (CatalogItem ci : items) {
            TrackedItem ti = new TrackedItem();
            ti.setGroup(g);
            ti.setCatalogItem(ci);
            g.getItems().add(ti);
        }
        return g;
    }

    @Test
    void checkPrices_usesFindActiveItems_notFindAll() {
        when(catalogItemRepo.findActiveItems()).thenReturn(List.of());
        when(groupRepo.findAll()).thenReturn(List.of());

        scheduler.checkPrices();

        verify(catalogItemRepo).findActiveItems();
        verify(catalogItemRepo, never()).findAll();
    }

    @Test
    void checkPrices_skipsBlockedGroups_inNotificationPhase() {
        CatalogItem ci = catalogItem(1L, 42000L);
        TrackedGroup blocked = group(1L, "user@example.com", 50000L, true, ci);

        when(catalogItemRepo.findActiveItems()).thenReturn(List.of());
        when(groupRepo.findAll()).thenReturn(List.of(blocked));

        scheduler.checkPrices();

        verify(telegramService, never()).send(any(), any());
    }

    @Test
    void checkPrices_sendsNotification_forUnblockedGroupWithPriceChange() {
        CatalogItem ci = catalogItem(1L, 40000L);
        TrackedGroup active = group(1L, "user@example.com", 50000L, false, ci);

        when(catalogItemRepo.findActiveItems()).thenReturn(List.of());
        when(groupRepo.findAll()).thenReturn(List.of(active));
        when(telegramService.send(anyString(), anyString())).thenReturn(TelegramSendResult.OK);

        scheduler.checkPrices();

        verify(telegramService).send(eq("chat-1"), anyString());
    }

    @Test
    void recalculateGroupMin_marksGroupBlocked_whenUserBlockedBot() {
        CatalogItem ci = catalogItem(1L, 40000L);
        TrackedGroup group = group(1L, "user@example.com", 50000L, false, ci);

        when(telegramService.send(anyString(), anyString())).thenReturn(TelegramSendResult.USER_BLOCKED_BOT);

        scheduler.recalculateGroupMin(group);

        assertThat(group.isTelegramBlocked()).isTrue();
        verify(groupRepo, atLeastOnce()).save(group);
    }

    @Test
    void recalculateGroupMin_doesNotMarkBlocked_onTransientError() {
        CatalogItem ci = catalogItem(1L, 40000L);
        TrackedGroup group = group(1L, "user@example.com", 50000L, false, ci);

        when(telegramService.send(anyString(), anyString())).thenReturn(TelegramSendResult.ERROR);

        scheduler.recalculateGroupMin(group);

        assertThat(group.isTelegramBlocked()).isFalse();
    }

    @Test
    void recalculateGroupMin_skipsNotification_whenGroupAlreadyBlocked() {
        CatalogItem ci = catalogItem(1L, 40000L);
        TrackedGroup group = group(1L, "user@example.com", 50000L, true, ci);

        scheduler.recalculateGroupMin(group);

        verify(telegramService, never()).send(any(), any());
    }

    @Test
    void recalculateGroupMin_noOp_whenPriceUnchanged() {
        CatalogItem ci = catalogItem(1L, 42000L);
        TrackedGroup group = group(1L, "user@example.com", 42000L, false, ci);

        scheduler.recalculateGroupMin(group);

        verify(telegramService, never()).send(any(), any());
        verify(groupRepo, never()).save(any());
    }
}
