package com.stockroom.dto;

import java.time.LocalDateTime;

/**
 * Broadcast payload sent to /topic/stock-updates whenever a product's
 * stock quantity changes via PATCH /api/products/{id}/stock.
 *
 * Every connected browser receives this in real time — no page refresh needed.
 */
public record StockUpdateEvent(
    Long          productId,
    String        productName,
    String        sku,
    int           oldQuantity,
    int           newQuantity,
    String        updatedBy,
    LocalDateTime timestamp
) {}
