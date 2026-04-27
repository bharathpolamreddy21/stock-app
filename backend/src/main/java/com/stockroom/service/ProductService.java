package com.stockroom.service;

import com.stockroom.config.AppProperties;
import com.stockroom.dto.ProductRequest;
import com.stockroom.dto.StockUpdateEvent;
import com.stockroom.model.Category;
import com.stockroom.model.Product;
import com.stockroom.repository.CategoryRepository;
import com.stockroom.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@Transactional
public class ProductService {

    private final ProductRepository    productRepository;
    private final CategoryRepository   categoryRepository;
    private final FileService          fileService;
    private final SimpMessagingTemplate broker;
    private final AppProperties        props;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          FileService fileService,
                          SimpMessagingTemplate broker,
                          AppProperties props) {
        this.productRepository  = productRepository;
        this.categoryRepository = categoryRepository;
        this.fileService        = fileService;
        this.broker             = broker;
        this.props              = props;
    }

    // ── List with search + pagination ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<Product> search(String search, Long categoryId, int page) {
        int size = props.getPagination().getDefaultSize();  // from ConfigMap
        String searchParam = (search == null || search.isBlank()) ? null : search.trim();
        return productRepository.search(searchParam, categoryId, PageRequest.of(page, size));
    }

    // ── Get single ────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    // ── Create with optional image ────────────────────────────────────────────
    public Product create(ProductRequest req, MultipartFile image) throws IOException {
        Product product = new Product();
        applyRequest(product, req);
        product = productRepository.save(product);

        if (image != null && !image.isEmpty()) {
            String filename = fileService.store(image, product.getId());
            product.setImagePath(filename);
            product = productRepository.save(product);
        }
        return product;
    }

    // ── Update ────────────────────────────────────────────────────────────────
    public Product update(Long id, ProductRequest req, MultipartFile image) throws IOException {
        Product product = getById(id);
        applyRequest(product, req);

        if (image != null && !image.isEmpty()) {
            fileService.delete(product.getImagePath());
            String filename = fileService.store(image, product.getId());
            product.setImagePath(filename);
        }
        return productRepository.save(product);
    }

    // ── Stock update — also broadcasts WebSocket event ────────────────────────
    public Product updateStock(Long id, int newQuantity, String updatedBy) {
        Product product  = getById(id);
        int     oldQty   = product.getStockQuantity();
        product.setStockQuantity(newQuantity);
        product = productRepository.save(product);

        // Broadcast to all WebSocket subscribers
        StockUpdateEvent event = new StockUpdateEvent(
                product.getId(),
                product.getName(),
                product.getSku(),
                oldQty,
                newQuantity,
                updatedBy,
                LocalDateTime.now()
        );
        broker.convertAndSend("/topic/stock-updates", event);
        return product;
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    public void delete(Long id) {
        Product product = getById(id);
        fileService.delete(product.getImagePath());
        productRepository.delete(product);
    }

    // ── Get image bytes ───────────────────────────────────────────────────────
    public byte[] getImage(Long id) throws IOException {
        Product product = getById(id);
        if (product.getImagePath() == null) {
            throw new RuntimeException("Product has no image");
        }
        return fileService.load(product.getImagePath());
    }

    public String getImageContentType(Long id) {
        return fileService.contentType(getById(id).getImagePath());
    }

    // ── Shared helper ─────────────────────────────────────────────────────────
    private void applyRequest(Product product, ProductRequest req) {
        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setSku(req.getSku());
        product.setPrice(req.getPrice());
        product.setStockQuantity(req.getStockQuantity() != null ? req.getStockQuantity() : 0);

        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + req.getCategoryId()));
            product.setCategory(category);
        }
    }
}
