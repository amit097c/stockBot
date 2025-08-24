package com.stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.stock.api.AlpacaApiClient;
import com.stock.dao.OrderDAO;
import com.stock.dao.StockPriceDAO;
import com.stock.model.StockBar;
import com.stock.strategy.StandardDeviationCal;
import java.io.*;
import java.time.*;


public class App {

    private static final long INTERVAL_SECONDS = 60;
    private static final int MAX_HOLDINGS = 2;

    static final StockPriceDAO dao = new StockPriceDAO();
    static final OrderDAO orderDao = new OrderDAO();
    //static int order_counter=1300;
    //To do write an exit condition for while(true) loop
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
                "TSLA", List.of(335.58, 0.4387),
                "AAPL", List.of(232.78, 0.2337),
                "AMZN", List.of(230.98, 0.2398),
                "GOOG", List.of(203.82, 0.2902),
                "META", List.of(782.13, 0.2536),
                "NVDA", List.of(182.02, 0.4269),
                "MSFT", List.of(522.48, 0.1806)
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
       System.out.println("App::liveTrade:130 Fetching data for: "+symbol+" Thread: "+Thread.currentThread().getId());

        // Uncomment the following lines if you want to fetch bars for the previous week
       /*  List<StockBar> bars = apiClient.fetchBarsForPreviousWeek(symbol);
        for(StockBar bar : bars) {*/
          //  System.out.println("App::liveTrade:109: Processing bar for " + symbol + ": " + bar);
        
        if (bar == null) {
            System.out.println("No stock bar found for symbol: " + symbol);
            return;
        }    

        boolean isBuySignal = isBuySignal(bar,stdDevs);        
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
            System.out.println("App::liveTrade:156: Current holdings for " + symbol + ": " + buyOrderHoldings+" isBuySignal: "+isBuySignal+" deviationId: "+deviationId+" Thread: "+Thread.currentThread().getId());
            if (isBuySignal && deviationId != -1 && buyOrderHoldings < MAX_HOLDINGS) {
                double stopLoss = round(stdDevs.get("1.77").low,2);
                double takeProfit = round(stdDevs.get("1.00").low,2);
                System.out.println("App::liveTrade:161: Placing buy order for " + symbol + " with stopLoss: " + stopLoss + ", takeProfit: " + takeProfit+ " Thread: "+Thread.currentThread().getId());
                int qty=2;
                //String order_id=apiClient.generateClientOrderId(symbol, "longBuy");
             // int order_status=apiClient.placeLongBuyBracketOrder(symbol,qty,stopLoss,takeProfit,order_id);
                //boolean order_status=apiClient.placeLongBuyWithManualOCO(symbol,qty,order_id);
                double limitPrice=round(stdDevs.get("1.50").low, 2);
                String order_id=apiClient.placeLongEntry(symbol, qty, "limit",limitPrice);
                if(!order_id.isEmpty()) {
                    {
                        System.out.println("App::liveTrade:170: Buy order placed for " + symbol + " with qty: " + qty+" order_id: " + order_id+ " Thread: "+Thread.currentThread().getId());                     
                        double order_filled_price = -1;
                        do
                        {
                            order_filled_price = apiClient.waitForFilledPrice(order_id, 1000, 2000);
                            System.out.println("App::liveTrade:175: Buy order filled for " + symbol + " at price: " + order_filled_price+" Thread: "+Thread.currentThread().getId());                         
                        } while(order_filled_price == -1);

                        orderDao.placeBuyOrder(
                            deviationId,
                            symbol,
                            order_filled_price, // Assuming this method returns the filled price
                            qty,
                            LocalDate.now(),
                            volatility,
                            order_id
                        );

                        if(stopLoss>=order_filled_price||takeProfit<=order_filled_price)
                        {
                            if(stopLoss>=order_filled_price)
                            {
                                stopLoss=order_filled_price - 0.50; // Adjust stop loss to be slightly below filled price
                                System.out.println("App::liveTrade:194 Stop loss price condition invalid for " + symbol + ": adjusted stopLoss: " + stopLoss+" Thread: "+Thread.currentThread().getId());
                            }
                            if(takeProfit<=order_filled_price)
                            {
                                takeProfit = order_filled_price + 0.50; // Adjust take profit to be slightly above filled price
                                System.out.println("App::liveTrade:199 Take profit price condition invalid for " + symbol + ": adjusted takeprofit: " + takeProfit+" Thread: "+Thread.currentThread().getId() );
                            }
                        }
                        stopLoss = round(stopLoss,2);
                        takeProfit = round(takeProfit,2);
                        String longBuyToCloseOrderId = apiClient.placeOcoSellToClose(symbol, qty, takeProfit,stopLoss,order_filled_price);
                        if(longBuyToCloseOrderId.isEmpty()) {
                                System.out.println("App::liveTrade:144: Failed to place stop loss order for " + symbol);
                            }else {

                                    OcoFill fill = apiClient.waitForOcoFillByClientId(longBuyToCloseOrderId, /*timeoutMs*/ 200*60_000L, /*pollMs*/ 2000L);
                                    if (fill != null) {
                                        System.out.println("App::liveTrade:209: OCO " + fill.which + " filled for " + symbol + " at " + fill.price);
                                        if ("TP".equals(fill.which)) {
                                        // orderDao.placeCancelStopSellOrder(symbol);     // because never logged/stored stopSell order
                                            orderDao.placeLongBuyToCloseOrder( deviationId,symbol, fill.price,qty,  LocalDate.now(),volatility, fill.orderId);
                                        } else {
                                            //orderDao.placeCancelLimitSellOrder(symbol); // because never logged/stored stopSell order
                                            orderDao.placeLongBuyToCloseOrder( deviationId,symbol, fill.price,qty,  LocalDate.now(),volatility, fill.orderId);
                                        }
                                    }else{
                                            System.out.println("App:liveTrade:219Timed out waiting for OCO fill for " + symbol);
                                         }
                               
                                 }
                    }
                }
                else    
                {
                    System.out.println("App::liveTrade:226: Failed to place buy order for " + symbol+" Thread: "+Thread.currentThread().getId());
                }
            }
            else if (buyOrderHoldings >= MAX_HOLDINGS) {
                                System.out.println("App::liveTrade:135: Buy blocked: Max holdings reached for " + symbol+" Thread: "+Thread.currentThread().getId());
                            }

            boolean isShortSellSignal = bar.getHigh() > stdDevs.get("1.50").high;
            System.out.println("App::liveTrade:303: Short sell signal: " + isShortSellSignal+" bar high: "+bar.getHigh()+" std_dev_1_50: "+stdDevs.get("1.50").high+" Thread: "+Thread.currentThread().getId());
            if (isShortSellSignal && deviationId != -1 && orderDao.getShortHoldings(symbol) < MAX_HOLDINGS) {
                System.out.println("App::liveTrade:305: isShortsellSignal  deviationId: "+ deviationId+" orderDao.getShortHoldings("+symbol+"): "+orderDao.getShortHoldings(symbol)+" isShortSellSignal: "+isShortSellSignal+" Thread: "+Thread.currentThread().getId());
                int qty = 2;           
                double stopLoss = round(stdDevs.get("1.75").high,2);
                double takeProfit = round(stdDevs.get("1.00").low,2);   
                double limitPrice=round(stdDevs.get("1.50").high, 2);
                String order_id=apiClient.placeShortEntry(symbol, qty,"limit",limitPrice);
                System.out.println("App::liveTrade:223: Short sell order id: " + order_id+" Thread: "+Thread.currentThread().getId());
                 if(order_id!=null&&!order_id.isEmpty())
                  {
                    System.out.println("App::liveTrade:245 Short sell order placed for " + symbol+" order id: "+order_id+" Thread: "+Thread.currentThread().getId());
                    double order_filled_price = -1;
                    do{
                        order_filled_price = apiClient.waitForFilledPrice(order_id, 10000, 5000);
                        System.out.println("App::liveTrade:300 Short sell order filled for " + symbol + " at price: " + order_filled_price+" order id: "+order_id+" Thread: "+Thread.currentThread().getId());
                    } while(order_filled_price == -1);
                     orderDao.placeShortSellOrder(
                            deviationId,
                            symbol,
                            order_filled_price,
                            qty,
                            LocalDate.now(),
                            volatility,
                            order_id
                        );
                  
                    if(stopLoss<=order_filled_price || takeProfit>=order_filled_price)
                        {
                            if(stopLoss<=order_filled_price)
                            {
                                stopLoss=order_filled_price+ 0.50; // buffer above stop price
                                System.out.println("App::liveTrade:340 Stop loss buy is below short sell entry price. Adjusting stopLossBuy to: "+stopLoss);
                            }
                            if(takeProfit>=order_filled_price)
                            {
                                takeProfit=order_filled_price- 0.50; // buffer below take profit price
                                System.out.println("App::liveTrade:345 Take profit limit buy is above short sell entry price. Adjusting takeProfitLimitBuy to: "+takeProfit);
                            }
                        } 
                   stopLoss = round(stopLoss,2);
                   takeProfit = round(takeProfit,2);     
                    String shortSellBuyToCloseOrderId= apiClient.placeOcoBuyToClose(symbol, qty,takeProfit,stopLoss,order_filled_price);            
                    if(shortSellBuyToCloseOrderId.isEmpty()){
                        System.out.println("App::liveTrade:278: Failed to place short sell buy to close order for " + symbol+" Thread: "+Thread.currentThread().getId());
                        // add retry logic here
                    } else {
                        System.out.println("App::liveTrade:280: Short sell buy to close order placed for " + symbol + " with ID: " + shortSellBuyToCloseOrderId+" Thread: "+Thread.currentThread().getId());
                        //double ocoOrderFilledPrice=apiClient.waitForFilledPrice(shortSellBuyToCloseOrderId, 30, 2000);
                        OcoFill fill = apiClient.waitForOcoFillByClientId(shortSellBuyToCloseOrderId, /*timeoutMs*/ 20*60_000L, /*pollMs*/ 2000L);
                         if (fill != null) {
                            System.out.println("OCO " + fill.which + " filled for " + symbol + " at " + fill.price);
                            if ("TP".equals(fill.which)) {
                               // orderDao.placeCancelStopBuyOrder(symbol);     // because stoploss buy order never logged/stored
                                orderDao.placeShortSellBuyToCloseOrder( deviationId,symbol, fill.price,qty,  LocalDate.now(),volatility, fill.orderId);
                            } else {
                               // orderDao.placeCancelLimitBuyOrder(symbol);    // because takeprofit buy order never logged/stored
                                orderDao.placeShortSellBuyToCloseOrder( deviationId,symbol, fill.price,qty,  LocalDate.now(),volatility, fill.orderId);
                            }
                        } else {
                            System.out.println("App:livetrade:293: Timed out waiting for OCO fill for " + symbol);
                        }
                    }
                }
            }
            else if(orderDao.getShortHoldings(symbol) >= MAX_HOLDINGS)  // check if short sell holdings are less than max holdings
            {
                System.out.println("App::liveTrade:306: Short sell blocked: Max holdings reached for " + symbol+" Thread: "+Thread.currentThread().getId());
            }
      
            
    }catch (Exception e) {
            System.out.println("App:livetrade:299: Could not fetch holdings from Alpaca: " + e.getMessage());
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
    public static final class OcoFill {
    public final String which;      // "TP" or "SL"
    public final double price;      // filled_avg_price
    public final String orderId;    // the leg that filled
    public OcoFill(String which, double price, String orderId) {
        this.which = which; this.price = price; this.orderId = orderId;
    }
}
}


