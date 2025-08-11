package com.stock;

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
    //static int order_counter=1300;
    public static void main(String[] args) {
    System.out.println("App::main: Starting Stock Trading Application...");
        try {
                PrintStream logStream = new PrintStream(new FileOutputStream("logs.txt", true)); // append mode
                System.setOut(logStream);
                System.setErr(logStream); // Optional: redirect System.err too
                System.out.println("----- Logging started -----");
            }
        catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        System.out.println("App::main: Initializing database connection...");
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
        System.out.println("App::testLiveTrade: Starting live trading simulation...");
        Map<String, List<Double>> symbolCloseVolMap = Map.of(
                "TSLA", List.of(319.04, 0.4541),
                "AAPL", List.of(209.05, 0.3062),
                "AMZN", List.of(230.19, 0.3368),
                "GOOG", List.of(197.44, 0.2693),
                "META", List.of(695.21, 0.3750),
                "NVDA", List.of(179.27, 0.4329),
                "MSFT", List.of(513.24, 0.2276)
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
               String order_id=apiClient.generateClientOrderId(symbol, "longBuy");
              // int order_status=apiClient.placeLongBuyBracketOrder(symbol,qty,stopLoss,takeProfit,order_id);
              boolean order_status=apiClient.placeLongBuyWithManualOCO(symbol,qty,order_id);
            //    apiClient.placeBracketOrder(
            //             symbol,
            //             qty,
            //             stopLoss,
            //             takeProfit,
            //             order_id
            //         );
                
               if(order_status)
                {
                    System.out.println("App::liveTrade:134: Buy bracket order placed for " + symbol + " with qty: " + qty);
                    
                    double order_filled_price = -1;
                    do
                     {
                        order_filled_price = apiClient.waitForFilledPrice(order_id, 30, 1000);
                        System.out.println("App::liveTrade:160: Buy order filled for " + symbol + " at price: " + order_filled_price);                         
                     } while(order_filled_price == -1);

                    orderDao.placeBuyOrder(
                        deviationId,
                        symbol,
                        order_filled_price, // Assuming this method returns the filled price
                        qty,
                        LocalDate.now(),
                        volatility
                    );
                    String stop_loss_id = "";
                    String take_profit_id = "";
                    if(stopLoss<order_filled_price)
                     {
                        stop_loss_id = apiClient.placeStopSellForLong(symbol, qty, stopLoss);
                        if(stop_loss_id.isEmpty()) {
                            System.out.println("App::liveTrade:144: Failed to place stop loss order for " + symbol);
                        } else {
                            System.out.println("App::liveTrade:146: Stop loss order placed for " + symbol + " with ID: " + stop_loss_id);
                            orderDao.placeSellStopOrder(
                                deviationId,
                                symbol,
                                stopLoss, // Assuming this method returns the filled price
                                qty,
                                LocalDate.now(),
                                volatility
                            );
                        }
                     }  
                    else if(stopLoss>order_filled_price){
                        System.out.println("App::liveTrade:150: Stop loss price condition invalid for " + symbol + ": " + stopLoss);
                        stopLoss = order_filled_price - 0.50; // Adjust stop loss to be slightly below filled price
                        stop_loss_id = apiClient.placeStopSellForLong(symbol, qty, stopLoss);
                        orderDao.placeSellStopOrder(
                                deviationId,
                                symbol,
                                stopLoss, // Assuming this method returns the filled price
                                qty,
                                LocalDate.now(),
                                volatility
                            );
                    }
                    if(takeProfit>order_filled_price){
                        take_profit_id = apiClient.placeLimitSellForLong(symbol, qty, takeProfit);
                        if(take_profit_id.isEmpty()) {
                            System.out.println("App::liveTrade:152: Failed to place take profit order for " + symbol);
                        } else {
                            System.out.println("App::liveTrade:154: Take profit order placed for " + symbol + " with ID: " + take_profit_id);
                            orderDao.placeLimitSellOrder(
                                deviationId,
                                symbol,
                                takeProfit, // Assuming this method returns the filled price
                                qty,
                                LocalDate.now(),
                                volatility
                            );
                        
                        }
                    }
                    else if(takeProfit<order_filled_price){
                        System.out.println("App::liveTrade:158: Take profit price condition invalid for " + symbol + ": " + takeProfit);
                        takeProfit = order_filled_price + 0.50; // Adjust take profit to be slightly above filled price
                        take_profit_id = apiClient.placeLimitBuy(symbol, qty, takeProfit);
                        orderDao.placeLimitSellOrder(
                                deviationId,
                                symbol,
                                takeProfit, // Assuming this method returns the filled price
                                qty,
                                LocalDate.now(),
                                volatility
                            );
                    } 
                    while(true){
                        if(!take_profit_id.isEmpty()&&apiClient.isOrderFilled(take_profit_id)) {
                            System.out.println("App::liveTrade:165: Take profit order filled for " + symbol + " at price: " + order_filled_price);
                            apiClient.cancelOrderByClientId(stop_loss_id);
                            orderDao.placeCancelStopLossOrder(symbol);
                            break;
                        } else if(!stop_loss_id.isEmpty()&& apiClient.isOrderFilled(stop_loss_id)) {
                            System.out.println("App::liveTrade:169: Stop loss order filled for " + symbol + " at price: " + order_filled_price);
                            apiClient.cancelOrderByClientId(take_profit_id);
                            orderDao.placeCancelLimitBuyOrder(symbol);
                            break;
                        } else {
                            System.out.println("App::liveTrade:173: Waiting for take profit/stop loss order to be filled for " + symbol);
                        }
                        Thread.sleep(1000); // Wait before checking again
                    }
                    
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
               // String order_id=symbol+order_counter++;    
                int qty = 5;           
                double stopLoss = round(stdDevs.get("1.75").high,2);
                double takeProfit = round(stdDevs.get("1.00").low,2);  
                String order_id = apiClient.generateClientOrderId(symbol, "shortSell");
                boolean order_status=apiClient.placeShortSellWithManualOCO( symbol, qty, stopLoss, takeProfit,order_id); 
                
                
                System.out.println("App::liveTrade:223: Short sell order status: " + order_status);
                 if(order_status)
                  {
                    System.out.println("App::liveTrade:208 Short sell order placed for " + symbol);
                    double order_filled_price = -1;
                    do{
                        order_filled_price = apiClient.waitForFilledPrice(order_id, 30, 1000);
                        System.out.println("App::liveTrade:300 Short sell order filled for " + symbol + " at price: " + order_filled_price);
                    } while(order_filled_price == -1);
                     orderDao.placeShortSellOrder(
                            deviationId,
                            symbol,
                            order_filled_price,
                            qty,
                            LocalDate.now(),
                            volatility
                        );
                  
                    
                    String stop_loss_id = "";
                    String take_profit_id = "";
                    if(stopLoss<order_filled_price)
                     { 
                        stop_loss_id=apiClient.placeStopBuy(symbol, qty, stopLoss);// order is triggered
                        if(stop_loss_id.isEmpty()) {
                            System.out.println("App::liveTrade:215: Failed to place stop loss order for " + symbol);
                        } else {
                            System.out.println("App::liveTrade:217: Stop loss order placed for " + symbol + " with ID: " + stop_loss_id);
                            orderDao.placeStopLossBuyOrder(
                                deviationId,
                                symbol,
                                stopLoss, // Assuming this method returns the filled price
                                qty,
                                LocalDate.now(),
                                volatility
                            );
                        }
                     }
                    else if (stopLoss>=order_filled_price)
                     {
                        stopLoss=order_filled_price-0.50;
                        stop_loss_id=apiClient.placeStopBuy(symbol, qty, stopLoss); // order is triggered
                        if(stop_loss_id.isEmpty()) {
                            System.out.println("App::liveTrade:328: Failed to place stop loss order for " + symbol);
                        } else {
                            System.out.println("App::liveTrade:330: Stop loss order placed for " + symbol + " with ID: " + stop_loss_id);
                            orderDao.placeStopLossBuyOrder(
                                deviationId,
                                symbol,
                                stopLoss, // Assuming this method returns the filled price
                                qty,
                                LocalDate.now(),
                                volatility
                            );
                        }
                     }
                    else
                     {
                        System.out.println("App::liveTrade:220 Short sell order stop loss and  take profit price condition invalid: " + order_filled_price+ "stopLoss: " + stopLoss );
                     }
                    if(takeProfit>order_filled_price){
                        take_profit_id=apiClient.placeLimitBuy(symbol, qty, takeProfit);
                        if(take_profit_id.isEmpty()) {
                            System.out.println("App::liveTrade:224: Failed to place take profit order for " + symbol);
                        } else {
                            System.out.println("App::liveTrade:226: Take profit order placed for " + symbol + " with ID: " + take_profit_id);
                            orderDao.placeStopLimitBuyOrder(
                                deviationId,
                                symbol,
                                takeProfit, // Assuming this method returns the filled price
                                qty,
                                LocalDate.now(),
                                volatility
                            );
                        }
                     }  
                    else if (takeProfit<=order_filled_price)
                     {
                        takeProfit=order_filled_price+0.50;
                        take_profit_id=apiClient.placeLimitBuy(symbol, qty, takeProfit);
                        if(take_profit_id.isEmpty()) {
                            System.out.println("App::liveTrade:224: Failed to place take profit order for " + symbol);
                        } else {
                            System.out.println("App::liveTrade:226: Take profit order placed for " + symbol + " with ID: " + take_profit_id);
                            orderDao.placeStopLimitBuyOrder(
                                deviationId,
                                symbol,
                                takeProfit, // Assuming this method returns the filled price
                                qty,
                                LocalDate.now(),
                                volatility
                            );
                        }

                     }             
                    else
                     {
                        System.out.println("App::liveTrade:382 Short sell order stop loss and  take profit price condition invalid: " + order_filled_price+ ", takeProfit: " + takeProfit);
                     }
                    while(true)
                     {
                        if(take_profit_id.isEmpty() || stop_loss_id.isEmpty())
                         {
                            System.out.println("App::liveTrade:233 Short sell order placed but no tp/sl orders placed for " + symbol);
                            break;
                         }
                        if(!take_profit_id.isEmpty()&&apiClient.isOrderFilled(take_profit_id))
                         {
                            System.out.println("App::liveTrade:222 Short sell takeProfit order filled for " + symbol + " at price: " + order_filled_price); 
                            apiClient.cancelOrderByClientId(stop_loss_id);
                            orderDao.placeCancelStopBuyOrder(symbol);
                            break;
                         }
                        else if(!stop_loss_id.isEmpty()&&apiClient.isOrderFilled(stop_loss_id))
                         {
                            System.out.println("App::liveTrade:225 Short sell stopLoss order stop loss filled for " + symbol + " at price: " + order_filled_price);
                            apiClient.cancelOrderByClientId(take_profit_id);
                            orderDao.placeCancelLimitBuyOrder(symbol);
                            break;
                         }
                        else
                         {
                            System.out.println("App::liveTrade:228 Waiting for short sell tp/sl order to be filled for " + symbol);
                         } 
                        Thread.sleep(1000);
                     }  
                    
                   
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

