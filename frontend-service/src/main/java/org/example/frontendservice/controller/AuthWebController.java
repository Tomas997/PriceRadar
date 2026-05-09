package org.example.frontendservice.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.frontendservice.client.GatewayClient;
import org.example.frontendservice.dto.AuthResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private final GatewayClient gatewayClient;

    @GetMapping("/")
    public String index(HttpSession session) {
        return session.getAttribute("token") != null ? "redirect:/search" : "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        return session.getAttribute("token") != null ? "redirect:/search" : "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        HttpSession session, RedirectAttributes ra) {
        try {
            AuthResponse resp = gatewayClient.login(email, password);
            session.setAttribute("token", resp.token());
            session.setAttribute("userEmail", resp.email());
            session.setAttribute("username", resp.username());
            session.setAttribute("telegramChatId", resp.telegramChatId());
            return "redirect:/search";
        } catch (RestClientResponseException e) {
            ra.addFlashAttribute("error", "Invalid email or password");
            return "redirect:/login";
        } catch (ResourceAccessException e) {
            log.error("Gateway unreachable: {}", e.getMessage());
            ra.addFlashAttribute("error", "Service unavailable, please try again later");
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        return session.getAttribute("token") != null ? "redirect:/search" : "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam(required = false) String telegramChatId,
                           HttpSession session, RedirectAttributes ra) {
        try {
            AuthResponse resp = gatewayClient.register(username, email, password, telegramChatId);
            session.setAttribute("token", resp.token());
            session.setAttribute("userEmail", resp.email());
            session.setAttribute("username", resp.username());
            session.setAttribute("telegramChatId", resp.telegramChatId());
            return "redirect:/search";
        } catch (RestClientResponseException e) {
            ra.addFlashAttribute("error", "Registration failed: " + extractError(e));
            return "redirect:/register";
        } catch (ResourceAccessException e) {
            log.error("Gateway unreachable: {}", e.getMessage());
            ra.addFlashAttribute("error", "Service unavailable, please try again later");
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private String extractError(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        log.error("Auth error: status={} body={}", e.getStatusCode(), body);
        if (body.contains("already exists")) return "email already registered";
        return "server error " + e.getStatusCode().value() + ": " + body;
    }
}
