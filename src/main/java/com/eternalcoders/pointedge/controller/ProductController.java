package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.entity.Product;
import com.eternalcoders.pointedge.service.ImageService;
import com.eternalcoders.pointedge.service.ProductService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;
    private final ImageService imageService;

    public ProductController(ProductService productService, ImageService imageService) {
        this.productService = productService;
        this.imageService = imageService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getFilteredProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean hidden
    ) {
        return ResponseEntity.ok(productService.getFilteredProducts(brandId, categoryId, hidden));
    }

    @PostMapping
    public ResponseEntity<Product> addProduct(@RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(product));
    }

    @PutMapping
    public ResponseEntity<Product> updateProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(product));
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getProductImage(@PathVariable String filename) {
        try {
            Resource image = imageService.getProductImage(filename);
            if (image != null) {
                String contentType = Files.probeContentType(Paths.get(image.getFile().getAbsolutePath()));
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFilename() + "\"")
                        .body(image);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{productId}/image")
    public ResponseEntity<Void> uploadProductImage(@PathVariable Long productId, @RequestParam("file") MultipartFile file) {
        try {
            boolean isSaved = imageService.saveProductImage(productId, file);
            if (isSaved) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{productId}/image")
    public ResponseEntity<Void> deleteProductImage(@PathVariable Long productId) {
        try {
            boolean isDeleted = imageService.deleteProductImage(productId);
            if (isDeleted) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
