package org.example.priceradar.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_entries")
public class PriceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private Long price;
    private LocalDateTime parsedAt;

    public PriceEntry() {}

    public PriceEntry(Long productId, Long price) {
        this.productId = productId;
        this.price = price;
        this.parsedAt = LocalDateTime.now();
    }

    // getters
    public Long getId()             { return id; }
    public Long getProductId()      { return productId; }
    public Long getPrice()          { return price; }
    public LocalDateTime getParsedAt() { return parsedAt; }
}