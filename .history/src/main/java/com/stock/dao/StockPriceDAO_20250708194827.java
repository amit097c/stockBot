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


    public int saveStockVolStdDev(StockBar bar,LocalDate date, double std_dev_1_high, double std_dev_1_low,
                                   double std_dev_2_high, double std_dev_2_low,
                                   double std_dev_3_high, double std_dev_3_low,double vol_val,
                                   String breach_band) {


        String sql = "INSERT INTO stock_price_deviation (symbol,date,close_price, high_price, low_price,std_dev_1_high,std_dev_1_low,std_dev_2_high,std_dev_2_low,std_dev_3_high,std_dev_3_low,vol_val, breach_band,timeframe) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);) {
            ps.setString(1, bar.getSymbol());
            ps.setString(2, date.toString());
            ps.setDouble(3, bar.getClose());
            ps.setDouble(4, bar.getHigh());
            ps.setDouble(5, bar.getLow());

            ps.setDouble(6, std_dev_1_high);            
            ps.setDouble(7, std_dev_1_low);
            
            ps.setDouble(8, std_dev_2_high);            
            ps.setDouble(9, std_dev_2_low);
            
            ps.setDouble(10, std_dev_3_high);            
            ps.setDouble(11, std_dev_3_low);
            
            ps.setDouble(12, vol_val);
            ps.setString(13, breach_band);
            ps.setString(14, bar.getTimeframe());
           
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);  // return the generated ID
               }
            }
            catch(Exception e)
             {
               System.out.println("StockPriceDAO:78  Error retrieving stock price deviation id");
               e.printStackTrace();
               System.exit(0);
             }
        } catch (Exception e) {
            System.out.println("StockPriceDAO:83 saveStockVolStdDev: Error saving stock price deviation");
            e.printStackTrace();
            System.exit(0);
        }
        return -1; // return -1 if insertion fails
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
            System.out.println("StockPriceDAO:94 saveStockBar: Error saving stock bar");
            e.printStackTrace();
            System.exit(0);
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
            System.out.println("StockPriceDAO:122 Error fetching stock bars");
            e.printStackTrace();
            System.exit(0);
        }
        return stockBars;
    }
    public List<Volatility> getAllVolatility(String symbol) {
        List<Volatility> vol_vals = new ArrayList<>();
        String sql = "SELECT date, volatility FROM Volatility WHERE symbol = ? ORDER BY date;"; // Example threshold of 5% volatility
        
        try (PreparedStatement ps = connection.prepareStatement(sql)){
             ps.setString(1, symbol);
             try(ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rs.getString("date");
                double vol = rs.getDouble("volatility");
                Volatility val = new Volatility(
                        rs.getDate("date").toLocalDate(),
                        vol
                        );
                vol_vals.add(val);
                System.out.println("StockPriceDAO:89 Volatility: " + val.toString());
            }
        } }catch (Exception e) {
        System.out.println("StockPriceDAO:92 Error fetching volatility data");
            e.printStackTrace();
            System.exit(0);
        }
        System.out.println("StockPriceDAO:93 Volatility return size: " + vol_vals.size());
        return vol_vals;
    }
}
