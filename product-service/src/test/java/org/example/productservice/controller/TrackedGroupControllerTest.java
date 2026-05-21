package org.example.productservice.controller;

import org.example.productservice.dto.CreateTrackedGroupRequest;
import org.example.productservice.dto.TrackedGroupResponse;
import org.example.productservice.dto.TrackedItemResponse;
import org.example.productservice.service.GroupTrackingService;
import org.example.productservice.service.PriceCheckScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrackedGroupControllerTest {

    @Mock private GroupTrackingService groupTrackingService;
    @Mock private PriceCheckScheduler priceCheckScheduler;

    private MockMvc mockMvc;

    private static final String USER_EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TrackedGroupController(groupTrackingService, priceCheckScheduler))
                .build();
    }

    private TrackedGroupResponse sampleGroup() {
        return new TrackedGroupResponse(
                1L, USER_EMAIL, 42999L, LocalDateTime.now(),
                List.of(
                        new TrackedItemResponse(1L, "Citrus", "iPhone 15", "https://citrus.ua/1", 42999L),
                        new TrackedItemResponse(2L, "OpenShop", "iPhone 15", "https://openshop.ua/1", 44999L)
                ),
                false, false
        );
    }

    @Test
    void createGroup_returns201_withGroupData() throws Exception {
        when(groupTrackingService.createGroup(any(), eq(USER_EMAIL))).thenReturn(sampleGroup());

        mockMvc.perform(post("/api/groups")
                        .header("X-User-Email", USER_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userEmail":"user@example.com",
                                  "telegramChatId":"123456789",
                                  "items":[
                                    {"marketplace":"Citrus","title":"iPhone 15","price":42999,"url":"https://citrus.ua/1"},
                                    {"marketplace":"OpenShop","title":"iPhone 15","price":44999,"url":"https://openshop.ua/1"}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lastMinPrice").value(42999))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void createGroup_returns400_whenNoAuthHeader() throws Exception {
        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userEmail":"user@example.com","telegramChatId":"123","items":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGroups_returnsListForUser() throws Exception {
        when(groupTrackingService.getGroupsByUser(USER_EMAIL)).thenReturn(List.of(sampleGroup()));

        mockMvc.perform(get("/api/groups").header("X-User-Email", USER_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userEmail").value(USER_EMAIL))
                .andExpect(jsonPath("$[0].lastMinPrice").value(42999));
    }

    @Test
    void getGroups_returnsEmptyList_whenNoGroups() throws Exception {
        when(groupTrackingService.getGroupsByUser("nobody@example.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/groups").header("X-User-Email", "nobody@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getGroups_returns400_whenNoAuthHeader() throws Exception {
        mockMvc.perform(get("/api/groups"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteGroup_returns204() throws Exception {
        doNothing().when(groupTrackingService).deleteGroup(1L, USER_EMAIL);

        mockMvc.perform(delete("/api/groups/1").header("X-User-Email", USER_EMAIL))
                .andExpect(status().isNoContent());

        verify(groupTrackingService).deleteGroup(1L, USER_EMAIL);
    }

    @Test
    void deleteGroup_returns400_whenNoAuthHeader() throws Exception {
        mockMvc.perform(delete("/api/groups/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testNotify_returns204_andCallsService() throws Exception {
        doNothing().when(groupTrackingService).sendTestNotification(eq(1L), any(), eq(USER_EMAIL));

        mockMvc.perform(post("/api/groups/1/test-notify")
                        .header("X-User-Email", USER_EMAIL)
                        .param("chatId", "123456789"))
                .andExpect(status().isNoContent());

        verify(groupTrackingService).sendTestNotification(1L, "123456789", USER_EMAIL);
    }

    @Test
    void triggerCheck_returns204_andCallsScheduler() throws Exception {
        doNothing().when(priceCheckScheduler).checkPrices();

        mockMvc.perform(post("/api/groups/trigger-check")
                        .header("X-User-Email", USER_EMAIL))
                .andExpect(status().isNoContent());

        verify(priceCheckScheduler).checkPrices();
    }

    @Test
    void triggerCheck_returns400_whenNoAuthHeader() throws Exception {
        mockMvc.perform(post("/api/groups/trigger-check"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simulatePriceChange_returns204_andCallsService() throws Exception {
        doNothing().when(groupTrackingService).simulatePriceChange(1L, USER_EMAIL);

        mockMvc.perform(post("/api/groups/1/simulate-price-change")
                        .header("X-User-Email", USER_EMAIL))
                .andExpect(status().isNoContent());

        verify(groupTrackingService).simulatePriceChange(1L, USER_EMAIL);
    }

    @Test
    void simulatePriceChange_returns400_whenNoAuthHeader() throws Exception {
        mockMvc.perform(post("/api/groups/1/simulate-price-change"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void seedHistory_returns204_andCallsService() throws Exception {
        doNothing().when(groupTrackingService).seedDemoHistory(1L, USER_EMAIL);

        mockMvc.perform(post("/api/groups/1/seed-history")
                        .header("X-User-Email", USER_EMAIL))
                .andExpect(status().isNoContent());

        verify(groupTrackingService).seedDemoHistory(1L, USER_EMAIL);
    }

    @Test
    void seedHistory_returns400_whenNoAuthHeader() throws Exception {
        mockMvc.perform(post("/api/groups/1/seed-history"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clearDemoHistory_returns204_andCallsService() throws Exception {
        doNothing().when(groupTrackingService).clearDemoHistory(1L, USER_EMAIL);

        mockMvc.perform(delete("/api/groups/1/demo-history")
                        .header("X-User-Email", USER_EMAIL))
                .andExpect(status().isNoContent());

        verify(groupTrackingService).clearDemoHistory(1L, USER_EMAIL);
    }

    @Test
    void clearDemoHistory_returns400_whenNoAuthHeader() throws Exception {
        mockMvc.perform(delete("/api/groups/1/demo-history"))
                .andExpect(status().isBadRequest());
    }
}
