package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.*;
import com.eternalcoders.pointedge.entity.*;
import com.eternalcoders.pointedge.enums.RequestStatus;
import com.eternalcoders.pointedge.exception.EntityNotFoundException;
import com.eternalcoders.pointedge.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnProcessorService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ProductRepository productRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final RequestReturnRepository requestReturnRepository;
    private final LoyaltyService loyaltyService;
    private final CardRefundRecordRepository cardRefundRecordRepository;
    private final CustomerRepository customerRepository;
    private final ReturnItemRepository returnItemRepository;

    @Autowired
    private ApplicationContext context; // Used for proxy-based method call

    @PersistenceContext
    private EntityManager entityManager; // Optional: for forced flush in debug

    public List<ReturnRecord> fetchReturnExchangeHistory(String invoiceNumber) {
        return returnRecordRepository.findByInvoiceNumber(invoiceNumber);
    }

    @Transactional
    public boolean simulateCardRefund(CardRefundRequestDTO dto) {
        log.info("Simulating card refund for invoice: {}", dto.getInvoiceNumber());

        Invoice invoice = invoiceRepository.findByInvoiceNumber(dto.getInvoiceNumber())
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        double totalRefundAmount = 0.0;
        List<ReturnItem> returnItems = new ArrayList<>();

        for (ReturnedItemDTO itemDTO : dto.getItems()) {
            Product product = productRepository.findById(itemDTO.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));

            InvoiceItem invoiceItem = invoiceItemRepository
                    .findByInvoiceNumberAndProductId(invoice.getInvoiceNumber(), product.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Invoice item not found"));

            double refundAmount = itemDTO.getQuantity() * itemDTO.getUnitPrice();

            ReturnItem returnItem = new ReturnItem();
            returnItem.setInvoiceItem(invoiceItem);
            returnItem.setProduct(product);
            returnItem.setQuantity(itemDTO.getQuantity());
            returnItem.setRefundAmount(refundAmount);
            returnItem.setPhotoPath(itemDTO.getPhotoPath());
            returnItem.setReason(itemDTO.getReason());
            returnItems.add(returnItem);

            totalRefundAmount += refundAmount;
        }

        RequestReturn requestReturn = new RequestReturn();
        requestReturn.setInvoice(invoice);
        requestReturn.setItems(returnItems);
        requestReturn.setRefundMethod("Card");
        requestReturn.setStatus(RequestStatus.COMPLETED);
        requestReturn.setCreatedAt(LocalDateTime.now());
        requestReturn.setTotalRefundAmount(totalRefundAmount);

        for (ReturnItem item : returnItems) {
            item.setRequestReturn(requestReturn);
        }

        requestReturnRepository.save(requestReturn);
        returnItemRepository.saveAll(returnItems);

        CardRefundRecord record = new CardRefundRecord();
        record.setInvoiceNumber(dto.getInvoiceNumber());
        record.setAmount(totalRefundAmount);
        record.setAccountHolderName(dto.getAccountHolderName());
        record.setBankName(dto.getBankName());
        record.setAccountNumber(dto.getAccountNumber());
        record.setCreatedAt(LocalDateTime.now());
        record.setStatus("SUCCESS");

        cardRefundRecordRepository.save(record);

        // ✅ Use proxy to invoke transactional method
        context.getBean(ReturnProcessorService.class).processApprovedRefund(requestReturn.getId());
        return true;
    }

    @Transactional
    public void initiateRefundRequest(List<ReturnedItemDTO> items, String invoiceNumber, String refundMethod) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        double totalRefundAmount = 0.0;
        List<ReturnItem> returnItems = new ArrayList<>();

        for (ReturnedItemDTO itemDTO : items) {
            InvoiceItem invoiceItem = invoiceItemRepository.findById(itemDTO.getInvoiceItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Invoice item not found"));

            Product product = invoiceItem.getOrderItem().getProduct();

            ReturnItem returnItem = new ReturnItem();
            returnItem.setProduct(product);
            returnItem.setQuantity(itemDTO.getQuantity());
            returnItem.setRefundAmount(itemDTO.getRefundAmount());
            returnItem.setReason(itemDTO.getReason());
            returnItem.setPhotoPath(itemDTO.getPhotoPath());
            returnItem.setInvoiceItem(invoiceItem);
            returnItems.add(returnItem);

            totalRefundAmount += itemDTO.getRefundAmount();
        }

        RequestReturn requestReturn = new RequestReturn();
        requestReturn.setInvoice(invoice);
        requestReturn.setItems(returnItems);
        requestReturn.setRefundMethod(refundMethod != null ? refundMethod : "Card");
        requestReturn.setStatus(RequestStatus.PENDING);
        requestReturn.setCreatedAt(LocalDateTime.now());
        requestReturn.setTotalRefundAmount(totalRefundAmount);

        for (ReturnItem item : returnItems) {
            item.setRequestReturn(requestReturn);
        }

        requestReturnRepository.save(requestReturn);
        returnItemRepository.saveAll(returnItems);
    }

    @Transactional
    public void processApprovedRefund(Long requestId) {
        RequestReturn request = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Refund request not found"));

        if (request.getStatus() != RequestStatus.APPROVED && request.getStatus() != RequestStatus.COMPLETED) {
            throw new IllegalStateException("Refund request must be APPROVED or COMPLETED to process.");
        }

        String method = request.getRefundMethod();
        Invoice invoice = request.getInvoice();
        double totalRefundAmount = 0.0;

        for (ReturnItem item : request.getItems()) {
            Product product = item.getProduct();
            InvoiceItem invoiceItem = item.getInvoiceItem();

            if (invoiceItem == null) {
                invoiceItem = invoiceItemRepository
                        .findByInvoiceNumberAndProductId(invoice.getInvoiceNumber(), product.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Invoice item not found"));
            }

            double unitPrice = invoiceItem.getPrice() != null ? invoiceItem.getPrice() : 0.0;
            double itemRefund = unitPrice * item.getQuantity();
            totalRefundAmount += itemRefund;

            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(invoice.getInvoiceNumber());
            record.setInvoiceItemId(invoiceItem.getId());
            record.setProductId(product.getId());
            record.setQuantityReturned(item.getQuantity());
            record.setReason(item.getReason());
            record.setRefundMethod(method);
            record.setReturnedAt(LocalDateTime.now());

            if ("Exchange".equalsIgnoreCase(method)) {
                record.setReplacementProduct(product);
            }

            returnRecordRepository.save(record);

            // Only modify stock and invoice for Cash/Card
            if ("Cash".equalsIgnoreCase(method) || "Card".equalsIgnoreCase(method)) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);

                int remainingQty = invoiceItem.getQuantity() - item.getQuantity();
                if (remainingQty < 0) {
                    throw new IllegalStateException("Returned quantity exceeds purchased quantity");
                }

                invoiceItem.setQuantity(remainingQty);
                invoiceItemRepository.save(invoiceItem);
            }
        }

        if ("Cash".equalsIgnoreCase(method) || "Card".equalsIgnoreCase(method)) {
            invoice.setTotalAmount(invoice.getTotalAmount() - totalRefundAmount);
            invoiceRepository.save(invoice);

            double currentPoints = invoice.getLoyaltyPoints() != null ? invoice.getLoyaltyPoints() : 0.0;
            double newPoints = Math.max(0, currentPoints - totalRefundAmount / 10);
            loyaltyService.updateLoyaltyPoints(invoice.getCustomerId(), newPoints);
        }

        request.setStatus(RequestStatus.COMPLETED);
        request.setReviewedAt(LocalDateTime.now());
        requestReturnRepository.save(request);

        log.info("Refund processed and completed for request id {} using method {}", requestId, method);
    }

    @Transactional
    public void processExchange(Long requestId) {
        RequestReturn requestReturn = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found with ID: " + requestId));

        if (requestReturn.getStatus() != RequestStatus.APPROVED) {
            throw new IllegalStateException("Refund request must be APPROVED to process exchange.");
        }

        for (ReturnItem item : requestReturn.getItems()) {
            if (item.getInvoiceItem() == null) {
                // Log and skip this item
                log.error("ReturnItem with id {} has null invoiceItem", item.getId());
                continue;
            }

            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(item.getInvoiceItem().getInvoice().getInvoiceNumber());
            record.setInvoiceItemId(item.getInvoiceItem().getId());
            record.setProductId(item.getProduct().getId());
            record.setQuantityReturned(item.getQuantity());
            record.setReason(item.getReason());
            record.setRefundMethod("EXCHANGE");
            record.setReturnedAt(LocalDateTime.now());

            // Replacement is same product for exchange
            record.setReplacementProduct(item.getProduct());

            returnRecordRepository.save(record);
        }

        requestReturn.setStatus(RequestStatus.COMPLETED);
        requestReturnRepository.save(requestReturn);
    }

    @Transactional
    public void markAsRejected(Long requestId) {
        RequestReturn request = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Refund request not found"));

        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new IllegalStateException("Already processed refund request cannot be rejected.");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        requestReturnRepository.save(request);
    }

    @Transactional
    public void processReturn(List<ReturnedItemDTO> items, String invoiceNumber, String refundMethod) {
        initiateRefundRequest(items, invoiceNumber, refundMethod);
    }

    @Transactional
    public void approveRefundRequest(Long requestId) {
        RequestReturn request = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Refund request is not in PENDING state.");
        }

        request.setStatus(RequestStatus.APPROVED);
        requestReturnRepository.save(request);
    }

    @Transactional
    public void processExchange(List<ReturnedItemDTO> returnedItems, String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        List<ReturnItem> returnItems = new ArrayList<>();

        for (ReturnedItemDTO dto : returnedItems) {
            Product product = productRepository.findById(dto.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));

            InvoiceItem invoiceItem = invoiceItemRepository
                    .findByInvoiceNumberAndProductId(invoiceNumber, product.getId())
                    .orElse(null); // Could be null if no matching invoice item

            ReturnItem returnItem = new ReturnItem();
            returnItem.setProduct(product);
            returnItem.setQuantity(dto.getQuantity());
            returnItem.setRefundAmount(0.0);  // No refund amount for exchange
            returnItem.setReason(dto.getReason());
            returnItem.setPhotoPath(dto.getPhotoPath());
            returnItem.setInvoiceItem(invoiceItem);
            returnItems.add(returnItem);
        }

        RequestReturn requestReturn = new RequestReturn();
        requestReturn.setInvoice(invoice);
        requestReturn.setItems(returnItems);
        requestReturn.setRefundMethod("Exchange");
        requestReturn.setStatus(RequestStatus.PENDING);  // Or COMPLETED depending on your flow
        requestReturn.setCreatedAt(LocalDateTime.now());
        requestReturn.setTotalRefundAmount(0.0); // No refund amount for exchange

        for (ReturnItem item : returnItems) {
            item.setRequestReturn(requestReturn);
        }

        requestReturnRepository.save(requestReturn);
        returnItemRepository.saveAll(returnItems);
    }

    // ReturnProcessorService.java



    @Transactional
    public void processCashRefund(Long requestId) {
        RequestReturn request = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found with ID: " + requestId));

        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new IllegalStateException("Refund request must be APPROVED to process refund.");
        }

        Invoice invoice = request.getInvoice();
        double totalRefundAmount = 0.0;

        for (ReturnItem item : request.getItems()) {
            Product product = item.getProduct();
            InvoiceItem invoiceItem = item.getInvoiceItem();

            if (product == null || invoiceItem == null) {
                log.error("Missing product or invoiceItem for ReturnItem id: {}", item.getId());
                continue;
            }

            // 1. Stock increase
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);

            // 2. Invoice quantity decrease
            invoiceItem.setQuantity(invoiceItem.getQuantity() - item.getQuantity());
            invoiceItemRepository.save(invoiceItem);

            // 3. Create return record
            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(invoice.getInvoiceNumber());
            record.setInvoiceItemId(invoiceItem.getId());
            record.setProductId(product.getId());
            record.setQuantityReturned(item.getQuantity());
            record.setReason(item.getReason());
            record.setRefundMethod("CASH");
            record.setReturnedAt(LocalDateTime.now());

            returnRecordRepository.save(record);

            totalRefundAmount += item.getQuantity() * item.getUnitPrice();
        }

        // 4. Adjust invoice total
        invoice.setTotalAmount(invoice.getTotalAmount() - totalRefundAmount);
        invoiceRepository.save(invoice);

        // 5. Adjust loyalty points
        double currentPoints = invoice.getLoyaltyPoints() != null ? invoice.getLoyaltyPoints() : 0.0;
        double newPoints = Math.max(0, currentPoints - totalRefundAmount / 10);
        loyaltyService.updateLoyaltyPoints(invoice.getCustomerId(), newPoints);

        request.setStatus(RequestStatus.COMPLETED);
        request.setReviewedAt(LocalDateTime.now());
        requestReturnRepository.save(request);
    }

    @Transactional
    public void processCardRefund(Long requestId, CardRefundRecord cardInfo) {
        RequestReturn request = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found with ID: " + requestId));

        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new IllegalStateException("Refund request must be APPROVED to process refund.");
        }

        Invoice invoice = request.getInvoice();
        double totalRefundAmount = 0.0;

        for (ReturnItem item : request.getItems()) {
            Product product = item.getProduct();
            InvoiceItem invoiceItem = item.getInvoiceItem();

            if (product == null || invoiceItem == null) {
                log.error("Missing product or invoiceItem for ReturnItem id: {}", item.getId());
                continue;
            }

            // 1. Stock increase
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);

            // 2. Invoice quantity decrease
            invoiceItem.setQuantity(invoiceItem.getQuantity() - item.getQuantity());
            invoiceItemRepository.save(invoiceItem);

            // 3. Create return record
            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(invoice.getInvoiceNumber());
            record.setInvoiceItemId(invoiceItem.getId());
            record.setProductId(product.getId());
            record.setQuantityReturned(item.getQuantity());
            record.setReason(item.getReason());
            record.setRefundMethod("CARD");
            record.setReturnedAt(LocalDateTime.now());

            returnRecordRepository.save(record);

            totalRefundAmount += item.getQuantity() * item.getUnitPrice();
        }

        // 4. Save card refund record
        cardInfo.setInvoiceNumber(invoice.getInvoiceNumber());
        cardInfo.setAmount(totalRefundAmount);
        cardInfo.setStatus("COMPLETED");
        cardInfo.setCreatedAt(LocalDateTime.now());

        cardRefundRecordRepository.save(cardInfo);

        // 5. Adjust invoice total
        invoice.setTotalAmount(invoice.getTotalAmount() - totalRefundAmount);
        invoiceRepository.save(invoice);

        // 6. Adjust loyalty points
        double currentPoints = invoice.getLoyaltyPoints() != null ? invoice.getLoyaltyPoints() : 0.0;
        double newPoints = Math.max(0, currentPoints - totalRefundAmount / 10);
        loyaltyService.updateLoyaltyPoints(invoice.getCustomerId(), newPoints);

        request.setStatus(RequestStatus.COMPLETED);
        request.setReviewedAt(LocalDateTime.now());
        requestReturnRepository.save(request);
    }


}