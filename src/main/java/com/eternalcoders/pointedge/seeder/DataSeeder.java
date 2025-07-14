package com.eternalcoders.pointedge.seeder;

import com.eternalcoders.pointedge.entity.Brand;
import com.eternalcoders.pointedge.entity.Category;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.entity.Product;
import com.eternalcoders.pointedge.repository.BrandRepository;
import com.eternalcoders.pointedge.repository.CategoryRepository;
import com.eternalcoders.pointedge.repository.EmployeeRepository;
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
    private final EmployeeRepository employeeRepository;
    private final Random random = new Random();

    public DataSeeder(BrandRepository brandRepository, CategoryRepository categoryRepository, ProductRepository productRepository, EmployeeRepository employeeRepository) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void run(String... args) {
        seedBrands();
        seedCategories();
        seedProducts();
        seedEmployees();
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

    private void seedEmployees() {
        if (employeeRepository.count() == 0) {
            List<Employee> employees = new ArrayList<>();

            Employee admin = new Employee();
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setEmail("admin@example.com");
            admin.setName("Admin User");
            admin.setTempPassword("$2a$12$xszQpWNzSB3/eQJqBDRzmuOEZ62BItKifio8HFCtEzpT5L6AAo15K");
            admin.setRole("ADMIN");
            admin.setStatus(Employee.EmployeeStatus.Active);

            Employee user = new Employee();
            user.setFirstName("Regular");
            user.setLastName("User");
            user.setEmail("user@example.com");
            user.setName("Regular User");
            user.setTempPassword("$2a$12$r6YXi/XO9d76HbTi2ODTievIAXqZIWA4qtohLSvdbEdfiv6WvF89y");
            user.setRole("USER");
            user.setStatus(Employee.EmployeeStatus.Active);

            employees.add(admin);
            employees.add(user);

            employeeRepository.saveAll(employees);
        }
    }
}
