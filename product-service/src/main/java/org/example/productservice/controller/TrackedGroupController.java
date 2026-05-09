package org.example.productservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.productservice.dto.CreateTrackedGroupRequest;
import org.example.productservice.dto.TrackedGroupResponse;
import org.example.productservice.service.GroupTrackingService;
import org.example.productservice.service.PriceCheckScheduler;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class TrackedGroupController {

    private final GroupTrackingService groupTrackingService;
    private final PriceCheckScheduler priceCheckScheduler;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrackedGroupResponse createGroup(@RequestBody CreateTrackedGroupRequest request) {
        return groupTrackingService.createGroup(request);
    }

    @GetMapping
    public List<TrackedGroupResponse> getGroups(@RequestParam String userEmail) {
        return groupTrackingService.getGroupsByUser(userEmail);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable Long id) {
        groupTrackingService.deleteGroup(id);
    }

    @PostMapping("/{id}/test-notify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void testNotify(@PathVariable Long id,
                           @RequestParam(required = false) String chatId) {
        groupTrackingService.sendTestNotification(id, chatId);
    }

    @PostMapping("/trigger-check")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void triggerCheck() {
        priceCheckScheduler.checkPrices();
    }

    @PostMapping("/{id}/simulate-price-change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void simulatePriceChange(@PathVariable Long id) {
        groupTrackingService.simulatePriceChange(id);
    }
}
