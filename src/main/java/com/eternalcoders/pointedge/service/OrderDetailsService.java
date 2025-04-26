package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.OrderDetailsDTO;
import com.eternalcoders.pointedge.entity.Customer;
import com.eternalcoders.pointedge.entity.OrderDetails;
import com.eternalcoders.pointedge.repository.CustomerRepository;
import com.eternalcoders.pointedge.repository.OrderDetailsRepository;
import com.eternalcoders.pointedge.repository.DiscountRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class OrderDetailsService {

    @Autowired
    private OrderDetailsRepository orderDetailsRepository;
    
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DiscountRepository discountRepository;

    // count orders by time range

    public Map<String, Long> getOrderCounts() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> orderCounts = new HashMap<>();
        
        // Last 24 hours
        orderCounts.put("last24Hours", 
            orderDetailsRepository.countTotalOrdersInDateRange(now.minusHours(24), now));
        
        // Last 7 days
        orderCounts.put("last7Days", 
            orderDetailsRepository.countTotalOrdersInDateRange(now.minusDays(7), now));
        
        // Last 30 days
        orderCounts.put("last30Days", 
            orderDetailsRepository.countTotalOrdersInDateRange(now.minusDays(30), now));
        
        // Last year
        orderCounts.put("lastYear", 
            orderDetailsRepository.countTotalOrdersInDateRange(now.minusYears(1), now));
        
        return orderCounts;
    }
    
    // count discounts by time range

    // Add this method to your OrderDetailsService class

public Map<String, Map<String, Long>> getDiscountCountsByType() {
    LocalDateTime now = LocalDateTime.now();
    Map<String, Map<String, Long>> result = new HashMap<>();
    
    // Define time periods
    Map<String, LocalDateTime> timePeriods = new HashMap<>();
    timePeriods.put("last24Hours", now.minusHours(24));
    timePeriods.put("last7Days", now.minusDays(7));
    timePeriods.put("last30Days", now.minusDays(30));
    timePeriods.put("lastYear", now.minusYears(1));
    
    // Initialize result maps for each discount type
    Map<String, Long> itemDiscountCounts = new HashMap<>();
    Map<String, Long> categoryDiscountCounts = new HashMap<>();
    Map<String, Long> loyaltyDiscountCounts = new HashMap<>();
    
    // Populate counts for each time period
    for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
        String periodName = period.getKey();
        LocalDateTime startDate = period.getValue();
        
        // Get counts for each discount type
        itemDiscountCounts.put(periodName, 
            orderDetailsRepository.countOrdersWithItemDiscountInDateRange(startDate, now));
        
        categoryDiscountCounts.put(periodName, 
            orderDetailsRepository.countOrdersWithCategoryDiscountInDateRange(startDate, now));
        
        loyaltyDiscountCounts.put(periodName, 
            orderDetailsRepository.countOrdersWithLoyaltyDiscountInDateRange(startDate, now));
    }
    
    // Add all discount type counts to result
    result.put("ITEM", itemDiscountCounts);
    result.put("CATEGORY", categoryDiscountCounts);
    result.put("LOYALTY", loyaltyDiscountCounts);
    
    return result;
}

// count active customers by time range  
public Map<String, Object> getCustomerCountsByTier() {
    LocalDateTime now = LocalDateTime.now();
    Map<String, Object> result = new HashMap<>();
    
    // Get total customer count from Customers table
    Long totalCustomers = orderDetailsRepository.countTotalCustomers();
    result.put("totalcustomers", totalCustomers);
    
    // Define time periods
    Map<String, LocalDateTime> timePeriods = new HashMap<>();
    timePeriods.put("last24Hours", now.minusHours(24));
    timePeriods.put("last7Days", now.minusDays(7));
    timePeriods.put("last30Days", now.minusDays(30));
    timePeriods.put("lastYear", now.minusYears(1));
    
    // Define loyalty tiers
    String[] loyaltyTiers = {"GOLD", "SILVER", "BRONZE", "NOTLOYALTY"};
    
    // Initialize result maps for each tier
    for (String tier : loyaltyTiers) {
        Map<String, Object> tierCounts = new HashMap<>();
        for (String period : timePeriods.keySet()) {
            tierCounts.put(period, 0L);
        }
        result.put(tier, tierCounts);
    }
    
    // Populate counts for each time period
    for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
        String periodName = period.getKey();
        LocalDateTime startDate = period.getValue();
        
        List<Object[]> counts = orderDetailsRepository.countCustomersByTierInDateRange(startDate, now);
        
        for (Object[] count : counts) {
            // Handle the Tier enum appropriately
            Object tierObj = count[0];
            String tierStr;
            
            if (tierObj == null) {
                tierStr = "NOTLOYALTY";
            } else {
                // Convert enum to string using toString()
                tierStr = tierObj.toString();
            }
            
            Long customerCount = ((Number) count[1]).longValue();
            
            // Make sure tier exists in our results map
            if (result.containsKey(tierStr)) {
                ((Map<String, Object>)result.get(tierStr)).put(periodName, customerCount);
            } else if ("NOTLOYALTY".equals(tierStr)) {
                ((Map<String, Object>)result.get("NOTLOYALTY")).put(periodName, customerCount);
            }
        }
    }
    
    return result;
}

// add total loyalty discount amount and counts

public Map<String, Object> getLoyaltyDiscountDataByTier() {
    LocalDateTime now = LocalDateTime.now();
    Map<String, Object> result = new HashMap<>();
    
    // Define time periods
    Map<String, LocalDateTime> timePeriods = new HashMap<>();
    timePeriods.put("last24Hours", now.minusHours(24));
    timePeriods.put("last7Days", now.minusDays(7));
    timePeriods.put("last30Days", now.minusDays(30));
    timePeriods.put("lastYear", now.minusYears(1));
    
    // Define loyalty tiers
    String[] loyaltyTiers = {"GOLD", "SILVER", "BRONZE", "NOTLOYALTY"};
    
    // Initialize result maps for each tier counts
    for (String tier : loyaltyTiers) {
        Map<String, Object> tierCounts = new HashMap<>();
        for (String period : timePeriods.keySet()) {
            tierCounts.put(period, 0L);
        }
        result.put(tier, tierCounts);
    }
    
    // Initialize total discount map (changed from totalamount to totaldiscount)
    Map<String, Double> totalDiscountMap = new HashMap<>();
    totalDiscountMap.put("goldtotaldiscount", 0.0);
    totalDiscountMap.put("silvertotaldiscount", 0.0);
    totalDiscountMap.put("bronzetotaldiscount", 0.0);
    result.put("totaldiscount", totalDiscountMap);
    
    // Populate counts for each time period (unchanged)
    for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
        String periodName = period.getKey();
        LocalDateTime startDate = period.getValue();
        
        // Get loyalty discount counts by tier
        List<Object[]> counts = orderDetailsRepository.countOrdersWithLoyaltyDiscountByTierInDateRange(startDate, now);
        
        // Process the counts
        for (Object[] count : counts) {
            Object tierObj = count[0];
            String tierStr = tierObj != null ? tierObj.toString() : "NOTLOYALTY";
            Long discountCount = ((Number) count[1]).longValue();
            
            if (result.containsKey(tierStr)) {
                ((Map<String, Object>)result.get(tierStr)).put(periodName, discountCount);
            }
        }
        
        // Only calculate total discounts for the full year
        if (periodName.equals("lastYear")) {
            // Changed to use the new sumLoyaltyDiscountByTierInDateRange method
            List<Object[]> discounts = orderDetailsRepository.sumLoyaltyDiscountByTierInDateRange(startDate, now);
            
            // Process the discounts
            for (Object[] discount : discounts) {
                Object tierObj = discount[0];
                if (tierObj != null) {
                    String tierStr = tierObj.toString();
                    Double totalDiscount = ((Number) discount[1]).doubleValue();
                    
                    // Update appropriate discount in the totaldiscount map
                    String discountKey = tierStr.toLowerCase() + "totaldiscount";
                    if (totalDiscountMap.containsKey(discountKey)) {
                        totalDiscountMap.put(discountKey, totalDiscount);
                    }
                }
            }
        }
    }
    
    return result;
}

// amount of item discount and top 3 items

public Map<String, Object> getItemDiscountAnalytics() {
    LocalDateTime now = LocalDateTime.now();
    Map<String, Object> result = new HashMap<>();
    
    // Define time periods
    Map<String, LocalDateTime> timePeriods = new HashMap<>();
    timePeriods.put("last24Hours", now.minusHours(24));
    timePeriods.put("last7Days", now.minusDays(7));
    timePeriods.put("last30Days", now.minusDays(30));
    timePeriods.put("lastYear", now.minusYears(1));
    
    // For each time period, get analytics
    for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
        String periodName = period.getKey();
        LocalDateTime startDate = period.getValue();
        
        List<Object[]> itemAnalytics = orderDetailsRepository.findItemDiscountAnalyticsByDateRange(startDate, now);
        
        // Calculate total amount and total discount for this period
        double totalAmount = 0.0;
        double totalDiscount = 0.0;
        
        // Create period result map
        Map<String, Object> periodResult = new HashMap<>();
        
        // Get top 3 items (or less if fewer items exist)
        List<Map<String, Object>> topItems = new ArrayList<>();
        int count = 0;
        for (Object[] item : itemAnalytics) {
            if (count >= 3) break;
            
            Long itemId = (Long) item[0];
            Double amount = ((Number) item[1]).doubleValue();
            Double discount = ((Number) item[2]).doubleValue();
            Long itemCount = ((Number) item[3]).longValue();
            
            // Get item name from Products table
            String itemName = "Unknown"; // Default value
            Optional<String> nameOpt = orderDetailsRepository.findNameById(itemId);
            if (nameOpt.isPresent()) {
                itemName = nameOpt.get();
            }
            
            // Add to total calculations
            totalAmount += amount;
            totalDiscount += discount;
            
            // Create item map
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("itemId", itemId);
            itemMap.put("itemName", itemName);
            itemMap.put("amount", amount);
            itemMap.put("discount", discount);
            itemMap.put("count", itemCount);
            
            topItems.add(itemMap);
            count++;
        }
        
        // Add totals to period result
        periodResult.put("totalAmount", totalAmount);
        periodResult.put("totalDiscount", totalDiscount);
        periodResult.put("topItems", topItems);
        
        // Add period result to overall result
        result.put(periodName, periodResult);
    }
    
    return result;
}

// total category discount and top 3 categories

public Map<String, Object> getCategoryDiscountAnalytics() {
    LocalDateTime now = LocalDateTime.now();
    Map<String, Object> result = new HashMap<>();
    
    // Define time periods
    Map<String, LocalDateTime> timePeriods = new HashMap<>();
    timePeriods.put("last24Hours", now.minusHours(24));
    timePeriods.put("last7Days", now.minusDays(7));
    timePeriods.put("last30Days", now.minusDays(30));
    timePeriods.put("lastYear", now.minusYears(1));
    
    // For each time period, get analytics
    for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
        String periodName = period.getKey();
        LocalDateTime startDate = period.getValue();
        
        List<Object[]> categoryAnalytics = orderDetailsRepository.findCategoryDiscountAnalyticsByDateRange(startDate, now);
        
        // Calculate total amount and total discount for this period
        double totalAmount = 0.0;
        double totalDiscount = 0.0;
        
        // Create period result map
        Map<String, Object> periodResult = new HashMap<>();
        
        // Get top 3 categories (or less if fewer categories exist)
        List<Map<String, Object>> topCategories = new ArrayList<>();
        int count = 0;
        for (Object[] category : categoryAnalytics) {
            if (count >= 3) break;
            
            Long categoryId = (Long) category[0];
            Double amount = ((Number) category[1]).doubleValue();
            Double discount = ((Number) category[2]).doubleValue();
            Long categoryCount = ((Number) category[3]).longValue();
            
            // Get category name
            String categoryName = "Unknown";
            Optional<String> nameOpt = orderDetailsRepository.findCategoryNameById(categoryId);
            if (nameOpt.isPresent()) {
                categoryName = nameOpt.get();
            }
            
            // Add to total calculations
            totalAmount += amount;
            totalDiscount += discount;
            
            // Create category map
            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("categoryId", categoryId);
            categoryMap.put("categoryName", categoryName);
            categoryMap.put("amount", amount);
            categoryMap.put("discount", discount);
            categoryMap.put("count", categoryCount);
            
            topCategories.add(categoryMap);
            count++;
        }
        
        // Add totals to period result
        periodResult.put("totalAmount", totalAmount);
        periodResult.put("totalDiscount", totalDiscount);
        periodResult.put("topCategories", topCategories);
        
        // Add period result to overall result
        result.put(periodName, periodResult);
    }
    
    return result;
}

// get total discount

public Map<String, Object> getAllDiscountTotals() {
    LocalDateTime now = LocalDateTime.now();
    Map<String, Object> result = new HashMap<>();
    
    // Define time periods
    Map<String, LocalDateTime> timePeriods = new HashMap<>();
    timePeriods.put("last24Hours", now.minusHours(24));
    timePeriods.put("last7Days", now.minusDays(7));
    timePeriods.put("last30Days", now.minusDays(30));
    timePeriods.put("lastYear", now.minusYears(1));
    
    // Initialize the result structure
    for (String period : timePeriods.keySet()) {
        Map<String, Double> periodTotals = new HashMap<>();
        periodTotals.put("loyaltyDiscount", 0.0);
        periodTotals.put("itemDiscount", 0.0);
        periodTotals.put("categoryDiscount", 0.0);
        periodTotals.put("totalDiscount", 0.0);
        result.put(period, periodTotals);
    }
    
    // Populate data for each time period
    for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
        String periodName = period.getKey();
        LocalDateTime startDate = period.getValue();
        Map<String, Double> periodTotals = (Map<String, Double>) result.get(periodName);
        
        // Get loyalty discount total
        Double loyaltyDiscount = orderDetailsRepository.sumLoyaltyDiscountInDateRange(startDate, now);
        if (loyaltyDiscount != null) {
            periodTotals.put("loyaltyDiscount", loyaltyDiscount);
        }
        
        // Get item discount total
        Double itemDiscount = orderDetailsRepository.sumItemDiscountInDateRange(startDate, now);
        if (itemDiscount != null) {
            periodTotals.put("itemDiscount", itemDiscount);
        }
        
        // Get category discount total
        Double categoryDiscount = orderDetailsRepository.sumCategoryDiscountInDateRange(startDate, now);
        if (categoryDiscount != null) {
            periodTotals.put("categoryDiscount", categoryDiscount);
        }
        
        // Get total discount (sum of all discount types)
        Double totalDiscount = orderDetailsRepository.sumTotalDiscountInDateRange(startDate, now);
        if (totalDiscount != null) {
            periodTotals.put("totalDiscount", totalDiscount);
        }
    }
    
    return result;
}

// add total amounts 

public Map<String, Object> getOrderSummaryMetrics() {
    LocalDateTime now = LocalDateTime.now();
    Map<String, Object> result = new HashMap<>();
    
    // Define time periods
    Map<String, LocalDateTime> timePeriods = new HashMap<>();
    timePeriods.put("last24Hours", now.minusHours(24));
    timePeriods.put("last7Days", now.minusDays(7));
    timePeriods.put("last30Days", now.minusDays(30));
    timePeriods.put("lastYear", now.minusYears(1));
    
    // For each time period, get summary metrics
    for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
        String periodName = period.getKey();
        LocalDateTime startDate = period.getValue();
        
        Map<String, Object> periodMetrics = new HashMap<>();
        
        // 1. Total points earned
        Double totalPointsEarned = orderDetailsRepository.sumPointsEarnedInDateRange(startDate, now);
        periodMetrics.put("totalPointsEarned", totalPointsEarned != null ? totalPointsEarned : 0.0);
        
        // 2. Total amount
        Double totalAmount = orderDetailsRepository.sumTotalAmountInDateRange(startDate, now);
        periodMetrics.put("totalAmount", totalAmount != null ? totalAmount : 0.0);
        
        // 3. Total loyalty amount (sum of amounts where loyalty_discount > 0)
        Double totalLoyaltyAmount = orderDetailsRepository.sumAmountWithLoyaltyDiscountInDateRange(startDate, now);
        periodMetrics.put("totalLoyaltyAmount", totalLoyaltyAmount != null ? totalLoyaltyAmount : 0.0);
        
        // 4. Total item amount (sum of amounts where item_discount > 0)
        Double totalItemAmount = orderDetailsRepository.sumAmountWithItemDiscountInDateRange(startDate, now);
        periodMetrics.put("totalItemAmount", totalItemAmount != null ? totalItemAmount : 0.0);
        
        // 5. Total category amount (sum of amounts where category_discount > 0)
        Double totalCategoryAmount = orderDetailsRepository.sumAmountWithCategoryDiscountInDateRange(startDate, now);
        periodMetrics.put("totalCategoryAmount", totalCategoryAmount != null ? totalCategoryAmount : 0.0);
        
        // Add period metrics to result
        result.put(periodName, periodMetrics);
    }
    
    return result;
}

}