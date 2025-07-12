package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.CardRefundRequestDTO;
import com.eternalcoders.pointedge.dto.ReturnedItemDTO;
import com.eternalcoders.pointedge.entity.*;
import com.eternalcoders.pointedge.enums.RequestStatus;
import com.eternalcoders.pointedge.exception.EntityNotFoundException;
import com.eternalcoders.pointedge.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private static final Logger logger = LoggerFactory.getLogger(ReturnProcessorService.class);

    public List<ReturnRecord> fetchReturnExchangeHistory(String invoiceNumber) {
        return returnRecordRepository.findByInvoiceNumber(invoiceNumber);
    }

    @Transactional
    public void processReturn(List<ReturnedItemDTO> returnedItems, String invoiceNumber, String refundMethod) {
        logger.info("Processing return for invoice: {}", invoiceNumber);

        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        double totalRefundAmount = 0.0;
        List<ReturnItem> returnItems = new ArrayList<>();

        for (ReturnedItemDTO itemDTO : returnedItems) {
            totalRefundAmount += processSingleItem(itemDTO, invoice, refundMethod, returnItems, true);
        }

        deductLoyaltyPoints(invoice, totalRefundAmount);

        RequestReturn requestReturn = new RequestReturn();
        requestReturn.setInvoice(invoice);
        requestReturn.setItems(returnItems);
        requestReturn.setRefundMethod(refundMethod);
        requestReturn.setTotalRefundAmount(totalRefundAmount);
        requestReturn.setCreatedAt(LocalDateTime.now());

        for (ReturnItem item : returnItems) {
            item.setRequestReturn(requestReturn);
        }

        requestReturnRepository.save(requestReturn);
    }


    private double processSingleItem(ReturnedItemDTO dto, Invoice invoice, String refundMethod,
                                     List<ReturnItem> returnItems, boolean deductLoyalty) {
        Long itemId = dto.getItemId();
        if (itemId == null) {
            throw new IllegalArgumentException("Return item ID must not be null");
        }

        InvoiceItem invoiceItem = invoiceItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice item not found"));

        Product product = productRepository.findById(invoiceItem.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        int returnQty = dto.getQuantity();
        product.setStockQuantity(product.getStockQuantity() + returnQty);
        productRepository.save(product);

        invoiceItem.setQuantity(invoiceItem.getQuantity() - returnQty);
        invoiceItemRepository.save(invoiceItem);

        double itemRefund = invoiceItem.getPrice() * returnQty;
        invoice.setTotalAmount(invoice.getTotalAmount() - itemRefund);
        invoiceRepository.save(invoice);

        ReturnRecord record = new ReturnRecord();
        record.setInvoiceNumber(invoice.getInvoiceNumber());
        record.setInvoiceItemId(invoiceItem.getId());
        record.setProductId(invoiceItem.getProductId());
        record.setQuantityReturned(returnQty);
        record.setReason(dto.getReason());
        record.setRefundMethod(refundMethod);
        record.setReturnedAt(LocalDateTime.now());
        returnRecordRepository.save(record);

        ReturnItem returnItem = new ReturnItem();
        returnItem.setProductId(invoiceItem.getProductId());
        returnItem.setQuantity(returnQty);
        returnItems.add(returnItem);

        return itemRefund;
    }

    private void deductLoyaltyPoints(Invoice invoice, double totalRefundAmount) {
        logger.info("Deducting loyalty points for invoice: {}", invoice.getInvoiceNumber());
        if (invoice.getLoyaltyPoints() > 0) {
            double pointsToDeduct = totalRefundAmount / 10;
            double remainingPoints = invoice.getLoyaltyPoints() - pointsToDeduct;
            if (remainingPoints < 0) remainingPoints = 0;
            loyaltyService.updateLoyaltyPoints(invoice.getCustomerId(), remainingPoints);
            logger.info("Loyalty points updated to: {}", remainingPoints);
        } else {
            logger.warn("No loyalty points to deduct.");
        }
    }

    @Transactional
    public boolean simulateCardRefund(CardRefundRequestDTO dto) {
        logger.info("Simulating card refund for invoice: {}", dto.getInvoiceNumber());

        Invoice invoice = invoiceRepository.findByInvoiceNumber(dto.getInvoiceNumber())
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        double totalRefundAmount = 0.0;
        List<ReturnItem> returnItems = new ArrayList<>();

        for (ReturnedItemDTO itemDTO : dto.getItems()) {
            totalRefundAmount += processSingleItem(itemDTO, invoice, "Card", returnItems, true);
        }

        // Save return request
        RequestReturn requestReturn = new RequestReturn();
        requestReturn.setInvoice(invoice);
        requestReturn.setItems(returnItems);
        requestReturn.setRefundMethod("Card");
        requestReturn.setTotalRefundAmount(totalRefundAmount);
        requestReturn.setCreatedAt(LocalDateTime.now());

        for (ReturnItem item : returnItems) {
            item.setRequestReturn(requestReturn);
        }

        requestReturnRepository.save(requestReturn);

        // Save card refund record
        CardRefundRecord record = new CardRefundRecord();
        record.setInvoiceNumber(dto.getInvoiceNumber());
        record.setAmount(dto.getTotalAmount());
        record.setAccountHolderName(dto.getAccountHolderName());
        record.setBankName(dto.getBankName());
        record.setAccountNumber(dto.getAccountNumber());
        record.setCreatedAt(LocalDateTime.now());
        record.setStatus("SUCCESS"); // Simulated success

        cardRefundRecordRepository.save(record);

        deductLoyaltyPoints(invoice, totalRefundAmount);

        return true;
    }

    @Transactional
    public void processExchange(List<ReturnedItemDTO> returnedItems, String invoiceNumber) {
        logger.info("Processing exchange (log-only) for invoice: {}", invoiceNumber);

        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        List<ReturnItem> returnItems = new ArrayList<>();

        for (ReturnedItemDTO dto : returnedItems) {
            Long itemId = dto.getItemId();
            int returnQty = dto.getQuantity();

            InvoiceItem invoiceItem = invoiceItemRepository.findById(itemId)
                    .orElseThrow(() -> new EntityNotFoundException("Invoice item not found"));

            Product product = invoiceItem.getOrderItem().getProduct();


            // Log ReturnRecord
            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(invoice.getInvoiceNumber());
            record.setInvoiceItemId(invoiceItem.getId());
            record.setProductId(product.getId());
            record.setQuantityReturned(returnQty);
            record.setReason(dto.getReason());
            record.setRefundMethod("Exchange");
            record.setReturnedAt(LocalDateTime.now());
            record.setReplacementProduct(product); // Same product
            returnRecordRepository.save(record);

            // Log for RequestReturn
            ReturnItem returnItem = new ReturnItem();
            returnItem.setProductId(product.getId());
            returnItem.setQuantity(returnQty);
            returnItem.setRefundAmount(0.0); // No cash refund
            returnItem.setReason(dto.getReason());
            returnItems.add(returnItem);
        }

        RequestReturn requestReturn = new RequestReturn();
        requestReturn.setInvoice(invoice);
        requestReturn.setItems(returnItems);
        requestReturn.setRefundMethod("Exchange");
        requestReturn.setTotalRefundAmount(0.0);
        requestReturn.setCreatedAt(LocalDateTime.now());

        for (ReturnItem item : returnItems) {
            item.setRequestReturn(requestReturn);
        }

        requestReturnRepository.save(requestReturn);

        logger.info("Exchange log-only operation completed for invoice {}", invoiceNumber);
    }

    @Transactional
    public void initiateRefundRequest(List<ReturnedItemDTO> returnedItems, String invoiceNumber, String refundMethod) {
        logger.info("Initiating refund request for invoice: {}", invoiceNumber);

        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        List<ReturnItem> returnItems = new ArrayList<>();

        for (ReturnedItemDTO itemDTO : returnedItems) {
            ReturnItem returnItem = new ReturnItem();
            returnItem.setProductId(itemDTO.getItemId());
            returnItem.setQuantity(itemDTO.getQuantity());
            returnItem.setReason(itemDTO.getReason());
            returnItems.add(returnItem);
        }

        RequestReturn requestReturn = new RequestReturn();
        requestReturn.setInvoice(invoice);
        requestReturn.setItems(returnItems);
        requestReturn.setRefundMethod(refundMethod);
        requestReturn.setTotalRefundAmount(
                returnItems.stream().mapToDouble(item -> {
                    InvoiceItem itemEntity = invoiceItemRepository.findById(item.getProductId())
                            .orElseThrow(() -> new EntityNotFoundException("Invoice item not found"));
                    return itemEntity.getPrice() * item.getQuantity();
                }).sum()
        );
        requestReturn.setCreatedAt(LocalDateTime.now());
        requestReturn.setStatus(RequestStatus.valueOf("PENDING"));

        for (ReturnItem item : returnItems) {
            item.setRequestReturn(requestReturn);
        }

        requestReturnRepository.save(requestReturn);
        logger.info("Refund request logged as PENDING.");
    }

    @Transactional
    public void processApprovedRefund(Long requestId) {
        RequestReturn request = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Refund request not found"));

        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new IllegalStateException("Refund request must be APPROVED to process.");
        }

        Invoice invoice = request.getInvoice();
        double totalRefundAmount = request.getTotalRefundAmount();

        for (ReturnItem item : request.getItems()) {
            Long itemId = item.getProductId();
            int returnQty = item.getQuantity();

            InvoiceItem invoiceItem = invoiceItemRepository.findById(itemId)
                    .orElseThrow(() -> new EntityNotFoundException("Invoice item not found"));

            Product product = productRepository.findById(invoiceItem.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));

            product.setStockQuantity(product.getStockQuantity() + returnQty);
            productRepository.save(product);

            invoiceItem.setQuantity(invoiceItem.getQuantity() - returnQty);
            invoiceItemRepository.save(invoiceItem);

            ReturnRecord record = new ReturnRecord();
            record.setInvoiceNumber(invoice.getInvoiceNumber());
            record.setInvoiceItemId(invoiceItem.getId());
            record.setProductId(product.getId());
            record.setQuantityReturned(returnQty);
            record.setReason(item.getReason());
            record.setRefundMethod(request.getRefundMethod());
            record.setReturnedAt(LocalDateTime.now());
            returnRecordRepository.save(record);
        }

        invoice.setTotalAmount(invoice.getTotalAmount() - totalRefundAmount);
        invoiceRepository.save(invoice);

        deductLoyaltyPoints(invoice, totalRefundAmount);

        logger.info("Approved refund processed for request ID {}", requestId);
    }
}