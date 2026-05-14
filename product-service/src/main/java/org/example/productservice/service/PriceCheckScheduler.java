package org.example.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.dto.MarketplaceSearchResult;
import org.example.productservice.model.GroupPriceEntry;
import org.example.productservice.model.TrackedGroup;
import org.example.productservice.model.TrackedItem;
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
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceCheckScheduler {

    private final TrackedGroupRepository groupRepo;
    private final GroupPriceEntryRepository priceEntryRepo;
    private final TelegramNotificationService telegramService;

    @Value("${parser.service.url:http://localhost:8082}")
    private String parserUrl;

    private final RestClient restClient = RestClient.create();

    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void checkPrices() {
        log.info("Daily price check started");
        for (TrackedGroup group : groupRepo.findAll()) {
            try {
                processGroup(group);
            } catch (Exception e) {
                log.error("Error processing group id={}: {}", group.getId(), e.getMessage());
            }
        }
        log.info("Daily price check completed");
    }

    @Transactional
    public void checkGroup(Long groupId) {
        groupRepo.findById(groupId).ifPresent(group -> {
            try {
                processGroup(group);
            } catch (Exception e) {
                log.error("Error processing group id={}: {}", groupId, e.getMessage());
            }
        });
    }

    void processGroup(TrackedGroup group) {
        Long newMin = null;
        for (TrackedItem item : group.getItems()) {
            Long fresh = fetchPrice(item);
            if (fresh != null) {
                item.setCurrentPrice(fresh);
                saveIfNeeded(item.getId(), item.getMarketplace(), fresh);
            }
            Long price = item.getCurrentPrice();
            if (price != null && (newMin == null || price < newMin)) newMin = price;
        }
        if (newMin == null) return;

        Long oldMin = group.getLastMinPrice();
        if (!newMin.equals(oldMin)) {
            group.setLastMinPrice(newMin);
            groupRepo.save(group);
            telegramService.send(group.getTelegramChatId(), buildMessage(group, newMin, oldMin));
            log.info("Group id={} min price changed {} -> {}", group.getId(), oldMin, newMin);
        }
    }

    private Long fetchPrice(TrackedItem item) {
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
                    .map(p -> Long.valueOf(p.price()))
                    .findFirst()
                    .orElse(null);
            if (price != null) {
                log.debug("fetchPrice item id={} [{}] → {} грн", item.getId(), item.getMarketplace(), price);
            } else {
                log.warn("fetchPrice item id={} [{}]: no URL match for '{}'", item.getId(), item.getMarketplace(), item.getUrl());
            }
            return price;
        } catch (Exception e) {
            log.warn("fetchPrice item id={} [{}]: {}", item.getId(), item.getMarketplace(), e.getMessage());
            return null;
        }
    }

    private void saveIfNeeded(Long itemId, String marketplace, Long price) {
        Optional<GroupPriceEntry> last = priceEntryRepo.findTopByTrackedItemIdOrderByRecordedAtDesc(itemId);
        boolean noEntryToday = last.isEmpty()
                || !last.get().getRecordedAt().toLocalDate().equals(LocalDate.now());
        boolean priceChanged = last.isEmpty() || !last.get().getPrice().equals(price);
        if (noEntryToday || priceChanged) {
            priceEntryRepo.save(new GroupPriceEntry(itemId, marketplace, price));
            log.debug("Saved price entry item id={} price={} (noEntryToday={}, priceChanged={})",
                    itemId, price, noEntryToday, priceChanged);
        } else {
            log.debug("Skipped duplicate entry item id={} price={} — already recorded today", itemId, price);
        }
    }

    private String buildMessage(TrackedGroup group, Long newMin, Long oldMin) {
        var sb = new StringBuilder();
        sb.append("<b>📊 PriceRadar — мінімальна ціна змінилась!</b>\n\n");
        if (oldMin != null) sb.append("Попередня мін. ціна: ").append(oldMin).append(" грн\n");
        sb.append("Нова мін. ціна: <b>").append(newMin).append(" грн</b>\n\n");
        sb.append("Товари у групі:\n");
        for (TrackedItem item : group.getItems()) {
            sb.append("• <b>[").append(item.getMarketplace()).append("]</b> ")
              .append(item.getTitle())
              .append(" — ").append(item.getCurrentPrice() != null ? item.getCurrentPrice() + " грн" : "N/A")
              .append("\n  <a href=\"").append(item.getUrl()).append("\">Переглянути</a>\n");
        }
        return sb.toString();
    }
}
