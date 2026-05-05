package org.example.priceradar.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String marketplace;
    private String url;
    private Boolean inStock;

    public Product() {}

    public Product(String title, String marketplace, String url, Boolean inStock) {
        this.title = title;
        this.marketplace = marketplace;
        this.url = url;
        this.inStock = inStock;
    }

    // getters
    public Long getId()          { return id; }
    public String getTitle()     { return title; }
    public String getMarketplace() { return marketplace; }
    public String getUrl()       { return url; }
    public Boolean getInStock()  { return inStock; }
}