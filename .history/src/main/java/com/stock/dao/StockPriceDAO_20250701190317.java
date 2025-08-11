package com.stock.dao;

import com.stock.model.StockBar;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class StockPriceDAO {

    private Connection connection;

    public StockPriceDAO() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/stockdb", "root", "root");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveStockBar(StockBar bar) {
        String sql = "INSERT INTO stock_prices (symbol, open, high, low, close, volume, timeframe, num_trades, vwap, add_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bar.getSymbol());
            ps.setDouble(2, bar.getOpen());
            ps.setDouble(3, bar.getHigh());
            ps.setDouble(4, bar.getLow());
            ps.setDouble(5, bar.getClose());
            ps.setLong(6, bar.getVolume());
            ps.setString(7, bar.getTimeframe());
            ps.setInt(8, bar.getNumTrades());
            ps.setDouble(9, bar.getVwap());
            ps.setString(10, bar.getAddDate());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
