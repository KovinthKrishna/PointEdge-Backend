package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.InvoiceDTO;
import com.eternalcoders.pointedge.dto.InvoiceItemDTO;
import com.eternalcoders.pointedge.dto.ReturnRequestDTO;
import com.eternalcoders.pointedge.dto.ReturnedItemDTO;
import com.eternalcoders.pointedge.entity.*;
import com.eternalcoders.pointedge.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ProductRepository productRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final RequestReturnRepository requestReturnRepository;

    /**
     * Handles return processing: inventory updates, history logging
     */
    @Transactional
    public void handleReturn(ReturnRequestDTO returnRequest) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(returnRequest.getInvoiceNumber())
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        double totalRefundAmount = 0.0;
        List<ReturnItem> returnItems = new ArrayList<>();

        for (ReturnedItemDTO itemDTO : returnRequest.getItems()) {
            totalRefundAmount += processReturnItem(itemDTO, invoice, returnRequest, returnItems);
        }

        RequestReturn requestReturn = new RequestReturn();
        requestReturn.setInvoice(invoice);
        requestReturn.setItems(returnItems);
        requestReturn.setRefundMethod(returnRequest.getRefundMethod());
        requestReturn.setTotalRefundAmount(totalRefundAmount);
        requestReturn.setCreatedAt(LocalDateTime.now());

        for (ReturnItem item : returnItems) {
            item.setRequestReturn(requestReturn);
        }

        requestReturnRepository.save(requestReturn);
    }

    /**
     * Placeholder for refund logic — to integrate with payment gateway
     */
    public void handleRefund(ReturnRequestDTO returnRequest) {
        // TODO: Integrate payment gateway refund logic here
        // Use returnRequest.getRefundMethod(), total amount, etc.
        System.out.println("Refund method: " + returnRequest.getRefundMethod());
        System.out.println("Amount: [will be calculated based on return record]");
        // Simulate refund response or integrate with real API
    }

    /**
     * Process individual returned item
     */
    private double processReturnItem(ReturnedItemDTO dto, Invoice invoice, ReturnRequestDTO request,
                                     List<ReturnItem> returnItems) {

        Long productId = dto.getItemId();
        if (productId == null) {
            throw new IllegalArgumentException("Return item product ID must not be null");
        }

        InvoiceItem invoiceItem = invoiceItemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new RuntimeException("Invoice item not found"));

        Product product = productRepository.findById(invoiceItem.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        int returnQty = dto.getQuantity();
        product.setStockQuantity(product.getStockQuantity() + returnQty);
        productRepository.save(product);

        double itemRefund = invoiceItem.getPrice() * returnQty;

        ReturnRecord record = new ReturnRecord();
        record.setInvoiceNumber(invoice.getInvoiceNumber());
        record.setInvoiceItemId(invoiceItem.getId());
        record.setProductId(invoiceItem.getProductId());
        record.setQuantityReturned(returnQty);
        record.setReason(dto.getReason());
        record.setRefundMethod(request.getRefundMethod());
        record.setReturnedAt(LocalDateTime.now());
        returnRecordRepository.save(record);

        ReturnItem returnItem = new ReturnItem();
        returnItem.setProductId(invoiceItem.getProductId());
        returnItem.setQuantity(returnQty);
        returnItems.add(returnItem);

        return itemRefund;
    }

    /**
     * Fetch invoice details for return UI
     */
    public InvoiceDTO fetchInvoiceDetails(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        InvoiceDTO invoiceDTO = new InvoiceDTO();
        invoiceDTO.setInvoiceNumber(invoice.getInvoiceNumber());
        invoiceDTO.setDate(invoice.getDate());
        invoiceDTO.setTotalAmount(invoice.getTotalAmount());
        invoiceDTO.setLoyaltyPoints(invoice.getLoyaltyPoints());

        List<InvoiceItemDTO> itemDTOs = invoice.getItems().stream().map(item -> {
            InvoiceItemDTO dto = new InvoiceItemDTO();
            dto.setItemId(item.getId());
            dto.setProductName(item.getProductName());
            dto.setPrice(item.getPrice());
            dto.setQuantity(item.getQuantity());
            return dto;
        }).collect(Collectors.toList());

        invoiceDTO.setItems(itemDTOs);
        return invoiceDTO;
    }
}