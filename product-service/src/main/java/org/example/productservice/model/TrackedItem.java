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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "catalog_item_id")
    private CatalogItem catalogItem;
}