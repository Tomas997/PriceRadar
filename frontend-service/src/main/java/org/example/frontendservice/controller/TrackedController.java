package org.example.frontendservice.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.frontendservice.client.GatewayClient;
import org.example.frontendservice.dto.GroupPriceHistoryResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TrackedController {

    private final GatewayClient gatewayClient;
    private final ObjectMapper objectMapper;

    @GetMapping("/tracked")
    public String tracked(HttpSession session, Model model) {
        String token = (String) session.getAttribute("token");
        if (token == null) return "redirect:/login";

        String userEmail = (String) session.getAttribute("userEmail");
        model.addAttribute("username", session.getAttribute("username"));
        try {
            model.addAttribute("groups", gatewayClient.getTrackedGroups(token, userEmail));
        } catch (Exception e) {
            log.error("Failed to load tracked groups: {}", e.getMessage());
            model.addAttribute("error", "Не вдалося завантажити групи. Спробуйте пізніше.");
            model.addAttribute("groups", java.util.Collections.emptyList());
        }
        try {
            var cfg = gatewayClient.getCheckConfig(token);
            model.addAttribute("showCheckButton", cfg.buttonVisible());
            model.addAttribute("checkHour", cfg.checkHour());
        } catch (Exception e) {
            log.warn("Failed to load check config: {}", e.getMessage());
            model.addAttribute("showCheckButton", true);
            model.addAttribute("checkHour", 10);
        }
        return "tracked";
    }

    @PostMapping("/tracked/group")
    public String trackGroup(HttpServletRequest request,
                             @RequestParam(required = false) String query,
                             HttpSession session, RedirectAttributes ra) {
        String token = (String) session.getAttribute("token");
        if (token == null) return "redirect:/login";

        String userEmail = (String) session.getAttribute("userEmail");
        String telegramChatId = (String) session.getAttribute("telegramChatId");

        List<Map<String, Object>> items = new ArrayList<>();
        request.getParameterMap().forEach((name, values) -> {
            if ("query".equals(name) || "_csrf".equals(name)) return;
            if (values.length > 0 && !values[0].isBlank()) {
                Map<String, Object> item = parseItem(values[0]);
                if (item != null) items.add(item);
            }
        });

        if (items.isEmpty()) {
            ra.addFlashAttribute("error", "Виберіть хоча б один товар");
            return "redirect:/search" + (query != null && !query.isBlank() ? "?query=" + query : "");
        }

        try {
            gatewayClient.createTrackedGroup(token, userEmail, telegramChatId, items);
            ra.addFlashAttribute("success", "Групу товарів додано до відстеження!");
        } catch (RestClientResponseException e) {
            log.warn("Create group failed: {}", e.getMessage());
            ra.addFlashAttribute("error", "Не вдалося додати групу");
        }
        return "redirect:/tracked";
    }

    @PostMapping("/tracked/delete-group")
    public String deleteGroup(@RequestParam Long groupId, HttpSession session, RedirectAttributes ra) {
        String token = (String) session.getAttribute("token");
        if (token == null) return "redirect:/login";

        try {
            gatewayClient.deleteTrackedGroup(token, groupId);
            ra.addFlashAttribute("success", "Групу видалено");
        } catch (RestClientResponseException e) {
            ra.addFlashAttribute("error", "Не вдалося видалити групу");
        }
        return "redirect:/tracked";
    }

    @PostMapping("/tracked/test-notify/{groupId}")
    public String testNotify(@PathVariable Long groupId, HttpSession session, RedirectAttributes ra) {
        String token = (String) session.getAttribute("token");
        if (token == null) return "redirect:/login";

        String telegramChatId = (String) session.getAttribute("telegramChatId");
        if (telegramChatId == null || telegramChatId.isBlank()) {
            ra.addFlashAttribute("error", "Вкажіть Telegram Chat ID у профілі");
            return "redirect:/tracked";
        }

        try {
            gatewayClient.testNotify(token, groupId, telegramChatId);
            ra.addFlashAttribute("success", "Тестове сповіщення надіслано у Telegram");
        } catch (RestClientResponseException e) {
            log.warn("Test notify failed: {}", e.getMessage());
            ra.addFlashAttribute("error", "Не вдалося надіслати сповіщення. Перевірте Chat ID у профілі");
        }
        return "redirect:/tracked";
    }

    @PostMapping("/tracked/trigger-check")
    public String triggerCheck(HttpSession session, RedirectAttributes ra) {
        String token = (String) session.getAttribute("token");
        if (token == null) return "redirect:/login";

        try {
            gatewayClient.triggerCheck(token);
            ra.addFlashAttribute("success", "Перевірку цін запущено. Сповіщення надійде якщо ціна змінилась");
        } catch (RestClientResponseException e) {
            ra.addFlashAttribute("error", "Помилка при запуску перевірки");
        }
        return "redirect:/tracked";
    }

    @PostMapping("/tracked/check-group/{groupId}")
    public String checkGroupPrices(@PathVariable Long groupId, HttpSession session, RedirectAttributes ra) {
        String token = (String) session.getAttribute("token");
        if (token == null) return "redirect:/login";

        try {
            gatewayClient.checkGroupPrices(token, groupId);
            ra.addFlashAttribute("success", "Ціни перевірено. Сповіщення надійде якщо мінімальна ціна змінилась");
        } catch (RestClientResponseException e) {
            log.warn("Check group prices failed: {}", e.getMessage());
            ra.addFlashAttribute("error", "Помилка при перевірці цін");
        }
        return "redirect:/tracked";
    }

    @PostMapping("/trends/{groupId}/seed")
    public String seedHistory(@PathVariable Long groupId, HttpSession session, RedirectAttributes ra) {
        String token = (String) session.getAttribute("token");
        if (token == null) return "redirect:/login";
        try {
            gatewayClient.seedDemoHistory(token, groupId);
            ra.addFlashAttribute("success", "Тестові дані за 7 днів згенеровано");
        } catch (Exception e) {
            log.error("Seed history failed for group {}: {}", groupId, e.getMessage());
            ra.addFlashAttribute("error", "Не вдалося згенерувати дані");
        }
        return "redirect:/trends/" + groupId;
    }

    @PostMapping("/trends/{groupId}/clear-demo")
    public String clearDemoHistory(@PathVariable Long groupId, HttpSession session, RedirectAttributes ra) {
        String token = (String) session.getAttribute("token");
        if (token == null) return "redirect:/login";
        try {
            gatewayClient.clearDemoHistory(token, groupId);
            ra.addFlashAttribute("success", "Тестові дані видалено");
        } catch (Exception e) {
            log.error("Clear demo history failed for group {}: {}", groupId, e.getMessage());
            ra.addFlashAttribute("error", "Не вдалося видалити тестові дані");
        }
        return "redirect:/trends/" + groupId;
    }

    @GetMapping("/trends/{groupId}")
    public String trends(@PathVariable Long groupId, HttpSession session, Model model) {
        String token = (String) session.getAttribute("token");
        if (token == null) return "redirect:/login";

        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("groupId", groupId);

        try {
            GroupPriceHistoryResponse history = gatewayClient.getGroupHistory(token, groupId);
            model.addAttribute("series", history.series());
            model.addAttribute("historyJson", objectMapper.writeValueAsString(history.series()));
            boolean fewPoints = history.series().stream()
                    .mapToInt(s -> s.entries().size())
                    .max().orElse(0) < 2;
            model.addAttribute("fewPoints", fewPoints);
            model.addAttribute("hasDemo", history.hasDemo());
        } catch (JacksonException e) {
            log.error("JSON serialization error for group {}: {}", groupId, e.getMessage());
            model.addAttribute("series", java.util.Collections.emptyList());
            model.addAttribute("historyJson", "[]");
            model.addAttribute("fewPoints", false);
            model.addAttribute("hasDemo", false);
        } catch (Exception e) {
            log.error("Failed to load history for group {}: {}", groupId, e.getMessage());
            model.addAttribute("error", "Не вдалося завантажити дані для графіку");
            model.addAttribute("series", java.util.Collections.emptyList());
            model.addAttribute("historyJson", "[]");
            model.addAttribute("fewPoints", false);
            model.addAttribute("hasDemo", false);
        }
        return "trends";
    }

    private Map<String, Object> parseItem(String encoded) {
        String[] parts = encoded.split("\\|\\|", 5);
        if (parts.length < 4) return null;
        try {
            return Map.of(
                    "marketplace", parts[0],
                    "title", parts[1],
                    "price", Long.parseLong(parts[2]),
                    "url", parts[3]
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
