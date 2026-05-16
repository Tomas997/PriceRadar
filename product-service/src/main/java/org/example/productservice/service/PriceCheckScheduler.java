package org.example.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.dto.MarketplaceSearchResult;
import org.example.productservice.model.CatalogItem;
import org.example.productservice.model.GroupPriceEntry;
import org.example.productservice.model.ProductCandidate;
import org.example.productservice.model.TrackedGroup;
import org.example.productservice.model.TrackedItem;
import org.example.productservice.repository.CatalogItemRepository;
import org.example.productservice.repository.GroupPriceEntryRepository;
import org.example.productservice.repository.TrackedGroupRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceCheckScheduler {

    private final CatalogItemRepository catalogItemRepo;
    private final TrackedGroupRepository groupRepo;
    private final GroupPriceEntryRepository priceEntryRepo;
    private final TelegramNotificationService telegramService;

    @Value("${price.check.hour:10}")
    private int checkHour;

    public int getCheckHour() { return checkHour; }

    @Value("${parser.service.url:http://localhost:8082}")
    private String parserUrl;

    private final RestClient restClient = RestClient.create();

    @Scheduled(cron = "${price.check.cron:0 0 10 * * *}")
    @Transactional
    public void checkPrices() {
        log.info("Daily price check started");

        for (CatalogItem item : catalogItemRepo.findAll()) {
            try {
                Long fresh = fetchPrice(item);
                if (fresh != null) {
                    item.setCurrentPrice(fresh);
                    item.setLastParsedAt(LocalDateTime.now());
                    catalogItemRepo.save(item);
                    saveIfNeeded(item.getId(), item.getMarketplace(), fresh);
                }
            } catch (Exception e) {
                log.error("Error updating catalog item id={}: {}", item.getId(), e.getMessage());
            }
        }

        for (TrackedGroup group : groupRepo.findAll()) {
            try {
                recalculateGroupMin(group);
            } catch (Exception e) {
                log.error("Error recalculating group id={}: {}", group.getId(), e.getMessage());
            }
        }

        log.info("Daily price check completed");
    }

    @Transactional
    public void checkGroup(Long groupId) {
        groupRepo.findById(groupId).ifPresent(group -> {
            for (TrackedItem item : group.getItems()) {
                CatalogItem ci = item.getCatalogItem();
                if (!isStale(ci)) {
                    log.debug("CatalogItem id={} is fresh, skipping fetch", ci.getId());
                    continue;
                }
                try {
                    Long fresh = fetchPrice(ci);
                    if (fresh != null) {
                        ci.setCurrentPrice(fresh);
                        ci.setLastParsedAt(LocalDateTime.now());
                        catalogItemRepo.save(ci);
                        saveIfNeeded(ci.getId(), ci.getMarketplace(), fresh);
                    }
                } catch (Exception e) {
                    log.error("Error updating catalog item id={}: {}", ci.getId(), e.getMessage());
                }
            }
            recalculateGroupMin(group);
        });
    }

    public boolean isButtonVisible() {
        LocalTime now = LocalTime.now();
        LocalTime windowStart = LocalTime.of(Math.max(0, checkHour - 4), 0);
        LocalTime windowEnd = LocalTime.of(Math.min(23, checkHour + 4), 0);
        return now.isBefore(windowStart) || now.isAfter(windowEnd);
    }

    boolean isStale(CatalogItem item) {
        if (item.getLastParsedAt() == null) return true;
        return item.getLastParsedAt().isBefore(LocalDateTime.now().minusHours(4));
    }

    void recalculateGroupMin(TrackedGroup group) {
        Long newMin = group.getItems().stream()
                .map(item -> item.getCatalogItem().getCurrentPrice())
                .filter(Objects::nonNull)
                .min(Long::compareTo)
                .orElse(null);

        if (newMin == null) return;

        Long oldMin = group.getLastMinPrice();
        if (!newMin.equals(oldMin)) {
            group.setLastMinPrice(newMin);
            groupRepo.save(group);
            telegramService.send(group.getTelegramChatId(), buildMessage(group, newMin, oldMin));
            log.info("Group id={} min price changed {} -> {}", group.getId(), oldMin, newMin);
        }
    }

    private Long fetchPrice(CatalogItem item) {
        try {
            String encoded = URLEncoder.encode(item.getTitle(), StandardCharsets.UTF_8);
            URI uri = URI.create(parserUrl + "/api/search?query=" + encoded);
            List<MarketplaceSearchResult> results = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (results == null) return null;
            Long price = results.stream()
                    .filter(r -> r.marketplace().equalsIgnoreCase(item.getMarketplace()))
                    .flatMap(r -> r.products().stream())
                    .filter(p -> item.getUrl().equals(p.url()))
                    .map(ProductCandidate::price)
                    .findFirst()
                    .orElse(null);
            if (price != null) {
                log.debug("fetchPrice catalogItem id={} [{}] -> {} грн", item.getId(), item.getMarketplace(), price);
            } else {
                log.warn("fetchPrice catalogItem id={} [{}]: no URL match for '{}'", item.getId(), item.getMarketplace(), item.getUrl());
            }
            return price;
        } catch (Exception e) {
            log.warn("fetchPrice catalogItem id={} [{}]: {}", item.getId(), item.getMarketplace(), e.getMessage());
            return null;
        }
    }

    private void saveIfNeeded(Long catalogItemId, String marketplace, Long price) {
        Optional<GroupPriceEntry> last = priceEntryRepo.findTopByCatalogItemIdOrderByRecordedAtDesc(catalogItemId);
        boolean noEntryToday = last.isEmpty()
                || !last.get().getRecordedAt().toLocalDate().equals(LocalDate.now());
        boolean priceChanged = last.isEmpty() || !last.get().getPrice().equals(price);
        if (noEntryToday || priceChanged) {
            priceEntryRepo.save(new GroupPriceEntry(catalogItemId, marketplace, price));
            log.debug("Saved price entry catalogItem id={} price={}", catalogItemId, price);
        } else {
            log.debug("Skipped duplicate entry catalogItem id={} — already recorded today", catalogItemId);
        }
    }

    private String buildMessage(TrackedGroup group, Long newMin, Long oldMin) {
        var sb = new StringBuilder();
        sb.append("<b>📊 PriceRadar — мінімальна ціна змінилась!</b>\n\n");
        if (oldMin != null) sb.append("Попередня мін. ціна: ").append(oldMin).append(" грн\n");
        sb.append("Нова мін. ціна: <b>").append(newMin).append(" грн</b>\n\n");
        sb.append("Товари у групі:\n");
        for (TrackedItem item : group.getItems()) {
            CatalogItem ci = item.getCatalogItem();
            sb.append("• <b>[").append(ci.getMarketplace()).append("]</b> ")
              .append(ci.getTitle())
              .append(" — ").append(ci.getCurrentPrice() != null ? ci.getCurrentPrice() + " грн" : "N/A")
              .append("\n  <a href=\"").append(ci.getUrl()).append("\">Переглянути</a>\n");
        }
        return sb.toString();
    }
}
