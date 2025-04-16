//not finished
package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.entity.*;
import com.eternalcoders.pointedge.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DiscountCalculatorService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoyaltyThresholdsRepository loyaltyThresholdsRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private Order_Discount_CustomerRepository orderDiscountCustomerRepository;

    /**
     * Calculate all applicable discounts for an order
     * @param orderData Order information containing items and their quantities
     * @param customerId Optional customer ID for loyalty discounts
     * @return DiscountResult containing detailed discount information
     */
    @Transactional
    public DiscountResult calculateDiscounts(OrderData orderData, Long customerId) {
        // Initialize result
        DiscountResult result = new DiscountResult();
        result.setItemDiscounts(new ArrayList<>());
        
        double totalBeforeDiscount = 0.0;
        double totalItemDiscount = 0.0;
        double totalCategoryDiscount = 0.0;
        double totalLoyaltyDiscount = 0.0;
        
        // Get customer information if available
        Customer customer = null;
        if (customerId != null) {
            Optional<Customer> customerOpt = customerRepository.findById(customerId);
            if (customerOpt.isPresent()) {
                customer = customerOpt.get();
                result.setCustomer(customer);
            }
        }
        
        // Process each item in the order
        for (OrderItem item : orderData.getItems()) {
            ItemDiscountResult itemResult = new ItemDiscountResult();
            itemResult.setItemName(item.getName());
            itemResult.setQuantity(item.getQuantity());
            
            // Find product by name
            Product product = productRepository.findByName(item.getName());
            if (product == null) {
                // Skip items that don't exist in our system
                continue;
            }
            
            double itemPrice = product.getPrice();
            double itemTotal = itemPrice * item.getQuantity();
            totalBeforeDiscount += itemTotal;
            
            itemResult.setUnitPrice(itemPrice);
            itemResult.setTotalPrice(itemTotal);
            
            // Check for item-specific discounts
            List<Discount> itemDiscounts = discountRepository.findByItemAndIsActiveAndStartDateBefore(
                product, true, LocalDateTime.now());
            
            double bestItemDiscountValue = 0.0;
            Discount bestItemDiscount = null;
            
            for (Discount discount : itemDiscounts) {
                double discountValue = calculateDiscountValue(discount, itemTotal);
                if (discountValue > bestItemDiscountValue) {
                    bestItemDiscountValue = discountValue;
                    bestItemDiscount = discount;
                }
            }
            
            if (bestItemDiscount != null) {
                itemResult.setItemDiscount(bestItemDiscountValue);
                itemResult.setItemDiscountName(bestItemDiscount.getName());
                totalItemDiscount += bestItemDiscountValue;
            }
            
            // Check for category-specific discounts
            Category category = product.getCategory();
            if (category != null) {
                List<Discount> categoryDiscounts = discountRepository.findByCategoryAndIsActiveAndStartDateBefore(
                    category, true, LocalDateTime.now());
                
                double bestCategoryDiscountValue = 0.0;
                Discount bestCategoryDiscount = null;
                
                for (Discount discount : categoryDiscounts) {
                    double discountValue = calculateDiscountValue(discount, itemTotal);
                    if (discountValue > bestCategoryDiscountValue) {
                        bestCategoryDiscountValue = discountValue;
                        bestCategoryDiscount = discount;
                    }
                }
                
                if (bestCategoryDiscount != null) {
                    // Apply category discount only if no item discount or if it's better
                    if (bestItemDiscount == null || bestCategoryDiscountValue > bestItemDiscountValue) {
                        // If there was an item discount, subtract it first
                        if (bestItemDiscount != null) {
                            totalItemDiscount -= bestItemDiscountValue;
                            itemResult.setItemDiscount(0.0);
                            itemResult.setItemDiscountName(null);
                        }
                        
                        itemResult.setCategoryDiscount(bestCategoryDiscountValue);
                        itemResult.setCategoryDiscountName(bestCategoryDiscount.getName());
                        totalCategoryDiscount += bestCategoryDiscountValue;
                    }
                }
            }
            
            result.getItemDiscounts().add(itemResult);
        }
        
        // Apply loyalty tier discount if applicable
        if (customer != null && customer.getTier() != Customer.Tier.NOTLOYALTY) {
            List<Discount> loyaltyDiscounts = discountRepository.findByLoyaltyTypeAndIsActiveAndStartDateBefore(
                Discount.LoyaltyTier.valueOf(customer.getTier().name()), true, LocalDateTime.now());
            
            if (!loyaltyDiscounts.isEmpty()) {
                // Get the best loyalty discount
                Discount bestLoyaltyDiscount = loyaltyDiscounts.stream()
                    .max((d1, d2) -> {
                        double v1 = (d1.getPercentage() != null) ? d1.getPercentage() : 0.0;
                        double v2 = (d2.getPercentage() != null) ? d2.getPercentage() : 0.0;
                        return Double.compare(v1, v2);
                    })
                    .orElse(null);
                
                if (bestLoyaltyDiscount != null) {
                    double loyaltyDiscountValue = calculateDiscountValue(bestLoyaltyDiscount, totalBeforeDiscount);
                    totalLoyaltyDiscount = loyaltyDiscountValue;
                    result.setLoyaltyDiscount(loyaltyDiscountValue);
                    result.setLoyaltyDiscountName(bestLoyaltyDiscount.getName());
                }
            }
        }
        
        // Calculate total discount
        double totalDiscount = totalItemDiscount + totalCategoryDiscount + totalLoyaltyDiscount;
        double finalTotal = totalBeforeDiscount - totalDiscount;
        
        result.setTotalBeforeDiscount(totalBeforeDiscount);
        result.setTotalDiscount(totalDiscount);
        result.setFinalTotal(finalTotal);
        
        // Calculate loyalty points earned if customer exists
        if (customer != null) {
            LoyaltyThresholds threshold = loyaltyThresholdsRepository.findFirstByOrderByIdDesc();
            if (threshold != null) {
                double pointsEarned = calculateLoyaltyPoints(finalTotal, threshold);
                result.setLoyaltyPointsEarned(pointsEarned);
            }
        }
        
        return result;
    }
    
    /**
     * Apply the discount to the checkout process, save to database
     * @param discountResult The calculated discount result
     * @param customerId The customer ID
     * @return Updated discount result with order ID
     */
    @Transactional
    public DiscountResult applyDiscount(DiscountResult discountResult, Long customerId) {
        // Create and save order
        Order order = new Order();
        order.setTotalAmount(discountResult.getFinalTotal());
        order.setCreationDate(LocalDateTime.now());
        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer != null) {
                order.setCustomer(customer);
                
                // Update customer loyalty points
                if (discountResult.getLoyaltyPointsEarned() != null && discountResult.getLoyaltyPointsEarned() > 0) {
                    customer.setPoints(customer.getPoints() + discountResult.getLoyaltyPointsEarned());
                    customerRepository.save(customer);
                }
            }
        }
        order = orderRepository.save(order);
        discountResult.setOrderId(order.getId());
        
        // Save item discounts
        for (ItemDiscountResult itemDiscount : discountResult.getItemDiscounts()) {
            if (itemDiscount.getItemDiscount() > 0) {
                saveOrderDiscountRecord(
                    order, 
                    findDiscountByName(itemDiscount.getItemDiscountName()), 
                    customerId, 
                    itemDiscount.getItemDiscount(), 
                    0.0, 
                    0.0
                );
            }
            
            if (itemDiscount.getCategoryDiscount() > 0) {
                saveOrderDiscountRecord(
                    order, 
                    findDiscountByName(itemDiscount.getCategoryDiscountName()), 
                    customerId, 
                    0.0, 
                    itemDiscount.getCategoryDiscount(), 
                    0.0
                );
            }
        }
        
        // Save loyalty discount if any
        if (discountResult.getLoyaltyDiscount() > 0) {
            saveOrderDiscountRecord(
                order, 
                findDiscountByName(discountResult.getLoyaltyDiscountName()), 
                customerId, 
                0.0, 
                0.0, 
                discountResult.getLoyaltyDiscount()
            );
        }
        
        return discountResult;
    }
    
    /**
     * Calculate the discount value based on discount type (percentage or fixed amount)
     */
    private double calculateDiscountValue(Discount discount, double totalAmount) {
        if (discount.getPercentage() != null) {
            return (totalAmount * discount.getPercentage()) / 100.0;
        } else if (discount.getAmount() != null) {
            return Math.min(discount.getAmount(), totalAmount);
        }
        return 0.0;
    }
    
    /**
     * Calculate loyalty points based on purchase amount and threshold
     */
    private double calculateLoyaltyPoints(double purchaseAmount, LoyaltyThresholds threshold) {
        // Basic calculation: points = amount * rate
        return purchaseAmount * threshold.getPoints();
    }
    
    /**
     * Helper method to find a discount by name
     */
    private Discount findDiscountByName(String name) {
        if (name == null) return null;
        return discountRepository.findByName(name);
    }
    
    /**
     * Save discount record to database
     */
    private void saveOrderDiscountRecord(Order order, Discount discount, Long customerId, 
                                         double itemDiscount, double categoryDiscount, double loyaltyDiscount) {
        if (discount == null) return;
        
        Order_Discount_Customer record = new Order_Discount_Customer();
        record.setOrder(order);
        record.setDiscount(discount);
        
        Customer customer = null;
        if (customerId != null) {
            customer = customerRepository.findById(customerId).orElse(null);
        }
        record.setCustomer(customer);
        
        record.setDatetime(LocalDateTime.now());
        record.setAmount(itemDiscount + categoryDiscount + loyaltyDiscount);
        record.setItemDiscount(itemDiscount > 0 ? itemDiscount : null);
        record.setCategoryDiscount(categoryDiscount > 0 ? categoryDiscount : null);
        record.setLoyaltyDiscount(loyaltyDiscount > 0 ? loyaltyDiscount : null);
        
        orderDiscountCustomerRepository.save(record);
    }
    
    // Inner classes for request/response data
    
    public static class OrderData {
        private List<OrderItem> items;
        
        public List<OrderItem> getItems() {
            return items;
        }
        
        public void setItems(List<OrderItem> items) {
            this.items = items;
        }
    }
    
    public static class OrderItem {
        private String name;
        private int quantity;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
    
    public static class DiscountResult {
        private Long orderId;
        private Customer customer;
        private List<ItemDiscountResult> itemDiscounts;
        private Double totalBeforeDiscount;
        private Double totalDiscount;
        private Double loyaltyDiscount;
        private String loyaltyDiscountName;
        private Double loyaltyPointsEarned;
        private Double finalTotal;
        
        // Getters and setters
        public Long getOrderId() {
            return orderId;
        }
        
        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }
        
        public Customer getCustomer() {
            return customer;
        }
        
        public void setCustomer(Customer customer) {
            this.customer = customer;
        }
        
        public List<ItemDiscountResult> getItemDiscounts() {
            return itemDiscounts;
        }
        
        public void setItemDiscounts(List<ItemDiscountResult> itemDiscounts) {
            this.itemDiscounts = itemDiscounts;
        }
        
        public Double getTotalBeforeDiscount() {
            return totalBeforeDiscount;
        }
        
        public void setTotalBeforeDiscount(Double totalBeforeDiscount) {
            this.totalBeforeDiscount = totalBeforeDiscount;
        }
        
        public Double getTotalDiscount() {
            return totalDiscount;
        }
        
        public void setTotalDiscount(Double totalDiscount) {
            this.totalDiscount = totalDiscount;
        }
        
        public Double getLoyaltyDiscount() {
            return loyaltyDiscount;
        }
        
        public void setLoyaltyDiscount(Double loyaltyDiscount) {
            this.loyaltyDiscount = loyaltyDiscount;
        }
        
        public String getLoyaltyDiscountName() {
            return loyaltyDiscountName;
        }
        
        public void setLoyaltyDiscountName(String loyaltyDiscountName) {
            this.loyaltyDiscountName = loyaltyDiscountName;
        }
        
        public Double getLoyaltyPointsEarned() {
            return loyaltyPointsEarned;
        }
        
        public void setLoyaltyPointsEarned(Double loyaltyPointsEarned) {
            this.loyaltyPointsEarned = loyaltyPointsEarned;
        }
        
        public Double getFinalTotal() {
            return finalTotal;
        }
        
        public void setFinalTotal(Double finalTotal) {
            this.finalTotal = finalTotal;
        }
    }
    
    public static class ItemDiscountResult {
        private String itemName;
        private int quantity;
        private Double unitPrice;
        private Double totalPrice;
        private Double itemDiscount = 0.0;
        private String itemDiscountName;
        private Double categoryDiscount = 0.0;
        private String categoryDiscountName;
        
        // Getters and setters
        public String getItemName() {
            return itemName;
        }
        
        public void setItemName(String itemName) {
            this.itemName = itemName;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
        
        public Double getUnitPrice() {
            return unitPrice;
        }
        
        public void setUnitPrice(Double unitPrice) {
            this.unitPrice = unitPrice;
        }
        
        public Double getTotalPrice() {
            return totalPrice;
        }
        
        public void setTotalPrice(Double totalPrice) {
            this.totalPrice = totalPrice;
        }
        
        public Double getItemDiscount() {
            return itemDiscount;
        }
        
        public void setItemDiscount(Double itemDiscount) {
            this.itemDiscount = itemDiscount;
        }
        
        public String getItemDiscountName() {
            return itemDiscountName;
        }
        
        public void setItemDiscountName(String itemDiscountName) {
            this.itemDiscountName = itemDiscountName;
        }
        
        public Double getCategoryDiscount() {
            return categoryDiscount;
        }
        
        public void setCategoryDiscount(Double categoryDiscount) {
            this.categoryDiscount = categoryDiscount;
        }
        
        public String getCategoryDiscountName() {
            return categoryDiscountName;
        }
        
        public void setCategoryDiscountName(String categoryDiscountName) {
            this.categoryDiscountName = categoryDiscountName;
        }
    }
}