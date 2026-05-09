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
    private String userEmail;
    private Long latestPrice;

    public Product(String title, String marketplace, String url, Boolean inStock) {
        this(title, marketplace, url, inStock, null, null);
    }

    public Product(String title, String marketplace, String url, Boolean inStock, String userEmail, Long latestPrice) {
        this.title = title;
        this.marketplace = marketplace;
        this.url = url;
        this.inStock = inStock;
        this.userEmail = userEmail;
        this.latestPrice = latestPrice;
    }
}