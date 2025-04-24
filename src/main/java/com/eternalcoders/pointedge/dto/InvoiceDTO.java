package com.eternalcoders.pointedge.dto;

import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

    @Setter
    public class InvoiceDTO {
        private String invoiceNumber;
        private List<ReturnedItemDTO> items;
        private LocalDateTime date;
        private Double totalAmount;
        private Integer loyaltyPoints;

        public String getInvoiceNumber() {
            return invoiceNumber;
        }

        public List<ReturnedItemDTO> getItems() {
            return items;
        }

    }


