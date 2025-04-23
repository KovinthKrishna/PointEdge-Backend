package com.eternalcoders.pointedge.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.eternalcoders.pointedge.entity.Customer;
import com.eternalcoders.pointedge.entity.Customer.Tier;
import com.eternalcoders.pointedge.entity.Discount;
import com.eternalcoders.pointedge.entity.Discount.DiscountType;
import com.eternalcoders.pointedge.entity.LoyaltyThresholds;
import com.eternalcoders.pointedge.repository.DiscountRepository;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.annotation.Propagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
@Transactional
public class DiscountService {

    private static final Logger logger = LoggerFactory.getLogger(DiscountService.class);

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

    public ResponseEntity<Map<String, Object>> getApplicableItemDiscounts(String phone, Map<Long, Integer> items) {
        // Step 1: Initialize response
        Map<String, Object> response = new HashMap<>();
        
        // Step 2: Handle case when phone is null or empty
        if (phone == null || phone.trim().isEmpty()) {
            Map<String, List<DiscountDTO>> universalDiscounts = new HashMap<>();
            
            for (Long itemId : items.keySet()) {
                List<Discount> allDiscounts = discountRepository.findActiveItemDiscounts(itemId, null);
                
                List<DiscountDTO> applicableDiscounts = allDiscounts.stream()
                    .filter(d -> d.getLoyaltyType() == null) // Only universal discounts
                    .map(d -> modelMapper.map(d, DiscountDTO.class))
                    .collect(Collectors.toList());
                    
                if (!applicableDiscounts.isEmpty()) {
                    universalDiscounts.put(itemId.toString(), applicableDiscounts);
                }
            }
            
            response.put("success", true);
            response.put("discounts", universalDiscounts);
            response.put("message", universalDiscounts.isEmpty() ?
                "No universal item discounts found" :
                "Found universal item discounts for " + universalDiscounts.size() + " items");
            response.put("customerTier", "UNKNOWN");
            
            return ResponseEntity.ok(response);
        }
    
        // Step 3: Get customer tier if exists
        Optional<Discount.LoyaltyTier> tierOptional = discountRepository.findCustomerLoyaltyTierByPhone(phone);
        Discount.LoyaltyTier tier = tierOptional.orElse(null);
        
        // Step 4: Get discounts for each item
        Map<String, List<DiscountDTO>> itemDiscounts = new HashMap<>();
        
        for (Long itemId : items.keySet()) {
            List<Discount> allDiscounts = discountRepository.findActiveItemDiscounts(itemId, null);
            
            List<DiscountDTO> applicableDiscounts = allDiscounts.stream()
                .filter(d -> tier != null ? 
                    (d.getLoyaltyType() == null || d.getLoyaltyType() == tier) :
                    d.getLoyaltyType() == null)
                .map(d -> modelMapper.map(d, DiscountDTO.class))
                .collect(Collectors.toList());
                
            if (!applicableDiscounts.isEmpty()) {
                itemDiscounts.put(itemId.toString(), applicableDiscounts);
            }
        }
        
        // Build response
        response.put("success", true);
        response.put("discounts", itemDiscounts);
        
        if (tier != null) {
            response.put("message", itemDiscounts.isEmpty() ?
                "No active item discounts found for customer tier " + tier :
                "Found item discounts for " + itemDiscounts.size() + " items for tier " + tier);
            response.put("customerTier", tier.toString());
        } else {
            response.put("message", itemDiscounts.isEmpty() ?
                "No universal item discounts found" :
                "Found universal item discounts for " + itemDiscounts.size() + " items");
            response.put("customerTier", "UNKNOWN");
        }
        
        return ResponseEntity.ok(response);
    }

    // get all applicable category discounts for a given product ID and customer phone number
    public ResponseEntity<Map<String, Object>> getApplicableCategoryDiscounts(String phone, Map<Long, Integer> items) {
        // Step 1: Initialize response
        Map<String, Object> response = new HashMap<>();
        
        // Step 2: Handle case when phone is null or empty
        if (phone == null || phone.trim().isEmpty()) {
            Map<String, List<DiscountDTO>> universalDiscounts = new HashMap<>();
            
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
                
                // Filter for universal discounts only
                List<DiscountDTO> applicableDiscounts = allDiscounts.stream()
                    .filter(d -> d.getLoyaltyType() == null) // Only universal discounts
                    .map(d -> {
                        DiscountDTO dto = modelMapper.map(d, DiscountDTO.class);
                        dto.setItemId(itemId);
                        return dto;
                    })
                    .collect(Collectors.toList());
                    
                if (!applicableDiscounts.isEmpty()) {
                    universalDiscounts.put(itemId + "-" + categoryId, applicableDiscounts);
                }
            }
            
            response.put("success", true);
            response.put("discounts", universalDiscounts);
            response.put("message", universalDiscounts.isEmpty() ?
                "No universal category discounts found" :
                "Found universal category discounts for " + universalDiscounts.size() + " item-category pairs");
            response.put("customerTier", "UNKNOWN");
            
            return ResponseEntity.ok(response);
        }
    
        // Step 3: Get customer tier if exists
        Optional<Discount.LoyaltyTier> tierOptional = discountRepository.findCustomerLoyaltyTierByPhone(phone);
        if (tierOptional.isEmpty()) {
            Map<String, List<DiscountDTO>> universalDiscounts = new HashMap<>();
            
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
                
                // Filter for universal discounts only
                List<DiscountDTO> applicableDiscounts = allDiscounts.stream()
                    .filter(d -> d.getLoyaltyType() == null) // Only universal discounts
                    .map(d -> {
                        DiscountDTO dto = modelMapper.map(d, DiscountDTO.class);
                        dto.setItemId(itemId);
                        return dto;
                    })
                    .collect(Collectors.toList());
                    
                if (!applicableDiscounts.isEmpty()) {
                    universalDiscounts.put(itemId + "-" + categoryId, applicableDiscounts);
                }
            }
            
            response.put("success", true);
            response.put("discounts", universalDiscounts);
            response.put("message", universalDiscounts.isEmpty() ?
                "No universal category discounts found" :
                "Found universal category discounts for " + universalDiscounts.size() + " item-category pairs");
            response.put("customerTier", "UNKNOWN");
            
            return ResponseEntity.ok(response);
        }
        
        Discount.LoyaltyTier tier = tierOptional.get();
        
        // Step 4: Get discounts for each item (original logic)
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
                    d.getLoyaltyType() == null || 
                    d.getLoyaltyType() == tier
                )
                .map(d -> {
                    DiscountDTO dto = modelMapper.map(d, DiscountDTO.class);
                    dto.setItemId(itemId);
                    return dto;
                })
                .collect(Collectors.toList());
                
            if (!applicableDiscounts.isEmpty()) {
                categoryDiscounts.put(itemId + "-" + categoryId, applicableDiscounts);
            }
        }
        
        // Build response
        response.put("success", true);
        response.put("discounts", categoryDiscounts);
        response.put("message", categoryDiscounts.isEmpty() ?
            "No active category discounts found for customer tier " + tier :
            "Found category discounts for " + categoryDiscounts.size() + " item-category pairs for tier " + tier);
        response.put("customerTier", tier.toString());
        
        return ResponseEntity.ok(response);
    }

    // get all applicable discounts for a given product ID and customer phone number

    public ResponseEntity<Map<String, Object>> getAllApplicableDiscounts(String phone, Map<Long, Integer> items) {
        // Initialize response
        Map<String, Object> response = new HashMap<>();
        
        // Handle case when phone is null or empty
        if (phone == null || phone.trim().isEmpty()) {
            // Get universal discounts for all types
            ResponseEntity<Map<String, Object>> itemDiscountsResponse = getApplicableItemDiscounts("", items);
            ResponseEntity<Map<String, Object>> categoryDiscountsResponse = getApplicableCategoryDiscounts("", items);
            
            // Combine results
            Map<String, Object> allDiscounts = new HashMap<>();
            allDiscounts.put("loyaltyDiscounts", Collections.emptyList()); // No universal loyalty discounts
            
            if (itemDiscountsResponse.getStatusCode().is2xxSuccessful() && 
                itemDiscountsResponse.getBody() != null && 
                (Boolean) itemDiscountsResponse.getBody().get("success")) {
                allDiscounts.put("itemDiscounts", itemDiscountsResponse.getBody().get("discounts"));
            } else {
                allDiscounts.put("itemDiscounts", Collections.emptyMap());
            }
            
            if (categoryDiscountsResponse.getStatusCode().is2xxSuccessful() && 
                categoryDiscountsResponse.getBody() != null && 
                (Boolean) categoryDiscountsResponse.getBody().get("success")) {
                allDiscounts.put("categoryDiscounts", categoryDiscountsResponse.getBody().get("discounts"));
            } else {
                allDiscounts.put("categoryDiscounts", Collections.emptyMap());
            }
            
            // Count discounts
            int itemCount = allDiscounts.get("itemDiscounts") instanceof Map ? ((Map<?, ?>) allDiscounts.get("itemDiscounts")).size() : 0;
            int categoryCount = allDiscounts.get("categoryDiscounts") instanceof Map ? ((Map<?, ?>) allDiscounts.get("categoryDiscounts")).size() : 0;
            int totalDiscounts = itemCount + categoryCount;
            
            response.put("success", true);
            response.put("discounts", allDiscounts);
            response.put("customerTier", "UNKNOWN");
            response.put("message", String.format(
                "Found %d universal discounts (item: %d, category: %d)",
                totalDiscounts,
                itemCount,
                categoryCount
            ));
            
            return ResponseEntity.ok(response);
        }
    
        // Get customer tier if exists
        Optional<Discount.LoyaltyTier> tierOptional = discountRepository.findCustomerLoyaltyTierByPhone(phone);
        if (tierOptional.isEmpty()) {
            // Customer not found - return universal discounts
            ResponseEntity<Map<String, Object>> itemDiscountsResponse = getApplicableItemDiscounts("", items);
            ResponseEntity<Map<String, Object>> categoryDiscountsResponse = getApplicableCategoryDiscounts("", items);
            
            Map<String, Object> allDiscounts = new HashMap<>();
            allDiscounts.put("loyaltyDiscounts", Collections.emptyList());
            
            if (itemDiscountsResponse.getStatusCode().is2xxSuccessful() && 
                itemDiscountsResponse.getBody() != null && 
                (Boolean) itemDiscountsResponse.getBody().get("success")) {
                allDiscounts.put("itemDiscounts", itemDiscountsResponse.getBody().get("discounts"));
            } else {
                allDiscounts.put("itemDiscounts", Collections.emptyMap());
            }
            
            if (categoryDiscountsResponse.getStatusCode().is2xxSuccessful() && 
                categoryDiscountsResponse.getBody() != null && 
                (Boolean) categoryDiscountsResponse.getBody().get("success")) {
                allDiscounts.put("categoryDiscounts", categoryDiscountsResponse.getBody().get("discounts"));
            } else {
                allDiscounts.put("categoryDiscounts", Collections.emptyMap());
            }
            
            int itemCount = allDiscounts.get("itemDiscounts") instanceof Map ? ((Map<?, ?>) allDiscounts.get("itemDiscounts")).size() : 0;
            int categoryCount = allDiscounts.get("categoryDiscounts") instanceof Map ? ((Map<?, ?>) allDiscounts.get("categoryDiscounts")).size() : 0;
            int totalDiscounts = itemCount + categoryCount;
            
            response.put("success", true);
            response.put("discounts", allDiscounts);
            response.put("customerTier", "UNKNOWN");
            response.put("message", String.format(
                "Found %d universal discounts (item: %d, category: %d)",
                totalDiscounts,
                itemCount,
                categoryCount
            ));
            
            return ResponseEntity.ok(response);
        }
        
        // Original logic for valid customer with tier
        Discount.LoyaltyTier tier = tierOptional.get();
        
        // Prepare response structure
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

    public Map<String, Object> getApplicableDiscountIds(String phone, Map<Long, Integer> items) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 1. Get all applicable discounts first
            Map<String, Object> allDiscounts = getAllApplicableDiscounts(phone, items).getBody();
            List<Map<String, Object>> discountDetails = new ArrayList<>();
            
            BigDecimal totalItemDiscount = BigDecimal.ZERO;
            BigDecimal totalCategoryDiscount = BigDecimal.ZERO;
            BigDecimal totalLoyaltyDiscount = BigDecimal.ZERO;
            BigDecimal fullSubtotal = BigDecimal.ZERO; // Includes ALL items (discounted or not)
            BigDecimal discountableSubtotal = BigDecimal.ZERO; // Only includes items with discounts
    
            // First pass: Calculate full subtotal (all items)
            for (Map.Entry<Long, Integer> entry : items.entrySet()) {
                BigDecimal price = discountRepository.findPriceByItemId(entry.getKey())
                    .orElse(BigDecimal.ZERO);
                fullSubtotal = fullSubtotal.add(
                    price.multiply(BigDecimal.valueOf(entry.getValue())));
            }
    
            // Identify which items have discounts
            Set<Long> discountedItemIds = new HashSet<>();
            
            if (allDiscounts != null && allDiscounts.containsKey("discounts")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> discounts = (Map<String, Object>) allDiscounts.get("discounts");
                
                // Check item discounts
                @SuppressWarnings("unchecked")
                Map<String, List<DiscountDTO>> itemDiscounts = (Map<String, List<DiscountDTO>>) 
                    discounts.getOrDefault("itemDiscounts", Collections.emptyMap());
                discountedItemIds.addAll(itemDiscounts.keySet().stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toSet()));
                
                // Check category discounts
                @SuppressWarnings("unchecked")
                Map<String, List<DiscountDTO>> categoryDiscounts = (Map<String, List<DiscountDTO>>) 
                    discounts.getOrDefault("categoryDiscounts", Collections.emptyMap());
                for (String key : categoryDiscounts.keySet()) {
                    Long itemId = Long.parseLong(key.split("-")[0]);
                    discountedItemIds.add(itemId);
                }
                
                // Calculate discountable subtotal (only items with discounts)
                for (Map.Entry<Long, Integer> entry : items.entrySet()) {
                    if (discountedItemIds.contains(entry.getKey())) {
                        BigDecimal price = discountRepository.findPriceByItemId(entry.getKey())
                            .orElse(BigDecimal.ZERO);
                        discountableSubtotal = discountableSubtotal.add(
                            price.multiply(BigDecimal.valueOf(entry.getValue())));
                    }
                }
                
                // Process item discounts
                for (Map.Entry<String, List<DiscountDTO>> entry : itemDiscounts.entrySet()) {
                    Long itemId = Long.parseLong(entry.getKey());
                    Integer quantity = items.get(itemId);
                    BigDecimal price = discountRepository.findPriceByItemId(itemId)
                        .orElse(BigDecimal.ZERO);
                    
                    BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(quantity));
                    
                    for (DiscountDTO discount : entry.getValue()) {
                        Map<String, Object> discountInfo = new HashMap<>();
                        discountInfo.put("id", discount.id);
                        discountInfo.put("itemId", itemId);
                        discountInfo.put("quantity", quantity);
                        discountInfo.put("price", price);
                        discountInfo.put("totalAmount", totalAmount);
                        
                        BigDecimal discountValue = calculateDiscountValue(discount, totalAmount, quantity);
                        BigDecimal discountedPrice = totalAmount.subtract(discountValue);
                        
                        discountInfo.put("totalDiscount", discountValue);
                        discountInfo.put("discountedPrice", discountedPrice);
                        
                        if (discount.getPercentage() != null) {
                            discountInfo.put("percentage", discount.getPercentage());
                        } else if (discount.getAmount() != null) {
                            discountInfo.put("amount", discount.getAmount());
                        }
                        
                        totalItemDiscount = totalItemDiscount.add(discountValue);
                        discountDetails.add(discountInfo);
                    }
                }
                
                // Process category discounts
                for (Map.Entry<String, List<DiscountDTO>> entry : categoryDiscounts.entrySet()) {
                    String[] parts = entry.getKey().split("-");
                    Long itemId = Long.parseLong(parts[0]);
                    Integer quantity = items.get(itemId);
                    BigDecimal price = discountRepository.findPriceByItemId(itemId)
                        .orElse(BigDecimal.ZERO);
                    
                    BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(quantity));
                    
                    for (DiscountDTO discount : entry.getValue()) {
                        Map<String, Object> discountInfo = new HashMap<>();
                        discountInfo.put("id", discount.id);
                        discountInfo.put("itemId", itemId);
                        discountInfo.put("quantity", quantity);
                        discountInfo.put("price", price);
                        discountInfo.put("totalAmount", totalAmount);
                        
                        BigDecimal discountValue = calculateDiscountValue(discount, totalAmount, quantity);
                        BigDecimal discountedPrice = totalAmount.subtract(discountValue);
                        
                        discountInfo.put("totalDiscount", discountValue);
                        discountInfo.put("discountedPrice", discountedPrice);
                        
                        if (discount.getPercentage() != null) {
                            discountInfo.put("percentage", discount.getPercentage());
                        } else if (discount.getAmount() != null) {
                            discountInfo.put("amount", discount.getAmount());
                        }
                        
                        totalCategoryDiscount = totalCategoryDiscount.add(discountValue);
                        discountDetails.add(discountInfo);
                    }
                }
                
                // Process loyalty discounts on the discountable subtotal
                @SuppressWarnings("unchecked")
                List<DiscountDTO> loyaltyDiscounts = (List<DiscountDTO>) 
                    discounts.getOrDefault("loyaltyDiscounts", Collections.emptyList());
                
                for (DiscountDTO discount : loyaltyDiscounts) {
                    Map<String, Object> discountInfo = new HashMap<>();
                    discountInfo.put("id", discount.id);
                    discountInfo.put("totalAmount", discountableSubtotal);
                    
                    BigDecimal discountValue = calculateDiscountValue(discount, discountableSubtotal, 1);
                    BigDecimal discountedPrice = discountableSubtotal.subtract(discountValue);
                    
                    discountInfo.put("totalDiscount", discountValue);
                    discountInfo.put("discountedPrice", discountedPrice);
                    
                    if (discount.getPercentage() != null) {
                        discountInfo.put("percentage", discount.getPercentage());
                    } else if (discount.getAmount() != null) {
                        discountInfo.put("amount", discount.getAmount());
                    }
                    
                    totalLoyaltyDiscount = totalLoyaltyDiscount.add(discountValue);
                    discountDetails.add(discountInfo);
                }
                
                // Calculate final totals
                BigDecimal finalTotalDiscount = totalItemDiscount.add(totalCategoryDiscount).add(totalLoyaltyDiscount);
                BigDecimal finalDiscountedPrice = fullSubtotal.subtract(finalTotalDiscount);
                
                response.put("success", true);
                response.put("discounts", discountDetails);
                response.put("totalItemDiscount", totalItemDiscount);
                response.put("totalCategoryDiscount", totalCategoryDiscount);
                response.put("totalLoyaltyDiscount", totalLoyaltyDiscount);
                response.put("finalTotalAmount", fullSubtotal);
                response.put("finalTotalDiscount", finalTotalDiscount);
                response.put("finalDiscountedPrice", finalDiscountedPrice);
            } else {
                // No discounts, just return full subtotal
                response.put("success", true);
                response.put("finalTotalAmount", fullSubtotal);
                response.put("finalDiscountedPrice", fullSubtotal);
                response.put("finalTotalDiscount", BigDecimal.ZERO);
                response.put("totalItemDiscount", BigDecimal.ZERO);
                response.put("totalCategoryDiscount", BigDecimal.ZERO);
                response.put("totalLoyaltyDiscount", BigDecimal.ZERO);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing discounts: " + e.getMessage());
        }
        return response;
    }
    
    private BigDecimal calculateDiscountValue(DiscountDTO discount, BigDecimal totalAmount, Integer quantity) {
        try {
            if (discount.getPercentage() != null) {
                // Handle percentage discount
                BigDecimal percentage = new BigDecimal(discount.getPercentage().toString());
                return totalAmount.multiply(percentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else if (discount.getAmount() != null) {
                // Handle fixed amount discount
                BigDecimal amount = new BigDecimal(discount.getAmount().toString());
                return amount.multiply(new BigDecimal(quantity));
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    //  final discount return with customer info

    public Map<String, Object> getFinalDiscountedOrderWithCustomerInfo(String phone, Map<Long, Integer> items) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Handle case when phone is null or empty
            if (phone == null || phone.trim().isEmpty()) {
                // Get universal discounts
                Map<String, Object> discountResponse = getApplicableDiscountIds("", items);
                
                response.put("finalDiscountedPrice", discountResponse.get("finalDiscountedPrice"));
                response.put("finalTotalDiscount", discountResponse.get("finalTotalDiscount"));
                response.put("finalTotalAmount", discountResponse.get("finalTotalAmount"));
                response.put("phone", "");
                response.put("loyaltyTier", "UNKNOWN");
                response.put("points", 0);
                response.put("name", "Guest");
                response.put("email", "");
                response.put("title", "OTHER");
                response.put("success", true);
                return response;
            }
    
            // 1. Get the discount information first
            Map<String, Object> discountResponse = getApplicableDiscountIds(phone, items);
            
            // 2. Get customer information
            Optional<Customer> customerOpt = discountRepository.findCustomerByPhone(phone);
            
            if (customerOpt.isEmpty()) {
                // Customer not found - return universal discounts with minimal customer info
                response.put("finalDiscountedPrice", discountResponse.get("finalDiscountedPrice"));
                response.put("finalTotalDiscount", discountResponse.get("finalTotalDiscount"));
                response.put("finalTotalAmount", discountResponse.get("finalTotalAmount"));
                response.put("phone", phone);
                response.put("loyaltyTier", "UNKNOWN");
                response.put("points", 0);
                response.put("name", "Guest");
                response.put("email", "");
                response.put("title", "OTHER");
                response.put("success", true);
                return response;
            }
            
            Customer customer = customerOpt.get();
            
            // 3. Format the title correctly
            String formattedTitle = "OTHER"; // Default value
            if (customer.getTitle() != null) {
                // Handle both String and Enum title types
                String titleStr = customer.getTitle() instanceof Enum ?
                    ((Enum<?>) customer.getTitle()).name() :
                    customer.getTitle().toString();
                
                switch (titleStr.toUpperCase()) {
                    case "MR":
                    case "MRS":
                    case "MS":
                    case "DR":
                        formattedTitle = titleStr.toUpperCase();
                        break;
                    default:
                        formattedTitle = "OTHER";
                }
            }
            
            // 4. Build the final response
            response.put("finalDiscountedPrice", discountResponse.get("finalDiscountedPrice"));
            response.put("finalTotalDiscount", discountResponse.get("finalTotalDiscount"));
                response.put("finalTotalAmount", discountResponse.get("finalTotalAmount"));
            response.put("phone", phone);
            response.put("loyaltyTier", customer.getTier() != null ? 
                            customer.getTier().toString() : "NONE");
            response.put("points", customer.getPoints());
            response.put("name", customer.getName());
            response.put("email", customer.getEmail());
            response.put("title", formattedTitle);
            response.put("success", true);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing order: " + e.getMessage());
        }
        
        return response;
    }

    ////////////////////////////////////////////// save and update order details

    // get customer points by phone number

    public Map<String, Object> getCustomerPointsByPhone(String phone) {
        Map<String, Object> response = new HashMap<>();
        
        if (phone == null || phone.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Phone number is required");
            return response;
        }
        
        Optional<Double> points = discountRepository.findCustomerPointsByPhone(phone);
        
        if (points.isEmpty()) {
            response.put("success", false);
            response.put("message", "Customer not found");
        } else {
            response.put("success", true);
            response.put("points", points.get());
        }
        
        return response;
    }

    // update customer points by phone number

    public Map<String, Object> updateCustomerPoints(String phone, Double points) {
        Map<String, Object> response = new HashMap<>();
        
        if (phone == null || phone.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Phone number is required");
            return response;
        }
        
        if (points == null || points < 0) {
            response.put("success", false);
            response.put("message", "Points must be a positive number");
            return response;
        }
        
        Optional<Customer> customer = discountRepository.findCustomerByPhone(phone);
        
        if (customer.isEmpty()) {
            response.put("success", false);
            response.put("message", "Customer not found");
        } else {
            discountRepository.updateCustomerPoints(phone, points);
            response.put("success", true);
            response.put("message", "Points updated successfully");
            response.put("newPoints", points);
        }
        
        return response;
    }

    // get used and earned points by phone number
    // In DiscountService.java

public Map<String, Object> calculatePointsUsageAndEarning(String phone, Map<Long, Integer> items) {
    Map<String, Object> response = new HashMap<>();
    
    try {
        // 1. Validate phone number
        if (phone == null || phone.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Phone number is required");
            return response;
        }
        
        // 2. Get discount information to get total amount and loyalty discount
        Map<String, Object> discountInfo = getApplicableDiscountIds(phone, items);
        if (!(Boolean) discountInfo.get("success")) {
            return discountInfo; // Return the error if discount calculation failed
        }
        
        BigDecimal finalTotalAmount = (BigDecimal) discountInfo.get("finalTotalAmount");
        BigDecimal totalLoyaltyDiscount = (BigDecimal) discountInfo.getOrDefault("totalLoyaltyDiscount", BigDecimal.ZERO);
        
        // 3. Get loyalty thresholds to get keypoints
        LoyaltyThresholdsDTO thresholds = getLoyaltyThresholds();
        double keyPoints = thresholds.points; // Points earned per 100 rupees
        
        // 4. Get customer's current points
        Optional<Double> customerPointsOpt = discountRepository.findCustomerPointsByPhone(phone);
        if (customerPointsOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Customer not found with phone: " + phone);
            return response;
        }
        double customerPoints = customerPointsOpt.get();
        
        // 5. Calculate earned points (points earned from this purchase)
        double earnedPoints = finalTotalAmount.doubleValue() / 100 * keyPoints;
        
        // 6. Calculate used points (points redeemed in this purchase)
        double usedPoints = 0;
        if (customerPoints >= totalLoyaltyDiscount.doubleValue()) {
            usedPoints = totalLoyaltyDiscount.doubleValue();
        } else {
            usedPoints = customerPoints;
        }
        
        // 7. Prepare response
        response.put("success", true);
        response.put("finalTotalAmount", finalTotalAmount);
        response.put("totalLoyaltyDiscount", totalLoyaltyDiscount);
        response.put("customerCurrentPoints", customerPoints);
        response.put("keyPointsRate", keyPoints); // Points per 100 rupees
        response.put("earnedPoints", earnedPoints);
        response.put("usedPoints", usedPoints);
        response.put("newPointsBalance", customerPoints - usedPoints + earnedPoints);
        
    } catch (Exception e) {
        response.put("success", false);
        response.put("message", "Error calculating points: " + e.getMessage());
    }
    
    return response;
}

    
// update customers points after calculations

public Map<String, Object> updateCustomerPointsAfterPurchase(String phone, Map<Long, Integer> items) {
    Map<String, Object> response = new HashMap<>();
    
    try {
        // 1. Calculate points usage and earning
        Map<String, Object> pointsCalculation = calculatePointsUsageAndEarning(phone, items);
        
        if (!(Boolean) pointsCalculation.get("success")) {
            return pointsCalculation; // Return error if calculation failed
        }
        
        // 2. Get the new points balance
        double newPointsBalance = (double) pointsCalculation.get("newPointsBalance");
        
        // 3. Update customer points
        Map<String, Object> updateResult = updateCustomerPoints(phone, newPointsBalance);
        
        if (!(Boolean) updateResult.get("success")) {
            return updateResult; // Return error if update failed
        }
        
        // 4. Return the complete points information
        response.putAll(pointsCalculation);
        response.put("message", "Customer points updated successfully");
        
    } catch (Exception e) {
        response.put("success", false);
        response.put("message", "Error updating customer points: " + e.getMessage());
    }
    
    return response;
}
    
// add order details


public Map<String, Object> getCompleteDiscountAndPointsInfo(String phone, Map<Long, Integer> items) {
    Map<String, Object> response = new HashMap<>();
    
    try {
        // 1. Get customer information
        Optional<Customer> customerOpt = discountRepository.findCustomerByPhone(phone);
        if (customerOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Customer not found");
            return response;
        }
        Customer customer = customerOpt.get();
        
        // 2. Get all discount information
        Map<String, Object> discountInfo = getApplicableDiscountIds(phone, items);
        if (!(Boolean) discountInfo.get("success")) {
            return discountInfo;
        }
        
        // 3. Get points calculation
        Map<String, Object> pointsInfo = calculatePointsUsageAndEarning(phone, items);
        if (!(Boolean) pointsInfo.get("success")) {
            return pointsInfo;
        }
        
        // 4. Get customer points
        Map<String, Object> customerPoints = getCustomerPointsByPhone(phone);
        if (!(Boolean) customerPoints.get("success")) {
            return customerPoints;
        }
        
        // 5. Get final order with customer info
        Map<String, Object> finalOrderInfo = getFinalDiscountedOrderWithCustomerInfo(phone, items);
        if (!(Boolean) finalOrderInfo.get("success")) {
            return finalOrderInfo;
        }
        
        // 6. Get all applicable discounts (for detailed discount info)
        Map<String, Object> allDiscounts = getAllApplicableDiscounts(phone, items).getBody();
        
        // 7. Compile all the information
        response.put("success", true);
        
        // Get customer ID from repository if needed
        Optional<Long> customerIdOpt = discountRepository.findCustomerIdByPhone(phone);
        if (customerIdOpt.isPresent()) {
            response.put("customerId", customerIdOpt.get());
        } else {
            response.put("customerId", null);
        }
        
        response.put("customerName", customer.getName());
        response.put("customerPhone", phone);
        response.put("customerTier", customer.getTier() != null ? 
                     customer.getTier().toString() : "NONE");
        
        // Rest of the method remains the same...
        // Discount amounts
        response.put("totalItemDiscount", discountInfo.get("totalItemDiscount"));
        response.put("totalCategoryDiscount", discountInfo.get("totalCategoryDiscount"));
        response.put("totalLoyaltyDiscount", discountInfo.get("totalLoyaltyDiscount"));
        response.put("finalTotalAmount", discountInfo.get("finalTotalAmount"));
        response.put("finalDiscountedPrice", discountInfo.get("finalDiscountedPrice"));
        
        // Points information
        response.put("currentPoints", customerPoints.get("points"));
        response.put("usedPoints", pointsInfo.get("usedPoints"));
        response.put("earnedPoints", pointsInfo.get("earnedPoints"));
        response.put("newPointsBalance", pointsInfo.get("newPointsBalance"));
        response.put("keyPointsRate", pointsInfo.get("keyPointsRate"));
        
        // Item details with discounts
        List<Map<String, Object>> itemDetails = new ArrayList<>();
        if (discountInfo.containsKey("discounts")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> discounts = (List<Map<String, Object>>) discountInfo.get("discounts");
            
            // Group discounts by itemId
            Map<Long, List<Map<String, Object>>> itemDiscountsMap = discounts.stream()
                .filter(d -> d.containsKey("itemId"))
                .collect(Collectors.groupingBy(
                    d -> ((Number) d.get("itemId")).longValue(),
                    Collectors.toList()
                ));
            
            // Create item details
            for (Map.Entry<Long, Integer> entry : items.entrySet()) {
                Long itemId = entry.getKey();
                Integer quantity = entry.getValue();
                
                Map<String, Object> itemDetail = new HashMap<>();
                itemDetail.put("itemId", itemId);
                itemDetail.put("quantity", quantity);
                
                // Get price
                Optional<BigDecimal> priceOpt = discountRepository.findPriceByItemId(itemId);
                if (priceOpt.isPresent()) {
                    BigDecimal price = priceOpt.get();
                    BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(quantity));
                    itemDetail.put("price", price);
                    itemDetail.put("totalAmount", totalAmount);
                    
                    // Add discounts if any
                    if (itemDiscountsMap.containsKey(itemId)) {
                        List<Map<String, Object>> itemDiscounts = itemDiscountsMap.get(itemId);
                        itemDetail.put("discounts", itemDiscounts);
                        
                        // Calculate total discount for this item
                        BigDecimal itemTotalDiscount = itemDiscounts.stream()
                            .map(d -> (BigDecimal) d.get("totalDiscount"))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        itemDetail.put("totalDiscount", itemTotalDiscount);
                    } else {
                        itemDetail.put("discounts", Collections.emptyList());
                        itemDetail.put("totalDiscount", BigDecimal.ZERO);
                    }
                }
                
                itemDetails.add(itemDetail);
            }
        }
        
        response.put("itemDetails", itemDetails);
        
        // Add loyalty discounts if any
        if (allDiscounts != null && allDiscounts.containsKey("discounts")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> discounts = (Map<String, Object>) allDiscounts.get("discounts");
            response.put("loyaltyDiscounts", discounts.getOrDefault("loyaltyDiscounts", Collections.emptyList()));
        } else {
            response.put("loyaltyDiscounts", Collections.emptyList());
        }
        
    } catch (Exception e) {
        response.put("success", false);
        response.put("message", "Error processing request: " + e.getMessage());
    }
    
    return response;
}

// update order details

// @Transactional
// public Map<String, Object> saveOrderDetails(String phone, Map<Long, Integer> items) {
//     // Get complete discount info first
//     Map<String, Object> discountInfo = getCompleteDiscountAndPointsInfo(phone, items);
    
//     if (!(Boolean) discountInfo.get("success")) {
//         return discountInfo;
//     }

//     // First update customer points
//     Map<String, Object> pointsUpdateResult = updateCustomerPointsAfterPurchase(phone, items);
//     if (!(Boolean) pointsUpdateResult.get("success")) {
//         return pointsUpdateResult;
//     }

//     // Extract common order details
//     Long customerId = (Long) discountInfo.get("customerId");
//     String loyaltyTier = (String) discountInfo.get("customerTier");
//     Double pointsEarned = (Double) discountInfo.get("earnedPoints");
//     Double totalLoyaltyDiscount = ((Number) discountInfo.get("totalLoyaltyDiscount")).doubleValue();
//     Double totalCategoryDiscount = ((Number) discountInfo.get("totalCategoryDiscount")).doubleValue();
    
//     // Get points rate from loyalty thresholds
//     LoyaltyThresholdsDTO thresholds = getLoyaltyThresholds();
//     double pointsRate = thresholds.points;
    
//     // Save each item
//     @SuppressWarnings("unchecked")
//     List<Map<String, Object>> itemDetails = (List<Map<String, Object>>) discountInfo.get("itemDetails");
    
//     for (Map<String, Object> item : itemDetails) {
//         Long itemId = ((Number) item.get("itemId")).longValue();
//         Double amount = ((Number) item.get("totalAmount")).doubleValue();
//         Double itemDiscount = ((Number) item.get("totalDiscount")).doubleValue();
        
//         // Get discount ID if available (from item discounts)
//         Long discountId = null;
//         @SuppressWarnings("unchecked")
//         List<Map<String, Object>> discounts = (List<Map<String, Object>>) item.get("discounts");
//         if (discounts != null && !discounts.isEmpty()) {
//             discountId = ((Number) discounts.get(0).get("id")).longValue();
//         }
        
//         // Calculate category and loyalty discounts for this item
//         Double categoryDiscount = totalCategoryDiscount / itemDetails.size();
//         Double loyaltyDiscount = totalLoyaltyDiscount / itemDetails.size();
//         Double totalDiscount = itemDiscount + categoryDiscount + loyaltyDiscount;
        
//         // Calculate points earned for this specific item
//         Double itemPointsEarned = (amount / 100) * pointsRate;
        
//         discountRepository.saveOrderDetails(
//             customerId,
//             itemId,
//             discountId,
//             LocalDateTime.now(),
//             amount,
//             totalDiscount,
//             itemDiscount,
//             categoryDiscount,
//             loyaltyDiscount,
//             loyaltyTier,
//             itemPointsEarned
//         );
//     }
    
//     // Combine both results
//     Map<String, Object> response = new HashMap<>();
//     response.putAll(discountInfo);
//     response.putAll(pointsUpdateResult);
//     response.put("message", "Order details and points updated successfully");
    
//     return response;
// }

@Transactional
public Map<String, Object> saveOrderDetails(String phone, Map<Long, Integer> items) {
    // Get complete discount info first
    Map<String, Object> discountInfo = getCompleteDiscountAndPointsInfo(phone, items);
    
    if (!(Boolean) discountInfo.get("success")) {
        return discountInfo;
    }

    // First update customer points
    Map<String, Object> pointsUpdateResult = updateCustomerPointsAfterPurchase(phone, items);
    if (!(Boolean) pointsUpdateResult.get("success")) {
        return pointsUpdateResult;
    }

    // Update customer loyalty status based on new points
    Map<String, Object> loyaltyUpdateResult = updateCustomerLoyaltyStatus(phone);
    if (!(Boolean) loyaltyUpdateResult.get("success")) {
        return loyaltyUpdateResult;
    }

    // Get the updated tier from the loyalty update result
    String loyaltyTier = (String) loyaltyUpdateResult.get("newTier");

    // Extract common order details
    Long customerId = (Long) discountInfo.get("customerId");
    Double pointsEarned = (Double) discountInfo.get("earnedPoints");
    Double totalLoyaltyDiscount = ((Number) discountInfo.get("totalLoyaltyDiscount")).doubleValue();
    Double totalCategoryDiscount = ((Number) discountInfo.get("totalCategoryDiscount")).doubleValue();
    
    // Get points rate from loyalty thresholds
    LoyaltyThresholdsDTO thresholds = getLoyaltyThresholds();
    double pointsRate = thresholds.points;
    
    // Save each item
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> itemDetails = (List<Map<String, Object>>) discountInfo.get("itemDetails");
    
    for (Map<String, Object> item : itemDetails) {
        Long itemId = ((Number) item.get("itemId")).longValue();
        Double amount = ((Number) item.get("totalAmount")).doubleValue();
        Double itemDiscount = ((Number) item.get("totalDiscount")).doubleValue();
        
        // Get discount ID if available (from item discounts)
        Long discountId = null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> discounts = (List<Map<String, Object>>) item.get("discounts");
        if (discounts != null && !discounts.isEmpty()) {
            discountId = ((Number) discounts.get(0).get("id")).longValue();
        }
        
        // Calculate category and loyalty discounts for this item
        Double categoryDiscount = totalCategoryDiscount / itemDetails.size();
        Double loyaltyDiscount = totalLoyaltyDiscount / itemDetails.size();
        Double totalDiscount = itemDiscount + categoryDiscount + loyaltyDiscount;
        
        // Calculate points earned for this specific item
        Double itemPointsEarned = (amount / 100) * pointsRate;
        
        discountRepository.saveOrderDetails(
            customerId,
            itemId,
            discountId,
            LocalDateTime.now(),
            amount,
            totalDiscount,
            itemDiscount,
            categoryDiscount,
            loyaltyDiscount,
            loyaltyTier,  // Use the updated tier
            itemPointsEarned
        );
    }
    
    // Combine all results
    Map<String, Object> response = new HashMap<>();
    response.putAll(discountInfo);
    response.putAll(pointsUpdateResult);
    response.putAll(loyaltyUpdateResult);
    response.put("message", "Order details, points, and loyalty status updated successfully");
    
    return response;
}


// update loyalty tier

public Map<String, Object> updateCustomerLoyaltyStatus(String phone) {
    Map<String, Object> response = new HashMap<>();
    
    try {
        // 1. Validate phone number
        if (phone == null || phone.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Phone number is required");
            return response;
        }
        
        // 2. Get customer's current points
        Optional<Double> customerPointsOpt = discountRepository.findCustomerPointsByPhone(phone);
        if (customerPointsOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Customer not found with phone: " + phone);
            return response;
        }
        double customerPoints = customerPointsOpt.get();
        
        // 3. Get loyalty thresholds
        LoyaltyThresholdsDTO thresholds = getLoyaltyThresholds();
        
        // 4. Determine new tier based on points
        Tier newTier;
        if (customerPoints >= thresholds.gold) {
            newTier = Tier.GOLD;
        } else if (customerPoints >= thresholds.silver) {
            newTier = Tier.SILVER;
        } else if (customerPoints >= thresholds.bronze) {
            newTier = Tier.BRONZE;
        } else {
            newTier = Tier.NOTLOYALTY;
        }
        
        // 5. Update customer tier
        discountRepository.updateCustomerTier(phone, newTier);
        
        // 6. Get updated customer info
        Optional<Customer> customerOpt = discountRepository.findCustomerByPhone(phone);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            response.put("success", true);
            response.put("message", "Customer loyalty status updated successfully");
            response.put("phone", phone);
            response.put("points", customer.getPoints());
            response.put("tier", customer.getTier());
            response.put("newTier", newTier.toString());
        } else {
            response.put("success", false);
            response.put("message", "Customer not found after update");
        }
        
    } catch (Exception e) {
        response.put("success", false);
        response.put("message", "Error updating customer loyalty status: " + e.getMessage());
    }
    
    return response;
}

}