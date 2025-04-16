package com.eternalcoders.pointedge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.eternalcoders.pointedge.entity.Customer;
import com.eternalcoders.pointedge.entity.Customer.Tier;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    // Count customers
    @Query("SELECT COUNT(c) FROM Customer c")
    long countCustomers();
    
    // Combined search (name OR email OR phone)
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "c.phone LIKE CONCAT('%', :searchTerm, '%')")
    List<Customer> searchCustomers(@Param("searchTerm") String searchTerm);
    
    // In CustomerRepository
Optional<Customer> findByPhone(String phone);
boolean existsByPhone(String phone);
void deleteByPhone(String phone);
}