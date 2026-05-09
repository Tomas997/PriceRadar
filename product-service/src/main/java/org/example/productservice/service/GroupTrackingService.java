package org.example.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.dto.CreateTrackedGroupRequest;
import org.example.productservice.dto.TrackedGroupResponse;
import org.example.productservice.model.TrackedGroup;
import org.example.productservice.model.TrackedItem;
import org.example.productservice.repository.TrackedGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupTrackingService {

    private final TrackedGroupRepository groupRepo;
    private final TelegramNotificationService telegramService;

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
