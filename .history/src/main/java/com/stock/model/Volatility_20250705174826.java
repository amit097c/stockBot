package com.stock.model;

import java.time.LocalDate;

public class Volatility {
    private LocalDate tradeDate;
    private double impliedVol; // in decimal form (e.g., 0.6213 for 62.13%)

    public Volatility(LocalDate tradeDate, double impliedVol) {
        this.tradeDate = tradeDate;
        this.impliedVol = impliedVol;
    }

    public LocalDate getDate() {
        return tradeDate;
    }

    public void setDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public double getVol() {
        return impliedVol;
    }

    public void setVol(double impliedVol) {
        this.impliedVol = impliedVol;
    }

    @Override
    public String toString() {
        return "Volatility{" +
                "tradeDate=" + tradeDate +
                ", impliedVol=" + impliedVol +
                '}';
    }
}
