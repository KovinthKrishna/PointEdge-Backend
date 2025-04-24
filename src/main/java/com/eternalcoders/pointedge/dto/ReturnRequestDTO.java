package com.eternalcoders.pointedge.dto;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ReturnRequestDTO {
    private String invoiceNumber;
    private List<ReturnedItemDTO> items;
    private String refundMethod; // "CASH" or "CARD"
    @Getter
    private String reason;

}
