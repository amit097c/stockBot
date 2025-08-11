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
                          LocalDate date, double volatility,String order_id) {

    String sql = "INSERT INTO orders (deviation_id, symbol, price, qty, date, " +
                 "volatility, side, amount, order_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?,?)";

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setInt(1, deviationId);
        ps.setString(2, symbol);
        ps.setDouble(3, price);
        ps.setInt(4, qty);
        ps.setDate(5, java.sql.Date.valueOf(date));
        ps.setDouble(6, volatility);
        ps.setString(7, "BUY");
        ps.setDouble(8, price * qty);  // amount_spent
        ps.setString(9,order_id);
        ps.executeUpdate();
        System.out.println("Buy order logged: " + symbol + " | " + price);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
public int getCurrentHoldings(String symbol) {
    String sql = "SELECT SUM(CASE WHEN side = 'BUY' THEN qty ELSE 0 END) - " +
                 "( SUM(CASE WHEN side = 'SELL STOP LOSS' THEN qty ELSE 0 END) + " +
                 "SUM(CASE WHEN side = 'SELL LIMIT' THEN qty ELSE 0 END)) AS net_qty " +
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
                System.out.println("OrderDAO::close:62 Connection closed.");
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
public void placeCancelStopLossOrder(String symbol) {
    String sql = "update orders  set side ='STOP LOSS SELL CANCEL' where symbol = ? and side = 'SELL STOP LOSS'";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
       
        ps.setString(1, symbol);

        ps.executeUpdate();
        System.out.println("Cancel Stop Loss order logged: " + symbol);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

public void placeCancelLimitSellOrder(String symbol) {
    String sql = "update orders  set side ='LIMIT SELL CANCEL' where symbol = ? and side = 'LIMIT SELL'";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
       
        ps.setString(1, symbol);

        ps.executeUpdate();
        System.out.println("Cancel Stop Loss order logged: " + symbol);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}


public void placeCancelLimitBuyOrder(String symbol) {
    String sql = "update orders  set side ='STOP LIMIT BUY CANCEL' where symbol = ? and side = 'STOP LIMIT BUY'";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
       
        ps.setString(1, symbol);

        ps.executeUpdate();
        System.out.println("Cancel Stop Loss order logged: " + symbol);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

public void placeCancelStopBuyOrder(String symbol) {
    String sql = "update orders  set side ='STOP LOSS BUY CANCEL' where symbol = ? and side = 'STOP LOSS BUY'";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
       
        ps.setString(1, symbol);

        ps.executeUpdate();
        System.out.println("Cancel Stop Loss order logged: " + symbol);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
public void placeSellStopOrder(int deviationId, String symbol, double price, int qty, LocalDate date, double volatility,String order_id) {
    String sql = "INSERT INTO orders (deviation_id, symbol, price, qty, date, volatility, side, amount, order_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?,?)";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setInt(1, deviationId);
        ps.setString(2, symbol);
        ps.setDouble(3, price);
        ps.setInt(4, qty);
        ps.setDate(5, java.sql.Date.valueOf(date));
        ps.setDouble(6, volatility);
        ps.setString(7, "SELL STOP LOSS");
        ps.setDouble(8, price * qty);
        ps.setString(9,order_id);
        ps.executeUpdate();
        System.out.println("STOP Sell order logged: " + symbol + " | " + price);
    } catch (SQLException e) {
        e.printStackTrace();
    }
} 
public void placeLimitSellOrder(int deviationId, String symbol, double price, int qty, LocalDate date, double volatility,String order_id) {
    String sql = "INSERT INTO orders (deviation_id, symbol, price, qty, date, volatility, side, amount, order_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?,?)";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setInt(1, deviationId);
        ps.setString(2, symbol);
        ps.setDouble(3, price);
        ps.setInt(4, qty);
        ps.setDate(5, java.sql.Date.valueOf(date));
        ps.setDouble(6, volatility);
        ps.setString(7, "SELL LIMIT");
        ps.setDouble(8, price * qty);
        ps.setString(9, order_id);
        ps.executeUpdate();
        System.out.println("Sell order logged: " + symbol + " | " + price);
    } catch (SQLException e) {
        e.printStackTrace();
    }
} 
 public void placeShortSellOrder(int deviationId, String symbol, double price, int qty, LocalDate date, double volatility,String order_id) {
    String sql = """
        INSERT INTO orders (deviation_id, symbol, price, qty, date, volatility, side, amount,order_id) 
        VALUES (?, ?, ?, ?, ?, ?, 'SHORT_SELL',?,?)
    """;

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setInt(1, deviationId);
        ps.setString(2, symbol);
        ps.setDouble(3, price);
        ps.setInt(4, qty);
        ps.setDate(5, java.sql.Date.valueOf(date));
        ps.setDouble(6, volatility);
        ps.setDouble(7, (price*qty));
        ps.setString(8,order_id);
        

        ps.executeUpdate();
        //System.out.println("Logged SHORT SELL order: " + symbol + " | qty: " + qty);
    } catch (SQLException e) {
        System.err.println("Error placing short sell order: " + e.getMessage());
    }
}
  public int getShortHoldings(String symbol) {
    String sql = """
        SELECT 
            COALESCE(SUM(CASE WHEN side = 'SHORT_SELL' THEN qty ELSE 0 END), 0) -
            (COALESCE(SUM(CASE WHEN side = 'STOP LOSS BUY' THEN qty ELSE 0 END), 0)+
           COALESCE(SUM(CASE WHEN side = 'STOP LIMIT BUY' THEN qty ELSE 0 END), 0) )
        AS net_short 
        FROM orders 
        WHERE symbol = ?
    """;

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setString(1, symbol);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("net_short");
            }
        }
    } catch (SQLException e) {
        System.err.println("Error fetching short holdings: " + e.getMessage());
    }
    return 0;
}

public void placeStopLossBuyOrder(int deviationId, String symbol, double price, int qty, LocalDate date, double volatility,String order_id) {
        String sql = """
            INSERT INTO orders (deviation_id, symbol, price, qty, date, volatility, side,amount,order_id) 
            VALUES (?, ?, ?, ?, ?, ?, 'STOP LOSS BUY',?,?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, deviationId);
            ps.setString(2, symbol);
            ps.setDouble(3, price);
            ps.setInt(4, qty);
            ps.setDate(5, java.sql.Date.valueOf(date));
            ps.setDouble(6, volatility);
            ps.setDouble(7, (price * qty)); // Assuming amount is price * qty
            ps.executeUpdate();
           // System.out.println("Logged COVER order: " + symbol + " | qty: " + qty);
        } catch (SQLException e) {
            System.err.println("Error placing cover order: " + e.getMessage());
        }
    }

 public void placeStopLimitBuyOrder(int deviationId, String symbol, double price, int qty, LocalDate date, double volatility) {
        String sql = """
            INSERT INTO orders (deviation_id, symbol, price, qty, date, volatility, side,amount) 
            VALUES (?, ?, ?, ?, ?, ?, 'STOP LIMIT BUY',?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, deviationId);
            ps.setString(2, symbol);
            ps.setDouble(3, price);
            ps.setInt(4, qty);
            ps.setDate(5, java.sql.Date.valueOf(date));
            ps.setDouble(6, volatility);
            ps.setDouble(7, (price * qty)); // Assuming amount is price * qty
            ps.executeUpdate();
           // System.out.println("Logged COVER order: " + symbol + " | qty: " + qty);
        } catch (SQLException e) {
            System.err.println("Error placing cover order: " + e.getMessage());
        }
    }   
}
