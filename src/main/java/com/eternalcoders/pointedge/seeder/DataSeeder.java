package com.eternalcoders.pointedge.seeder;

import com.eternalcoders.pointedge.entity.Brand;
import com.eternalcoders.pointedge.entity.Category;
import com.eternalcoders.pointedge.entity.Product;
import com.eternalcoders.pointedge.repository.BrandRepository;
import com.eternalcoders.pointedge.repository.CategoryRepository;
import com.eternalcoders.pointedge.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.eternalcoders.pointedge.entity.Attendance;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.repository.AttendanceRepository;
import com.eternalcoders.pointedge.repository.EmployeeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class DataSeeder implements CommandLineRunner {
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final Random random = new Random();
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;

    public DataSeeder(BrandRepository brandRepository, CategoryRepository categoryRepository, 
                     ProductRepository productRepository, EmployeeRepository employeeRepository, 
                     AttendanceRepository attendanceRepository) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Override
    public void run(String... args) {
        seedBrands();
        seedCategories();
        seedProducts();
        if (employeeRepository.count() == 0) {
            seedEmployees();
        }
        seedAttendances();
    }

    private void seedEmployees() {
        Employee emp1 = new Employee();
        emp1.setName("Kasun Silva");
        emp1.setRole("Cashier");
        emp1.setStatus(Employee.EmployeeStatus.Active);
        emp1.setAvatar("https://bit.ly/dan-abramov");
        employeeRepository.save(emp1);
        
        
        Employee emp2 = new Employee();
        emp2.setName("Nimal Silva");
        emp2.setRole("Cashier");
        emp2.setStatus(Employee.EmployeeStatus.Leave);
        emp2.setAvatar("https://bit.ly/ryan-florence");
        employeeRepository.save(emp2);
       

        for (int i = 3; i <= 7; i++) {
            Employee emp = new Employee();
            emp.setName("Employee " + i);
            emp.setRole("Cashier");
            emp.setStatus(i % 2 == 0 ? Employee.EmployeeStatus.Active : Employee.EmployeeStatus.Leave);
            emp.setAvatar("https://bit.ly/ryan-florence");
            employeeRepository.save(emp);
        
        }
    }
        
    // This method should be outside of seedEmployees
    // Add to DataSeeder.java
private void seedAttendances() {
    if (attendanceRepository.count() == 0) {
        List<Employee> employees = employeeRepository.findAll();
        if (employees.isEmpty()) return;
        
        List<Attendance> attendances = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 0; i < Math.min(10, employees.size()); i++) {
            Attendance attendance = new Attendance();
            attendance.setEmployee(employees.get(i));
            attendance.setDate(today);
            attendance.setClockIn(LocalTime.of(8, 0));
            attendance.setClockOut(LocalTime.of(17, 0));
            attendance.setTotalHours("09:00:00");
            attendance.setOtHours("00:00:00");
            
            attendances.add(attendance);
        }
        
        attendanceRepository.saveAll(attendances);
    }
}

    private void seedBrands() {
        if (brandRepository.count() == 0) {
            List<Brand> brands = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                Brand brand = new Brand();
                brand.setName("Brand " + i);
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
                category.setName("Category " + i);
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
                product.setName("Product " + i);
                product.setPrice(random.nextInt(500));
                product.setStockQuantity(random.nextInt(1000));
                product.setBrand(brands.get(random.nextInt(brands.size())));
                product.setCategory(categories.get(random.nextInt(categories.size())));

                products.add(product);
            }
            productRepository.saveAll(products);
        }
    }
}