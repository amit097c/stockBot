package com.stock.model;

import java.time.LocalDate;

public class Volatility {
    private LocalDate date;
    private double vol; // in decimal form (e.g., 0.6213 for 62.13%)

    public Volatility(LocalDate date, double vol) {
        this.date = date;
        this.vol = vol;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getVol() {
        return vol;
    }

    public void setVol(double vol) {
        this.vol = vol;
    }

    @Override
    public String toString() {
        return "Volatility{" +
                "date=" + date +
                ",vol=" + vol +
                '}';
    }
}
