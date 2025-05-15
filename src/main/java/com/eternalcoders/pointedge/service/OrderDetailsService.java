package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.repository.CustomerRepository;
import com.eternalcoders.pointedge.repository.OrderDetailsRepository;
import com.eternalcoders.pointedge.repository.DiscountRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public Map<String, Map<String, Long>> getDiscountCountsByType() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Map<String, Long>> result = new HashMap<>();
        
        Map<String, LocalDateTime> timePeriods = new HashMap<>();
        timePeriods.put("last24Hours", now.minusHours(24));
        timePeriods.put("last7Days", now.minusDays(7));
        timePeriods.put("last30Days", now.minusDays(30));
        timePeriods.put("lastYear", now.minusYears(1));
        
        Map<String, Long> itemDiscountCounts = new HashMap<>();
        Map<String, Long> categoryDiscountCounts = new HashMap<>();
        Map<String, Long> loyaltyDiscountCounts = new HashMap<>();
        
        for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
            String periodName = period.getKey();
            LocalDateTime startDate = period.getValue();
            
            itemDiscountCounts.put(periodName, 
                orderDetailsRepository.countOrdersWithItemDiscountInDateRange(startDate, now));
            
            categoryDiscountCounts.put(periodName, 
                orderDetailsRepository.countOrdersWithCategoryDiscountInDateRange(startDate, now));
            
            loyaltyDiscountCounts.put(periodName, 
                orderDetailsRepository.countOrdersWithLoyaltyDiscountInDateRange(startDate, now));
        }
        
        result.put("ITEM", itemDiscountCounts);
        result.put("CATEGORY", categoryDiscountCounts);
        result.put("LOYALTY", loyaltyDiscountCounts);
        
        return result;
    }

    // count active customers by time range  
    public Map<String, Object> getCustomerCountsByTier() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> result = new HashMap<>();
        
        Long totalCustomers = orderDetailsRepository.countTotalCustomers();
        result.put("totalcustomers", totalCustomers);
        
        Map<String, LocalDateTime> timePeriods = new HashMap<>();
        timePeriods.put("last24Hours", now.minusHours(24));
        timePeriods.put("last7Days", now.minusDays(7));
        timePeriods.put("last30Days", now.minusDays(30));
        timePeriods.put("lastYear", now.minusYears(1));
        
        String[] loyaltyTiers = {"GOLD", "SILVER", "BRONZE", "NOTLOYALTY"};
        
        for (String tier : loyaltyTiers) {
            Map<String, Object> tierCounts = new HashMap<>();
            for (String period : timePeriods.keySet()) {
                tierCounts.put(period, 0L);
            }
            result.put(tier, tierCounts);
        }
        
        for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
            String periodName = period.getKey();
            LocalDateTime startDate = period.getValue();
            
            List<Object[]> counts = orderDetailsRepository.countCustomersByTierInDateRange(startDate, now);
            
            for (Object[] count : counts) {
                Object tierObj = count[0];
                String tierStr;
                
                if (tierObj == null) {
                    tierStr = "NOTLOYALTY";
                } else {
                    tierStr = tierObj.toString();
                }
                
                Long customerCount = ((Number) count[1]).longValue();
                
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
        
        Map<String, LocalDateTime> timePeriods = new HashMap<>();
        timePeriods.put("last24Hours", now.minusHours(24));
        timePeriods.put("last7Days", now.minusDays(7));
        timePeriods.put("last30Days", now.minusDays(30));
        timePeriods.put("lastYear", now.minusYears(1));
      
        String[] loyaltyTiers = {"GOLD", "SILVER", "BRONZE", "NOTLOYALTY"};
        
        for (String tier : loyaltyTiers) {
            Map<String, Object> tierCounts = new HashMap<>();
            for (String period : timePeriods.keySet()) {
                tierCounts.put(period, 0L);
            }
            result.put(tier, tierCounts);
        }
        
        Map<String, Double> totalDiscountMap = new HashMap<>();
        totalDiscountMap.put("goldtotaldiscount", 0.0);
        totalDiscountMap.put("silvertotaldiscount", 0.0);
        totalDiscountMap.put("bronzetotaldiscount", 0.0);
        result.put("totaldiscount", totalDiscountMap);
        
        for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
            String periodName = period.getKey();
            LocalDateTime startDate = period.getValue();
            
            List<Object[]> counts = orderDetailsRepository.countOrdersWithLoyaltyDiscountByTierInDateRange(startDate, now);
            
            for (Object[] count : counts) {
                Object tierObj = count[0];
                String tierStr = tierObj != null ? tierObj.toString() : "NOTLOYALTY";
                Long discountCount = ((Number) count[1]).longValue();
                
                if (result.containsKey(tierStr)) {
                    ((Map<String, Object>)result.get(tierStr)).put(periodName, discountCount);
                }
            }
            
            if (periodName.equals("lastYear")) {
                List<Object[]> discounts = orderDetailsRepository.sumLoyaltyDiscountByTierInDateRange(startDate, now);
                
                for (Object[] discount : discounts) {
                    Object tierObj = discount[0];
                    if (tierObj != null) {
                        String tierStr = tierObj.toString();
                        Double totalDiscount = ((Number) discount[1]).doubleValue();
                        
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
        
        Map<String, LocalDateTime> timePeriods = new HashMap<>();
        timePeriods.put("last24Hours", now.minusHours(24));
        timePeriods.put("last7Days", now.minusDays(7));
        timePeriods.put("last30Days", now.minusDays(30));
        timePeriods.put("lastYear", now.minusYears(1));
        
        for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
            String periodName = period.getKey();
            LocalDateTime startDate = period.getValue();
            
            List<Object[]> itemAnalytics = orderDetailsRepository.findItemDiscountAnalyticsByDateRange(startDate, now);
            
            double totalAmount = 0.0;
            double totalDiscount = 0.0;
            
            Map<String, Object> periodResult = new HashMap<>();
            
            List<Map<String, Object>> topItems = new ArrayList<>();
            int count = 0;
            for (Object[] item : itemAnalytics) {
                if (count >= 3) break;
                
                Long itemId = (Long) item[0];
                Double amount = ((Number) item[1]).doubleValue();
                Double discount = ((Number) item[2]).doubleValue();
                Long itemCount = ((Number) item[3]).longValue();
                
                String itemName = "Unknown"; 
                Optional<String> nameOpt = orderDetailsRepository.findNameById(itemId);
                if (nameOpt.isPresent()) {
                    itemName = nameOpt.get();
                }
                
                totalAmount += amount;
                totalDiscount += discount;
                
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("itemId", itemId);
                itemMap.put("itemName", itemName);
                itemMap.put("amount", amount);
                itemMap.put("discount", discount);
                itemMap.put("count", itemCount);
                
                topItems.add(itemMap);
                count++;
            }
            
            periodResult.put("totalAmount", totalAmount);
            periodResult.put("totalDiscount", totalDiscount);
            periodResult.put("topItems", topItems);
            
            result.put(periodName, periodResult);
        }
        
        return result;
    }

    // total category discount and top 3 categories
    public Map<String, Object> getCategoryDiscountAnalytics() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> result = new HashMap<>();
        
        Map<String, LocalDateTime> timePeriods = new HashMap<>();
        timePeriods.put("last24Hours", now.minusHours(24));
        timePeriods.put("last7Days", now.minusDays(7));
        timePeriods.put("last30Days", now.minusDays(30));
        timePeriods.put("lastYear", now.minusYears(1));
        
        for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
            String periodName = period.getKey();
            LocalDateTime startDate = period.getValue();
            
            List<Object[]> categoryAnalytics = orderDetailsRepository.findCategoryDiscountAnalyticsByDateRange(startDate, now);
            
            double totalAmount = 0.0;
            double totalDiscount = 0.0;
            
            Map<String, Object> periodResult = new HashMap<>();
            
            List<Map<String, Object>> topCategories = new ArrayList<>();
            int count = 0;
            for (Object[] category : categoryAnalytics) {
                if (count >= 3) break;
                
                Long categoryId = (Long) category[0];
                Double amount = ((Number) category[1]).doubleValue();
                Double discount = ((Number) category[2]).doubleValue();
                Long categoryCount = ((Number) category[3]).longValue();
               
                String categoryName = "Unknown";
                Optional<String> nameOpt = orderDetailsRepository.findCategoryNameById(categoryId);
                if (nameOpt.isPresent()) {
                    categoryName = nameOpt.get();
                }
                
                totalAmount += amount;
                totalDiscount += discount;
                
                Map<String, Object> categoryMap = new HashMap<>();
                categoryMap.put("categoryId", categoryId);
                categoryMap.put("categoryName", categoryName);
                categoryMap.put("amount", amount);
                categoryMap.put("discount", discount);
                categoryMap.put("count", categoryCount);
                
                topCategories.add(categoryMap);
                count++;
            }
            
            periodResult.put("totalAmount", totalAmount);
            periodResult.put("totalDiscount", totalDiscount);
            periodResult.put("topCategories", topCategories);
            
            result.put(periodName, periodResult);
        }
        
        return result;
    }

    // get total discount
    public Map<String, Object> getAllDiscountTotals() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> result = new HashMap<>();
        
        Map<String, LocalDateTime> timePeriods = new HashMap<>();
        timePeriods.put("last24Hours", now.minusHours(24));
        timePeriods.put("last7Days", now.minusDays(7));
        timePeriods.put("last30Days", now.minusDays(30));
        timePeriods.put("lastYear", now.minusYears(1));
        
        for (String period : timePeriods.keySet()) {
            Map<String, Double> periodTotals = new HashMap<>();
            periodTotals.put("loyaltyDiscount", 0.0);
            periodTotals.put("itemDiscount", 0.0);
            periodTotals.put("categoryDiscount", 0.0);
            periodTotals.put("totalDiscount", 0.0);
            result.put(period, periodTotals);
        }
        
        for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
            String periodName = period.getKey();
            LocalDateTime startDate = period.getValue();
            Map<String, Double> periodTotals = (Map<String, Double>) result.get(periodName);
            
            Double loyaltyDiscount = orderDetailsRepository.sumLoyaltyDiscountInDateRange(startDate, now);
            if (loyaltyDiscount != null) {
                periodTotals.put("loyaltyDiscount", loyaltyDiscount);
            }
            
            Double itemDiscount = orderDetailsRepository.sumItemDiscountInDateRange(startDate, now);
            if (itemDiscount != null) {
                periodTotals.put("itemDiscount", itemDiscount);
            }
            
            Double categoryDiscount = orderDetailsRepository.sumCategoryDiscountInDateRange(startDate, now);
            if (categoryDiscount != null) {
                periodTotals.put("categoryDiscount", categoryDiscount);
            }
            
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
        
        Map<String, LocalDateTime> timePeriods = new HashMap<>();
        timePeriods.put("last24Hours", now.minusHours(24));
        timePeriods.put("last7Days", now.minusDays(7));
        timePeriods.put("last30Days", now.minusDays(30));
        timePeriods.put("lastYear", now.minusYears(1));
        
        for (Map.Entry<String, LocalDateTime> period : timePeriods.entrySet()) {
            String periodName = period.getKey();
            LocalDateTime startDate = period.getValue();
            
            Map<String, Object> periodMetrics = new HashMap<>();
            
            Double totalPointsEarned = orderDetailsRepository.sumPointsEarnedInDateRange(startDate, now);
            periodMetrics.put("totalPointsEarned", totalPointsEarned != null ? totalPointsEarned : 0.0);
            
            Double totalAmount = orderDetailsRepository.sumTotalAmountInDateRange(startDate, now);
            periodMetrics.put("totalAmount", totalAmount != null ? totalAmount : 0.0);
            
            Double totalLoyaltyAmount = orderDetailsRepository.sumAmountWithLoyaltyDiscountInDateRange(startDate, now);
            periodMetrics.put("totalLoyaltyAmount", totalLoyaltyAmount != null ? totalLoyaltyAmount : 0.0);
            
            Double totalItemAmount = orderDetailsRepository.sumAmountWithItemDiscountInDateRange(startDate, now);
            periodMetrics.put("totalItemAmount", totalItemAmount != null ? totalItemAmount : 0.0);
            
            Double totalCategoryAmount = orderDetailsRepository.sumAmountWithCategoryDiscountInDateRange(startDate, now);
            periodMetrics.put("totalCategoryAmount", totalCategoryAmount != null ? totalCategoryAmount : 0.0);
            
            result.put(periodName, periodMetrics);
        }
        
        return result;
    }

}