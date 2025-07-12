package com.eternalcoders.pointedge.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReturnedItemDTO {
    @NotNull
    private Long itemId;

    @Min(1)
    private int quantity;

    private String reason;

}