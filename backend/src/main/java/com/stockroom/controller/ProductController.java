package com.stockroom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockroom.dto.ProductRequest;
import com.stockroom.model.Product;
import com.stockroom.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ObjectMapper   objectMapper;

    public ProductController(ProductService productService, ObjectMapper objectMapper) {
        this.productService = productService;
        this.objectMapper   = objectMapper;
    }

    /**
     * GET /api/products?search=&category=&page=0
     * Public — no auth needed to browse.
     * Page size comes from PAGINATION_DEFAULT_SIZE (ConfigMap).
     */
    @GetMapping
    public Page<Product> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long   category,
            @RequestParam(defaultValue = "0") int  page) {
        return productService.search(search, category, page);
    }

    /** GET /api/products/{id} — public */
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/products — ADMIN only.
     * Accepts multipart/form-data with:
     *   product: JSON string (ProductRequest)
     *   image:   optional image file
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(
            @RequestPart("product")          String         productJson,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            ProductRequest req = objectMapper.readValue(productJson, ProductRequest.class);
            Product product = productService.create(req, image);
            return ResponseEntity.status(201).body(product);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "File upload failed: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/products/{id} — ADMIN only.
     * Same multipart format as POST.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable                    Long           id,
            @RequestPart("product")          String         productJson,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            ProductRequest req     = objectMapper.readValue(productJson, ProductRequest.class);
            Product        product = productService.update(id, req, image);
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "File upload failed: " + e.getMessage()));
        }
    }

    /**
     * PATCH /api/products/{id}/stock — ADMIN only.
     * Updates stock quantity AND broadcasts a WebSocket event to all
     * connected clients on /topic/stock-updates.
     *
     * Body: { "quantity": 42 }
     */
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStock(
            @PathVariable                     Long        id,
            @RequestBody                      Map<String, Integer> body,
            @AuthenticationPrincipal          UserDetails userDetails) {

        Integer qty = body.get("quantity");
        if (qty == null || qty < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "quantity must be a non-negative integer"));
        }
        try {
            String username = userDetails != null ? userDetails.getUsername() : "system";
            Product product = productService.updateStock(id, qty, username);
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** DELETE /api/products/{id} — ADMIN only */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            productService.delete(id);
            return ResponseEntity.ok(Map.of("deleted", id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** GET /api/products/{id}/image — public, serves the uploaded image file */
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        try {
            byte[] bytes       = productService.getImage(id);
            String contentType = productService.getImageContentType(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
