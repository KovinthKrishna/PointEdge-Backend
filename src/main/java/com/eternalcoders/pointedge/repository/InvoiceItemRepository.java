package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.dto.*;
import com.eternalcoders.pointedge.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoice_InvoiceNumber(String invoiceNumber);

    @Query("SELECT new com.eternalcoders.pointedge.dto.TopSellingProductDTO(i.productId, i.productName, SUM(i.quantity)) " +
            "FROM InvoiceItem i GROUP BY i.productId, i.productName ORDER BY SUM(i.quantity) DESC")
    List<TopSellingProductDTO> findTopSellingProducts();

    @Query("SELECT new com.eternalcoders.pointedge.dto.DailySalesDTO(i.invoice.date, SUM(i.price * i.quantity)) " +
            "FROM InvoiceItem i GROUP BY i.invoice.date ORDER BY i.invoice.date")
    List<DailySalesDTO> getDailySales();

    @Query("SELECT new com.eternalcoders.pointedge.dto.ProductRevenueDTO(i.productId, i.productName, SUM(i.price * i.quantity)) " +
            "FROM InvoiceItem i GROUP BY i.productId, i.productName ORDER BY SUM(i.price * i.quantity) DESC")
    List<ProductRevenueDTO> getRevenueByProduct();

    @Query("""
       SELECT new com.eternalcoders.pointedge.dto.ProductReturnRateDTO(
           i.productId, i.productName,
           COALESCE(SUM(r.quantityReturned), 0.0) / SUM(CAST(i.quantity AS double))
       )
       FROM InvoiceItem i
       LEFT JOIN ReturnRecord r ON i.id = r.invoiceItemId
       GROUP BY i.productId, i.productName
       """)
    List<ProductReturnRateDTO> getReturnRatesByProduct();

    @Query("""
           SELECT new com.eternalcoders.pointedge.dto.CategoryDistributionDTO(
               p.category, COUNT(i.id)
           )
           FROM InvoiceItem i
           JOIN Product p ON i.productId = p.id
           GROUP BY p.category
           """)
    List<CategoryDistributionDTO> getProductCategoryDistribution();
}