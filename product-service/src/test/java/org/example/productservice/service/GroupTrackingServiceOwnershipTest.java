package org.example.productservice.service;

import org.example.productservice.model.TrackedGroup;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupTrackingServiceOwnershipTest {

    @Mock private TrackedGroupRepository groupRepo;
    @Mock private CatalogItemRepository catalogItemRepo;
    @Mock private GroupPriceEntryRepository priceEntryRepo;
    @Mock private TelegramNotificationService telegramService;

    @InjectMocks private GroupTrackingService service;

    private TrackedGroup groupOwnedBy(String email) {
        TrackedGroup group = new TrackedGroup();
        group.setUserEmail(email);
        return group;
    }

    // ─── deleteGroup ─────────────────────────────────────────────────────────

    @Test
    void deleteGroup_throwsForbidden_whenEmailDoesNotMatch() {
        when(groupRepo.findById(1L)).thenReturn(Optional.of(groupOwnedBy("owner@example.com")));

        assertThatThrownBy(() -> service.deleteGroup(1L, "intruder@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(groupRepo, never()).deleteById(any());
    }

    @Test
    void deleteGroup_succeeds_whenEmailMatches() {
        when(groupRepo.findById(1L)).thenReturn(Optional.of(groupOwnedBy("owner@example.com")));

        service.deleteGroup(1L, "owner@example.com");

        verify(groupRepo).deleteById(1L);
    }

    @Test
    void deleteGroup_throwsNotFound_whenGroupDoesNotExist() {
        when(groupRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteGroup(99L, "owner@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteGroup_throwsUnauthorized_whenEmailIsNull() {
        when(groupRepo.findById(1L)).thenReturn(Optional.of(groupOwnedBy("owner@example.com")));

        assertThatThrownBy(() -> service.deleteGroup(1L, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(groupRepo, never()).deleteById(any());
    }

    @Test
    void deleteGroup_throwsUnauthorized_whenEmailIsBlank() {
        when(groupRepo.findById(1L)).thenReturn(Optional.of(groupOwnedBy("owner@example.com")));

        assertThatThrownBy(() -> service.deleteGroup(1L, "   "))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(groupRepo, never()).deleteById(any());
    }

    // ─── getGroupHistory ─────────────────────────────────────────────────────

    @Test
    void getGroupHistory_returnsEmptySeries_whenOwnerAndNoItems() {
        when(groupRepo.findById(1L)).thenReturn(Optional.of(groupOwnedBy("owner@example.com")));
        when(priceEntryRepo.findByCatalogItemIdInOrderByRecordedAtAsc(List.of())).thenReturn(List.of());

        var result = service.getGroupHistory(1L, "owner@example.com");

        assertThat(result.groupId()).isEqualTo(1L);
        assertThat(result.series()).isEmpty();
        assertThat(result.hasDemo()).isFalse();
    }

}
