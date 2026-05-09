package org.example.productservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tracked_items")
@Data
@NoArgsConstructor
public class TrackedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private TrackedGroup group;

    private String marketplace;
    private String title;
    private String url;
    private Long currentPrice;
}
