package org.example.productservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_price_entries")
@Data
@NoArgsConstructor
public class GroupPriceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long trackedItemId;
    private String marketplace;
    private Long price;
    private LocalDateTime recordedAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean demo;

    public GroupPriceEntry(Long trackedItemId, String marketplace, Long price) {
        this.trackedItemId = trackedItemId;
        this.marketplace = marketplace;
        this.price = price;
        this.recordedAt = LocalDateTime.now();
        this.demo = false;
    }
}