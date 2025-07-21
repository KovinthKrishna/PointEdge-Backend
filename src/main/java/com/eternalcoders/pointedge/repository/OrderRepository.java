package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT COUNT(o) FROM Order o WHERE o.employeeId = :employeeId")
    Long countTotalOrdersByEmployee(@Param("employeeId") Long employeeId);}
