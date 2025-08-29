package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.RefundRequestViewDTO;
import com.eternalcoders.pointedge.dto.ReturnedItemDTO;
import com.eternalcoders.pointedge.entity.RequestReturn;
import com.eternalcoders.pointedge.enums.RequestStatus;
import com.eternalcoders.pointedge.repository.RequestReturnRepository;
import com.eternalcoders.pointedge.service.ImageStorageService;
import com.eternalcoders.pointedge.service.ReturnProcessorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;

@CrossOrigin(origins = {"http://localhost:5173"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/admin/refund-requests")
@RequiredArgsConstructor
public class AdminReturnReviewController {

    private final RequestReturnRepository requestReturnRepository;
    private final ReturnProcessorService returnProcessorService;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;

    @GetMapping("/pending")
    public ResponseEntity<List<RefundRequestViewDTO>> getPendingRequests() {
        List<RequestReturn> pendingRequests = requestReturnRepository.findByStatus(RequestStatus.PENDING);

        List<RefundRequestViewDTO> dtoList = pendingRequests.stream().map(req -> {
            RefundRequestViewDTO dto = new RefundRequestViewDTO();
            dto.setId(req.getId());
            dto.setInvoiceNumber(req.getInvoice().getInvoiceNumber());
            dto.setCustomerId(req.getInvoice().getCustomerId());
            dto.setRefundMethod(req.getRefundMethod());
            dto.setTotalRefundAmount(req.getTotalRefundAmount());
            dto.setCreatedAt(req.getCreatedAt());

            List<ReturnedItemDTO> returnedItems = req.getItems().stream().map(item -> {
                ReturnedItemDTO itemDTO = new ReturnedItemDTO();
                itemDTO.setItemId(item.getProduct().getId());
                itemDTO.setInvoiceItemId(item.getInvoiceItem() != null ? item.getInvoiceItem().getId() : null);
                itemDTO.setProductName(item.getProduct().getName());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setReason(item.getReason());
                itemDTO.setPhotoPath(item.getPhotoPath());
                itemDTO.setUnitPrice(item.getRefundAmount() / Math.max(item.getQuantity(), 1));
                itemDTO.setRefundAmount(item.getRefundAmount());
                return itemDTO;
            }).toList();

            dto.setItems(returnedItems);
            return dto;
        }).toList();

        return ResponseEntity.ok(dtoList);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/approve-request")
    public ResponseEntity<?> approveRequest(@RequestParam Long requestId,@RequestParam Long adminId) {
        returnProcessorService.approveRefundRequest(requestId,adminId);
        return ResponseEntity.ok("Approved");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reject-request")
    public ResponseEntity<String> rejectRequest(@RequestParam Long requestId) {
        returnProcessorService.markAsRejected(requestId);
        return ResponseEntity.ok("Refund request rejected.");
    }

    @PostMapping("/submit-refund-request")
    public ResponseEntity<String> submitRefundRequest(
            @RequestParam Long employeeId,
            @RequestPart("data") String jsonData,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        RefundRequestWrapper wrapper = objectMapper.readValue(jsonData, RefundRequestWrapper.class);
        List<ReturnedItemDTO> items = wrapper.getItems();

        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                if (i < images.size()) {
                    String path = imageStorageService.saveImage(images.get(i));
                    items.get(i).setPhotoPath(path);
                }
            }
        }

        returnProcessorService.initiateRefundRequest(items, wrapper.getInvoiceNumber(), wrapper.getRefundMethod(), employeeId);
        return ResponseEntity.ok("Refund request submitted for admin review.");
    }

    @GetMapping("/status/{invoiceNumber}")
    public ResponseEntity<String> getRequestStatus(@PathVariable String invoiceNumber) {
        RequestReturn request = requestReturnRepository.findTopByInvoice_InvoiceNumberOrderByCreatedAtDesc(invoiceNumber)
                .orElseThrow(() -> new EntityNotFoundException("Refund request not found"));

        return ResponseEntity.ok(request.getStatus().name());
    }

    @GetMapping("/image/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path imagePath = Paths.get("uploads/return-images").resolve(filename).normalize();
            Resource resource = new UrlResource(imagePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "image/jpeg";
            if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // DTO wrapper for multipart JSON+images
    private static class RefundRequestWrapper {
        private String invoiceNumber;
        private String refundMethod;
        private List<ReturnedItemDTO> items;

        public String getInvoiceNumber() { return invoiceNumber; }
        public String getRefundMethod() { return refundMethod; }
        public List<ReturnedItemDTO> getItems() { return items; }

        public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
        public void setRefundMethod(String refundMethod) { this.refundMethod = refundMethod; }
        public void setItems(List<ReturnedItemDTO> items) { this.items = items; }
    }

}