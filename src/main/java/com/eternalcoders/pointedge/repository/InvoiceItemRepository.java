package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.InvoiceItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    @Query("""
SELECT ii FROM InvoiceItem ii
JOIN ii.orderItem oi
JOIN oi.product p
WHERE ii.invoice.invoiceNumber = :invoiceNumber AND p.id = :productId
""")
    Optional<InvoiceItem> findByInvoiceNumberAndProductId(@Param("invoiceNumber") String invoiceNumber,
                                                          @Param("productId") Long productId);

    @Query("""
SELECT ii FROM InvoiceItem ii
WHERE ii.invoice.invoiceNumber = :invoiceNumber AND ii.productId = :productId
""")
    Optional<InvoiceItem> findByInvoiceNumberAndProductIdFromDirectField(@Param("invoiceNumber") String invoiceNumber,
                                                                         @Param("productId") Long productId);




}