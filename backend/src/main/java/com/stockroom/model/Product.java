package com.stockroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(unique = true)
    private String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    /** Stored filename only — served via GET /api/products/{id}/image */
    private String imagePath;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(String name, String description, String sku,
                   BigDecimal price, int stockQuantity, Category category) {
        this.name          = name;
        this.description   = description;
        this.sku           = sku;
        this.price         = price;
        this.stockQuantity = stockQuantity;
        this.category      = category;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public Long          getId()            { return id; }
    public String        getName()          { return name; }
    public String        getDescription()   { return description; }
    public String        getSku()           { return sku; }
    public BigDecimal    getPrice()         { return price; }
    public Integer       getStockQuantity() { return stockQuantity; }
    public Category      getCategory()      { return category; }
    public String        getImagePath()     { return imagePath; }
    public LocalDateTime getCreatedAt()     { return createdAt; }
    public LocalDateTime getUpdatedAt()     { return updatedAt; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setId(Long id)                       { this.id = id; }
    public void setName(String name)                 { this.name = name; }
    public void setDescription(String description)   { this.description = description; }
    public void setSku(String sku)                   { this.sku = sku; }
    public void setPrice(BigDecimal price)           { this.price = price; }
    public void setStockQuantity(Integer qty)        { this.stockQuantity = qty; }
    public void setCategory(Category category)       { this.category = category; }
    public void setImagePath(String imagePath)       { this.imagePath = imagePath; }
}
