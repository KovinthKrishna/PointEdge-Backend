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
    private final EmployeeRepository employeeRepository;

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
    public void initiateRefundRequest(List<ReturnedItemDTO> items, String invoiceNumber, String refundMethod, long createdById) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
        Employee creator = employeeRepository.findById(createdById)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

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
        requestReturn.setCreatedBy(creator);
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
            record.setRefundAmount(item.getRefundAmount());
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
    public void processReturn(List<ReturnedItemDTO> items, String invoiceNumber, String refundMethod,long createdById) {
        initiateRefundRequest(items, invoiceNumber, refundMethod,createdById);
    }

    @Transactional
    public void approveRefundRequest(Long requestId, long adminId) {
        RequestReturn request = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found"));
        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Refund request is not in PENDING state.");
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedBy(admin);
        requestReturnRepository.save(request);
    }



    @Transactional
    public void processExchangeByInvoiceNumber(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new IllegalArgumentException("Invoice number must not be null or empty.");
        }

        RequestReturn requestReturn = requestReturnRepository
                .findTopByInvoice_InvoiceNumberOrderByCreatedAtDesc(invoiceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found with invoice: " + invoiceNumber));

        if (requestReturn.getStatus() != RequestStatus.APPROVED) {
            throw new IllegalStateException("Refund request must be APPROVED to process exchange.");
        }

        for (ReturnItem item : requestReturn.getItems()) {
            if (item.getInvoiceItem() == null || item.getProduct() == null) {
                log.error("Missing invoiceItem or product for ReturnItem id: {}", item.getId());
                continue;
            }

            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(invoiceNumber);
            record.setInvoiceItemId(item.getInvoiceItem().getId());
            record.setProductId(item.getProduct().getId());
            record.setQuantityReturned(item.getQuantity());
            record.setReason(item.getReason());
            record.setRefundMethod("EXCHANGE");
            record.setReturnedAt(LocalDateTime.now());
            record.setRefundAmount(item.getRefundAmount());
            record.setReplacementProduct(item.getProduct());

            returnRecordRepository.save(record);
        }

        requestReturn.setStatus(RequestStatus.COMPLETED);
        requestReturn.setReviewedAt(LocalDateTime.now());
        requestReturnRepository.save(requestReturn);
    }

    @Transactional
    public void processCashRefundByInvoiceNumber(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new IllegalArgumentException("Invoice number must not be null or empty.");
        }

        RequestReturn request = requestReturnRepository
                .findTopByInvoice_InvoiceNumberOrderByCreatedAtDesc(invoiceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found with invoice: " + invoiceNumber));

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

            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);

            invoiceItem.setQuantity(invoiceItem.getQuantity() - item.getQuantity());
            invoiceItemRepository.save(invoiceItem);

            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(invoice.getInvoiceNumber());
            record.setInvoiceItemId(invoiceItem.getId());
            record.setProductId(product.getId());
            record.setQuantityReturned(item.getQuantity());
            record.setReason(item.getReason());
            record.setRefundMethod("CASH");
            record.setReturnedAt(LocalDateTime.now());
            record.setRefundAmount(item.getRefundAmount());

            returnRecordRepository.save(record);

            totalRefundAmount += item.getRefundAmount();
        }

        invoice.setTotalAmount(invoice.getTotalAmount() - totalRefundAmount);
        invoiceRepository.save(invoice);

        double currentPoints = invoice.getLoyaltyPoints() != null ? invoice.getLoyaltyPoints() : 0.0;
        double newPoints = Math.max(0, currentPoints - totalRefundAmount / 10);
        loyaltyService.updateLoyaltyPoints(invoice.getCustomerId(), newPoints);

        request.setStatus(RequestStatus.COMPLETED);
        request.setReviewedAt(LocalDateTime.now());
        requestReturnRepository.save(request);
    }

    @Transactional
    public void finalizeCardRefundByInvoiceNumber(String invoiceNumber, CardRefundRequestDTO dto) {
        RequestReturn request = requestReturnRepository
                .findTopByInvoice_InvoiceNumberOrderByCreatedAtDesc(invoiceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found with invoice: " + invoiceNumber));

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

            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);

            invoiceItem.setQuantity(invoiceItem.getQuantity() - item.getQuantity());
            invoiceItemRepository.save(invoiceItem);

            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(invoice.getInvoiceNumber());
            record.setInvoiceItemId(invoiceItem.getId());
            record.setProductId(product.getId());
            record.setQuantityReturned(item.getQuantity());
            record.setReason(item.getReason());
            record.setRefundMethod("CARD");
            record.setReturnedAt(LocalDateTime.now());
            record.setRefundAmount(item.getRefundAmount());

            returnRecordRepository.save(record);

            totalRefundAmount += item.getRefundAmount();
        }

        CardRefundRecord cardRecord = new CardRefundRecord();
        cardRecord.setInvoiceNumber(invoice.getInvoiceNumber());
        cardRecord.setAmount(totalRefundAmount);
        cardRecord.setAccountHolderName(dto.getAccountHolderName());
        cardRecord.setBankName(dto.getBankName());
        cardRecord.setAccountNumber(dto.getAccountNumber());
        cardRecord.setCreatedAt(LocalDateTime.now());
        cardRecord.setStatus("COMPLETED");

        cardRefundRecordRepository.save(cardRecord);

        invoice.setTotalAmount(invoice.getTotalAmount() - totalRefundAmount);
        invoiceRepository.save(invoice);

        double currentPoints = invoice.getLoyaltyPoints() != null ? invoice.getLoyaltyPoints() : 0.0;
        double newPoints = Math.max(0, currentPoints - totalRefundAmount / 10);
        loyaltyService.updateLoyaltyPoints(invoice.getCustomerId(), newPoints);

        request.setStatus(RequestStatus.COMPLETED);
        request.setReviewedAt(LocalDateTime.now());
        requestReturnRepository.save(request);
    }

    public List<RefundRequestDetailsDTO> getRefundRequestsByInvoice(String invoiceNumber) {
        List<RequestReturn> requests = requestReturnRepository.findAllByInvoice_InvoiceNumberOrderByCreatedAtDesc(invoiceNumber);

        List<RefundRequestDetailsDTO> dtos = new ArrayList<>();
        for (RequestReturn request : requests) {
            RefundRequestDetailsDTO dto = new RefundRequestDetailsDTO();
            dto.setRequestId(request.getId());
            dto.setInvoiceNumber(request.getInvoice().getInvoiceNumber());
            dto.setRefundMethod(request.getRefundMethod());
            dto.setTotalRefundAmount(request.getTotalRefundAmount());
            dto.setCreatedAt(request.getCreatedAt());
            dto.setReviewedAt(request.getReviewedAt());
            dto.setStatus(request.getStatus());

            if (request.getCustomer() != null) {
                dto.setCustomerId(request.getCustomer().getId());
                dto.setCustomerName((String) request.getCustomer().getName());
            }

            List<ReturnedItemDTO> itemDTOs = new ArrayList<>();
            for (ReturnItem item : request.getItems()) {
                ReturnedItemDTO itemDTO = new ReturnedItemDTO();
                itemDTO.setItemId(item.getProduct().getId());
                itemDTO.setProductName(item.getProduct().getName());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setUnitPrice(item.getRefundAmount() / item.getQuantity()); // approximate
                itemDTO.setRefundAmount(item.getRefundAmount());
                itemDTO.setReason(item.getReason());
                itemDTO.setPhotoPath(item.getPhotoPath());
                itemDTOs.add(itemDTO);
            }
            dto.setItems(itemDTOs);

            dtos.add(dto);
        }

        return dtos;
    }
    @Transactional
    public void cancelRefundRequest(Long requestId) {
        RequestReturn request = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Refund request not found"));

        if (request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.REJECTED) {
            throw new IllegalStateException("Cannot cancel a request that is already completed or rejected.");
        }

        request.setStatus(RequestStatus.valueOf("CANCELLED"));
        request.setReviewedAt(LocalDateTime.now());
        requestReturnRepository.save(request);
    }

    @Transactional
    public void processCashRefund(Long requestId) {
        RequestReturn request = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found with ID: " + requestId));

        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new IllegalStateException("Refund request must be APPROVED to process cash refund.");
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

            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);

            invoiceItem.setQuantity(invoiceItem.getQuantity() - item.getQuantity());
            invoiceItemRepository.save(invoiceItem);

            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(invoice.getInvoiceNumber());
            record.setInvoiceItemId(invoiceItem.getId());
            record.setProductId(product.getId());
            record.setQuantityReturned(item.getQuantity());
            record.setReason(item.getReason());
            record.setRefundMethod("CASH");
            record.setReturnedAt(LocalDateTime.now());
            record.setRefundAmount(item.getRefundAmount());

            returnRecordRepository.save(record);

            totalRefundAmount += item.getRefundAmount();
        }

        invoice.setTotalAmount(invoice.getTotalAmount() - totalRefundAmount);
        invoiceRepository.save(invoice);

        double currentPoints = invoice.getLoyaltyPoints() != null ? invoice.getLoyaltyPoints() : 0.0;
        double newPoints = Math.max(0, currentPoints - totalRefundAmount / 10);
        loyaltyService.updateLoyaltyPoints(invoice.getCustomerId(), newPoints);

        request.setStatus(RequestStatus.COMPLETED);
        request.setReviewedAt(LocalDateTime.now());
        requestReturnRepository.save(request);
    }

    @Transactional
    public void finalizeCardRefund(Long requestId, CardRefundRequestDTO dto) {
        RequestReturn request = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found with ID: " + requestId));

        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new IllegalStateException("Refund request must be APPROVED to process card refund.");
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

            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);

            invoiceItem.setQuantity(invoiceItem.getQuantity() - item.getQuantity());
            invoiceItemRepository.save(invoiceItem);

            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(invoice.getInvoiceNumber());
            record.setInvoiceItemId(invoiceItem.getId());
            record.setProductId(product.getId());
            record.setQuantityReturned(item.getQuantity());
            record.setReason(item.getReason());
            record.setRefundMethod("CARD");
            record.setReturnedAt(LocalDateTime.now());
            record.setRefundAmount(item.getRefundAmount());

            returnRecordRepository.save(record);

            totalRefundAmount += item.getRefundAmount();
        }

        CardRefundRecord cardRecord = new CardRefundRecord();
        cardRecord.setInvoiceNumber(invoice.getInvoiceNumber());
        cardRecord.setAmount(totalRefundAmount);
        cardRecord.setAccountHolderName(dto.getAccountHolderName());
        cardRecord.setBankName(dto.getBankName());
        cardRecord.setAccountNumber(dto.getAccountNumber());
        cardRecord.setCreatedAt(LocalDateTime.now());
        cardRecord.setStatus("COMPLETED");

        cardRefundRecordRepository.save(cardRecord);

        invoice.setTotalAmount(invoice.getTotalAmount() - totalRefundAmount);
        invoiceRepository.save(invoice);

        double currentPoints = invoice.getLoyaltyPoints() != null ? invoice.getLoyaltyPoints() : 0.0;
        double newPoints = Math.max(0, currentPoints - totalRefundAmount / 10);
        loyaltyService.updateLoyaltyPoints(invoice.getCustomerId(), newPoints);

        request.setStatus(RequestStatus.COMPLETED);
        request.setReviewedAt(LocalDateTime.now());
        requestReturnRepository.save(request);
    }

    @Transactional
    public void processExchange(Long requestId) {
        RequestReturn requestReturn = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found with ID: " + requestId));

        if (requestReturn.getStatus() != RequestStatus.APPROVED) {
            throw new IllegalStateException("Refund request must be APPROVED to process exchange.");
        }

        for (ReturnItem item : requestReturn.getItems()) {
            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(requestReturn.getInvoice().getInvoiceNumber());
            record.setInvoiceItemId(item.getInvoiceItem().getId());
            record.setProductId(item.getProduct().getId());
            record.setQuantityReturned(item.getQuantity());
            record.setReason(item.getReason());
            record.setRefundMethod("EXCHANGE");
            record.setReturnedAt(LocalDateTime.now());
            record.setRefundAmount(item.getRefundAmount());
            record.setReplacementProduct(item.getProduct());

            returnRecordRepository.save(record);
        }

        requestReturn.setStatus(RequestStatus.COMPLETED);
        requestReturn.setReviewedAt(LocalDateTime.now());
        requestReturnRepository.save(requestReturn);
    }


}