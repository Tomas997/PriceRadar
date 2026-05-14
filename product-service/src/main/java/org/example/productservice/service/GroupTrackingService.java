package org.example.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.dto.CreateTrackedGroupRequest;
import org.example.productservice.dto.GroupPriceHistoryResponse;
import org.example.productservice.dto.GroupPriceHistoryResponse.ItemSeries;
import org.example.productservice.dto.GroupPriceHistoryResponse.PricePoint;
import org.example.productservice.dto.TrackedGroupResponse;
import org.example.productservice.model.GroupPriceEntry;
import org.example.productservice.model.TrackedGroup;
import org.example.productservice.model.TrackedItem;
import org.example.productservice.repository.GroupPriceEntryRepository;
import org.example.productservice.repository.TrackedGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupTrackingService {

    private final TrackedGroupRepository groupRepo;
    private final GroupPriceEntryRepository priceEntryRepo;
    private final TelegramNotificationService telegramService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional
    public TrackedGroupResponse createGroup(CreateTrackedGroupRequest req) {
        TrackedGroup group = new TrackedGroup();
        group.setUserEmail(req.userEmail());
        group.setTelegramChatId(req.telegramChatId());
        group.setCreatedAt(LocalDateTime.now());

        Long minPrice = null;
        for (var itemReq : req.items()) {
            TrackedItem item = new TrackedItem();
            item.setGroup(group);
            item.setMarketplace(itemReq.marketplace());
            item.setTitle(itemReq.title());
            item.setUrl(itemReq.url());
            item.setCurrentPrice(itemReq.price());
            group.getItems().add(item);
            if (itemReq.price() != null && (minPrice == null || itemReq.price() < minPrice)) {
                minPrice = itemReq.price();
            }
        }
        group.setLastMinPrice(minPrice);

        TrackedGroup saved = groupRepo.save(group);

        // record initial price snapshot for each item
        for (TrackedItem item : saved.getItems()) {
            if (item.getCurrentPrice() != null) {
                priceEntryRepo.save(new GroupPriceEntry(item.getId(), item.getMarketplace(), item.getCurrentPrice()));
            }
        }

        log.info("Created tracked group id={} for user={} with {} items",
                saved.getId(), req.userEmail(), req.items().size());
        return TrackedGroupResponse.from(saved);
    }

    public List<TrackedGroupResponse> getGroupsByUser(String userEmail) {
        return groupRepo.findByUserEmail(userEmail).stream()
                .map(TrackedGroupResponse::from)
                .toList();
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        groupRepo.deleteById(groupId);
        log.info("Deleted tracked group id={}", groupId);
    }

    @Transactional
    public void simulatePriceChange(Long groupId) {
        groupRepo.findById(groupId).ifPresent(group -> {
            group.setLastMinPrice(1L);
            groupRepo.save(group);
            log.info("Simulated price change for group id={}: lastMinPrice set to 1", groupId);
        });
    }

    @Transactional
    public void seedDemoHistory(Long groupId) {
        TrackedGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));

        List<Long> itemIds = group.getItems().stream().map(TrackedItem::getId).toList();
        if (priceEntryRepo.existsByTrackedItemIdInAndDemoTrue(itemIds)) {
            log.info("Demo history already exists for group id={}, skipping", groupId);
            return;
        }

        var rng = new java.util.Random();
        for (TrackedItem item : group.getItems()) {
            if (item.getCurrentPrice() == null) continue;
            long base = item.getCurrentPrice();
            for (int daysAgo = 6; daysAgo >= 1; daysAgo--) {
                long variation = (long) (base * (rng.nextDouble() * 0.16 - 0.08));
                long price = Math.max(1, base + variation);
                GroupPriceEntry entry = new GroupPriceEntry(item.getId(), item.getMarketplace(), price);
                entry.setRecordedAt(LocalDateTime.now().minusDays(daysAgo).withHour(10).withMinute(0).withSecond(0).withNano(0));
                entry.setDemo(true);
                priceEntryRepo.save(entry);
            }
        }
        log.info("Seeded demo history for group id={}", groupId);
    }

    @Transactional
    public void clearDemoHistory(Long groupId) {
        TrackedGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        List<Long> itemIds = group.getItems().stream().map(TrackedItem::getId).toList();
        priceEntryRepo.deleteByTrackedItemIdInAndDemoTrue(itemIds);
        log.info("Cleared demo history for group id={}", groupId);
    }

    public GroupPriceHistoryResponse getGroupHistory(Long groupId) {
        TrackedGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));

        List<Long> itemIds = group.getItems().stream().map(TrackedItem::getId).toList();
        List<GroupPriceEntry> entries = priceEntryRepo.findByTrackedItemIdInOrderByRecordedAtAsc(itemIds);

        boolean hasDemo = entries.stream().anyMatch(GroupPriceEntry::isDemo);

        Map<Long, List<GroupPriceEntry>> byItem = entries.stream()
                .collect(Collectors.groupingBy(GroupPriceEntry::getTrackedItemId));

        List<ItemSeries> series = group.getItems().stream()
                .map(item -> {
                    List<PricePoint> points = byItem.getOrDefault(item.getId(), List.of()).stream()
                            .map(e -> new PricePoint(e.getRecordedAt().format(DATE_FMT), e.getPrice()))
                            .toList();
                    return new ItemSeries(item.getId(), item.getMarketplace(), item.getTitle(), points);
                })
                .toList();

        return new GroupPriceHistoryResponse(groupId, hasDemo, series);
    }

    public void sendTestNotification(Long groupId, String overrideChatId) {
        groupRepo.findById(groupId).ifPresent(group -> {
            String chatId = (overrideChatId != null && !overrideChatId.isBlank())
                    ? overrideChatId
                    : group.getTelegramChatId();
            var sb = new StringBuilder();
            sb.append("🔔 <b>Тест PriceRadar — сповіщення працює!</b>\n\n");
            sb.append("Мінімальна ціна у групі: <b>")
              .append(group.getLastMinPrice() != null ? group.getLastMinPrice() + " грн" : "N/A")
              .append("</b>\n\n");
            sb.append("Товари:\n");
            for (TrackedItem item : group.getItems()) {
                sb.append("• <b>[").append(item.getMarketplace()).append("]</b> ")
                  .append(item.getTitle())
                  .append(" — ").append(item.getCurrentPrice() != null ? item.getCurrentPrice() + " грн" : "N/A")
                  .append("\n  <a href=\"").append(item.getUrl()).append("\">Переглянути</a>\n");
            }
            telegramService.send(chatId, sb.toString());
            log.info("Sent test notification for group id={} to chatId={}", groupId, chatId);
        });
    }
}
