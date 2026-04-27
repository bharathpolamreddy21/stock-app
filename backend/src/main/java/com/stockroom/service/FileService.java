package com.stockroom.service;

import com.stockroom.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

/**
 * Handles multipart image uploads for products.
 *
 * Files are stored at UPLOAD_PATH (from ConfigMap).
 * In K8s this path should be backed by a PersistentVolumeClaim so files
 * survive pod restarts — a key student exercise in Phase 3.
 *
 * K8s teaching point:
 *   Without a PVC, uploaded images vanish when the pod restarts.
 *   Students experience this firsthand: upload an image → kubectl delete pod
 *   → image is gone. Then add the PVC manifest and the problem is solved.
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final AppProperties props;

    public FileService(AppProperties props) {
        this.props = props;
    }

    /**
     * Saves the uploaded file to UPLOAD_PATH.
     * Returns the stored filename (to be persisted in the product row).
     */
    public String store(MultipartFile file, Long productId) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, WebP and GIF images are allowed");
        }

        String extension = getExtension(file.getOriginalFilename());
        String filename  = "product-" + productId + "-" + UUID.randomUUID() + extension;

        Path uploadDir = Path.of(props.getUpload().getPath());
        Files.createDirectories(uploadDir);
        Files.copy(file.getInputStream(), uploadDir.resolve(filename),
                   StandardCopyOption.REPLACE_EXISTING);

        log.debug("Stored image {} for product {}", filename, productId);
        return filename;
    }

    /**
     * Returns the stored file as bytes for serving via GET /api/products/{id}/image.
     */
    public byte[] load(String filename) throws IOException {
        Path filePath = Path.of(props.getUpload().getPath()).resolve(filename);
        if (!Files.exists(filePath)) {
            throw new NoSuchFileException("Image not found: " + filename);
        }
        return Files.readAllBytes(filePath);
    }

    /** Deletes the old image file when a product image is replaced or product deleted */
    public void delete(String filename) {
        if (filename == null) return;
        try {
            Files.deleteIfExists(Path.of(props.getUpload().getPath()).resolve(filename));
        } catch (IOException e) {
            log.warn("Could not delete image {}: {}", filename, e.getMessage());
        }
    }

    public String contentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif"))  return "image/gif";
        return "image/jpeg";
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot) : ".jpg";
    }
}
