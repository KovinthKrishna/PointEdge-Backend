package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.*;
import com.eternalcoders.pointedge.service.ReturnService;
import com.eternalcoders.pointedge.exception.EntityNotFoundException;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/return-exchange")
public class ReturnExchangeController {

    private final ReturnService returnService;
    private static final Logger logger = LoggerFactory.getLogger(ReturnExchangeController.class);

    public ReturnExchangeController(ReturnService returnService) {
        this.returnService = returnService;
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
        logger.info("Processing card refund for invoice: {}", dto.getInvoiceNumber());

        boolean success = returnService.processCardRefund(dto);

        if (success) {
            return ResponseEntity.ok("Card refund simulated successfully");
        } else {
            return ResponseEntity.status(500).body("Card refund simulation failed");
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
    public ResponseEntity<?> processApproved(@PathVariable Long requestId) {
        try {
            logger.info("Finalizing refund for approved requestId: {}", requestId);
            returnService.finalizeApprovedRefund(requestId);
            return ResponseEntity.ok("Refund finalized successfully");
        } catch (Exception e) {
            logger.error("Error finalizing refund for requestId: {}", requestId, e);
            return ResponseEntity.status(500).body("Refund finalization failed");
        }
    }
}