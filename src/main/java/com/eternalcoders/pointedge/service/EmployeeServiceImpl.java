package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.EmployeeDTO;
import com.eternalcoders.pointedge.dto.EmployeeDashboardDTO;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.entity.PasswordResetToken;
import com.eternalcoders.pointedge.repository.EmployeeRepository;
import com.eternalcoders.pointedge.repository.OrderRepository;
import com.eternalcoders.pointedge.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service("employeeServiceImpl")
public class EmployeeServiceImpl extends EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OrderRepository orderRepository;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            OrderRepository orderRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.orderRepository = orderRepository;
    }

    @Override
    public void registerEmployee(EmployeeDTO dto) {
        if (employeeRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (!dto.getTempPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        Employee employee = new Employee();
        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setPhoneNumber(dto.getPhoneNumber());
        employee.setEmail(dto.getEmail());
        employee.setRole("USER");
        employee.setTempPassword(passwordEncoder.encode(dto.getTempPassword()));

        employeeRepository.save(employee);
    }

    @Override
    @Transactional
    public void sendResetPasswordToken(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete existing token (if any) and flush to ensure it's committed before inserting new
        tokenRepository.deleteByUser(employee);
        tokenRepository.flush();

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(employee);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(5));

        tokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(email, token);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        Employee employee = resetToken.getUser();
        employee.setTempPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);

        tokenRepository.delete(resetToken); // Clean up used token
    }

    @Override
    public EmployeeDashboardDTO getDashboardStats() {
        EmployeeDashboardDTO dto = new EmployeeDashboardDTO();

        // Get total orders from orders table
        long totalOrders = orderRepository.count();

        // Get total sales from orders table
        double totalSales = orderRepository.findAll()
            .stream()
            .mapToDouble(order -> {
                Double total = order.getTotal();
                return total != null ? total : 0.0;
            })
            .sum();

        dto.setTotalOrders(totalOrders);
        dto.setTotalSales(totalSales);

        return dto;
    }
}