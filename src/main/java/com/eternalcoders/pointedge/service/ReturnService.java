package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.*;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final ReturnProcessorService returnProcessorService;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final RequestReturnRepository requestReturnRepository;
    private final CardRefundRecordRepository cardRefundRecordRepository;
    private final CustomerRepository customerRepository;

    private static final Logger logger = LoggerFactory.getLogger(ReturnService.class);

    @Transactional
    public void handleReturn(ReturnRequestDTO returnRequest) {
        logger.info("Delegating return processing for invoice: {}", returnRequest.getInvoiceNumber());
        returnProcessorService.processReturn(
                returnRequest.getItems(),
                returnRequest.getInvoiceNumber(),
                returnRequest.getRefundMethod()
        );
        logger.info("Return processed successfully.");
    }

    @Transactional
    public void handleRefund(ReturnRequestDTO returnRequest) {
        String refundMethod = returnRequest.getRefundMethod();
        String invoiceNumber = returnRequest.getInvoiceNumber();

        if ("Exchange".equalsIgnoreCase(refundMethod)) {
            returnProcessorService.processExchange(
                    returnRequest.getItems(),
                    invoiceNumber
            );
        } else {
            returnProcessorService.processReturn(
                    returnRequest.getItems(),
                    invoiceNumber,
                    refundMethod
            );
        }
    }

    public InvoiceDTO fetchInvoiceDetails(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        InvoiceDTO invoiceDTO = new InvoiceDTO();
        invoiceDTO.setInvoiceNumber(invoice.getInvoiceNumber());
        invoiceDTO.setDate(invoice.getDate());
        invoiceDTO.setTotalAmount(invoice.getTotalAmount());
        invoiceDTO.setLoyaltyPoints(invoice.getLoyaltyPoints());

        List<InvoiceItemDTO> itemDTOs = invoice.getItems().stream().map(item -> {
            InvoiceItemDTO dto = new InvoiceItemDTO();
            dto.setItemId(item.getId());           // InvoiceItem.id
            dto.setProductId(item.getProductId()); // Product.id - ADD THIS
            dto.setProductName(item.getProductName());
            dto.setPrice(item.getPrice());
            dto.setQuantity(item.getQuantity());
            return dto;
        }).collect(Collectors.toList());

        invoiceDTO.setItems(itemDTOs);
        return invoiceDTO;
    }

    public boolean processCardRefund(CardRefundRequestDTO dto) {
        return returnProcessorService.simulateCardRefund(dto);
    }

    public void handleExchange(ExchangeRequestDTO exchangeRequest) {
        returnProcessorService.processExchange(
                exchangeRequest.getReturnedItems(),
                exchangeRequest.getInvoiceNumber()
        );
    }

    public List<ReturnRecord> getReturnExchangeHistory(String invoiceNumber) {
        return returnProcessorService.fetchReturnExchangeHistory(invoiceNumber);
    }

    public void finalizeApprovedRefund(Long requestId) {
        returnProcessorService.processApprovedRefund(requestId);
    }

    @Transactional
    public void finalizeCardRefund(Long requestId, CardRefundRequestDTO dto) {
        RequestReturn request = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getStatus().equals(RequestStatus.APPROVED)) {
            throw new RuntimeException("Refund not approved by admin");
        }

        // Optionally save bank info directly in request (if needed)
        request.setStatus(RequestStatus.COMPLETED);
        requestReturnRepository.save(request);
    }

}