package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.*;
import com.eternalcoders.pointedge.entity.CardRefundRecord;
import com.eternalcoders.pointedge.service.ReturnProcessorService;
import com.eternalcoders.pointedge.service.ReturnService;
import com.eternalcoders.pointedge.exception.EntityNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/return-exchange")
public class ReturnExchangeController {

    private final ReturnService returnService;
    private static final Logger logger = LoggerFactory.getLogger(ReturnExchangeController.class);
    private final ValidatorFactory validatorFactory;
    private final ReturnProcessorService returnProcessorService;

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

    // ----------------- Process Return -----------------
    @PostMapping("/return")
    public ResponseEntity<String> processReturn(@RequestBody ReturnRequestDTO returnRequest) {
        try {
            logger.info("Processing return request for invoice: {}", returnRequest.getInvoiceNumber());
            returnService.handleReturn(returnRequest);
            return ResponseEntity.ok("Return processed successfully");
        } catch (Exception e) {
            logger.error("Error processing return for invoice: {}", returnRequest.getInvoiceNumber(), e);
            return ResponseEntity.status(500).body("Error processing return");
        }
    }

    // ----------------- Process Exchange -----------------
    @PostMapping("/exchange")
    public ResponseEntity<String> processExchange(@RequestBody ExchangeRequestDTO exchangeRequest) {
        try {
            logger.info("Processing exchange for invoice: {}", exchangeRequest.getInvoiceNumber());
            returnService.handleExchange(exchangeRequest);
            return ResponseEntity.ok("Exchange processed successfully");
        } catch (Exception e) {
            logger.error("Error processing exchange for invoice: {}", exchangeRequest.getInvoiceNumber(), e);
            return ResponseEntity.status(500).body("Error processing exchange");
        }
    }

    // ----------------- Process Refund (Optional) -----------------
    @PostMapping("/refund")
    public ResponseEntity<String> processRefund(@RequestBody ReturnRequestDTO returnRequest) {
        try {
            logger.info("Processing refund for invoice: {}", returnRequest.getInvoiceNumber());
            returnService.handleRefund(returnRequest);
            return ResponseEntity.ok("Refund processed successfully (stub)");
        } catch (Exception e) {
            logger.error("Error processing refund for invoice: {}", returnRequest.getInvoiceNumber(), e);
            return ResponseEntity.status(500).body("Error processing refund");
        }
    }


    @PostMapping("/card-refund")
    public ResponseEntity<String> processCardRefund(@RequestBody @Valid CardRefundRequestDTO dto) {
        try {
            logger.info("Processing card refund for invoice: {}", dto.getInvoiceNumber());
            boolean success = returnService.processCardRefund(dto);
            // or processCardRefund

            if (success) {
                return ResponseEntity.ok("Card refund processed successfully");
            } else {
                return ResponseEntity.status(500).body("Card refund processing failed");
            }
        } catch (EntityNotFoundException e) {
            logger.error("Entity not found during card refund: {}", e.getMessage());
            return ResponseEntity.status(404).body("Required data not found: " + e.getMessage());
        } catch (IllegalStateException e) {
            logger.error("Invalid state during card refund: {}", e.getMessage());
            return ResponseEntity.status(400).body("Invalid operation: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing card refund for invoice: {}", dto.getInvoiceNumber(), e);
            return ResponseEntity.status(500).body("Unexpected error: " + e.getMessage());
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
    @PostMapping("/finalize-exchange/{requestId}")
    public ResponseEntity<String> finalizeExchange(@PathVariable Long requestId) {
        try {
            returnProcessorService.processExchange(requestId);
            return ResponseEntity.ok("Exchange finalized and completed.");
        } catch (Exception e) {
            logger.error("Exchange finalization failed for request ID {}: {}", requestId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Exchange finalization failed");
        }
    }

    @PostMapping("/finalize-cash/{requestId}")
    public ResponseEntity<String> finalizeCashRefund(@PathVariable Long requestId) {
        try {
            returnProcessorService.processCashRefund(requestId);
            return ResponseEntity.ok("Cash refund finalized and completed.");
        } catch (Exception e) {
            logger.error("Cash refund finalization failed for request ID {}: {}", requestId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Cash refund finalization failed");
        }
    }

    @PostMapping("/finalize-card/{requestId}")
    public ResponseEntity<String> finalizeCardRefund(
            @PathVariable Long requestId,
            @Valid @RequestBody CardRefundRequestDTO dto
    ) {
        try {
            returnService.finalizeCardRefund(requestId, dto);
            return ResponseEntity.ok("Card refund processed");
        } catch (Exception e) {
            logger.error("Card refund failed for request ID {}: {}", requestId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Card refund failed");
        }
    }
}