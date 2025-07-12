package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.ReturnRequestDTO;
import com.eternalcoders.pointedge.entity.RequestReturn;
import com.eternalcoders.pointedge.enums.RequestStatus;
import com.eternalcoders.pointedge.repository.RequestReturnRepository;
import com.eternalcoders.pointedge.service.ReturnProcessorService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/refund-requests")
@RequiredArgsConstructor
public class AdminReturnReviewController {

    private final RequestReturnRepository requestReturnRepository;
    private final ReturnProcessorService returnProcessorService;

    // Get all pending refund requests
    @GetMapping("/pending")
    public List<RequestReturn> getPendingRequests() {
        return requestReturnRepository.findByStatus(RequestStatus.valueOf("PENDING"));
    }

    // Approve a refund request
    @PostMapping("/{id}/approve")
    public ResponseEntity<String> approveRequest(@PathVariable Long id) {
        RequestReturn request = requestReturnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("RequestReturn not found"));

        request.setStatus(RequestStatus.valueOf("APPROVED"));
        requestReturnRepository.save(request);

        return ResponseEntity.ok("Refund request approved.");
    }

    // Reject a refund request
    @PostMapping("/{id}/reject")
    public ResponseEntity<String> rejectRequest(@PathVariable Long id) {
        RequestReturn request = requestReturnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("RequestReturn not found"));

        request.setStatus(RequestStatus.valueOf("REJECTED"));
        requestReturnRepository.save(request);

        return ResponseEntity.ok("Refund request rejected.");
    }

    @PostMapping("/initiate")
    public ResponseEntity<String> initiateRefund(@RequestBody ReturnRequestDTO dto) {
        returnProcessorService.initiateRefundRequest(dto.getItems(), dto.getInvoiceNumber(), dto.getRefundMethod());
        return ResponseEntity.ok("Refund request submitted for admin approval.");
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<String> processApprovedRefund(@PathVariable Long id) {
        returnProcessorService.processApprovedRefund(id);
        return ResponseEntity.ok("Approved refund processed successfully.");
    }
}