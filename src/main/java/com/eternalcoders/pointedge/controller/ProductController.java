package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.ProductOrderQuantityDTO;
import com.eternalcoders.pointedge.entity.Product;
import com.eternalcoders.pointedge.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PostMapping
    public ResponseEntity<Product> addProduct(@RequestBody Product product) {
        return ResponseEntity.status(201).body(productService.addProduct(product));
    }

    @GetMapping("/sales-quantities")
    public ResponseEntity<List<ProductOrderQuantityDTO>> getFilteredProductOrderQuantities(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String timeFilter
    ) {
        return ResponseEntity.ok(productService.getFilteredProductOrderQuantities(brandId, categoryId, timeFilter));
    }
}
