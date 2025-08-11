package com.stock.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class OrderDAO {
 private Connection connection;
 public OrderDAO() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/stockdb", "root", "root");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void placeBuyOrder(int deviationId, String symbol, double price, int qty,
                          LocalDate date, double volatility) {

    String sql = "INSERT INTO orders (deviation_id, symbol, price, qty, date, " +
                 "volatility, side, amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setInt(1, deviationId);
        ps.setString(2, symbol);
        ps.setDouble(3, price);
        ps.setInt(4, qty);
        ps.setDate(5, java.sql.Date.valueOf(date));
        ps.setDouble(6, volatility);
        ps.setString(7, "BUY");
        ps.setDouble(8, price * qty);  // amount_spent

        ps.executeUpdate();
        System.out.println("Buy order logged: " + symbol + " | " + price);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
public int getCurrentHoldings(String symbol) {
    String sql = "SELECT SUM(CASE WHEN side = 'BUY' THEN qty ELSE 0 END) - " +
                 "SUM(CASE WHEN side = 'SELL' THEN qty ELSE 0 END) AS net_qty " +
                 "FROM orders WHERE symbol = ?";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setString(1, symbol);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("net_qty");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return 0;
    }
   public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("StockPriceDAO: Connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
   public void placeSellOrder(int deviationId, String symbol, double price, int qty, LocalDate date, double volatility) {
    String sql = "INSERT INTO orders (deviation_id, symbol, price, qty, date, volatility, side, amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setInt(1, deviationId);
        ps.setString(2, symbol);
        ps.setDouble(3, price);
        ps.setInt(4, qty);
        ps.setDate(5, java.sql.Date.valueOf(date));
        ps.setDouble(6, volatility);
        ps.setString(7, "SELL");
        ps.setDouble(8, price * qty);
        ps.executeUpdate();
        System.out.println("Sell order logged: " + symbol + " | " + price);
    } catch (SQLException e) {
        e.printStackTrace();
    }
} 
}
