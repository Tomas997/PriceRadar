package com.example.parserservice.controller;

import com.example.parserservice.dto.MarketplaceSearchResult;
import com.example.parserservice.service.ParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class ParserController {

    private final ParserService parserService;

    @GetMapping
    public List<MarketplaceSearchResult> search(@RequestParam String query) {
        return parserService.searchAll(query);
    }
}
