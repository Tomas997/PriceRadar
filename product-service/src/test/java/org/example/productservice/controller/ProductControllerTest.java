package org.example.productservice.controller;

import org.example.productservice.exception.GlobalExceptionHandler;
import org.example.productservice.exception.ProductNotFoundException;
import org.example.productservice.model.PriceEntry;
import org.example.productservice.model.Product;
import org.example.productservice.service.ProductSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductSearchService productSearchService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productSearchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Product sampleProduct() {
        Product p = new Product("iPhone 15", "Rozetka", "https://rozetka.com/1", true);
        p.setId(1L);
        return p;
    }

    @Test
    void getAll_returnsListOfProducts() throws Exception {
        when(productSearchService.getAllTrackedProducts()).thenReturn(List.of(sampleProduct()));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("iPhone 15"))
                .andExpect(jsonPath("$[0].marketplace").value("Rozetka"));
    }

    @Test
    void getAll_returnsEmptyList() throws Exception {
        when(productSearchService.getAllTrackedProducts()).thenReturn(List.of());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void search_returnsMatchingProducts() throws Exception {
        when(productSearchService.searchByTitle("iphone")).thenReturn(List.of(sampleProduct()));

        mockMvc.perform(get("/api/products/search").param("query", "iphone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("iPhone 15"));
    }

    @Test
    void getById_returnsProduct_whenExists() throws Exception {
        when(productSearchService.getProductById(1L)).thenReturn(sampleProduct());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.marketplace").value("Rozetka"))
                .andExpect(jsonPath("$.inStock").value(true));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(productSearchService.getProductById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void track_createsProduct_andReturns201() throws Exception {
        when(productSearchService.trackProduct(any())).thenReturn(sampleProduct());
        String body = """
                {"marketplace":"Rozetka","title":"iPhone 15","price":40000,"url":"https://rozetka.com/1","inStock":true}
                """;

        mockMvc.perform(post("/api/products/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("iPhone 15"));
    }

    @Test
    void track_returns400_whenBlankMarketplace() throws Exception {
        String body = """
                {"marketplace":"","title":"iPhone 15","price":40000,"url":"https://rozetka.com","inStock":true}
                """;

        mockMvc.perform(post("/api/products/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void track_returns400_whenBlankTitle() throws Exception {
        String body = """
                {"marketplace":"Rozetka","title":"","price":40000,"url":"https://rozetka.com","inStock":true}
                """;

        mockMvc.perform(post("/api/products/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void track_returns400_whenNegativePrice() throws Exception {
        String body = """
                {"marketplace":"Rozetka","title":"iPhone 15","price":-1,"url":"https://rozetka.com","inStock":true}
                """;

        mockMvc.perform(post("/api/products/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPriceHistory_returnsList() throws Exception {
        PriceEntry entry = new PriceEntry(1L, 40000L);
        entry.setId(10L);
        when(productSearchService.getPriceHistory(1L)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/products/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].price").value(40000));
    }

    @Test
    void getPriceHistory_returns404_whenProductNotFound() throws Exception {
        when(productSearchService.getPriceHistory(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99/history"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204_whenExists() throws Exception {
        doNothing().when(productSearchService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productSearchService).deleteProduct(1L);
    }

    @Test
    void delete_returns404_whenNotFound() throws Exception {
        doThrow(new ProductNotFoundException(99L)).when(productSearchService).deleteProduct(99L);

        mockMvc.perform(delete("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}