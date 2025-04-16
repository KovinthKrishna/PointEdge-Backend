package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyThresholdsDTO {
    private double gold;
    private double silver;
    private double bronze;
    private double points;
}