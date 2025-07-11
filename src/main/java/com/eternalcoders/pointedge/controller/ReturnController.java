package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.InvoiceDTO;
import com.eternalcoders.pointedge.dto.ReturnRequestDTO;
import com.eternalcoders.pointedge.service.ReturnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    // Fetch invoice details
    @GetMapping("/invoice/{invoiceNumber}")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable String invoiceNumber) {
        InvoiceDTO invoiceDTO = returnService.fetchInvoiceDetails(invoiceNumber);
        return ResponseEntity.ok(invoiceDTO);
    }

    // Process return request (stock update, history)
    @PostMapping("/process")
    public ResponseEntity<String> processReturn(@RequestBody ReturnRequestDTO returnRequest) {
        returnService.handleReturn(returnRequest); // Handles inventory and logs
        return ResponseEntity.ok("Return processed successfully");
    }

    // (Optional) Refund trigger — for future payment gateway integration
    @PostMapping("/refund")
    public ResponseEntity<String> processRefund(@RequestBody ReturnRequestDTO returnRequest) {
        returnService.handleRefund(returnRequest); // Placeholder for actual refund logic
        return ResponseEntity.ok("Refund processed (stub)");
    }
}