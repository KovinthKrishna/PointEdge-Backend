package com.eternalcoders.pointedge.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExchangeRequestDTO {

    @NotNull
    private String invoiceNumber;

    @NotNull
    private List<ReturnedItemDTO> returnedItems;

}