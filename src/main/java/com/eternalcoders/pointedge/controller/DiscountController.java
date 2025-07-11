package com.eternalcoders.pointedge.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.eternalcoders.pointedge.dto.DiscountDTO;
import com.eternalcoders.pointedge.dto.LoyaltyThresholdsDTO;
import com.eternalcoders.pointedge.entity.Discount;
import com.eternalcoders.pointedge.entity.Discount.DiscountType;
import com.eternalcoders.pointedge.service.DiscountService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@CrossOrigin
@RequestMapping(value = "api/v1/discount")
public class DiscountController {
    
    @Autowired
    private DiscountService discountService;
    
    // Get all discounts
    @GetMapping("/get-all-discounts")
    public List<DiscountDTO> getDiscountsDetails() {
        return discountService.getAllDiscounts();
    }
    
    // add discount
    @PostMapping("/add-discount")
    public DiscountDTO addDiscountDetails(@RequestBody DiscountDTO discountDTO) {
        return discountService.addDiscount(discountDTO);
    }
    
    // update discount by id
    @PutMapping("/update-discount-by-id/{id}")
    public DiscountDTO updateDiscountDetails(
        @PathVariable Long id, 
        @RequestBody DiscountDTO discountDTO) {
        discountDTO.id = id; 
        return discountService.updateDiscount(discountDTO);
    }
    
    // get discount by type
    @GetMapping("/get-discounts-by-type/{discountType}")
    public List<DiscountDTO> getDiscountsByType(@PathVariable DiscountType discountType) {
        return discountService.getDiscountsByType(discountType);
    }
      
    // get discount by id
    @GetMapping("/get-discount-by-id/{id}")
    public DiscountDTO getDiscountById(@PathVariable Long id) {
        return discountService.getDiscountById(id);
    }

    // get all discount names
    @GetMapping("/discount-names")
    public ResponseEntity<List<String>> getAllDiscountNames() {
        return ResponseEntity.ok(discountService.getAllDiscountNames());
    }
     
    // delete discount by id
    @DeleteMapping("/delete-discount-by-id/{id}")
    public ResponseEntity<String> deleteDiscountById(@PathVariable Long id) {
        discountService.deleteDiscountById(id);
        return new ResponseEntity<>("Discount with ID: " + id + " deleted successfully", HttpStatus.OK);
    }

    // get all product names 
    @GetMapping("/product-names")
    public ResponseEntity<List<String>> getAllProductNames() {
        return ResponseEntity.ok(discountService.getAllProductNames());
    }

    // get all category names
    @GetMapping("/category-names")
    public ResponseEntity<List<String>> getAllCategoryNames() {
        return ResponseEntity.ok(discountService.getAllCategoryNames());
    }

    //delete all discounts
    @PostMapping("/delete-all-discounts")
    public ResponseEntity<Map<String, Object>> deleteAllDiscounts(@RequestBody Map<String, String> request) {
        String adminPassword = request.get("adminPassword");
        
        if (adminPassword == null || adminPassword.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "message", "Admin password is required"
                ));
        }

        try {
            boolean success = discountService.deleteAllDiscountsWithAuth(adminPassword);
            
            return ResponseEntity.ok()
                .body(Map.of(
                    "success", success,
                    "message", success ? "All discounts deleted successfully" : "Deletion failed"
                ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "success", false,
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "Error deleting discounts: " + e.getMessage()
                ));
        }
    }

    // delete discounts by type
    @PostMapping("/delete-discounts-by-type/{discountType}")
    public ResponseEntity<Map<String, Object>> deleteDiscountsByTypeWithAuth(
            @PathVariable DiscountType discountType,
            @RequestBody Map<String, String> request) {
        
        String adminPassword = request.get("adminPassword");
        
        if (adminPassword == null || adminPassword.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "message", "Admin password is required"
                ));
        }

        try {
            boolean success = discountService.deleteDiscountsByTypeWithAuth(discountType, adminPassword);
            
            return ResponseEntity.ok()
                .body(Map.of(
                    "success", success,
                    "message", success ? "Discounts of type " + discountType + " deleted successfully" : "Deletion failed"
                ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "success", false,
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "Error deleting discounts: " + e.getMessage()
                ));
        }
    }

    // get discount count by type
    @GetMapping("/count-by-type/{discountType}")
    public ResponseEntity<Map<String, Object>> getDiscountCountByType(@PathVariable DiscountType discountType) {
        long count = discountService.countDiscountsByType(discountType);
        return ResponseEntity.ok(Map.of(
            "type", discountType,
            "count", count
        ));
    }

    // get all loyalty thersholds
    @GetMapping("/loyalty-thresholds")
    public ResponseEntity<LoyaltyThresholdsDTO> getLoyaltyThresholds() {
        return ResponseEntity.ok(discountService.getLoyaltyThresholds());
    }
    
    // update loyalty thresholds
    @PutMapping("/loyalty-thresholds")
    public ResponseEntity<LoyaltyThresholdsDTO> updateLoyaltyThresholds(
        @RequestBody LoyaltyThresholdsDTO thresholdsDTO
    ) {
        return ResponseEntity.ok(discountService.updateLoyaltyThresholds(thresholdsDTO));
    }

    // get all active discounts
    @GetMapping("/active/item/{itemId}")
    public ResponseEntity<List<DiscountDTO>> getActiveItemDiscounts(
            @PathVariable Long itemId,
            @RequestParam(required = false) Discount.LoyaltyTier loyaltyTier) {
        List<DiscountDTO> discounts = discountService.getActiveItemDiscounts(itemId, loyaltyTier);
        return ResponseEntity.ok(discounts);
    }

    // get all active discounts by category
    @GetMapping("/active/category/{categoryId}")
    public ResponseEntity<List<DiscountDTO>> getActiveCategoryDiscounts(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Discount.LoyaltyTier loyaltyTier) {
        List<DiscountDTO> discounts = discountService.getActiveCategoryDiscounts(categoryId, loyaltyTier);
        return ResponseEntity.ok(discounts);
    }

    // get all active discounts by loyalty tier
    @GetMapping("/active/loyalty/{tier}")
    public ResponseEntity<List<DiscountDTO>> getActiveLoyaltyDiscounts(
            @PathVariable Discount.LoyaltyTier tier) {
        List<DiscountDTO> discounts = discountService.getActiveLoyaltyDiscounts(tier);
        return ResponseEntity.ok(discounts);
    }

    // get price of an item by id
    @GetMapping("/product-price/{itemId}")
    public ResponseEntity<Map<String, Object>> getProductPrice(@PathVariable Long itemId) {
        return discountService.getProductPriceById(itemId);
    }

    // method for calculate total  price without discounts
    @PostMapping("/calculate-total")
    public ResponseEntity<Map<String, Object>> calculateTotalAmount(@RequestBody Map<Long, Integer> itemQuantities) {
        BigDecimal totalAmount = discountService.calculateTotalAmount(itemQuantities);
        
        Map<String, Object> response = new HashMap<>();
        response.put("total", totalAmount);
        response.put("success", true);
        
        return ResponseEntity.ok(response);
    }

    // get category ID of a product by product ID
    @GetMapping("/product-category/{productId}")
    public ResponseEntity<Map<String, Object>> getProductCategory(@PathVariable Long productId) {
        return discountService.getCategoryIdByProductId(productId);
    }

    // get all applicable loyalty discounts for a given product ID and customer phone number
    @PostMapping("/applicable-loyalty-discounts")
    public ResponseEntity<Map<String, Object>> getApplicableLoyaltyDiscounts(
        @RequestBody Map<String, Object> request) {
        
        String phone = (String) request.get("phone");
        return discountService.getApplicableLoyaltyDiscounts(phone);
    } 
    
    // get all applicable item discounts for a given product ID and customer phone number
    @PostMapping("/applicable-item-discounts")
    public ResponseEntity<Map<String, Object>> getApplicableItemDiscounts(
        @RequestBody Map<String, Object> request) {
        
        String phone = (String) request.get("phone");
        @SuppressWarnings("unchecked")
        Map<String, Object> itemsMap = (Map<String, Object>) request.get("items");
        
        if (phone == null || itemsMap == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Both phone and items are required"
            ));
        }
        
        Map<Long, Integer> items = new HashMap<>();
        for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
            try {
                Long itemId = Long.parseLong(entry.getKey());
                Integer quantity = (entry.getValue() instanceof Integer) ? 
                    (Integer) entry.getValue() : 
                    Integer.parseInt(entry.getValue().toString());
                items.put(itemId, quantity);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid item ID or quantity format"
                ));
            }
        }
        
        return discountService.getApplicableItemDiscounts(phone, items);
    }

    // get all applicable category discounts for a given product ID and customer phone number
    @PostMapping("/applicable-category-discounts")
    public ResponseEntity<Map<String, Object>> getApplicableCategoryDiscounts(
        @RequestBody Map<String, Object> request) {
        
        String phone = (String) request.get("phone");
        @SuppressWarnings("unchecked")
        Map<String, Object> itemsMap = (Map<String, Object>) request.get("items");
        
        if (phone == null || itemsMap == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Both phone and items are required"
            ));
        }
        
        Map<Long, Integer> items = new HashMap<>();
        for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
            try {
                Long itemId = Long.parseLong(entry.getKey());
                Integer quantity = (entry.getValue() instanceof Integer) ? 
                    (Integer) entry.getValue() : 
                    Integer.parseInt(entry.getValue().toString());
                items.put(itemId, quantity);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid item ID or quantity format"
                ));
            }
        }
        
        return discountService.getApplicableCategoryDiscounts(phone, items);
    }

    // get all applicable discounts for a given product ID and customer phone number
    @PostMapping("/all-applicable-discounts")
    public ResponseEntity<Map<String, Object>> getAllApplicableDiscounts(
        @RequestBody Map<String, Object> request) {
        
        String phone = (String) request.get("phone");
        @SuppressWarnings("unchecked")
        Map<String, Object> itemsMap = (Map<String, Object>) request.get("items");
        
        if (phone == null || itemsMap == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Both phone and items are required"
            ));
        }
        
        Map<Long, Integer> items = new HashMap<>();
        for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
            try {
                Long itemId = Long.parseLong(entry.getKey());
                Integer quantity = (entry.getValue() instanceof Integer) ? 
                    (Integer) entry.getValue() : 
                    Integer.parseInt(entry.getValue().toString());
                items.put(itemId, quantity);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid item ID or quantity format"
                ));
            }
        }
        
        return discountService.getAllApplicableDiscounts(phone, items);
    }
    
    // get all applicable discount IDs for a given product ID and customer phone number
    @PostMapping("/applicable-discount-ids")
    public ResponseEntity<Map<String, Object>> getApplicableDiscountIds(
        @RequestBody Map<String, Object> request) {
        
        try {
            String phone = (String) request.get("phone");
            @SuppressWarnings("unchecked")
            Map<String, Object> itemsMap = (Map<String, Object>) request.get("items");
            
            if (phone == null || itemsMap == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Both phone and items are required"
                ));
            }
            
            Map<Long, Integer> items = new HashMap<>();
            for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
                try {
                    Long itemId = Long.parseLong(entry.getKey());
                    Integer quantity = (entry.getValue() instanceof Integer) ? 
                        (Integer) entry.getValue() : 
                        Integer.parseInt(entry.getValue().toString());
                    items.put(itemId, quantity);
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid item ID or quantity format"
                    ));
                }
            }
            
            return ResponseEntity.ok(discountService.getApplicableDiscountIds(phone, items));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error processing request: " + e.getMessage()
            ));
        }
    }

    // get final discounted price with customer info
    @PostMapping("/calculate-total-discount")
    public ResponseEntity<Map<String, Object>> getFinalDiscountedOrderWithCustomerInfo(
        @RequestBody Map<String, Object> request) {
        
        try {
            String phone = (String) request.get("phone");
            @SuppressWarnings("unchecked")
            Map<String, Object> itemsMap = (Map<String, Object>) request.get("items");
            
            if (phone == null || itemsMap == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Both phone and items are required"
                ));
            }
            
            Map<Long, Integer> items = new HashMap<>();
            for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
                try {
                    Long itemId = Long.parseLong(entry.getKey());
                    Integer quantity = (entry.getValue() instanceof Integer) ? 
                        (Integer) entry.getValue() : 
                        Integer.parseInt(entry.getValue().toString());
                    items.put(itemId, quantity);
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid item ID or quantity format"
                    ));
                }
            }
            
            return ResponseEntity.ok(discountService.getFinalDiscountedOrderWithCustomerInfo(phone, items));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error processing request: " + e.getMessage()
            ));
        }
    }
    // save order details

   // get customer points by number
   @GetMapping("/customer-points/{phone}")
    public ResponseEntity<Map<String, Object>> getCustomerPoints(@PathVariable String phone) {
        return ResponseEntity.ok(discountService.getCustomerPointsByPhone(phone));
    }

    //update customer points by phone
    @PutMapping("/update-customer-points/{phone}")
    public ResponseEntity<Map<String, Object>> updateCustomerPoints(
        @PathVariable String phone, 
        @RequestBody Map<String, Double> request) {
        
        Double points = request.get("points");
        return ResponseEntity.ok(discountService.updateCustomerPoints(phone, points));
    }

    //get used and erned points by phone number
    @PostMapping("/calculate-points")
    public ResponseEntity<Map<String, Object>> calculatePointsUsageAndEarning(
        @RequestBody Map<String, Object> request) {
        
        try {
            String phone = (String) request.get("phone");
            @SuppressWarnings("unchecked")
            Map<String, Object> itemsMap = (Map<String, Object>) request.get("items");
            
            if (phone == null || itemsMap == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Both phone and items are required"
                ));
            }
            
            Map<Long, Integer> items = new HashMap<>();
            for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
                try {
                    Long itemId = Long.parseLong(entry.getKey());
                    Integer quantity = (entry.getValue() instanceof Integer) ? 
                        (Integer) entry.getValue() : 
                        Integer.parseInt(entry.getValue().toString());
                    items.put(itemId, quantity);
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid item ID or quantity format"
                    ));
                }
            }
            
            return ResponseEntity.ok(discountService.calculatePointsUsageAndEarning(phone, items));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error processing request: " + e.getMessage()
            ));
        }
    }

    // update customer points after calculations
    @PostMapping("/update-customer-points-after-purchase")
    public ResponseEntity<Map<String, Object>> updateCustomerPointsAfterPurchase(
        @RequestBody Map<String, Object> request) {
        
        try {
            String phone = (String) request.get("phone");
            @SuppressWarnings("unchecked")
            Map<String, Object> itemsMap = (Map<String, Object>) request.get("items");
            
            if (phone == null || itemsMap == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Both phone and items are required"
                ));
            }
            
            Map<Long, Integer> items = new HashMap<>();
            for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
                try {
                    Long itemId = Long.parseLong(entry.getKey());
                    Integer quantity = (entry.getValue() instanceof Integer) ? 
                        (Integer) entry.getValue() : 
                        Integer.parseInt(entry.getValue().toString());
                    items.put(itemId, quantity);
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid item ID or quantity format"
                    ));
                }
            }
            
            return ResponseEntity.ok(discountService.updateCustomerPointsAfterPurchase(phone, items));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error processing request: " + e.getMessage()
            ));
        }
    }

    // get complete discount and points info
    @PostMapping("/complete-discount-info")
    public ResponseEntity<Map<String, Object>> getCompleteDiscountInfo(
        @RequestBody Map<String, Object> request) {
        
        try {
            String phone = (String) request.get("phone");
            @SuppressWarnings("unchecked")
            Map<String, Object> itemsMap = (Map<String, Object>) request.get("items");
            
            if (phone == null || itemsMap == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Both phone and items are required"
                ));
            }
            
            Map<Long, Integer> items = new HashMap<>();
            for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
                try {
                    Long itemId = Long.parseLong(entry.getKey());
                    Integer quantity = (entry.getValue() instanceof Integer) ? 
                        (Integer) entry.getValue() : 
                        Integer.parseInt(entry.getValue().toString());
                    items.put(itemId, quantity);
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid item ID or quantity format"
                    ));
                }
            }
            
            // Call the service method through the discountService instance
            return ResponseEntity.ok(discountService.getCompleteDiscountAndPointsInfo(phone, items));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error processing request: " + e.getMessage()
            ));
        }
    }

    // save order details - this method is used to save order details(should call in when complete order)
    @PostMapping("/save-order-details")
    public ResponseEntity<Map<String, Object>> saveOrderDetails(
        @RequestBody Map<String, Object> request) {
        
        try {
            String phone = (String) request.get("phone");
            @SuppressWarnings("unchecked")
            Map<String, Object> itemsMap = (Map<String, Object>) request.get("items");
            
            if (phone == null || itemsMap == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Both phone and items are required"
                ));
            }
            
            // Convert String keys to Long keys
            Map<Long, Integer> items = new HashMap<>();
            for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
                try {
                    Long itemId = Long.parseLong(entry.getKey());
                    Integer quantity = (entry.getValue() instanceof Integer) ? 
                        (Integer) entry.getValue() : 
                        Integer.parseInt(entry.getValue().toString());
                    items.put(itemId, quantity);
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid item ID or quantity format"
                    ));
                }
            }
            
            return ResponseEntity.ok(discountService.saveOrderDetails(phone, items));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error saving order details: " + e.getMessage()
            ));
        }
    }

    // update loyalty tier by phone number
    @PutMapping("/update-loyalty-status/{phone}")
    public ResponseEntity<Map<String, Object>> updateCustomerLoyaltyStatus(@PathVariable String phone) {
        try {
            return ResponseEntity.ok(discountService.updateCustomerLoyaltyStatus(phone));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error processing request: " + e.getMessage()
            ));
        }
    }


}