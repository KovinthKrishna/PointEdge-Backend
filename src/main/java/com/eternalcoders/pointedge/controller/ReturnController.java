package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.service.ReturnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.eternalcoders.pointedge.dto.InvoiceDTO;
import com.eternalcoders.pointedge.dto.ReturnRequestDTO;


@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }


    @GetMapping("/invoice/{invoiceNumber}")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(returnService.fetchInvoiceDetails(invoiceNumber));
    }

    @PostMapping("/items")
    public ResponseEntity<String> processReturn(@RequestBody ReturnRequestDTO returnRequest) {
        returnService.handleReturn(returnRequest);
        return ResponseEntity.ok("Return processed successfully");
    }
}

