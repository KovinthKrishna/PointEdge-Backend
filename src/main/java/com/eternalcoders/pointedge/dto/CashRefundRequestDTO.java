package com.eternalcoders.pointedge.dto;

import lombok.Getter;

import java.util.List;

public class CashRefundRequestDTO {
    @Getter
    private String invoiceNumber;
    @Getter
    private List<ReturnedItemDTO> items;

}