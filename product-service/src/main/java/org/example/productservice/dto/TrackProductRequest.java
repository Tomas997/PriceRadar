package org.example.productservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TrackProductRequest(
        @NotBlank String marketplace,
        @NotBlank String title,
        @Min(0) long price,
        @NotBlank String url,
        boolean inStock,
        String userEmail
) {}