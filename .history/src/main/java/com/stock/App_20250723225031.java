package com.stock;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Driver;
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
import java.util.*;
import java.util.concurrent.*;

public class App {

    private static final long INTERVAL_SECONDS = 60;
    private static final int MAX_HOLDINGS = 5;

    static final StockPriceDAO dao = new StockPriceDAO();
    static final OrderDAO orderDao = new OrderDAO();
    static int order_counter=1;
    public static void main(String[] args) {
        testLiveTrade();

         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down... Closing DB connections.");
            dao.close();
            orderDao.close();
        }));
    }

    private static long getInitialDelayTo930AM() {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
    ZonedDateTime targetTime = now.withHour(9).withMinute(30).withSecond(0).withNano(0);

    // If it's already past 9:30 AM today, schedule for tomorrow
    if (now.isAfter(targetTime)) {
        targetTime = targetTime.plusDays(1);
    }

    return Duration.between(now, targetTime).toMillis();
}
    public static void testLiveTrade() {
        Map<String, List<Double>> symbolCloseVolMap = Map.of(
                "TSLA", List.of(328.49, 0.5593),
                "AAPL", List.of(212.48, 0.2880),
                "AMZN", List.of(229.30, 0.3290),
                "GOOG", List.of(191.15, 0.3550),
                "META", List.of(712.97, 0.3749),
                "NVDA", List.of(171.38, 0.3375),
                "MSFT", List.of(510.06, 0.2411)
        );

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(symbolCloseVolMap.size());
        long initialDelay = getInitialDelayTo930AM();
        long intervalMillis = INTERVAL_SECONDS * 1000L;
        App app = new App();
        for (String symbol : symbolCloseVolMap.keySet()) {
            Double previousClose = symbolCloseVolMap.get(symbol).get(0);
            Double volatility = symbolCloseVolMap.get(symbol).get(1);
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    app.liveTrade(symbol, previousClose, volatility);
                } catch (Exception e) {
                    System.err.println("Error for symbol " + symbol + ": " + e.getMessage());
                    e.printStackTrace();
                }
           }, initialDelay, intervalMillis, TimeUnit.MILLISECONDS);
        }

      ScheduledExecutorService shutdownScheduler = Executors.newSingleThreadScheduledExecutor();
        shutdownScheduler.scheduleAtFixedRate(() -> {
            LocalTime now = LocalTime.now(ZoneId.of("America/New_York"));
            if (now.isAfter(LocalTime.of(16, 0))) {
                System.out.println("Stopping trading scheduler at: " + now);
                scheduler.shutdownNow();
                shutdownScheduler.shutdownNow();
                System.exit(0);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void liveTrade(String symbol, double previousClose, double volatility) {
        AlpacaApiClient apiClient = new AlpacaApiClient();
        Map<String, StdDevRange> stdDevs = computeStdDevs(previousClose, volatility);

       // computeStdDevs(previousClose, volatility);
       StockBar bar = apiClient.fetchLatest1MinBar(symbol);
       System.out.println("App::liveTrade:106 Fetching data for: "+symbol);
       /*  List<StockBar> bars = apiClient.fetchBarsForPreviousWeek(symbol);
        for(StockBar bar : bars) {*/
          //  System.out.println("App::liveTrade:109: Processing bar for " + symbol + ": " + bar);
        
            if (bar == null) {
                System.out.println("No stock bar found for symbol: " + symbol);
                return;
            }    

        boolean isBuySignal = isBuySignal(bar,stdDevs);
        boolean isSellSignal = isSellSignal(bar,stdDevs, orderDao.getCurrentHoldings(symbol));
        String breachLow = detectLowBreach(bar.getLow(),stdDevs);
        String breachHigh = detectHighBreach(bar.getHigh(),stdDevs);
       // System.out.println("App::liveTrade:119: Buy signal: " + isBuySignal + ", Sell signal: " + isSellSignal+" breachLow: "+breachLow+" breachHigh: "+breachHigh);
        int deviationId = dao.saveStockVolStdDev(
                bar,
                LocalDate.now(),
                stdDevs,
                volatility,
                breachLow,
                breachHigh
        );

        try {
            int alpacaHoldings = orderDao.getCurrentHoldings(symbol);//apiClient.getBuyOrders(symbol); // live

            //System.out.println("App::liveTrade:128: Current holdings for " + symbol + ": " + alpacaHoldings);
            if (isBuySignal && deviationId != -1 && alpacaHoldings < MAX_HOLDINGS) {
                double stopLoss = stdDevs.get("1.77").low;
                double takeProfit = stdDevs.get("1.00").low;
               // apiClient.placeBracketOrder(symbol, 5, stopLoss, takeProfit);
               int qty=5;
               String order_id=symbol+order_counter++;
               int order_status=apiClient.placeBuyOrder(symbol,qty,order_id);
               if(order_status==200||order_status==201){
                   orderDao.placeBuyOrder(deviationId, symbol, bar.getLow(), 5, LocalDate.now(), volatility);
               }
               double order_filled_price=apiClient.getOrderFilledPrice(order_id);
               Map<String, StdDevRange> std_dev_fill_price=computeStdDevs(order_filled_price, volatility);
               double stop_loss=std_dev_fill_price.get("1.77").low;
               double take_profit=std_dev_fill_price.get("1").high;
               order_status=apiClient.placeSellBracketOrder(symbol,qty,stop_loss,take_profit);
               if(order_status==200||order_status==201)
                {
                    System.out.println("App::liveTrade:156 bracket sell order placed");
                }
            } else if (alpacaHoldings >= MAX_HOLDINGS) {
                System.out.println("App::liveTrade:135: Buy blocked: Max holdings reached for " + symbol);
            }
            // comment the below code during live trading run
             if (isSellSignal && deviationId != -1&&!isBuySignal) {
                int qty=5;
                int sellQty = Math.min(orderDao.getCurrentHoldings(symbol), qty); // Sell only what you have or up to your default qty
                if(sellQty> 0){
                    System.out.println("App::liveTrade:147: Placing sell order for " + symbol + " with qty: " + sellQty);              
                    orderDao.placeSellOrder(
                    deviationId,
                    symbol,
                    bar.getOpen(),
                    sellQty,
                    LocalDate.now(),
                    volatility
                );}
            }
            
            boolean isShortSellSignal = bar.getHigh() > stdDevs.get("1.50").high;
            boolean isCoverSignal = bar.getHigh() < stdDevs.get("1.00").high;
            boolean isShortStopLoss = bar.getHigh() > stdDevs.get("1.75").high;

            if (isShortSellSignal && deviationId != -1 && orderDao.getShortHoldings(symbol) == 0) {
                int qty = 5;
                double entryPrice = bar.getHigh();
                double stopLoss = stdDevs.get("1.75").high;
                double takeProfit = stdDevs.get("1.00").high;

               // System.out.println("App::liveTrade:168 Short selling " + symbol + " at: " + entryPrice);
               String order_id=symbol+order_counter++;
               
               int order_status=apiClient.placeShortSellOrder(symbol, qty,order_id); 
               if(order_status!=-1){
                    orderDao.placeShortSellOrder(deviationId, symbol, entryPrice, qty, LocalDate.now(), volatility);
                   }
            }

            if (!isShortSellSignal && (isCoverSignal || isShortStopLoss) && deviationId != -1 && orderDao.getShortHoldings(symbol) > 0) {
                int qty = orderDao.getShortHoldings(symbol);
                double exitPrice = bar.getHigh();
               // System.out.println("App:liveTrade:176 Covering short for " + symbol + " at: " + exitPrice);
               // apiClient.coverShort(symbol, qty, exitPrice); commenting this should be automatically placed with bracket order order
                orderDao.placeCoverOrder(deviationId, symbol, exitPrice, qty, LocalDate.now(), volatility);
            }

        }catch (Exception e) {
            System.out.println("Could not fetch holdings from Alpaca: " + e.getMessage());
        }
      //}
    }

    private Map<String,StdDevRange> computeStdDevs(double previousClose, double volatility) {
        StandardDeviationCal cal = new StandardDeviationCal();
        Map<String, StdDevRange> stdDevs = new HashMap<>();
        double[] levels = {1, 1.25, 1.5, 1.75, 1.77, 2, 2.25, 2.5, 2.75, 3};

        for (double level : levels) {
            double dev = cal.compute(previousClose, volatility, level);
            String key = String.format("%.2f", level);
            stdDevs.put(key, new StdDevRange(level, previousClose - dev, previousClose + dev));
            //System.out.println("App::computeStdDevs:169: Level: " + String.valueOf(level) + ", Deviation: " + stdDevs.get( String.valueOf(level)).low + " to " + stdDevs.get(String.valueOf(level)).high);
        }
        return stdDevs;
    }

    private boolean isBuySignal(StockBar bar,Map<String, StdDevRange> stdDevs) {
        //System.out.println("App::isBuySignal:170: Checking buy signal for bar: " + bar);
        if (stdDevs.isEmpty()) {
            System.out.println("App::isBuySignal:172: No standard deviations computed yet.");
            return false;
        }
       // System.out.println("App::isBuySignal:170: Checking buy signal for bar: " + bar);
        for(String level: stdDevs.keySet()) {
            StdDevRange dev = stdDevs.get(level);
            //System.out.println("App::isBuySignal:174: level: " + level+ "low: " + dev.low + ", high: " + dev.high);          
            }
        return bar.getLow() < stdDevs.get("1.50").low;
    }


    private boolean isSellSignal(StockBar bar,Map<String, StdDevRange> stdDevs, int currentHoldings) {
   for(String level: stdDevs.keySet()) {
            StdDevRange dev = stdDevs.get(level);
            //System.out.println("App::isSellSignal:183: level: " + level+ "low: " + dev.low + ", high: " + dev.high);          
            }
    return bar.getOpen() > stdDevs.get("1.00").low && currentHoldings > 0;
    }

    private String detectLowBreach(double low,Map<String, StdDevRange> stdDevs) {
        for (String level : List.of("3.00", "2.75", "2.50", "2.25", "2.00", "1.77", "1.75", "1.50", "1.25", "1.00")) {
            StdDevRange dev = stdDevs.get(level);
            //System.out.println("App::detectLowBreach:191: level: " + level+ "low: " + dev.low + ", high: " + dev.high);          
            if (low < stdDevs.get(level).low) {
                return "low_" + level + "_breach";
            }
        }
        return "none";
    }

    private String detectHighBreach(double high,Map<String, StdDevRange> stdDevs) {
        for (String level : List.of("3.00", "2.75", "2.50", "2.25", "2.00", "1.77", "1.75", "1.50", "1.25", "1.00")) {
            StdDevRange dev = stdDevs.get(level);
            //System.out.println("App::detectHighBreach:202: level: " + level+ "low: " + dev.low + ", high: " + dev.high);          
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

