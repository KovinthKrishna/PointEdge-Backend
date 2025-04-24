package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.InvoiceDTO;
import com.eternalcoders.pointedge.dto.ReturnRequestDTO;
import com.eternalcoders.pointedge.dto.ReturnedItemDTO;
import com.eternalcoders.pointedge.entity.*;
import com.eternalcoders.pointedge.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReturnService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ProductRepository productRepository;
    @Autowired
    private RequestReturnRepository requestReturnRepository;

    @Autowired
    private ReturnRecordRepository returnRecordRepository;


    public ReturnService(InvoiceRepository invoiceRepository,
                         InvoiceItemRepository invoiceItemRepository,
                         ProductRepository productRepository,
                         RequestReturnRepository requestReturnRepository,
                         ReturnRecordRepository returnRecordRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.productRepository = productRepository;
        this.requestReturnRepository = requestReturnRepository;
        this.returnRecordRepository = returnRecordRepository;
    }

    public Invoice validateInvoice(String invoiceNumber) {
        return invoiceRepository.findById(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invalid Invoice Number"));
    }

    public InvoiceDTO fetchInvoiceDetails(String invoiceNumber) {
        Invoice invoice = validateInvoice(invoiceNumber);
        List<InvoiceItem> items = invoice.getItems();

        InvoiceDTO dto = new InvoiceDTO();
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setDate(invoice.getDate());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setLoyaltyPoints(invoice.getLoyaltyPoints());

        dto.setItems(items.stream().map(item -> {
            ReturnedItemDTO returnedItem = new ReturnedItemDTO();
            returnedItem.setItemId(item.getId());
            returnedItem.setQuantity(item.getQuantity());
            return returnedItem;
        }).collect(Collectors.toList()));

        return dto;
    }

    @Transactional
    public void handleReturn(ReturnRequestDTO returnRequest) {
        Invoice invoice = validateInvoice(returnRequest.getInvoiceNumber());

        double totalRefundAmount = 0;

        // Create RequestReturn entry
        RequestReturn requestReturn = new RequestReturn();
        requestReturn.setInvoiceNumber(returnRequest.getInvoiceNumber());
        requestReturn.setReturnDate(LocalDateTime.now());
        requestReturn.setRefundMethod(returnRequest.getRefundMethod());
        requestReturn.setReason(returnRequest.getReason());

        List<ReturnItem> returnItems = new ArrayList<>();

        for (ReturnedItemDTO returnedItemDTO : returnRequest.getItems()) {
            InvoiceItem invoiceItem = invoiceItemRepository.findById(returnedItemDTO.getItemId())
                    .orElseThrow(() -> new RuntimeException("Invoice item not found"));

            Product product = productRepository.findById(invoiceItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            int returnQty = returnedItemDTO.getQuantity();
            product.setStockQuantity(product.getStockQuantity() + returnQty);
            productRepository.save(product);

            double itemRefund = invoiceItem.getPrice() * returnQty;
            totalRefundAmount += itemRefund;

            // Save individual ReturnRecord for history
            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(invoice.getInvoiceNumber());
            record.setInvoiceItemId(invoiceItem.getId());
            record.setProductId(invoiceItem.getProductId());
            record.setQuantityReturned(returnQty);
            record.setReason(returnedItemDTO.getReason());
            record.setRefundMethod(returnRequest.getRefundMethod());
            record.setReturnedAt(LocalDateTime.now());
            returnRecordRepository.save(record);

            // Add ReturnItem for RequestReturn tracking
            ReturnItem returnItem = new ReturnItem();
            returnItem.setProductId(invoiceItem.getProductId());
            returnItem.setQuantity(returnQty);
            returnItem.setRequestReturn(requestReturn); // associate with request
            returnItems.add(returnItem);
        }

        requestReturn.setRefundAmount(totalRefundAmount);
        requestReturn.setItems(returnItems); // all linked items
        requestReturnRepository.save(requestReturn);

        // Adjust loyalty points
        int pointsToDeduct = (int) (totalRefundAmount / 100);
        invoice.setLoyaltyPoints(Math.max(0, invoice.getLoyaltyPoints() - pointsToDeduct));
        invoiceRepository.save(invoice);

        // Refund customer
        refundCustomer(totalRefundAmount, returnRequest.getRefundMethod());
    }



    private void refundCustomer(double amount, String method) {
        if ("cash".equalsIgnoreCase(method)) {
            System.out.println("Refunded " + amount + " in cash.");
        } else if ("card".equalsIgnoreCase(method)) {
            System.out.println("Refunded " + amount + " to card.");
        } else {
            throw new RuntimeException("Unsupported refund method: " + method);
        }
    }
}
