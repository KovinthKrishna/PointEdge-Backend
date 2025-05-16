package com.eternalcoders.pointedge.seeder;

import com.eternalcoders.pointedge.entity.Brand;
import com.eternalcoders.pointedge.entity.Category;
import com.eternalcoders.pointedge.entity.Product;
import com.eternalcoders.pointedge.entity.SalesTransaction;
import com.eternalcoders.pointedge.repository.BrandRepository;
import com.eternalcoders.pointedge.repository.CategoryRepository;
import com.eternalcoders.pointedge.repository.ProductRepository;
import com.eternalcoders.pointedge.repository.SalesTransactionRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.eternalcoders.pointedge.entity.Attendance;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.repository.AttendanceRepository;
import com.eternalcoders.pointedge.repository.EmployeeRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final Random random = new Random();
    

    public DataSeeder(BrandRepository brandRepository, CategoryRepository categoryRepository, 
                     ProductRepository productRepository, EmployeeRepository employeeRepository, 
                     AttendanceRepository attendanceRepository,
                     SalesTransactionRepository salesTransactionRepository) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.salesTransactionRepository = salesTransactionRepository;
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
        seedSalesTransactions();
    }

    private void seedEmployees() {
        List<Employee> employees = new ArrayList<>();
        
        // Create specific employees with recognizable names
        String[][] employeeData = {
            {"Devon Lane", "Cashier", "https://bit.ly/ryan-florence"},
            {"Eleanor Pena", "Cashier", "https://bit.ly/kent-c-dodds"},
            {"Floyd Miles", "Cashier", "https://bit.ly/prosper-baba"},
            {"Annette Black", "Cashier", "https://bit.ly/code-beast"},
            {"Guy Hawkins", "Cashier", "https://bit.ly/sage-adebayo"},
            {"Kasun Silva", "Manager", "https://bit.ly/dan-abramov"},
            {"Nimal Silva", "Assistant Manager", "https://bit.ly/ryan-florence"}
        };
        
        for (int i = 0; i < employeeData.length; i++) {
            Employee emp = new Employee();
            emp.setName(employeeData[i][0]);
            emp.setRole(employeeData[i][1]);
            emp.setStatus(i % 3 == 0 ? Employee.EmployeeStatus.Leave : Employee.EmployeeStatus.Active);
            emp.setAvatar(employeeData[i][2]);
            employees.add(emp);
        }
        
        employeeRepository.saveAll(employees);
    }
        
    // This method should be outside of seedEmployees
    // Add to DataSeeder.java
    private void seedAttendances() {
        // Only seed if no attendance records exist
        if (attendanceRepository.count() == 0) {
            List<Employee> employees = employeeRepository.findAll();
            if (employees.isEmpty()) return;
            
            List<Attendance> allAttendances = new ArrayList<>();
            LocalDate today = LocalDate.now();
            
            // Create attendance records for the last 30 days
            for (int day = 0; day < 30; day++) {
                LocalDate date = today.minusDays(day);
                
                for (Employee employee : employees) {
                    // Skip some days randomly for realism (20% chance to skip)
                    if (random.nextInt(100) < 20) continue;
                    
                    // Create attendance with variable hours
                    Attendance attendance = new Attendance();
                    attendance.setEmployee(employee);
                    attendance.setDate(date);
                    
                    // Clock in between 7:30 and 9:00
                    int startHour = 7 + random.nextInt(2);
                    int startMinute = random.nextInt(60);
                    LocalTime clockIn = LocalTime.of(startHour, startMinute);
                    attendance.setClockIn(clockIn);
                    
                    // Standard end time (17:00)
                    LocalTime standardEnd = LocalTime.of(17, 0);
                    
                    // Clock out between 17:00 and 19:00 (some OT)
                    int endHour = 17 + random.nextInt(3);
                    int endMinute = random.nextInt(60);
                    LocalTime clockOut = LocalTime.of(endHour, endMinute);
                    attendance.setClockOut(clockOut);
                    
                    // Calculate total hours using the service
                    java.time.Duration duration = java.time.Duration.between(clockIn, clockOut);
                    long hours = duration.toHours();
                    long minutes = duration.toMinutesPart();
                    long seconds = duration.toSecondsPart();
                    attendance.setTotalHours(String.format("%d:%02d:%02d", hours, minutes, seconds));
                    
                    // Calculate OT hours
                    if (clockOut.isAfter(standardEnd)) {
                        java.time.Duration otDuration = java.time.Duration.between(standardEnd, clockOut);
                        long otHours = otDuration.toHours();
                        long otMinutes = otDuration.toMinutesPart();
                        long otSeconds = otDuration.toSecondsPart();
                        attendance.setOtHours(String.format("%d:%02d:%02d", otHours, otMinutes, otSeconds));
                    } else {
                        attendance.setOtHours("0:00:00");
                    }
                    
                    allAttendances.add(attendance);
                }
            }
            
            attendanceRepository.saveAll(allAttendances);
        }
    }

     private void seedSalesTransactions() {
        // Only seed if no sales transactions exist
        if (salesTransactionRepository.count() == 0) {
            List<Employee> employees = employeeRepository.findAll();
            if (employees.isEmpty()) return;
            
            List<SalesTransaction> transactions = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            
            // Create sales for the last 30 days
            for (int day = 0; day < 30; day++) {
                LocalDateTime transactionDate = now.minusDays(day);
                
                for (Employee employee : employees) {
                    // Vary the number of orders per employee per day
                    // Top performers will have more orders (based on employee index)
                    int employeeIndex = employees.indexOf(employee);
                    int baseOrderCount = Math.max(1, 5 - employeeIndex); // Top employees get more orders
                    int orderCount = random.nextInt(3) + baseOrderCount;
                    
                    for (int o = 0; o < orderCount; o++) {
                        String orderId = UUID.randomUUID().toString();
                        
                        // Each order has 1-5 items
                        int itemCount = random.nextInt(5) + 1;
                        
                        for (int i = 0; i < itemCount; i++) {
                            // Amount between $5 and $200, with top performers having higher value sales
                            double baseAmount = 5 + random.nextDouble() * 50;
                            // Top employees get higher value sales
                            double multiplier = Math.max(1.0, (5.0 - employeeIndex) / 2.0);
                            BigDecimal amount = BigDecimal.valueOf(baseAmount * multiplier);
                            
                            SalesTransaction transaction = new SalesTransaction();
                            transaction.setEmployee(employee);
                            // Distribute throughout the day
                            LocalDateTime transactionTime = transactionDate
                                .withHour(9 + random.nextInt(8))  // Between 9AM and 5PM
                                .withMinute(random.nextInt(60))
                                .withSecond(random.nextInt(60));
                            transaction.setTransactionDateTime(transactionTime);
                            transaction.setAmount(amount);
                            transaction.setItemCount(1);
                            transaction.setOrderId(orderId);
                            
                            transactions.add(transaction);
                        }
                    }
                }
            }
            
            salesTransactionRepository.saveAll(transactions);
        }
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

    private static class Duration {
        public static java.time.Duration between(LocalTime start, LocalTime end) {
            return java.time.Duration.between(start, end);
        }
    }
}