package com.stock;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Driver;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import com.stock.api.AlpacaApiClient;
import com.stock.dao.OrderDAO;
import com.stock.dao.StockPriceDAO;
import com.stock.model.StockBar;
import com.stock.model.Volatility;
import com.stock.strategy.MovingAverageStrategy;
import com.stock.strategy.StandardDeviationCal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;

public class App {

    private static final long INTERVAL_SECONDS = 60;
    private static final int MAX_HOLDINGS = 5;

    static final StockPriceDAO dao = new StockPriceDAO();
    static final OrderDAO orderDao = new OrderDAO();

    private final Map<String, StdDevRange> stdDevs = new HashMap<>();

    public static void main(String[] args) {
        testLiveTrade();

       /*  Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down... Closing DB connections.");
            dao.close();
            orderDao.close();
        }));*/
    }

    public static void testLiveTrade() {
        Map<String, List<Double>> symbolCloseVolMap = Map.of(
                "TSLA", List.of(319.41, 0.5477),
                "AAPL", List.of(210.02, 0.2926),
                "AMZN", List.of(223.88, 0.3475),
                "GOOG", List.of(184.70, 0.3651),
                "META", List.of(702.91, 0.3938),
                "NVDA", List.of(173.00, 0.3524),
                "MSFT", List.of(505.82, 0.2510)
        );

       // ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(symbolCloseVolMap.size());
        App app = new App();
        for (String symbol : symbolCloseVolMap.keySet()) {
            Double previousClose = symbolCloseVolMap.get(symbol).get(0);
            Double volatility = symbolCloseVolMap.get(symbol).get(1);
           // scheduler.scheduleAtFixedRate(() -> {
                try {
                    app.liveTrade(symbol, previousClose, volatility);
                } catch (Exception e) {
                    System.err.println("Error for symbol " + symbol + ": " + e.getMessage());
                    e.printStackTrace();
                }
           // }, 0, INTERVAL_SECONDS, TimeUnit.SECONDS);
        }

       /*  ScheduledExecutorService shutdownScheduler = Executors.newSingleThreadScheduledExecutor();
        shutdownScheduler.scheduleAtFixedRate(() -> {
            LocalTime now = LocalTime.now(ZoneId.of("America/New_York"));
            if (now.isAfter(LocalTime.of(16, 0))) {
                System.out.println("Stopping trading scheduler at: " + now);
                scheduler.shutdownNow();
                shutdownScheduler.shutdownNow();
                System.exit(0);
            }
        }, 0, 1, TimeUnit.MINUTES);*/
    }

    public void liveTrade(String symbol, double previousClose, double volatility) {
        AlpacaApiClient apiClient = new AlpacaApiClient();
        computeStdDevs(previousClose, volatility);
        //StockBar bar = apiClient.fetchLatest1MinBar(symbol);
        List<StockBar> bars = apiClient.fetchBarsForPreviousMonday(symbol);
        for(StockBar bar : bars) {
            System.out.println("App::liveTrade:109: Processing bar for " + symbol + ": " + bar);
        
            if (bar == null) {
                System.out.println("No stock bar found for symbol: " + symbol);
                return;
            }       

        boolean isBuySignal = isBuySignal(bar);
        boolean isSellSignal = isSellSignal(bar, orderDao.getCurrentHoldings(symbol));
        String breachLow = detectLowBreach(bar.getLow());
        String breachHigh = detectHighBreach(bar.getHigh());
        System.out.println("App::liveTrade:119: Buy signal: " + isBuySignal + ", Sell signal: " + isSellSignal+" breachLow: "+breachLow+" breachHigh: "+breachHigh);
        int deviationId = dao.saveStockVolStdDev(
                bar,
                LocalDate.now(),
                stdDevs,
                volatility,
                breachLow,
                breachHigh
        );

        try {
            int alpacaHoldings = apiClient.getBuyOrders(symbol); // live

            System.out.println("App::liveTrade:128: Current holdings for " + symbol + ": " + alpacaHoldings);
            if (isBuySignal && deviationId != -1 && alpacaHoldings < MAX_HOLDINGS) {
                double stopLoss = stdDevs.get("1.77").low;
                double takeProfit = stdDevs.get("1").low;
                apiClient.placeBracketOrder(symbol, 5, stopLoss, takeProfit);
                orderDao.placeBuyOrder(deviationId, symbol, bar.getLow(), 5, LocalDate.now(), volatility);
            } else if (alpacaHoldings >= MAX_HOLDINGS) {
                System.out.println("App::liveTrade:135: Buy blocked: Max holdings reached for " + symbol);
            }
            // comment the below code during live trading run
             if (isSellSignal && deviationId != -1&&!isBuySignal) {
                int qty=5;
                int sellQty = Math.min(orderDao.getCurrentHoldings(symbol), qty); // Sell only what you have or up to your default qty
                orderDao.placeSellOrder(
                    deviationId,
                    symbol,
                    bar.getOpen(),
                    sellQty,
                    LocalDate.now(),
                    volatility
                );
            } 

        }catch (Exception e) {
            System.out.println("Could not fetch holdings from Alpaca: " + e.getMessage());
        }
      }
    }

    private void computeStdDevs(double previousClose, double volatility) {
        StandardDeviationCal cal = new StandardDeviationCal();
        double[] levels = {1, 1.25, 1.5, 1.75, 1.77, 2, 2.25, 2.5, 2.75, 3};

        for (double level : levels) {
            double dev = cal.compute(previousClose, volatility, level);
          
            stdDevs.put(String.valueOf(level), new StdDevRange(level, previousClose - dev, previousClose + dev));
            System.out.println("App::computeStdDevs:169: Level: " + String.valueOf(level) + ", Deviation: " + stdDevs.get( String.valueOf(level)).low + " to " + stdDevs.get(String.valueOf(level)).high);
        }
    }

    private boolean isBuySignal(StockBar bar) {
        System.out.println("App::isBuySignal:170: Checking buy signal for bar: " + bar);
        for(String level: stdDevs.keySet()) {
            StdDevRange dev = stdDevs.get(level);
            System.out.println("App::isBuySignal:174: level: " + level+ "low: " + dev.low + ", high: " + dev.high);          
            }
        return bar.getLow() < stdDevs.get("1.5").low;
    }

    private boolean isSellSignal(StockBar bar, int currentHoldings) {
   for(String level: stdDevs.keySet()) {
            StdDevRange dev = stdDevs.get(level);
            System.out.println("App::isSellSignal:183: level: " + level+ "low: " + dev.low + ", high: " + dev.high);          
            }
    return bar.getOpen() > stdDevs.get("1").low && currentHoldings > 0;
    }

    private String detectLowBreach(double low) {
        for (String level : List.of("3", "2.75", "2.5", "2.25", "2", "1.77", "1.75", "1.5", "1.25", "1")) {
            StdDevRange dev = stdDevs.get(level);
            System.out.println("App::detectLowBreach:191: level: " + level+ "low: " + dev.low + ", high: " + dev.high);          
            if (low < stdDevs.get(level).low) {
                return "low_" + level + "_breach";
            }
        }
        return "none";
    }

    private String detectHighBreach(double high) {
        for (String level : List.of("3", "2.75", "2.5", "2.25", "2", "1.75", "1.5", "1.25", "1")) {
            StdDevRange dev = stdDevs.get(level);
            System.out.println("App::detectHighBreach:202: level: " + level+ "low: " + dev.low + ", high: " + dev.high);          
            if (high > stdDevs.get(level).high) {
                return "high_" + level + "_breach";
            }
        }
        return "none";
    }

    public static class StdDevRange {
       public  double level;
       public  double low;
       public  double high;

        public StdDevRange(double level, double low, double high) {
            this.level = level;
            this.low = low;
            this.high = high;
        }
    }
}

