package com.eternalcoders.pointedge.mapper;

import com.eternalcoders.pointedge.dto.RefundRequestDetailsDTO;
import com.eternalcoders.pointedge.dto.ReturnedItemDTO;
import com.eternalcoders.pointedge.entity.Customer;
import com.eternalcoders.pointedge.entity.RequestReturn;

import org.springframework.stereotype.Component;

@Component
public class RequestMapper {

    public RefundRequestDetailsDTO toRefundRequestDetailsDTO(RequestReturn request) {
        RefundRequestDetailsDTO dto = new RefundRequestDetailsDTO();
        dto.setRequestId(request.getId());
        dto.setInvoiceNumber(request.getInvoice().getInvoiceNumber());
        dto.setRefundMethod(request.getRefundMethod());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setReviewedAt(request.getReviewedAt());
        dto.setStatus(request.getStatus());


        Customer customer = request.getInvoice().getCustomer();
        if (customer != null) {
            dto.setCustomerId(customer.getId());
            dto.setCustomerName(customer.getName() != null ? customer.getName().toString() : null);
        } else {
            dto.setCustomerId(null);
            dto.setCustomerName(null);
        }

        if (request.getReviewedBy() != null) {
            dto.setReviewedBy(request.getReviewedBy().getFirstName() + " " + request.getReviewedBy().getLastName());
        }

        dto.setItems(
                request.getItems().stream().map(item -> {
                    ReturnedItemDTO itemDTO = new ReturnedItemDTO();
                    itemDTO.setItemId(item.getId());
                    itemDTO.setInvoiceItemId(item.getInvoiceItem().getId());
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setUnitPrice(item.getUnitPrice());
                    itemDTO.setProductName(item.getProduct().getName());
                    itemDTO.setRefundAmount(item.getRefundAmount());
                    itemDTO.setReason(item.getReason());
                    itemDTO.setPhotoPath(item.getPhotoPath());
                    return itemDTO;
                }).toList()
        );

        return dto;
    }
}