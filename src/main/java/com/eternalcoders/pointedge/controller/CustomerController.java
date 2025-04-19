package com.eternalcoders.pointedge.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.eternalcoders.pointedge.dto.CustomerDTO;
import com.eternalcoders.pointedge.entity.Customer;
import com.eternalcoders.pointedge.entity.Customer.Tier;
import com.eternalcoders.pointedge.service.CustomerService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@CrossOrigin
@RequestMapping(value = "api/v1/discount/customer")
public class CustomerController {
    
    @Autowired
    private CustomerService customerService;
    
    @GetMapping("/get-all-customers")
    public List<CustomerDTO> getCustomersDetails() {
        return customerService.getAllCustomers();
    }
    
    @PostMapping("/add-customer")
    public CustomerDTO addCustomerDetails(@RequestBody CustomerDTO customerDTO) {
        return customerService.addCustomer(customerDTO);
    }

    @GetMapping("/get-customer/{phone}")
    public CustomerDTO getCustomerById(@PathVariable String phone) {  // Changed from Long to String
        return customerService.getCustomerById(phone);
    }

    @DeleteMapping("/delete-customer/{phone}")
    public ResponseEntity<String> deleteCustomer(@PathVariable String phone) {
        try {
            customerService.deleteByPhone(phone);
            return ResponseEntity.ok("Customer deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Customer not found: " + e.getMessage());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countCustomers() {
        long count = customerService.countCustomers();
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<CustomerDTO>> searchCustomers(@RequestParam String query) {
        List<CustomerDTO> customers = customerService.searchCustomers(query);
        return ResponseEntity.ok(customers);
    }

    @PutMapping("/update-customer/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(@PathVariable Long id, @RequestBody CustomerDTO customerDTO) {
        try {
            CustomerDTO updatedCustomer = customerService.updateCustomerById(id, customerDTO);
            return ResponseEntity.ok(updatedCustomer);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + e.getMessage(), e);
        }
    }    
    
    //////////////below methods for integration
    
    // get customer by phone
    @GetMapping("/get-customer-by-phone/{phone}")
    public ResponseEntity<Map<String, Object>> getCustomerByPhone(@PathVariable String phone) {
        CustomerDTO customer = customerService.getCustomerByPhoneNullable(phone);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", customer != null);
        response.put("customer", customer);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/update-points/{phone}")
    public ResponseEntity<CustomerDTO> updateCustomerPoints(
            @PathVariable String phone,
            @RequestParam Double points) {  // Changed from int to Double
        try {
            CustomerDTO updatedCustomer = customerService.updateCustomerPoints(phone, points);
            return ResponseEntity.ok(updatedCustomer);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + e.getMessage(), e);
        }
    }

    @PatchMapping("/update-tier/{phone}")
    public ResponseEntity<CustomerDTO> updateCustomerTier(
            @PathVariable String phone,
            @RequestParam Customer.Tier tier) {  // Using fully qualified enum name
        try {
            CustomerDTO updatedCustomer = customerService.updateCustomerTier(phone, tier);
            return ResponseEntity.ok(updatedCustomer);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + e.getMessage(), e);
        }
    }

    
    
}