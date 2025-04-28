package com.eternalcoders.pointedge.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.eternalcoders.pointedge.entity.Customer;
import com.eternalcoders.pointedge.entity.Customer.Tier;
import com.eternalcoders.pointedge.entity.LoyaltyThresholds;

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

    // below methods for integration

    @Modifying
    @Query("UPDATE Customer c SET c.points = :points WHERE c.phone = :phone")
    void updatePointsByPhone(@Param("phone") String phone, @Param("points") Double points);
    
    @Modifying
    @Query("UPDATE Customer c SET c.tier = :tier WHERE c.phone = :phone")
    void updateTierByPhone(@Param("phone") String phone, @Param("tier") Tier tier);

    // In CustomerRepository.java
@Query("SELECT " +
"SUM(CASE WHEN c.tier = 'GOLD' THEN 1 ELSE 0 END) as goldCount, " +
"SUM(CASE WHEN c.tier = 'SILVER' THEN 1 ELSE 0 END) as silverCount, " +
"SUM(CASE WHEN c.tier = 'BRONZE' THEN 1 ELSE 0 END) as bronzeCount, " +
"SUM(CASE WHEN c.tier = 'NOTLOYALTY' THEN 1 ELSE 0 END) as notLoyaltyCount " +
"FROM Customer c")
Map<String, Long> countCustomersByTier();



// fetch orders


@Query("SELECT o.orderId, COUNT(o), SUM(o.amount), SUM(o.pointsEarned), MIN(o.datetime) " +
       "FROM OrderDetails o " +
       "WHERE o.customer.phone = :phone " +
       "GROUP BY o.orderId")
List<Object[]> getOrderDetailsGroupedByOrderIdAndPhone(@Param("phone") String phone);

// update customers tiers when update settings

@Query("SELECT lt FROM LoyaltyThresholds lt WHERE lt.id = 1")
    Optional<LoyaltyThresholds> findLoyaltyThresholds();

@Modifying
@Query("UPDATE Customer c SET c.tier = CASE " +
       "WHEN c.points >= :gold THEN 'GOLD' " +
       "WHEN c.points >= :silver THEN 'SILVER' " +
       "WHEN c.points >= :bronze THEN 'BRONZE' " +
       "ELSE 'NOTLOYALTY' END")
void updateAllCustomerTiers(@Param("gold") double gold, 
                          @Param("silver") double silver, 
                          @Param("bronze") double bronze);

}