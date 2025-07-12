package com.eternalcoders.pointedge.seeder;

import com.eternalcoders.pointedge.entity.Brand;
import com.eternalcoders.pointedge.entity.Category;
import com.eternalcoders.pointedge.entity.Product;
import com.eternalcoders.pointedge.repository.BrandRepository;
import com.eternalcoders.pointedge.repository.CategoryRepository;
import com.eternalcoders.pointedge.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final Random random = new Random();

    public DataSeeder(BrandRepository brandRepository, CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        seedBrands();
        seedCategories();
        seedProducts();
    }

    private void seedBrands() {
        if (brandRepository.count() == 0) {
            List<Brand> brands = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                Brand brand = new Brand();
                brand.setName(String.format("Brand %02d", i));
                brands.add(brand);
            }
            brandRepository.saveAll(brands);
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            List<Category> categories = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                Category category = new Category();
                category.setName(String.format("Category %02d", i));
                categories.add(category);
            }
            categoryRepository.saveAll(categories);
        }
    }

    private void seedProducts() {
        if (productRepository.count() == 0) {
            List<Brand> brands = brandRepository.findAll();
            List<Category> categories = categoryRepository.findAll();
            List<Product> products = new ArrayList<>();

            for (int i = 1; i <= 200; i++) {
                Product product = new Product();
                product.setName(String.format("Product %03d", i));
                product.setPrice(Math.round((100 + (random.nextDouble() * 899.99)) * 100) / 100.0);
                product.setStockQuantity(200 + random.nextInt(300));
                product.setBrand(brands.get(random.nextInt(brands.size())));
                product.setCategory(categories.get(random.nextInt(categories.size())));

                products.add(product);
            }
            productRepository.saveAll(products);
        }
    }
}
