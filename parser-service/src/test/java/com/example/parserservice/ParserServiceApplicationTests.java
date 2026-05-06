package com.example.parserservice;

import com.example.parserservice.dto.MarketplaceSearchResult;
import com.example.parserservice.marketplace.MarketplaceSearchParser;
import com.example.parserservice.model.ProductCandidate;
import com.example.parserservice.service.ParserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParserServiceApplicationTests {

    @Mock
    private MarketplaceSearchParser parserA;

    @Mock
    private MarketplaceSearchParser parserB;

    @Test
    void searchAll_returnsCombinedResults() {
        when(parserA.marketplaceName()).thenReturn("A");
        when(parserB.marketplaceName()).thenReturn("B");
        when(parserA.searchProducts("iphone")).thenReturn(List.of(
                new ProductCandidate("A", "iPhone 15", 40000L, "https://a.com/1", true)
        ));
        when(parserB.searchProducts("iphone")).thenReturn(List.of());

        ParserService service = new ParserService(List.of(parserA, parserB));
        List<MarketplaceSearchResult> results = service.searchAll("iphone");

        assertThat(results).hasSize(2);
        assertThat(results).extracting(MarketplaceSearchResult::marketplace)
                .containsExactlyInAnyOrder("A", "B");
        assertThat(results.stream()
                .filter(r -> r.marketplace().equals("A"))
                .findFirst().orElseThrow().products())
                .hasSize(1);
    }

    @Test
    void searchAll_failingParserReturnsEmptyList() {
        when(parserA.marketplaceName()).thenReturn("A");
        when(parserA.searchProducts(any())).thenThrow(new RuntimeException("timeout"));

        ParserService service = new ParserService(List.of(parserA));
        List<MarketplaceSearchResult> results = service.searchAll("test");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).products()).isEmpty();
    }

    @Test
    void searchAll_blankQueryThrowsException() {
        ParserService service = new ParserService(List.of(parserA));
        assertThatThrownBy(() -> service.searchAll("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
