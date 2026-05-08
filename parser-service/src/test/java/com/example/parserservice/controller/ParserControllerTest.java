package com.example.parserservice.controller;

import com.example.parserservice.dto.MarketplaceSearchResult;
import com.example.parserservice.exception.GlobalExceptionHandler;
import com.example.parserservice.model.ProductCandidate;
import com.example.parserservice.service.ParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ParserControllerTest {

    @Mock
    private ParserService parserService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ParserController(parserService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void search_returnsResults() throws Exception {
        List<ProductCandidate> products = List.of(
                new ProductCandidate("Citrus", "iPhone 15", 42000L, "https://citrus.ua/1", true)
        );
        when(parserService.searchAll("iphone")).thenReturn(List.of(
                new MarketplaceSearchResult("Citrus", products)
        ));

        mockMvc.perform(get("/api/search").param("query", "iphone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].marketplace").value("Citrus"))
                .andExpect(jsonPath("$[0].products[0].title").value("iPhone 15"))
                .andExpect(jsonPath("$[0].products[0].price").value(42000));
    }

    @Test
    void search_returnsResultsFromMultipleMarketplaces() throws Exception {
        List<MarketplaceSearchResult> results = List.of(
                new MarketplaceSearchResult("Citrus", List.of(
                        new ProductCandidate("Citrus", "Samsung S24", 35000L, "https://citrus.ua/1", true)
                )),
                new MarketplaceSearchResult("Rozetka", List.of(
                        new ProductCandidate("Rozetka", "Samsung S24", 34000L, "https://rozetka.com/1", true)
                ))
        );
        when(parserService.searchAll("samsung")).thenReturn(results);

        mockMvc.perform(get("/api/search").param("query", "samsung"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].marketplace").value("Citrus"))
                .andExpect(jsonPath("$[1].marketplace").value("Rozetka"));
    }

    @Test
    void search_returnsEmptyResults_whenNothingFound() throws Exception {
        when(parserService.searchAll("xyz")).thenReturn(List.of(
                new MarketplaceSearchResult("Citrus", List.of()),
                new MarketplaceSearchResult("Rozetka", List.of())
        ));

        mockMvc.perform(get("/api/search").param("query", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].products").isEmpty());
    }

    @Test
    void search_returns400_onBlankQuery() throws Exception {
        when(parserService.searchAll("  ")).thenThrow(new IllegalArgumentException("Query must not be blank"));

        mockMvc.perform(get("/api/search").param("query", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Query must not be blank"));
    }

}
