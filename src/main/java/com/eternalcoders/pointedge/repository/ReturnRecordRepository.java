package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.dto.ProductReturnRateDTO;
import com.eternalcoders.pointedge.entity.ReturnRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReturnRecordRepository extends JpaRepository<ReturnRecord, Long> {
    List<ReturnRecord> findByInvoiceNumber(String invoiceNumber);

    @Query("""
    SELECT new com.eternalcoders.pointedge.dto.ProductReturnRateDTO(
        p.id, p.name,
        COALESCE(SUM(CAST(r.quantityReturned AS double)), 0.0) / 
        COALESCE((SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.product.id = p.id), 1.0)
    )
    FROM ReturnRecord r
    JOIN Product p ON r.productId = p.id
    GROUP BY p.id, p.name
    """)
    List<ProductReturnRateDTO> getReturnRatesByProduct();
}
