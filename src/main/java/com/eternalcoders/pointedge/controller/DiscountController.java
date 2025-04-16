package com.eternalcoders.pointedge.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eternalcoders.pointedge.dto.DiscountDTO;
import com.eternalcoders.pointedge.dto.LoyaltyThresholdsDTO;
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
    
    @GetMapping("/get-all-discounts")
    public List<DiscountDTO> getDiscountsDetails() {
        return discountService.getAllDiscounts();
    }
    
    @PostMapping("/add-discount")
    public DiscountDTO addDiscountDetails(@RequestBody DiscountDTO discountDTO) {
        return discountService.addDiscount(discountDTO);
    }
        
    @PutMapping("/update-discount-by-id/{id}")
    public DiscountDTO updateDiscountDetails(
        @PathVariable Long id, 
        @RequestBody DiscountDTO discountDTO) {
        discountDTO.setId(id); // Ensure ID from path is used
        return discountService.updateDiscount(discountDTO);
    }
    
    @GetMapping("/get-discounts-by-type/{discountType}")
    public List<DiscountDTO> getDiscountsByType(@PathVariable DiscountType discountType) {
        return discountService.getDiscountsByType(discountType);
    }
        
    @GetMapping("/get-discount-by-id/{id}")
    public DiscountDTO getDiscountById(@PathVariable Long id) {
        return discountService.getDiscountById(id);
    }

    @GetMapping("/discount-names")
    public ResponseEntity<List<String>> getAllDiscountNames() {
        return ResponseEntity.ok(discountService.getAllDiscountNames());
    }
        
    @DeleteMapping("/delete-discount-by-id/{id}")
    public ResponseEntity<String> deleteDiscountById(@PathVariable Long id) {
        discountService.deleteDiscountById(id);
        return new ResponseEntity<>("Discount with ID: " + id + " deleted successfully", HttpStatus.OK);
    }

    @GetMapping("/product-names")
    public ResponseEntity<List<String>> getAllProductNames() {
        return ResponseEntity.ok(discountService.getAllProductNames());
    }
    
    @GetMapping("/category-names")
    public ResponseEntity<List<String>> getAllCategoryNames() {
        return ResponseEntity.ok(discountService.getAllCategoryNames());
    }

    // Changed from DELETE to POST to handle request body more naturally
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

    @GetMapping("/count-by-type/{discountType}")
    public ResponseEntity<Map<String, Object>> getDiscountCountByType(@PathVariable DiscountType discountType) {
        long count = discountService.countDiscountsByType(discountType);
        return ResponseEntity.ok(Map.of(
            "type", discountType,
            "count", count
        ));
    }

    @GetMapping("/loyalty-thresholds")
    public ResponseEntity<LoyaltyThresholdsDTO> getLoyaltyThresholds() {
        return ResponseEntity.ok(discountService.getLoyaltyThresholds());
    }
    
    @PutMapping("/loyalty-thresholds")
    public ResponseEntity<LoyaltyThresholdsDTO> updateLoyaltyThresholds(
        @RequestBody LoyaltyThresholdsDTO thresholdsDTO
    ) {
        return ResponseEntity.ok(discountService.updateLoyaltyThresholds(thresholdsDTO));
    }
}