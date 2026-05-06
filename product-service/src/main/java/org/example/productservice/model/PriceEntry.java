package org.example.productservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "price_entries")
@Data
@NoArgsConstructor
public class PriceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private Long price;
    private LocalDateTime parsedAt;

    public PriceEntry(Long productId, Long price) {
        this.productId = productId;
        this.price = price;
        this.parsedAt = LocalDateTime.now();
    }
}