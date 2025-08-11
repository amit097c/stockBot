package com.stock.dao;

import com.stock.model.StockBar;
import com.stock.model.Volatility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StockPriceDAO {

    private Connection connection;

    public StockPriceDAO() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/stockdb", "root", "root");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


/*saveStockVolStdDev(
                    bar,
                    date,
                    std_dev_1_high,
                    std_dev_1_low,
                    std_dev_2_high,
                    std_dev_2_low,
                    std_dev_3_high,
                    std_dev_3_low,
                    breach, 

              }*/


    public void saveStockVolStdDev(StockBar bar,LocalDate date, double std_dev_1_high, double std_dev_1_low,
                                   double std_dev_2_high, double std_dev_2_low,
                                   double std_dev_3_high, double std_dev_3_low,
                                   boolean breach) {


        String sql = "INSERT INTO stock_price_deviation (symbol,date,close, high, low, timeframe) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bar.getSymbol());
                        ps.setDouble(, bar.getClose());
            ps.setDouble(2, bar.getHigh());
            ps.setDouble(3, bar.getLow());

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
     public List<StockBar> getAllStockBar() {

        List<StockBar> stockBars = new ArrayList<>();
        String sql = "Select * from stock_prices";
        
        
        try (PreparedStatement ps = connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
              while (rs.next()) {
                StockBar bar = new StockBar(
                        rs.getString("symbol"),
                        rs.getDouble("open"),
                        rs.getDouble("high"),
                        rs.getDouble("low"),
                        rs.getDouble("close"),
                        rs.getLong("volume"),
                        rs.getString("timeframe"),
                        rs.getInt("num_trades"),
                        rs.getDouble("vwap"),
                        rs.getString("add_date")
                );
                stockBars.add(bar);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stockBars;
    }
    public List<Volatility> getAllVolatility() {
        List<Volatility> vol_vals = new ArrayList<>();
        String sql = "SELECT date,volatility FROM Volatility"; // Example threshold of 5% volatility
        
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rs.getString("date");
                double vol = rs.getDouble("volatility");
                Volatility val = new Volatility(
                        rs.getDate("date").toLocalDate(),
                        vol / 100.0 // Convert percentage to decimal
                        );
                vol_vals.add(val);
                System.out.println("StockPriceDAO:89 Volatility: " + val.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("StockPriceDAO:93 Volatility return size: " + vol_vals.size());
        return vol_vals;
    }
}
