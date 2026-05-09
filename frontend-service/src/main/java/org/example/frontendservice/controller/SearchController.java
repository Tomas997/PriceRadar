package org.example.frontendservice.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.frontendservice.client.GatewayClient;
import org.example.frontendservice.dto.MarketplaceSearchResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final GatewayClient gatewayClient;

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String query,
                         HttpSession session, Model model) {
        if (session.getAttribute("token") == null) return "redirect:/login";

        model.addAttribute("query", query);
        model.addAttribute("username", session.getAttribute("username"));

        List<MarketplaceSearchResult> results = Collections.emptyList();
        if (query != null && !query.isBlank()) {
            results = gatewayClient.search(query);
        }
        model.addAttribute("results", results);
        return "search";
    }
}
