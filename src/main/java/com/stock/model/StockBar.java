package com.stock.model;

public class StockBar {
    private String symbol;
    private double open, high, low, close, vwap;
    private long volume;
    private String timeframe, addDate;
    private int numTrades;

    public StockBar(String symbol, double open, double high, double low, double close,
                    long volume, String timeframe, int numTrades, double vwap, String addDate) {
        this.symbol = symbol;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.timeframe = timeframe;
        this.numTrades = numTrades;
        this.vwap = vwap;
        this.addDate = addDate;
    }

    public String getSymbol() { return symbol; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public long getVolume() { return volume; }
    public String getTimeframe() { return timeframe; }
    public int getNumTrades() { return numTrades; }
    public double getVwap() { return vwap; }
    public String getAddDate() { return addDate; }
}
