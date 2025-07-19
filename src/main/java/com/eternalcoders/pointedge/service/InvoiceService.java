package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.entity.Invoice;
import com.eternalcoders.pointedge.entity.InvoiceItem;
import com.eternalcoders.pointedge.entity.Order;
import com.eternalcoders.pointedge.repository.InvoiceRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Transactional
    public Invoice createInvoiceFromOrder(Order order) {
        Invoice invoice = new Invoice();

        // Generate Invoice Number (Example: INV-UUID)
        invoice.setInvoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8));
        invoice.setDate(LocalDateTime.now());
        invoice.setTotalAmount(order.getTotal());
        invoice.setLoyaltyPoints(order.getLoyaltyPoints());
        invoice.setCustomer(null);

        List<InvoiceItem> invoiceItems = order.getOrderItems().stream().map(orderItem -> {
            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.setOrderItem(orderItem);
            invoiceItem.setInvoice(invoice);
            invoiceItem.setQuantity((int) orderItem.getQuantity());
            invoiceItem.setPrice(orderItem.getPricePerUnit());
            invoiceItem.setProductName(orderItem.getProduct().getName());
            invoiceItem.setProductId(orderItem.getProduct().getId());
            return invoiceItem;
        }).toList();

        invoice.setItems(invoiceItems);
        return invoiceRepository.save(invoice);
    }
}
