package org.example.frontendservice.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.frontendservice.client.GatewayClient;
import org.example.frontendservice.dto.UserResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final GatewayClient gatewayClient;

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        if (session.getAttribute("token") == null) return "redirect:/login";

        UserResponse user = gatewayClient.withAutoRefresh(session, gatewayClient::getMe);
        model.addAttribute("user", user);
        model.addAttribute("username", session.getAttribute("username"));
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam(required = false) String telegramChatId,
                                HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("token") == null) return "redirect:/login";

        UserResponse updated = gatewayClient.withAutoRefresh(session,
                token -> gatewayClient.updateProfile(token, telegramChatId));
        session.setAttribute("telegramChatId", updated.telegramChatId());
        ra.addFlashAttribute("success", "Profile updated");
        return "redirect:/profile";
    }
}
