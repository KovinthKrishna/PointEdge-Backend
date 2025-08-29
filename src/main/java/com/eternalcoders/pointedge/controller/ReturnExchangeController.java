package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.*;
import com.eternalcoders.pointedge.entity.CardRefundRecord;
import com.eternalcoders.pointedge.entity.RequestReturn;
import com.eternalcoders.pointedge.mapper.RequestMapper;
import com.eternalcoders.pointedge.repository.RequestReturnRepository;
import com.eternalcoders.pointedge.service.ReturnProcessorService;
import com.eternalcoders.pointedge.service.ReturnService;
import com.eternalcoders.pointedge.exception.EntityNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/return-exchange")
public class ReturnExchangeController {

    private final ReturnService returnService;
    private static final Logger logger = LoggerFactory.getLogger(ReturnExchangeController.class);
    private final ValidatorFactory validatorFactory;
    private final ReturnProcessorService returnProcessorService;
    @Autowired
    private RequestReturnRepository requestReturnRepository;
    @Autowired
    private RequestMapper requestMapper;

    public ReturnExchangeController(ReturnService returnService, ValidatorFactory validatorFactory, ReturnProcessorService returnProcessorService) {
        this.returnService = returnService;
        this.validatorFactory = validatorFactory;
        this.returnProcessorService = returnProcessorService;
    }

    // ----------------- Fetch Invoice -----------------
    @GetMapping("/invoice/{invoiceNumber}")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable String invoiceNumber) {
        try {
            logger.info("Fetching invoice details for invoice number: {}", invoiceNumber);
            InvoiceDTO invoiceDTO = returnService.fetchInvoiceDetails(invoiceNumber);
            return ResponseEntity.ok(invoiceDTO);
        } catch (EntityNotFoundException e) {
            logger.error("Invoice not found for invoice number: {}", invoiceNumber);
            return ResponseEntity.status(404).body(null);
        }
    }


    // ----------------- Optional: View Exchange or Return History -----------------
    @GetMapping("/history/{invoiceNumber}")
    public ResponseEntity<?> getReturnExchangeHistory(@PathVariable String invoiceNumber) {
        try {
            logger.info("Fetching return/exchange history for invoice: {}", invoiceNumber);
            var history = returnService.getReturnExchangeHistory(invoiceNumber); // Should combine return + exchange data
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error fetching history for invoice: {}", invoiceNumber, e);
            return ResponseEntity.status(500).body("Error fetching return/exchange history");
        }
    }

    @PostMapping("/process-approved/{requestId}")
    public ResponseEntity<?> finalizeApprovedRefund(@PathVariable Long requestId) {
        try {
            logger.info("Processing approved refund for request ID {}", requestId);
            returnService.finalizeApprovedRefund(requestId);
            return ResponseEntity.ok("Refund finalized");
        } catch (Exception e) {
            logger.error("Refund finalization failed for request ID {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Refund finalization failed");
        }
    }

    @GetMapping("/refund-requests/invoice/{invoiceNumber}/details")
    public ResponseEntity<List<RefundRequestDetailsDTO>> getDetailedRequestsByInvoice(@PathVariable String invoiceNumber) {
        List<RefundRequestDetailsDTO> dtos = returnProcessorService.getRefundRequestsByInvoice(invoiceNumber);
        return ResponseEntity.ok(dtos);
    }


    @GetMapping("/salesperson/{salespersonId}/refund-requests")
    public ResponseEntity<List<RefundRequestDetailsDTO>> getRefundRequestsForSalesperson(
            @PathVariable Long salespersonId) {

        List<RequestReturn> requests = requestReturnRepository.findByCreatedBy_Id(salespersonId);
        List<RefundRequestDetailsDTO> dtos = requests.stream()
                .map(requestMapper::toRefundRequestDetailsDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/refund-requests/{requestId}")
    public ResponseEntity<RefundRequestDetailsDTO> getRefundRequestById(
            @PathVariable Long requestId) {

        RequestReturn request = requestReturnRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Refund request not found"));

        RefundRequestDetailsDTO dto = requestMapper.toRefundRequestDetailsDTO(request);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/cancel-request/{requestId}")
    public ResponseEntity<?> cancelRefundRequest(@PathVariable Long requestId) {
        try {
            returnProcessorService.cancelRefundRequest(requestId);
            return ResponseEntity.ok("Refund request cancelled successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @DeleteMapping("/return-exchange/delete-request/{requestId}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long requestId) {
        try {
            returnService.deleteRequestById(requestId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete request: " + e.getMessage());
        }
    }

    @PostMapping("/finalize-exchange/{requestId}")
    public ResponseEntity<?> finalizeExchange(@PathVariable Long requestId) {
        try {
            returnProcessorService.processExchange(requestId);
            return ResponseEntity.ok("Exchange finalized successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Exchange finalization failed.");
        }
    }

    @PostMapping("/finalize-cash/{requestId}")
    public ResponseEntity<?> finalizeCashRefund(@PathVariable Long requestId) {
        try {
            returnProcessorService.processCashRefund(requestId);
            return ResponseEntity.ok("Cash refund finalized successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Cash refund finalization failed.");
        }
    }

    @PostMapping("/finalize-card/{requestId}")
    public ResponseEntity<?> finalizeCardRefund(
            @PathVariable Long requestId,
            @RequestBody CardRefundRequestDTO dto) {
        try {
            returnProcessorService.simulateCardRefund(dto);
            return ResponseEntity.ok("Card refund processed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Card refund failed.");
        }
    }

}