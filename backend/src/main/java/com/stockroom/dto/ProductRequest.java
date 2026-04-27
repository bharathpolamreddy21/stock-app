package com.stockroom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    private String sku;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity = 0;

    private Long categoryId;

    // ── Getters / Setters ────────────────────────────────────────────────────
    public String     getName()          { return name; }
    public String     getDescription()   { return description; }
    public String     getSku()           { return sku; }
    public BigDecimal getPrice()         { return price; }
    public Integer    getStockQuantity() { return stockQuantity; }
    public Long       getCategoryId()    { return categoryId; }

    public void setName(String name)               { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setSku(String sku)                 { this.sku = sku; }
    public void setPrice(BigDecimal price)         { this.price = price; }
    public void setStockQuantity(Integer qty)      { this.stockQuantity = qty; }
    public void setCategoryId(Long categoryId)     { this.categoryId = categoryId; }
}
