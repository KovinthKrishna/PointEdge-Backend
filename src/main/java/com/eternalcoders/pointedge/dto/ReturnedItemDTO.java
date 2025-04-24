package com.eternalcoders.pointedge.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnedItemDTO {
    private Long itemId;
    private int quantity;
    private boolean returned;
    private String reason;

}
