package com.eternalcoders.pointedge.service;

import java.time.LocalDateTime;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eternalcoders.pointedge.dto.DiscountDTO;
import com.eternalcoders.pointedge.dto.LoyaltyThresholdsDTO;
import com.eternalcoders.pointedge.entity.Discount;
import com.eternalcoders.pointedge.entity.Discount.DiscountType;
import com.eternalcoders.pointedge.entity.LoyaltyThresholds;
import com.eternalcoders.pointedge.repository.DiscountRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class DiscountService {
    @Autowired
    private DiscountRepository discountRepository;
    
    @Autowired
    private ModelMapper modelMapper;
    
    public List<DiscountDTO> getAllDiscounts() {
        List<Discount> discountsList = discountRepository.findAll();
        return modelMapper.map(discountsList, new TypeToken<List<DiscountDTO>>(){}.getType());
    }
    
    public DiscountDTO addDiscount(DiscountDTO discountDTO) {
        Discount discount = modelMapper.map(discountDTO, Discount.class);
        Discount savedDiscount = discountRepository.save(discount);
        return modelMapper.map(savedDiscount, DiscountDTO.class);
    }
    
    public DiscountDTO updateDiscount(DiscountDTO discountDTO) {
        Discount discount = modelMapper.map(discountDTO, Discount.class);
        Discount updatedDiscount = discountRepository.save(discount);
        return modelMapper.map(updatedDiscount, DiscountDTO.class);
    }
    
    public List<DiscountDTO> getDiscountsByType(DiscountType discountType) {
        List<Discount> discounts = discountRepository.findByType(discountType);
        return modelMapper.map(discounts, new TypeToken<List<DiscountDTO>>(){}.getType());
    }
    
    public DiscountDTO getDiscountById(Long id) {
        Discount discount = discountRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Discount not found with id: " + id));
        return modelMapper.map(discount, DiscountDTO.class);
    }

    public List<String> getAllDiscountNames() {
        return discountRepository.findAllDiscountNames();
    }
    
    public void deleteDiscountById(Long id) {
        if (!discountRepository.existsById(id)) {
            throw new RuntimeException("Discount not found with id: " + id);
        }
        discountRepository.deleteById(id);
    }

    public List<String> getAllProductNames() {
        return discountRepository.findAllProductNames();
    }
    
    public List<String> getAllCategoryNames() {
        return discountRepository.findAllCategoryNames();
    }

    private boolean validateAdminPassword(String password) {
        return discountRepository.existsByAdminPassword(password);
    }

    public boolean deleteAllDiscountsWithAuth(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        
        // Add debug logging
        System.out.println("Validating password: " + password);
        boolean isValid = discountRepository.existsByAdminPassword(password);
        System.out.println("Password validation result: " + isValid);
        
        if (!isValid) {
            throw new SecurityException("Invalid admin credentials");
        }
        
        try {
            long countBefore = discountRepository.count();
            discountRepository.deleteAll();
            long countAfter = discountRepository.count();
            
            System.out.println("Deleted " + (countBefore - countAfter) + " discounts");
            return countAfter == 0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete discounts: " + e.getMessage(), e);
        }
    }

    public boolean deleteDiscountsByTypeWithAuth(DiscountType type, String adminPassword) {
        if (!discountRepository.existsByAdminPassword(adminPassword)) {
            throw new SecurityException("Invalid admin password");
        }
        
        try {
            discountRepository.deleteByType(type); // Using the existing deleteByType method
            return true;
        } catch (Exception e) {
            // log.error("Error deleting discounts by type", e);
            return false;
        }
    }

    public long countDiscountsByType(DiscountType type) {
        return discountRepository.countByType(type);
    }

    public LoyaltyThresholdsDTO getLoyaltyThresholds() {
        LoyaltyThresholds thresholds = discountRepository.findLoyaltyThresholds()
            .orElseThrow(() -> new RuntimeException("Loyalty thresholds not found"));
        return modelMapper.map(thresholds, LoyaltyThresholdsDTO.class);
    }
    
    public LoyaltyThresholdsDTO updateLoyaltyThresholds(LoyaltyThresholdsDTO thresholdsDTO) {
        discountRepository.updateLoyaltyThresholds(
            thresholdsDTO.gold,
            thresholdsDTO.silver,
            thresholdsDTO.bronze,
            thresholdsDTO.points
        );
        return thresholdsDTO;
    }
}