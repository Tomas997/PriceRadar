package org.example.productservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.productservice.dto.CheckConfigResponse;
import org.example.productservice.dto.CreateTrackedGroupRequest;
import org.example.productservice.dto.GroupPriceHistoryResponse;
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
    public TrackedGroupResponse createGroup(
            @RequestBody CreateTrackedGroupRequest request,
            @RequestHeader("X-User-Email") String userEmail) {
        return groupTrackingService.createGroup(request, userEmail);
    }

    @GetMapping
    public List<TrackedGroupResponse> getGroups(
            @RequestHeader("X-User-Email") String userEmail) {
        return groupTrackingService.getGroupsByUser(userEmail);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail) {
        groupTrackingService.deleteGroup(id, userEmail);
    }

    @PostMapping("/{id}/test-notify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void testNotify(
            @PathVariable Long id,
            @RequestParam(required = false) String chatId,
            @RequestHeader("X-User-Email") String userEmail) {
        groupTrackingService.sendTestNotification(id, chatId, userEmail);
    }

    @PostMapping("/trigger-check")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void triggerCheck(@RequestHeader("X-User-Email") String userEmail) {
        priceCheckScheduler.checkPrices();
    }

    @GetMapping("/check-config")
    public CheckConfigResponse getCheckConfig() {
        return new CheckConfigResponse(priceCheckScheduler.getCheckHour(), priceCheckScheduler.isButtonVisible());
    }

    @PostMapping("/{id}/check-prices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void checkGroupPrices(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail) {
        groupTrackingService.verifyGroupOwnership(id, userEmail);
        priceCheckScheduler.checkGroup(id);
    }

    @GetMapping("/{id}/history")
    public GroupPriceHistoryResponse getHistory(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail) {
        return groupTrackingService.getGroupHistory(id, userEmail);
    }

    @PostMapping("/{id}/seed-history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void seedHistory(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail) {
        groupTrackingService.seedDemoHistory(id, userEmail);
    }

    @DeleteMapping("/{id}/demo-history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearDemoHistory(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail) {
        groupTrackingService.clearDemoHistory(id, userEmail);
    }

    @PostMapping("/{id}/simulate-price-change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void simulatePriceChange(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail) {
        groupTrackingService.simulatePriceChange(id, userEmail);
    }
}
