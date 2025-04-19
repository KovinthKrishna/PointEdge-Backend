package com.eternalcoders.pointedge.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.eternalcoders.pointedge.dto.CustomerDTO;
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

    // below methods for integration
    
    public List<DiscountDTO> getActiveItemDiscounts(Long itemId, Discount.LoyaltyTier loyaltyTier) {
        List<Discount> discounts = discountRepository.findActiveItemDiscounts(itemId, loyaltyTier);
        return modelMapper.map(discounts, new TypeToken<List<DiscountDTO>>(){}.getType());
    }

    public List<DiscountDTO> getActiveCategoryDiscounts(Long categoryId, Discount.LoyaltyTier loyaltyTier) {
        List<Discount> discounts = discountRepository.findActiveCategoryDiscounts(categoryId, loyaltyTier);
        return modelMapper.map(discounts, new TypeToken<List<DiscountDTO>>(){}.getType());
    }

    public List<DiscountDTO> getActiveLoyaltyDiscounts(Discount.LoyaltyTier tier) {
        List<Discount> discounts = discountRepository.findActiveLoyaltyDiscounts(tier);
        return modelMapper.map(discounts, new TypeToken<List<DiscountDTO>>(){}.getType());
    }

    // get price of an item by id
    public ResponseEntity<Map<String, Object>> getProductPriceById(Long itemId) {
        Optional<Double> priceOptional = discountRepository.findProductPriceById(itemId);
        
        if (priceOptional.isPresent()) {
            return ResponseEntity.ok(Map.of(
                "itemId", itemId,
                "price", priceOptional.get(),
                "success", true
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "itemId", itemId,
                    "message", "Product not found with id: " + itemId,
                    "success", false
                ));
        }
    }

    // calculate total price without discounts
    /**
     * Calculate the total amount for given items with their quantities
     * @param itemQuantities Map of item IDs to their quantities
     * @return The total calculated amount
     */
    public BigDecimal calculateTotalAmount(Map<Long, Integer> itemQuantities) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Map.Entry<Long, Integer> entry : itemQuantities.entrySet()) {
            Long itemId = entry.getKey();
            Integer quantity = entry.getValue();
            
            // Skip if quantity is zero or negative
            if (quantity <= 0) {
                continue;
            }
            
            // Get price from the existing API
            ResponseEntity<Map<String, Object>> response = getProductPriceById(itemId);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> priceData = response.getBody();
                
                // Check if the product was found and price exists
                if ((Boolean) priceData.get("success") && priceData.containsKey("price")) {
                    Object priceObj = priceData.get("price");
                    BigDecimal price;
                    
                    // Convert the price object to BigDecimal based on its type
                    if (priceObj instanceof BigDecimal) {
                        price = (BigDecimal) priceObj;
                    } else if (priceObj instanceof Double) {
                        price = BigDecimal.valueOf((Double) priceObj);
                    } else if (priceObj instanceof Number) {
                        price = BigDecimal.valueOf(((Number) priceObj).doubleValue());
                    } else {
                        price = new BigDecimal(priceObj.toString());
                    }
                    
                    // Calculate item total (price * quantity) and add to running sum
                    BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(quantity));
                    totalAmount = totalAmount.add(itemTotal);
                }
            }
        }
        
        return totalAmount;
    }

    // get category ID of a product by product ID
    public ResponseEntity<Map<String, Object>> getCategoryIdByProductId(Long productId) {
        Optional<Long> categoryIdOptional = discountRepository.findCategoryIdByProductId(productId);
        
        if (categoryIdOptional.isPresent()) {
            return ResponseEntity.ok(Map.of(
                "productId", productId,
                "categoryId", categoryIdOptional.get(),
                "success", true
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "productId", productId,
                    "message", "Product not found with id: " + productId,
                    "success", false
                ));
        }
    }

    // get all applicable loyslty discounts for a given product ID and customer phone number
    public ResponseEntity<Map<String, Object>> getApplicableLoyaltyDiscounts(String phone) {
        // Step 1: Find customer tier
        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Invalid customer: no phone provided",
                "discounts", Collections.emptyList()
            ));
        }
    
        // Get customer tier from repository
        Optional<Discount.LoyaltyTier> tierOptional = discountRepository.findCustomerLoyaltyTierByPhone(phone);
        
        if (tierOptional.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Invalid customer: no customer found for phone",
                "discounts", Collections.emptyList()
            ));
        }
    
        Discount.LoyaltyTier tier = tierOptional.get();
        
        // Steps 2 & 3 combined using repository method
        List<Discount> discounts = discountRepository.findActiveLoyaltyDiscountsByTypeAndTier(tier);
        List<DiscountDTO> discountDTOs = modelMapper.map(discounts, new TypeToken<List<DiscountDTO>>(){}.getType());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", discountDTOs.isEmpty() 
                ? "No active LOYALTY discounts found for customer tier " + tier 
                : "Found " + discountDTOs.size() + " active LOYALTY discounts for tier " + tier,
            "discounts", discountDTOs
        ));
    }

    /// get all applicable item discounts for a given product ID and customer phone number
    public ResponseEntity<Map<String, Object>> getApplicableItemDiscounts(String phone, Map<Long, Integer> items) {
        // Step 1: Find customer tier
        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Invalid customer: no phone provided",
                "discounts", Collections.emptyMap()
            ));
        }
    
        // Get customer tier from repository
        Optional<Discount.LoyaltyTier> tierOptional = discountRepository.findCustomerLoyaltyTierByPhone(phone);
        
        if (tierOptional.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Invalid customer: no customer found for phone",
                "discounts", Collections.emptyMap()
            ));
        }
    
        Discount.LoyaltyTier tier = tierOptional.get();
        
        // Step 2: Get discounts for each item
        Map<String, List<DiscountDTO>> itemDiscounts = new HashMap<>();
        
        for (Long itemId : items.keySet()) {
            // Get all active discounts for this item
            List<Discount> allDiscounts = discountRepository.findActiveItemDiscounts(itemId, null);
            
            // Filter discounts based on loyalty tier
            List<DiscountDTO> applicableDiscounts = allDiscounts.stream()
                .filter(d -> 
                    // Include if discount has no loyalty requirement
                    d.getLoyaltyType() == null || 
                    // OR discount matches customer's exact tier
                    d.getLoyaltyType() == tier
                )
                .map(d -> modelMapper.map(d, DiscountDTO.class))
                .collect(Collectors.toList());
                
            if (!applicableDiscounts.isEmpty()) {
                itemDiscounts.put(itemId.toString(), applicableDiscounts);
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", itemDiscounts.isEmpty() 
                ? "No active item discounts found for customer tier " + tier 
                : "Found item discounts for " + itemDiscounts.size() + " items for tier " + tier,
            "discounts", itemDiscounts
        ));
    }

    // get all applicable category discounts for a given product ID and customer phone number
    public ResponseEntity<Map<String, Object>> getApplicableCategoryDiscounts(String phone, Map<Long, Integer> items) {
        // Step 1: Find customer tier
        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Invalid customer: no phone provided",
                "discounts", Collections.emptyMap()
            ));
        }
    
        // Get customer tier from repository
        Optional<Discount.LoyaltyTier> tierOptional = discountRepository.findCustomerLoyaltyTierByPhone(phone);
        
        if (tierOptional.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Invalid customer: no customer found for phone",
                "discounts", Collections.emptyMap()
            ));
        }
    
        Discount.LoyaltyTier tier = tierOptional.get();
        
        // Step 2: Get category discounts for each item
        Map<String, List<DiscountDTO>> categoryDiscounts = new HashMap<>();
        
        for (Long itemId : items.keySet()) {
            // Get category ID for this item
            ResponseEntity<Map<String, Object>> categoryResponse = getCategoryIdByProductId(itemId);
            
            if (!categoryResponse.getStatusCode().is2xxSuccessful() || 
                !(Boolean)categoryResponse.getBody().get("success")) {
                continue; // Skip if category not found
            }
            
            Long categoryId = (Long) categoryResponse.getBody().get("categoryId");
            
            // Get all active discounts for this category
            List<Discount> allDiscounts = discountRepository.findActiveCategoryDiscounts(categoryId, null);
            
            // Filter discounts based on loyalty tier
            List<DiscountDTO> applicableDiscounts = allDiscounts.stream()
                .filter(d -> 
                    // Include if discount has no loyalty requirement
                    d.getLoyaltyType() == null || 
                    // OR discount matches customer's exact tier
                    d.getLoyaltyType() == tier
                )
                .map(d -> modelMapper.map(d, DiscountDTO.class))
                .collect(Collectors.toList());
                
            if (!applicableDiscounts.isEmpty()) {
                // Key format: "itemId-categoryId" to show both item and category context
                categoryDiscounts.put(itemId + "-" + categoryId, applicableDiscounts);
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", categoryDiscounts.isEmpty() 
                ? "No active category discounts found for customer tier " + tier 
                : "Found category discounts for " + categoryDiscounts.size() + " item-category pairs for tier " + tier,
            "discounts", categoryDiscounts
        ));
    }

    // get all applicable discounts for a given product ID and customer phone number

    public ResponseEntity<Map<String, Object>> getAllApplicableDiscounts(String phone, Map<Long, Integer> items) {
        // Validate input
        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Invalid customer: no phone provided",
                "discounts", Collections.emptyMap()
            ));
        }
    
        // Get customer tier
        Optional<Discount.LoyaltyTier> tierOptional = discountRepository.findCustomerLoyaltyTierByPhone(phone);
        if (tierOptional.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Invalid customer: no customer found for phone",
                "discounts", Collections.emptyMap()
            ));
        }
    
        Discount.LoyaltyTier tier = tierOptional.get();
        
        // Prepare response structure
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("customerTier", tier.toString());
        
        // Get all three types of discounts
        ResponseEntity<Map<String, Object>> loyaltyDiscountsResponse = getApplicableLoyaltyDiscounts(phone);
        ResponseEntity<Map<String, Object>> itemDiscountsResponse = getApplicableItemDiscounts(phone, items);
        ResponseEntity<Map<String, Object>> categoryDiscountsResponse = getApplicableCategoryDiscounts(phone, items);
        
        // Combine results
        Map<String, Object> allDiscounts = new HashMap<>();
        
        // Process loyalty discounts (returns List)
        if (loyaltyDiscountsResponse.getStatusCode().is2xxSuccessful() && 
            loyaltyDiscountsResponse.getBody() != null && 
            (Boolean) loyaltyDiscountsResponse.getBody().get("success")) {
            allDiscounts.put("loyaltyDiscounts", loyaltyDiscountsResponse.getBody().get("discounts"));
        } else {
            allDiscounts.put("loyaltyDiscounts", Collections.emptyList());
        }
        
        // Process item discounts (returns Map)
        if (itemDiscountsResponse.getStatusCode().is2xxSuccessful() && 
            itemDiscountsResponse.getBody() != null && 
            (Boolean) itemDiscountsResponse.getBody().get("success")) {
            allDiscounts.put("itemDiscounts", itemDiscountsResponse.getBody().get("discounts"));
        } else {
            allDiscounts.put("itemDiscounts", Collections.emptyMap());
        }
        
        // Process category discounts (returns Map)
        if (categoryDiscountsResponse.getStatusCode().is2xxSuccessful() && 
            categoryDiscountsResponse.getBody() != null && 
            (Boolean) categoryDiscountsResponse.getBody().get("success")) {
            allDiscounts.put("categoryDiscounts", categoryDiscountsResponse.getBody().get("discounts"));
        } else {
            allDiscounts.put("categoryDiscounts", Collections.emptyMap());
        }
        
        response.put("discounts", allDiscounts);
        
        // Count total discounts
        int loyaltyCount = allDiscounts.get("loyaltyDiscounts") instanceof List ? ((List<?>) allDiscounts.get("loyaltyDiscounts")).size() : 0;
        int itemCount = allDiscounts.get("itemDiscounts") instanceof Map ? ((Map<?, ?>) allDiscounts.get("itemDiscounts")).size() : 0;
        int categoryCount = allDiscounts.get("categoryDiscounts") instanceof Map ? ((Map<?, ?>) allDiscounts.get("categoryDiscounts")).size() : 0;
        int totalDiscounts = loyaltyCount + itemCount + categoryCount;
            
        response.put("message", String.format(
            "Found %d total applicable discounts (loyalty: %d, item: %d, category: %d) for tier %s",
            totalDiscounts,
            loyaltyCount,
            itemCount,
            categoryCount,
            tier
        ));
        
        return ResponseEntity.ok(response);
    }

    ///////////////////////////////////// calculate total discount amount

}