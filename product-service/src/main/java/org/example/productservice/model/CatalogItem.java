package org.example.productservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_items")
@Data
@NoArgsConstructor
public class CatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String marketplace;
    private String title;

    @Column(unique = true, nullable = false)
    private String url;

    private Long currentPrice;

    private LocalDateTime lastParsedAt;

    public CatalogItem(String marketplace, String title, String url, Long currentPrice) {
        this.marketplace = marketplace;
        this.title = title;
        this.url = url;
        this.currentPrice = currentPrice;
    }
}