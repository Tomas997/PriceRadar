package org.example.frontendservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.frontendservice.client.GatewayClient;
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
