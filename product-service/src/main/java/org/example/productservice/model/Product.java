package org.example.productservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String marketplace;
    private String url;
    private Boolean inStock;

    public Product(String title, String marketplace, String url, Boolean inStock) {
        this.title = title;
        this.marketplace = marketplace;
        this.url = url;
        this.inStock = inStock;
    }
}