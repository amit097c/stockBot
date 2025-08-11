package com.stock;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    static int order_counter=1200;
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
                "TSLA", List.of(305.30, 0.4656),
                "AAPL", List.of(213.76, 0.2974),
                "AMZN", List.of(232.23, 0.3302),
                "GOOG", List.of(193.20, 0.2900),
                "META", List.of(714.80, 0.3646),
                "NVDA", List.of(173.74, 0.3306),
                "MSFT", List.of(510.88, 0.2380)
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
           }, 0, intervalMillis, TimeUnit.MILLISECONDS);
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
public static double round(double value, int places) {
    if (places < 0) throw new IllegalArgumentException("Decimal places must be non-negative.");

    BigDecimal bd = BigDecimal.valueOf(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
}
    public void liveTrade(String symbol, double previousClose, double volatility) {
        AlpacaApiClient apiClient = new AlpacaApiClient();
        Map<String, StdDevRange> stdDevs = computeStdDevs(previousClose, volatility);


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
            int buyOrderHoldings = orderDao.getCurrentHoldings(symbol);//apiClient.getBuyOrders(symbol); // live
            System.out.println("App::liveTrade:129: Current holdings for " + symbol + ": " + buyOrderHoldings+" isBuySignal: "+isBuySignal+" deviationId: "+deviationId);
            if (isBuySignal && deviationId != -1 && buyOrderHoldings < MAX_HOLDINGS) {
                double stopLoss = round(stdDevs.get("1.77").low,2);
                double takeProfit = round(stdDevs.get("1.00").low,2);
               // apiClient.placeBracketOrder(symbol, 5, stopLoss, takeProfit);
               int qty=5;
               String order_id=symbol+order_counter++;
               int order_status=apiClient.placeLongBuyBracketOrder(symbol,qty,stopLoss,takeProfit,order_id);
               if(order_status!=-1)
                {
                    System.out.println("App::liveTrade:134: Buy bracket order placed for " + symbol + " with qty: " + qty);
                    double buyPrice = apiClient.waitForFilledPrice(order_id, 10, 1000);
                    orderDao.placeBuyOrder(
                        deviationId,
                        symbol,
                        buyPrice, // Assuming this method returns the filled price
                        qty,
                        LocalDate.now(),
                        volatility
                    );
                } else {
                    System.out.println("App::liveTrade:138: Failed to place buy bracket order for " + symbol);
                }
              /*  String order_id=symbol+order_counter++;
               int order_status=apiClient.placeBuyOrder(symbol,qty,order_id);
               if(order_status==200||order_status==201){

               //double order_filled_price=apiClient.getOrderFilledPrice(order_id);
               double order_filled_price = apiClient.waitForFilledPrice(order_id, 10, 1000);
               if(order_filled_price != -1)
                {
                    orderDao.placeBuyOrder(deviationId, symbol, bar.getLow(), 5, LocalDate.now(), volatility);  
                    Map<String, StdDevRange> std_dev_fill_price=computeStdDevs(order_filled_price, volatility);
                    double stop_loss=round(std_dev_fill_price.get("1.77").low,2);
                    double take_profit=round(std_dev_fill_price.get("1.00").high,2);
                      System.out.println("App::liveTrade:157 Buy bracket order for " + symbol + " at price: " + order_filled_price +
                        ", stopLoss: " + stop_loss + ", takeProfit: " + take_profit);
                    order_status=apiClient.placeSellBracketOrder(symbol,qty,stop_loss,take_profit);
                  
                    if(order_status==200||order_status==201)
                        {
                            System.out.println("App::liveTrade:160 bracket sell order placed");
                        }
                 }
            }*/
        }    
            else if (buyOrderHoldings >= MAX_HOLDINGS) {
                    System.out.println("App::liveTrade:135: Buy blocked: Max holdings reached for " + symbol);
                }

            boolean isShortSellSignal = bar.getHigh() > stdDevs.get("1.50").high;
            System.out.println("App::liveTrade:197: Short sell signal: " + isShortSellSignal+" bar high: "+bar.getHigh()+" std_dev_1_50: "+stdDevs.get("1.50").high);
            if (isShortSellSignal && deviationId != -1 && orderDao.getShortHoldings(symbol) < MAX_HOLDINGS) {
                System.out.println("App::liveTrade:199: isShortsellSignal  deviationId: "+ deviationId+" orderDao.getShortHoldings("+symbol+"): "+orderDao.getShortHoldings(symbol)+" isShortSellSignal: "+isShortSellSignal);
                String order_id=symbol+order_counter++;    
                int qty = 5;           
                double stopLoss = round(stdDevs.get("1.75").high,2);
                double takeProfit = round(stdDevs.get("1.00").low,2);  
                boolean order_status=apiClient.placeShortSellWithManualOCO( symbol, qty, stopLoss, takeProfit,order_id); 
                
                
                System.out.println("App::liveTrade:223: Short sell order status: " + order_status);
                 if(order_status)
                  {
                    System.out.println("App::liveTrade:208 Short sell order placed for " + symbol);
                    double order_filled_price = apiClient.waitForFilledPrice(order_id, 10, 1000);
                    String stop_loss_id = "";
                    String take_profit_id = "";
                    if(stopLoss<order_filled_price)
                     { 
                        stop_loss_id=apiClient.placeStopBuy(symbol, qty, stopLoss);
                     }
                    if(takeProfit>order_filled_price){
                        take_profit_id=apiClient.placeLimitBuy(symbol, qty, stopLoss);
                     }              
                    else
                     {
                        System.out.println("App::liveTrade:217 Short sell order stop loss and  take profit price condition invalid: " + order_filled_price+ "stopLoss: " + stopLoss + ", takeProfit: " + takeProfit);
                     }
                    while(true)
                     {
                        if(apiClient.isOrderFilled(take_profit_id))
                         {
                            System.out.println("App::liveTrade:222 Short sell takeProfit order filled for " + symbol + " at price: " + order_filled_price); 
                            apiClient.cancelOrder(stop_loss_id);
                            break;
                         }
                        else if(apiClient.isOrderFilled(stop_loss_id))
                         {
                            System.out.println("App::liveTrade:225 Short sell stopLoss order stop loss filled for " + symbol + " at price: " + order_filled_price);
                            apiClient.cancelOrder(take_profit_id);
                            break;
                         }
                        else
                         {
                            System.out.println("App::liveTrade:228 Waiting for short sell tp/sl order to be filled for " + symbol);
                         } 
                        Thread.sleep(1000);
                     }  
                    
                    orderDao.placeShortSellOrder(
                            deviationId,
                            symbol,
                            order_filled_price,
                            qty,
                            LocalDate.now(),
                            volatility
                        );
                      }
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

