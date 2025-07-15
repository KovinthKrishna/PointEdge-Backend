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
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.eternalcoders.pointedge.dto.DiscountDTO;
import com.eternalcoders.pointedge.dto.LoyaltyThresholdsDTO;
import com.eternalcoders.pointedge.entity.Customer;
import com.eternalcoders.pointedge.entity.Customer.Tier;
import com.eternalcoders.pointedge.entity.Discount;
import com.eternalcoders.pointedge.entity.Discount.DiscountType;
import com.eternalcoders.pointedge.entity.LoyaltyThresholds;
import com.eternalcoders.pointedge.repository.DiscountRepository;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private CustomerService customerService;

    // get all discounts
    public List<DiscountDTO> getAllDiscounts() {
        List<Discount> discountsList = discountRepository.findAll();
        return modelMapper.map(discountsList, new TypeToken<List<DiscountDTO>>(){}.getType());
    }

    //add discount
    public DiscountDTO addDiscount(DiscountDTO discountDTO) {
        Discount discount = modelMapper.map(discountDTO, Discount.class);
        Discount savedDiscount = discountRepository.save(discount);
        return modelMapper.map(savedDiscount, DiscountDTO.class);
    }
    
    //update discount
    public DiscountDTO updateDiscount(DiscountDTO discountDTO) {
        Discount discount = modelMapper.map(discountDTO, Discount.class);
        Discount updatedDiscount = discountRepository.save(discount);
        return modelMapper.map(updatedDiscount, DiscountDTO.class);
    }
    
    //get discount by type
    public List<DiscountDTO> getDiscountsByType(DiscountType discountType) {
        List<Discount> discounts = discountRepository.findByType(discountType);
        return modelMapper.map(discounts, new TypeToken<List<DiscountDTO>>(){}.getType());
    }
    
    //get discount by id
    public DiscountDTO getDiscountById(Long id) {
        Discount discount = discountRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Discount not found with id: " + id));
        return modelMapper.map(discount, DiscountDTO.class);
    }

    //get discount by name
    public List<String> getAllDiscountNames() {
        return discountRepository.findAllDiscountNames();
    }
    
    //delete discount by id
    public void deleteDiscountById(Long id) {
        if (!discountRepository.existsById(id)) {
            throw new RuntimeException("Discount not found with id: " + id);
        }
        discountRepository.deleteById(id);
    }

    //get all product names 
    public List<String> getAllProductNames() {
        return discountRepository.findAllProductNames();
    }
    
    //get all category names
    public List<String> getAllCategoryNames() {
        return discountRepository.findAllCategoryNames();
    }

    //validate admin password
    private boolean validateAdminPassword(String password) {
        return discountRepository.existsByAdminPassword(password);
    }

    //delete all discounts
    public boolean deleteAllDiscountsWithAuth(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

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

    //delete discounts by type
    public boolean deleteDiscountsByTypeWithAuth(DiscountType type, String adminPassword) {
        if (!discountRepository.existsByAdminPassword(adminPassword)) {
            throw new SecurityException("Invalid admin password");
        }
        
        try {
            discountRepository.deleteByType(type); 
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //count discounts by type
    public long countDiscountsByType(DiscountType type) {
        return discountRepository.countByType(type);
    }

    //get loyalty thresholds
    public LoyaltyThresholdsDTO getLoyaltyThresholds() {
        LoyaltyThresholds thresholds = discountRepository.findLoyaltyThresholds()
            .orElseThrow(() -> new RuntimeException("Loyalty thresholds not found"));
        return modelMapper.map(thresholds, LoyaltyThresholdsDTO.class);
    }
    
    // Update loyalty thresholds with admin password validation
public LoyaltyThresholdsDTO updateLoyaltyThresholds(LoyaltyThresholdsDTO thresholdsDTO) {
    if (thresholdsDTO.getAdminPassword() == null || thresholdsDTO.getAdminPassword().trim().isEmpty()) {
        throw new IllegalArgumentException("Admin password is required");
    }

    // Validate admin password
    boolean isValid = discountRepository.existsByAdminPassword(thresholdsDTO.getAdminPassword());
    if (!isValid) {
        throw new SecurityException("Invalid admin credentials");
    }

    // Update thresholds if password is valid
    discountRepository.updateLoyaltyThresholds(
        thresholdsDTO.getGold(),
        thresholdsDTO.getSilver(),
        thresholdsDTO.getBronze(),
        thresholdsDTO.getPoints()
    );
    
    // Update all customer tiers based on new thresholds
    customerService.updateAllCustomerTiers();
    
    // Return the updated thresholds (without the password)
    LoyaltyThresholdsDTO resultDTO = new LoyaltyThresholdsDTO();
    resultDTO.setGold(thresholdsDTO.getGold());
    resultDTO.setSilver(thresholdsDTO.getSilver());
    resultDTO.setBronze(thresholdsDTO.getBronze());
    resultDTO.setPoints(thresholdsDTO.getPoints());
    // Admin password is intentionally not set in the response
    return resultDTO;
}
    
    //get active item discounts
    public List<DiscountDTO> getActiveItemDiscounts(Long itemId, Discount.LoyaltyTier loyaltyTier) {
        List<Discount> discounts = discountRepository.findActiveItemDiscounts(itemId, loyaltyTier);
        return modelMapper.map(discounts, new TypeToken<List<DiscountDTO>>(){}.getType());
    }

    //get active category discounts
    public List<DiscountDTO> getActiveCategoryDiscounts(Long categoryId, Discount.LoyaltyTier loyaltyTier) {
        List<Discount> discounts = discountRepository.findActiveCategoryDiscounts(categoryId, loyaltyTier);
        return modelMapper.map(discounts, new TypeToken<List<DiscountDTO>>(){}.getType());
    }

    //get active loyalty discounts
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
      
     @param itemQuantities
     * @return 
     */
    public BigDecimal calculateTotalAmount(Map<Long, Integer> itemQuantities) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Map.Entry<Long, Integer> entry : itemQuantities.entrySet()) {
            Long itemId = entry.getKey();
            Integer quantity = entry.getValue();
            if (quantity <= 0) {
                continue;
            }
            
            ResponseEntity<Map<String, Object>> response = getProductPriceById(itemId);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> priceData = response.getBody();
                
                if ((Boolean) priceData.get("success") && priceData.containsKey("price")) {
                    Object priceObj = priceData.get("price");
                    BigDecimal price;
                    
                    if (priceObj instanceof BigDecimal) {
                        price = (BigDecimal) priceObj;
                    } else if (priceObj instanceof Double) {
                        price = BigDecimal.valueOf((Double) priceObj);
                    } else if (priceObj instanceof Number) {
                        price = BigDecimal.valueOf(((Number) priceObj).doubleValue());
                    } else {
                        price = new BigDecimal(priceObj.toString());
                    }
                    
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

    // get all applicable loyalty discounts for a given product ID and customer phone number
    public ResponseEntity<Map<String, Object>> getApplicableLoyaltyDiscounts(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Invalid customer: no phone provided",
                "discounts", Collections.emptyList()
            ));
        }
    
        Optional<Discount.LoyaltyTier> tierOptional = discountRepository.findCustomerLoyaltyTierByPhone(phone);
        
        if (tierOptional.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Invalid customer: no customer found for phone",
                "discounts", Collections.emptyList()
            ));
        }
    
        Discount.LoyaltyTier tier = tierOptional.get();
       
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

    // get applicable item discounts
    public ResponseEntity<Map<String, Object>> getApplicableItemDiscounts(String phone, Map<Long, Integer> items) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (phone == null || phone.trim().isEmpty()) {
            Map<String, List<DiscountDTO>> universalDiscounts = new HashMap<>();
            
            for (Long itemId : items.keySet()) {
                List<Discount> allDiscounts = discountRepository.findActiveItemDiscounts(itemId, null);
                
                List<DiscountDTO> applicableDiscounts = allDiscounts.stream()
                    .filter(d -> d.getLoyaltyType() == null) 
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
    
        Optional<Discount.LoyaltyTier> tierOptional = discountRepository.findCustomerLoyaltyTierByPhone(phone);
        Discount.LoyaltyTier tier = tierOptional.orElse(null);
      
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
        
        Map<String, Object> response = new HashMap<>();
        
        if (phone == null || phone.trim().isEmpty()) {
            Map<String, List<DiscountDTO>> universalDiscounts = new HashMap<>();
            
            for (Long itemId : items.keySet()) {
                
                ResponseEntity<Map<String, Object>> categoryResponse = getCategoryIdByProductId(itemId);
                
                if (!categoryResponse.getStatusCode().is2xxSuccessful() || 
                    !(Boolean)categoryResponse.getBody().get("success")) {
                    continue;
                }
                
                Long categoryId = (Long) categoryResponse.getBody().get("categoryId");
                
                List<Discount> allDiscounts = discountRepository.findActiveCategoryDiscounts(categoryId, null);
                
                List<DiscountDTO> applicableDiscounts = allDiscounts.stream()
                    .filter(d -> d.getLoyaltyType() == null) 
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
    
        Optional<Discount.LoyaltyTier> tierOptional = discountRepository.findCustomerLoyaltyTierByPhone(phone);
        if (tierOptional.isEmpty()) {
            Map<String, List<DiscountDTO>> universalDiscounts = new HashMap<>();
            
            for (Long itemId : items.keySet()) {
                ResponseEntity<Map<String, Object>> categoryResponse = getCategoryIdByProductId(itemId);
                
                if (!categoryResponse.getStatusCode().is2xxSuccessful() || 
                    !(Boolean)categoryResponse.getBody().get("success")) {
                    continue;
                }
                
                Long categoryId = (Long) categoryResponse.getBody().get("categoryId");
                
                List<Discount> allDiscounts = discountRepository.findActiveCategoryDiscounts(categoryId, null);
                
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
        
        Map<String, List<DiscountDTO>> categoryDiscounts = new HashMap<>();
        
        for (Long itemId : items.keySet()) {
            ResponseEntity<Map<String, Object>> categoryResponse = getCategoryIdByProductId(itemId);
            
            if (!categoryResponse.getStatusCode().is2xxSuccessful() || 
                !(Boolean)categoryResponse.getBody().get("success")) {
                continue; 
            }
            
            Long categoryId = (Long) categoryResponse.getBody().get("categoryId");
            
            List<Discount> allDiscounts = discountRepository.findActiveCategoryDiscounts(categoryId, null);
            
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
        
        Map<String, Object> response = new HashMap<>();
        
        if (phone == null || phone.trim().isEmpty()) {
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
    
        Optional<Discount.LoyaltyTier> tierOptional = discountRepository.findCustomerLoyaltyTierByPhone(phone);
        if (tierOptional.isEmpty()) {
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
        
        Discount.LoyaltyTier tier = tierOptional.get();
        
        response.put("success", true);
        response.put("customerTier", tier.toString());
        
        ResponseEntity<Map<String, Object>> loyaltyDiscountsResponse = getApplicableLoyaltyDiscounts(phone);
        ResponseEntity<Map<String, Object>> itemDiscountsResponse = getApplicableItemDiscounts(phone, items);
        ResponseEntity<Map<String, Object>> categoryDiscountsResponse = getApplicableCategoryDiscounts(phone, items);
        
        Map<String, Object> allDiscounts = new HashMap<>();
        
        if (loyaltyDiscountsResponse.getStatusCode().is2xxSuccessful() && 
            loyaltyDiscountsResponse.getBody() != null && 
            (Boolean) loyaltyDiscountsResponse.getBody().get("success")) {
            allDiscounts.put("loyaltyDiscounts", loyaltyDiscountsResponse.getBody().get("discounts"));
        } else {
            allDiscounts.put("loyaltyDiscounts", Collections.emptyList());
        }
        
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
        
        response.put("discounts", allDiscounts);
        
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

    // get applicable discount IDs
    public Map<String, Object> getApplicableDiscountIds(String phone, Map<Long, Integer> items) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> allDiscounts = getAllApplicableDiscounts(phone, items).getBody();
            List<Map<String, Object>> discountDetails = new ArrayList<>();
            
            BigDecimal totalItemDiscount = BigDecimal.ZERO;
            BigDecimal totalCategoryDiscount = BigDecimal.ZERO;
            BigDecimal totalLoyaltyDiscount = BigDecimal.ZERO;
            BigDecimal fullSubtotal = BigDecimal.ZERO; 
            BigDecimal discountableSubtotal = BigDecimal.ZERO; 
    
            for (Map.Entry<Long, Integer> entry : items.entrySet()) {
                BigDecimal price = discountRepository.findPriceByItemId(entry.getKey())
                    .orElse(BigDecimal.ZERO);
                fullSubtotal = fullSubtotal.add(
                    price.multiply(BigDecimal.valueOf(entry.getValue())));
            }
    
            Set<Long> discountedItemIds = new HashSet<>();
            
            if (allDiscounts != null && allDiscounts.containsKey("discounts")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> discounts = (Map<String, Object>) allDiscounts.get("discounts");
                
                @SuppressWarnings("unchecked")
                Map<String, List<DiscountDTO>> itemDiscounts = (Map<String, List<DiscountDTO>>) 
                    discounts.getOrDefault("itemDiscounts", Collections.emptyMap());
                discountedItemIds.addAll(itemDiscounts.keySet().stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toSet()));
                
                @SuppressWarnings("unchecked")
                Map<String, List<DiscountDTO>> categoryDiscounts = (Map<String, List<DiscountDTO>>) 
                    discounts.getOrDefault("categoryDiscounts", Collections.emptyMap());
                for (String key : categoryDiscounts.keySet()) {
                    Long itemId = Long.parseLong(key.split("-")[0]);
                    discountedItemIds.add(itemId);
                }
                
                for (Map.Entry<Long, Integer> entry : items.entrySet()) {
                    if (discountedItemIds.contains(entry.getKey())) {
                        BigDecimal price = discountRepository.findPriceByItemId(entry.getKey())
                            .orElse(BigDecimal.ZERO);
                        discountableSubtotal = discountableSubtotal.add(
                            price.multiply(BigDecimal.valueOf(entry.getValue())));
                    }
                }
                
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
    
    // calculate discount value
    private BigDecimal calculateDiscountValue(DiscountDTO discount, BigDecimal totalAmount, Integer quantity) {
        try {
            if (discount.getPercentage() != null) {
                BigDecimal percentage = new BigDecimal(discount.getPercentage().toString());
                return totalAmount.multiply(percentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else if (discount.getAmount() != null) {
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
            if (phone == null || phone.trim().isEmpty()) {
                
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
    
            Map<String, Object> discountResponse = getApplicableDiscountIds(phone, items);
            
            Optional<Customer> customerOpt = discountRepository.findCustomerByPhone(phone);
            
            if (customerOpt.isEmpty()) {
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
            
            String formattedTitle = "OTHER"; 
            if (customer.getTitle() != null) {
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
    public Map<String, Object> calculatePointsUsageAndEarning(String phone, Map<Long, Integer> items) {
        Map<String, Object> response = new HashMap<>();
        
        try {
           
            if (phone == null || phone.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Phone number is required");
                return response;
            }
            
            Map<String, Object> discountInfo = getApplicableDiscountIds(phone, items);
            if (!(Boolean) discountInfo.get("success")) {
                return discountInfo; 
            }
            
            BigDecimal finalTotalAmount = (BigDecimal) discountInfo.get("finalTotalAmount");
            BigDecimal totalLoyaltyDiscount = (BigDecimal) discountInfo.getOrDefault("totalLoyaltyDiscount", BigDecimal.ZERO);
           
            LoyaltyThresholdsDTO thresholds = getLoyaltyThresholds();
            double keyPoints = thresholds.points; 
           
            Optional<Double> customerPointsOpt = discountRepository.findCustomerPointsByPhone(phone);
            if (customerPointsOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Customer not found with phone: " + phone);
                return response;
            }
            double customerPoints = customerPointsOpt.get();
            
            double earnedPoints = finalTotalAmount.doubleValue() / 100 * keyPoints;
            
            double usedPoints = 0;
            if (customerPoints >= totalLoyaltyDiscount.doubleValue()) {
                usedPoints = totalLoyaltyDiscount.doubleValue();
            } else {
                usedPoints = customerPoints;
            }
            
            response.put("success", true);
            response.put("finalTotalAmount", finalTotalAmount);
            response.put("totalLoyaltyDiscount", totalLoyaltyDiscount);
            response.put("customerCurrentPoints", customerPoints);
            response.put("keyPointsRate", keyPoints); 
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
            Map<String, Object> pointsCalculation = calculatePointsUsageAndEarning(phone, items);
            
            if (!(Boolean) pointsCalculation.get("success")) {
                return pointsCalculation; 
            }
          
            double newPointsBalance = (double) pointsCalculation.get("newPointsBalance");
            
            Map<String, Object> updateResult = updateCustomerPoints(phone, newPointsBalance);
            
            if (!(Boolean) updateResult.get("success")) {
                return updateResult; 
            }
            
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
            Optional<Customer> customerOpt = discountRepository.findCustomerByPhone(phone);
            if (customerOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Customer not found");
                return response;
            }
            Customer customer = customerOpt.get();
            Map<String, Object> discountInfo = getApplicableDiscountIds(phone, items);
            if (!(Boolean) discountInfo.get("success")) {
                return discountInfo;
            }
            
            Map<String, Object> pointsInfo = calculatePointsUsageAndEarning(phone, items);
            if (!(Boolean) pointsInfo.get("success")) {
                return pointsInfo;
            }
            
            Map<String, Object> customerPoints = getCustomerPointsByPhone(phone);
            if (!(Boolean) customerPoints.get("success")) {
                return customerPoints;
            }
            
            Map<String, Object> finalOrderInfo = getFinalDiscountedOrderWithCustomerInfo(phone, items);
            if (!(Boolean) finalOrderInfo.get("success")) {
                return finalOrderInfo;
            }
            
            Map<String, Object> allDiscounts = getAllApplicableDiscounts(phone, items).getBody();
            
            response.put("success", true);
            
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
            
            response.put("totalItemDiscount", discountInfo.get("totalItemDiscount"));
            response.put("totalCategoryDiscount", discountInfo.get("totalCategoryDiscount"));
            response.put("totalLoyaltyDiscount", discountInfo.get("totalLoyaltyDiscount"));
            response.put("finalTotalAmount", discountInfo.get("finalTotalAmount"));
            response.put("finalDiscountedPrice", discountInfo.get("finalDiscountedPrice"));
            
            response.put("currentPoints", customerPoints.get("points"));
            response.put("usedPoints", pointsInfo.get("usedPoints"));
            response.put("earnedPoints", pointsInfo.get("earnedPoints"));
            response.put("newPointsBalance", pointsInfo.get("newPointsBalance"));
            response.put("keyPointsRate", pointsInfo.get("keyPointsRate"));
            
            List<Map<String, Object>> itemDetails = new ArrayList<>();
            if (discountInfo.containsKey("discounts")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> discounts = (List<Map<String, Object>>) discountInfo.get("discounts");
                
                Map<Long, List<Map<String, Object>>> itemDiscountsMap = discounts.stream()
                    .filter(d -> d.containsKey("itemId"))
                    .collect(Collectors.groupingBy(
                        d -> ((Number) d.get("itemId")).longValue(),
                        Collectors.toList()
                    ));
                
                for (Map.Entry<Long, Integer> entry : items.entrySet()) {
                    Long itemId = entry.getKey();
                    Integer quantity = entry.getValue();
                    
                    Map<String, Object> itemDetail = new HashMap<>();
                    itemDetail.put("itemId", itemId);
                    itemDetail.put("quantity", quantity);
                   
                    Optional<BigDecimal> priceOpt = discountRepository.findPriceByItemId(itemId);
                    if (priceOpt.isPresent()) {
                        BigDecimal price = priceOpt.get();
                        BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(quantity));
                        itemDetail.put("price", price);
                        itemDetail.put("totalAmount", totalAmount);
                        
                        if (itemDiscountsMap.containsKey(itemId)) {
                            List<Map<String, Object>> itemDiscounts = itemDiscountsMap.get(itemId);
                            itemDetail.put("discounts", itemDiscounts);
                            
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
    @Transactional
    public Map<String, Object> saveOrderDetails(String phone, Map<Long, Integer> items) {
        Map<String, Object> discountInfo = getCompleteDiscountAndPointsInfo(phone, items);
        
        if (!(Boolean) discountInfo.get("success")) {
            return discountInfo;
        }

        Map<String, Object> pointsUpdateResult = updateCustomerPointsAfterPurchase(phone, items);
        if (!(Boolean) pointsUpdateResult.get("success")) {
            return pointsUpdateResult;
        }

        Map<String, Object> loyaltyUpdateResult = updateCustomerLoyaltyStatus(phone);
        if (!(Boolean) loyaltyUpdateResult.get("success")) {
            return loyaltyUpdateResult;
        }

        String loyaltyTier = (String) loyaltyUpdateResult.get("newTier");

        Long customerId = (Long) discountInfo.get("customerId");
        Double pointsEarned = (Double) discountInfo.get("earnedPoints");
        Double totalLoyaltyDiscount = ((Number) discountInfo.get("totalLoyaltyDiscount")).doubleValue();
        Double totalCategoryDiscount = ((Number) discountInfo.get("totalCategoryDiscount")).doubleValue();
       
        String orderId = "ORD-" + System.currentTimeMillis() + "-" + customerId;
        
        LoyaltyThresholdsDTO thresholds = getLoyaltyThresholds();
        double pointsRate = thresholds.points;
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemDetails = (List<Map<String, Object>>) discountInfo.get("itemDetails");
        
        for (Map<String, Object> item : itemDetails) {
            Long itemId = ((Number) item.get("itemId")).longValue();
            Double amount = ((Number) item.get("totalAmount")).doubleValue();
            Double itemDiscount = ((Number) item.get("totalDiscount")).doubleValue();
            
            Long discountId = null;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> discounts = (List<Map<String, Object>>) item.get("discounts");
            if (discounts != null && !discounts.isEmpty()) {
                discountId = ((Number) discounts.get(0).get("id")).longValue();
            }
            
            Double categoryDiscount = totalCategoryDiscount / itemDetails.size();
            Double loyaltyDiscount = totalLoyaltyDiscount / itemDetails.size();
            Double totalDiscount = itemDiscount + categoryDiscount + loyaltyDiscount;
           
            Double itemPointsEarned = (amount / 100) * pointsRate;
            
            discountRepository.saveOrderDetails(
                orderId,  
                customerId,
                itemId,
                discountId,
                LocalDateTime.now(),
                amount,
                totalDiscount,
                itemDiscount,
                categoryDiscount,
                loyaltyDiscount,
                loyaltyTier,
                itemPointsEarned
            );
        }
        
        Map<String, Object> response = new HashMap<>();
        response.putAll(discountInfo);
        response.putAll(pointsUpdateResult);
        response.putAll(loyaltyUpdateResult);
        response.put("orderId", orderId);  
        response.put("message", "Order details, points, and loyalty status updated successfully");
        
        return response;
    }

    // update loyalty tier
    public Map<String, Object> updateCustomerLoyaltyStatus(String phone) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (phone == null || phone.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Phone number is required");
                return response;
            }
            
            Optional<Double> customerPointsOpt = discountRepository.findCustomerPointsByPhone(phone);
            if (customerPointsOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Customer not found with phone: " + phone);
                return response;
            }
            double customerPoints = customerPointsOpt.get();
            
            LoyaltyThresholdsDTO thresholds = getLoyaltyThresholds();
            
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
            
            discountRepository.updateCustomerTier(phone, newTier);
            
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