package org.example.productservice.controller;

import org.example.productservice.exception.GlobalExceptionHandler;
import org.example.productservice.model.Product;
import org.example.productservice.service.ProductSearchService;
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
class ProductControllerTest {

    @Mock private ProductSearchService productSearchService;

    private MockMvc mockMvc;

    private static final String USER_EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productSearchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAll_returns401_whenNoAuthHeader() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void getAll_returns401_whenAuthHeaderIsBlank() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("X-User-Email", "   "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void getAll_returnsUserProducts_whenAuthHeaderPresent() throws Exception {
        when(productSearchService.getProductsByUser(USER_EMAIL)).thenReturn(List.of());

        mockMvc.perform(get("/api/products")
                        .header("X-User-Email", USER_EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
