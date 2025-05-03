package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.entity.Brand;
import com.eternalcoders.pointedge.entity.Category;
import com.eternalcoders.pointedge.entity.Product;
import com.eternalcoders.pointedge.repository.BrandRepository;
import com.eternalcoders.pointedge.repository.CategoryRepository;
import com.eternalcoders.pointedge.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, BrandRepository brandRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<Product> getFilteredProducts(Long brandId, Long categoryId, Boolean hidden, String search, Pageable pageable) {
        return productRepository.findFilteredProducts(brandId, categoryId, hidden, search, pageable);
    }

    public Product getProductByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode).orElse(null);
        if (product != null && !product.isHidden())
            return product;
        return null;
    }

    public Product addProduct(Product product) {
        product.setId(null);

        persistNewBrandAndCategory(product);

        return productRepository.save(product);
    }

    public Product updateProduct(Product product) {
        productRepository.findById(product.getId()).ifPresent(existingProduct -> product.setImageName(existingProduct.getImageName()));

        persistNewBrandAndCategory(product);

        return productRepository.save(product);
    }

    private void persistNewBrandAndCategory(Product product) {
        if (product.getBrand().getId() == 0) {
            product.getBrand().setId(null);
            Brand newBrand = brandRepository.save(product.getBrand());
            product.setBrand(newBrand);
        }

        if (product.getCategory().getId() == 0) {
            product.getCategory().setId(null);
            Category newCategory = categoryRepository.save(product.getCategory());
            product.setCategory(newCategory);
        }
    }
}
