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
    public double gold;
    public double silver;
    public double bronze;
    public double points;
}