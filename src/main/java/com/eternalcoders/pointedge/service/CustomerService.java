package com.eternalcoders.pointedge.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eternalcoders.pointedge.dto.CustomerDTO;
import com.eternalcoders.pointedge.dto.LoyaltyThresholdsDTO;
import com.eternalcoders.pointedge.entity.Customer;
import com.eternalcoders.pointedge.entity.Customer.Tier;
import com.eternalcoders.pointedge.entity.LoyaltyThresholds;
import com.eternalcoders.pointedge.repository.CustomerRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CustomerService {
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private ModelMapper modelMapper;
    
    public List<CustomerDTO> getAllCustomers() {
        List<Customer> customersList = customerRepository.findAll();
        return modelMapper.map(customersList, new TypeToken<List<CustomerDTO>>(){}.getType());
    }
    
    public CustomerDTO addCustomer(CustomerDTO customerDTO) {
        Customer customer = modelMapper.map(customerDTO, Customer.class);
        Customer savedCustomer = customerRepository.save(customer);
        return modelMapper.map(savedCustomer, CustomerDTO.class);
    }
    
    public CustomerDTO getCustomerById(String phone) {
        Customer customer = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Customer not found with phone: " + phone));
        return modelMapper.map(customer, CustomerDTO.class);
    }

    public void deleteByPhone(String phone) {
        Customer customer = customerRepository.findByPhone(phone)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
        customerRepository.delete(customer);
    }

    public long countCustomers() {
        return customerRepository.countCustomers();
    }
    
    public List<CustomerDTO> searchCustomers(String searchTerm) {
        List<Customer> customers = customerRepository.searchCustomers(searchTerm);
        return modelMapper.map(customers, new TypeToken<List<CustomerDTO>>(){}.getType());
    }
    
    public CustomerDTO updateCustomerById(Long id, CustomerDTO customerDTO) {
        Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        
        // Configure ModelMapper to skip ID during update
        modelMapper.getConfiguration()
            .setSkipNullEnabled(true)
            .setMatchingStrategy(MatchingStrategies.STRICT);
        
        // Map all fields except ID
        modelMapper.map(customerDTO, existingCustomer);
        
        Customer updatedCustomer = customerRepository.save(existingCustomer);
        return modelMapper.map(updatedCustomer, CustomerDTO.class);
    }
    
    //////////////below methods for integration
    // get customer by phone
    public CustomerDTO getCustomerByPhoneNullable(String phone) {
        Optional<Customer> customer = customerRepository.findByPhone(phone);
        return customer.map(c -> modelMapper.map(c, CustomerDTO.class)).orElse(null);
    }

    public CustomerDTO updateCustomerPoints(String phone, Double points) {
        // Find the customer first to ensure it exists
        Customer existing = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        // Create a custom repository method to update points directly in the database
        // This is a new method we'll add to CustomerRepository interface
        customerRepository.updatePointsByPhone(phone, points);
        
        // Fetch the updated customer from the database
        Customer updated = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Customer not found after update"));
                
        return modelMapper.map(updated, CustomerDTO.class);
    }
    
    public CustomerDTO updateCustomerTier(String phone, Tier tier) {
        // Find the customer first to ensure it exists
        Customer existing = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        // Create a custom repository method to update tier directly in the database
        // This is a new method we'll add to CustomerRepository interface
        customerRepository.updateTierByPhone(phone, tier);
        
        // Fetch the updated customer from the database
        Customer updated = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Customer not found after update"));
                
        return modelMapper.map(updated, CustomerDTO.class);
    }

    //update customer tier by phone

    // In CustomerService.java
// In CustomerService.java
public Map<Tier, Long> countCustomersByTier() {
    Map<String, Long> results = customerRepository.countCustomersByTier();
    Map<Tier, Long> tierCounts = new EnumMap<>(Tier.class);
    
    tierCounts.put(Tier.GOLD, results.get("goldCount"));
    tierCounts.put(Tier.SILVER, results.get("silverCount"));
    tierCounts.put(Tier.BRONZE, results.get("bronzeCount"));
    tierCounts.put(Tier.NOTLOYALTY, results.get("notLoyaltyCount"));
    
    return tierCounts;
}

// find tier by phone

// In CustomerService.java
public Tier getCustomerTierByPhone(String phone) {
    Customer customer = customerRepository.findByPhone(phone)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
    return (Tier) customer.getTier();
}

// fetch orders


public List<Map<String, Object>> getOrderDetailsGroupedByOrderIdAndPhone(String phone) {
    // First check if customer exists
    Customer customer = customerRepository.findByPhone(phone)
            .orElseThrow(() -> new RuntimeException("Customer not found with phone: " + phone));
            
    List<Object[]> results = customerRepository.getOrderDetailsGroupedByOrderIdAndPhone(phone);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm:ssa");
    
    return results.stream().map(row -> {
        Map<String, Object> map = new LinkedHashMap<>();
        LocalDateTime datetime = (LocalDateTime) row[4];
        String formattedDateTime = datetime.format(formatter);
        
        map.put("orderId", row[0]);
        map.put("itemCount", row[1]);
        map.put("totalAmount", row[2]);
        map.put("totalPointsEarned", row[3]);
        map.put("orderDateTime", formattedDateTime);
        return map;
    }).collect(Collectors.toList());
}

// update customers tiers when update settings

public LoyaltyThresholdsDTO getLoyaltyThresholds() {
        LoyaltyThresholds thresholds = customerRepository.findLoyaltyThresholds()
            .orElseThrow(() -> new RuntimeException("Loyalty thresholds not found"));
        return modelMapper.map(thresholds, LoyaltyThresholdsDTO.class);
    }

public void updateAllCustomerTiers() {
    LoyaltyThresholdsDTO thresholds = getLoyaltyThresholds();
    customerRepository.updateAllCustomerTiers(
        thresholds.getGold(),
        thresholds.getSilver(),
        thresholds.getBronze()
    );
}

}