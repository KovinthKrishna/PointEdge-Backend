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
    public String adminPassword;


    // Getter for gold
    public Double getGold() {
        return gold;
    }

    // Setter for gold
    public void setGold(Double gold) {
        this.gold = gold;
    }

    // Getter for silver
    public Double getSilver() {
        return silver;
    }

    // Setter for silver
    public void setSilver(Double silver) {
        this.silver = silver;
    }

    // Getter for bronze
    public Double getBronze() {
        return bronze;
    }

    // Setter for bronze
    public void setBronze(Double bronze) {
        this.bronze = bronze;
    }

    // getter for adminPassword
    public String getAdminPassword() {
        return adminPassword;
    }

    // setter for adminPassword
    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    // Getter for points
    public Double getPoints() {
        return points;
    }

    // Setter for points
    public void setPoints(Double points) {
        this.points = points;
    }

}