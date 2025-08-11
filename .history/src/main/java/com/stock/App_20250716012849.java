package com.stock;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Driver;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

public class App 
{

    //Standard deviations variables
    Double std_dev_1=0.0;
    Double std_dev_1_25=0.0;
    Double std_dev_1_50=0.0;
    Double std_dev_1_75=0.0;
    Double std_dev_1_77=0.0;          
    Double std_dev_2=0.0;  
    Double std_dev_2_25=0.0;
    Double std_dev_2_50=0.0;
    Double std_dev_2_75=0.0; 
    Double std_dev_3=0.0;   
          
    Double std_dev_1_low=0.0;
    Double std_dev_1_25_low=0.0;
    Double std_dev_1_50_low=0.0;
    Double std_dev_1_75_low=0.0;
    Double std_dev_1_77_low=0.0;
    Double std_dev_1_high=0.0;
    Double std_dev_1_25_high=0.0;
    Double std_dev_1_50_high=0.0;
    Double std_dev_1_75_high=0.0;
    Double std_dev_1_77_high=0.0;

    Double std_dev_2_low=0.0;
    Double std_dev_2_25_low=0.0;
    Double std_dev_2_50_low=0.0;
    Double std_dev_2_75_low=0.0;
    Double std_dev_2_high=0.0;
    Double std_dev_2_25_high=0.0;
    Double std_dev_2_50_high=0.0;
    Double std_dev_2_75_high=0.0;
    
    Double std_dev_3_low=0.0;
    Double std_dev_3_high=0.0;
    private static final long INTERVAL_SECONDS = 60;
    static final StockPriceDAO dao = new StockPriceDAO();
    static final OrderDAO orderDao = new OrderDAO();

    public static void main(String[] args) {
       String symbol = "TSLA";//"AAPL";
       AlpacaApiClient apiClient = new AlpacaApiClient();
       
       int alpacaHoldings = 0;
         try {
        alpacaHoldings = apiClient.getBuyOrders(symbol).length(); // You need to implement this
        System.out.println("Current holdings for " + symbol + ": " + alpacaHoldings);
    } catch (Exception e) {
        System.out.println("Could not fetch position from Alpaca: " + e.getMessage());
    }
       //simlateTrade( symbol);
      // apiClient.placePaperOrder("TSLA", 10, "buy", "market", null, null, null);
      // AlpacaApiClient apiClient = new AlpacaApiClient();
       //apiClient.placeBracketOrder("TSLA", 5, 312.14, 315.01);
       /*test_live_trade();
       Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("Shutting down... Closing DB connections.");
        dao.close();
        orderDao.close();
    }));*/
    }
    //To do : limit qty , parallel hourly and 
    public static void test_live_trade()
     {
        Map<String,List<Double>> symbolClosePriceVolatilityMap = new HashMap<>();
        symbolClosePriceVolatilityMap.put("TSLA", List.of(310.78, 0.5566)); // Close price, volatility
        symbolClosePriceVolatilityMap.put("AAPL", List.of(209.11, 0.2967));
        symbolClosePriceVolatilityMap.put("AMZN", List.of(226.35, 0.3485)); // Close price, volatility
        symbolClosePriceVolatilityMap.put("GOOG", List.of(183.10, 0.3657)); // Close price, volatility  
        symbolClosePriceVolatilityMap.put("META", List.of(720.39, 0.3940)); // Close price, volatility  
        symbolClosePriceVolatilityMap.put("NVDA", List.of(170.70, 0.3550)); // Close price, volatility  
        symbolClosePriceVolatilityMap.put("MSFT", List.of(505.82, 0.2510));
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(symbolClosePriceVolatilityMap.size());
               
        for (String symbol : symbolClosePriceVolatilityMap.keySet()) {
            final String sym = symbol;
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    App app = new App();
                    Double previousClose = symbolClosePriceVolatilityMap.get(sym).get(0);
                    Double volatility = symbolClosePriceVolatilityMap.get(sym).get(1);
                    app.liveTrade(sym, previousClose, volatility);
                } catch (Exception e) {
                    System.err.println("Error for symbol " + sym + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }, 0, INTERVAL_SECONDS, TimeUnit.SECONDS);
        }

     }
    public void pre_compute_standard_deviations(StandardDeviationCal stdDevCal, Double previousClose,Double volatility)
     {
                        
        std_dev_1=stdDevCal.compute_std_dev_1(previousClose,volatility);
        std_dev_1_25=stdDevCal.compute_std_dev_1_25(previousClose,volatility);
        std_dev_1_50=stdDevCal.compute_std_dev_1_50(previousClose,volatility);
        std_dev_1_75=stdDevCal.compute_std_dev_1_75(previousClose,volatility);
        std_dev_1_77=stdDevCal.compute_std_dev_1_77(previousClose,volatility);
        std_dev_2=stdDevCal.compute_std_dev_2(previousClose,volatility);
        std_dev_2_25=stdDevCal.compute_std_dev_2_25(previousClose,volatility);
        std_dev_2_50=stdDevCal.compute_std_dev_2_50(previousClose,volatility);
        std_dev_2_75=stdDevCal.compute_std_dev_2_75(previousClose,volatility);
        std_dev_3=stdDevCal.compute_std_dev_3(previousClose,volatility);
            
        std_dev_1_low=previousClose - std_dev_1;
        std_dev_1_high=previousClose + std_dev_1;

        std_dev_1_25_low=previousClose - std_dev_1_25;
        std_dev_1_25_high=previousClose + std_dev_1_25;
        
        std_dev_1_50_low=previousClose - std_dev_1_50;
        std_dev_1_50_high=previousClose + std_dev_1_50;

        std_dev_1_75_low=previousClose - std_dev_1_75;
        std_dev_1_75_high=previousClose + std_dev_1_75;
        
        std_dev_1_77_low=previousClose - std_dev_1_77;
        std_dev_1_77_high=previousClose + std_dev_1_77;
        

        std_dev_2_high=previousClose + std_dev_2;
        std_dev_2_low=previousClose - std_dev_2;
        
        std_dev_2_25_low=previousClose - std_dev_2_25;
        std_dev_2_25_high=previousClose + std_dev_2_25;

        std_dev_2_50_low=previousClose - std_dev_2_50;
        std_dev_2_50_high=previousClose + std_dev_2_50;

        std_dev_2_75_low=previousClose - std_dev_2_75;
        std_dev_2_75_high=previousClose + std_dev_2_75;


        std_dev_3_high=previousClose + std_dev_3;
        std_dev_3_low=previousClose - std_dev_3;
     }
    public void liveTrade(String symbol, Double previousClose,Double volatility)
     {
       AlpacaApiClient apiClient = new AlpacaApiClient();
      // StockPriceDAO dao = new StockPriceDAO();
       StandardDeviationCal stdDevCal = new StandardDeviationCal();
       pre_compute_standard_deviations(stdDevCal, previousClose,volatility);
      // OrderDAO orderDao = new OrderDAO();
       StockBar bar = apiClient.fetchLatest1MinBar(symbol);//apiClient.fetchLatest1HrBar(symbol);
       if (bar == null) {
            System.out.println("No stock bar found for symbol: " + symbol);
            return;
           }
        
          double high = bar.getHigh();
          double low = bar.getLow();  
          double close = bar.getClose();
          double open = bar.getOpen();
          String breach_low = "none";
          String breach_high = "none";
          boolean isBuySignal = false;
          boolean isSellSignal = false;
          boolean isStopLossSignal=false;
           // Check LOW breaches
            if (low < std_dev_3_low) {
                breach_low = "low_3_breach";
            } else if (low < std_dev_2_75_low) {
                breach_low = "low_2_75_breach";
            } else if (low < std_dev_2_50_low) {
                breach_low = "low_2_50_breach";
            } else if (low < std_dev_2_25_low) {
                breach_low = "low_2_25_breach";
            } else if (low < std_dev_2_low) {
                breach_low = "low_2_breach";
            } else if (low < std_dev_1_77_low) {
                breach_low = "low_1_77_breach";
                isStopLossSignal = true;
            } else if (low < std_dev_1_75_low) {
                breach_low = "low_1_75_breach";
                
            } 
            else if (low < std_dev_1_50_low) {
                breach_low = "low_1_50_breach";
                isBuySignal = true;
            } else if (low < std_dev_1_25_low) {
                breach_low = "low_1_25_breach";
            } else if (low < std_dev_1_low) {
                breach_low = "low_1_breach";
            }

            int currentHoldings = orderDao.getCurrentHoldings(symbol);

            if (open > std_dev_1_50_low&& currentHoldings > 0) {
                System.out.println("App: SELL signal for " + symbol + " high_price: " + bar.getOpen()+" > "+std_dev_1_50_low);
                isSellSignal = true;
            }

            // Check HIGH breaches
            if (high > std_dev_3_high) {
                breach_high = "high_3_breach";              
            } else if (high > std_dev_2_75_high) {
                breach_high = "high_2_75_breach";
            } else if (high > std_dev_2_50_high) {
                breach_high = "high_2_50_breach";
            } else if (high > std_dev_2_25_high) {
                breach_high = "high_2_25_breach";
            } else if (high > std_dev_2_high) {
                breach_high = "high_2_breach";
            } else if (high > std_dev_1_75_high) {
                breach_high = "high_1_75_breach";
            } else if (high > std_dev_1_50_high) {
                breach_high = "high_1_50_breach";
            } else if (high > std_dev_1_25_high) {
                breach_high = "high_1_25_breach";
            } else if (high > std_dev_1_high) {
                breach_high = "high_1_breach";
            }
           
            int deviationId =dao.saveStockVolStdDev(
                bar,
                LocalDate.now(),
                std_dev_1_low,
                std_dev_1_high,

                std_dev_1_25_low,
                std_dev_1_25_high,
                
                std_dev_1_50_low,
                std_dev_1_50_high,
                
                std_dev_1_75_low,
                std_dev_1_75_high,

                std_dev_2_low,
                std_dev_2_high,

                std_dev_2_25_low,
                std_dev_2_25_high,
                
                std_dev_2_50_low,
                std_dev_2_50_high,

                std_dev_2_75_low,
                std_dev_2_75_high,

                std_dev_3_low,
                std_dev_3_high,

                volatility,
                breach_low,
                breach_high
               );
    int alpacaHoldings = 0;

            try {
        alpacaHoldings = apiClient.getPositionQty(symbol); // You need to implement this
    } catch (Exception e) {
        System.out.println("Could not fetch position from Alpaca: " + e.getMessage());
    }
   
            int quantity=5;   
            if (isBuySignal && deviationId != -1&&alpacaHoldings<=50) 
              { 
                //apiClient.placeBracketOrder("TSLA", 5, 312.14, 315.01);
                apiClient.placeBracketOrder(symbol, quantity, std_dev_1_77_low, std_dev_1_low);
                 orderDao.placeBuyOrder(
                    deviationId,
                    symbol,
                    bar.getLow(),
                    quantity,
                    LocalDate.now(),
                    volatility
                    );
              }  
              else if (alpacaHoldings > 50) {
        System.out.println("App::liveTrade:300 Buy blocked: Already holding max (50) shares of " + symbol);
    } 

             //commenting the below code as sell will be handled by bracket order 
           /*  if (isSellSignal && deviationId != -1&&!isBuySignal) {
                int sellQty = Math.min(currentHoldings, quantity); // Sell only what you have or up to your default qty
                orderDao.placeSellOrder(
                    deviationId,
                    symbol,
                    bar.getOpen(),
                    sellQty,
                    LocalDate.now(),
                    volatility
                );
            } */ 

     }
    public static void simlateTrade(String symbol)
     {
      AlpacaApiClient apiClient = new AlpacaApiClient();
      StockPriceDAO dao = new StockPriceDAO();
      StandardDeviationCal stdDevCal = new StandardDeviationCal();
      List<Volatility> vol_vals=dao.getAllVolatility(symbol);
      OrderDAO orderDao = new OrderDAO();
      Double previousClose = 282.16; // last close price for TSLA
      for(Volatility vol:vol_vals) 
        {
          LocalDate date=vol.getDate();
          double vol_val=vol.getVol();
          System.out.println("App:54 date: " + date + ",  Volatility: " + vol_val);
          //Fetch stock bars for the date
          List<StockBar> bars = apiClient.fetchStockBarsForDay(symbol, date);
          if (bars == null || bars.isEmpty()) {
            System.out.println("No stock bars found for symbol: " + symbol + " on date: " + date);
            continue;
           }
          Double std_dev_1=0.0;
          Double std_dev_1_25=0.0;
          Double std_dev_1_50=0.0;
          Double std_dev_1_75=0.0;
          Double std_dev_1_77=0.0;          
          Double std_dev_2=0.0;  
          Double std_dev_2_25=0.0;
          Double std_dev_2_50=0.0;
          Double std_dev_2_75=0.0; 
          Double std_dev_3=0.0;   
          
          Double std_dev_1_low=0.0;
          Double std_dev_1_25_low=0.0;
          Double std_dev_1_50_low=0.0;
          Double std_dev_1_75_low=0.0;
          Double std_dev_1_77_low=0.0;
          Double std_dev_1_high=0.0;
          Double std_dev_1_25_high=0.0;
          Double std_dev_1_50_high=0.0;
          Double std_dev_1_75_high=0.0;
          Double std_dev_1_77_high=0.0;

          Double std_dev_2_low=0.0;
          Double std_dev_2_25_low=0.0;
          Double std_dev_2_50_low=0.0;
          Double std_dev_2_75_low=0.0;
          Double std_dev_2_high=0.0;
          Double std_dev_2_25_high=0.0;
          Double std_dev_2_50_high=0.0;
          Double std_dev_2_75_high=0.0;
          
          Double std_dev_3_low=0.0;
          Double std_dev_3_high=0.0;
          
                  
       
          std_dev_1=stdDevCal.compute_std_dev_1(previousClose,vol_val);
          std_dev_1_25=stdDevCal.compute_std_dev_1_25(previousClose,vol_val);
          std_dev_1_50=stdDevCal.compute_std_dev_1_50(previousClose,vol_val);
          std_dev_1_75=stdDevCal.compute_std_dev_1_75(previousClose,vol_val);
          std_dev_1_77=stdDevCal.compute_std_dev_1_77(previousClose,vol_val);
          std_dev_2=stdDevCal.compute_std_dev_2(previousClose,vol_val);
          std_dev_2_25=stdDevCal.compute_std_dev_2_25(previousClose,vol_val);
          std_dev_2_50=stdDevCal.compute_std_dev_2_50(previousClose,vol_val);
          std_dev_2_75=stdDevCal.compute_std_dev_2_75(previousClose,vol_val);
          std_dev_3=stdDevCal.compute_std_dev_3(previousClose,vol_val);
               
          std_dev_1_low=previousClose - std_dev_1;
          std_dev_1_high=previousClose + std_dev_1;

          std_dev_1_25_low=previousClose - std_dev_1_25;
          std_dev_1_25_high=previousClose + std_dev_1_25;
          
          std_dev_1_50_low=previousClose - std_dev_1_50;
          std_dev_1_50_high=previousClose + std_dev_1_50;

          std_dev_1_75_low=previousClose - std_dev_1_75;
          std_dev_1_75_high=previousClose + std_dev_1_75;
          
          std_dev_1_77_low=previousClose - std_dev_1_77;
          std_dev_1_77_high=previousClose + std_dev_1_77;
          

          std_dev_2_high=previousClose + std_dev_2;
          std_dev_2_low=previousClose - std_dev_2;
          
          std_dev_2_25_low=previousClose - std_dev_2_25;
          std_dev_2_25_high=previousClose + std_dev_2_25;

          std_dev_2_50_low=previousClose - std_dev_2_50;
          std_dev_2_50_high=previousClose + std_dev_2_50;

          std_dev_2_75_low=previousClose - std_dev_2_75;
          std_dev_2_75_high=previousClose + std_dev_2_75;


          std_dev_3_high=previousClose + std_dev_3;
          std_dev_3_low=previousClose - std_dev_3;
        
          for(int i=0;i<bars.size();i++) {
            
            System.out.println("App:235 StockBar for " + symbol + " on " + date + ": " +date);                            
            StockBar bar = bars.get(i);  
            double high = bar.getHigh();
            double low = bar.getLow();  
            double close = bar.getClose();
            double open = bar.getOpen();
            String breach_low = "none";
            String breach_high = "none";
            boolean isBuySignal = false;
            boolean isSellSignal = false;
            boolean isStopLossSignal=false;

            // Check LOW breaches
            if (low < std_dev_3_low) {
                breach_low = "low_3_breach";
            } else if (low < std_dev_2_75_low) {
                breach_low = "low_2_75_breach";
            } else if (low < std_dev_2_50_low) {
                breach_low = "low_2_50_breach";
            } else if (low < std_dev_2_25_low) {
                breach_low = "low_2_25_breach";
            } else if (low < std_dev_2_low) {
                breach_low = "low_2_breach";
            } else if (low < std_dev_1_77_low) {
                breach_low = "low_1_77_breach";
                isStopLossSignal = true;
            } else if (low < std_dev_1_75_low) {
                breach_low = "low_1_75_breach";
                isBuySignal = true;
            } 
            else if (low < std_dev_1_50_low) {
                breach_low = "low_1_50_breach";
            } else if (low < std_dev_1_25_low) {
                breach_low = "low_1_25_breach";
            } else if (low < std_dev_1_low) {
                breach_low = "low_1_breach";
            }

            int currentHoldings = orderDao.getCurrentHoldings(symbol);

            if (open > std_dev_1_50_low&& currentHoldings > 0) {
                System.out.println("App: SELL signal for " + symbol + " on " + date + " high_price: " + bar.getHigh());
                isSellSignal = true;
            }
           

         
            // Check HIGH breaches
            if (high > std_dev_3_high) {
                breach_high = "high_3_breach";              
            } else if (high > std_dev_2_75_high) {
                breach_high = "high_2_75_breach";
            } else if (high > std_dev_2_50_high) {
                breach_high = "high_2_50_breach";
            } else if (high > std_dev_2_25_high) {
                breach_high = "high_2_25_breach";
            } else if (high > std_dev_2_high) {
                breach_high = "high_2_breach";
            } else if (high > std_dev_1_75_high) {
                breach_high = "high_1_75_breach";
            } else if (high > std_dev_1_50_high) {
                breach_high = "high_1_50_breach";
            } else if (high > std_dev_1_25_high) {
                breach_high = "high_1_25_breach";
            } else if (high > std_dev_1_high) {
                breach_high = "high_1_breach";
            }
           
            
          
            int deviationId =dao.saveStockVolStdDev(
                bar,
                date,
                std_dev_1_low,
                std_dev_1_high,

                std_dev_1_25_low,
                std_dev_1_25_high,
                
                std_dev_1_50_low,
                std_dev_1_50_high,
                
                std_dev_1_75_low,
                std_dev_1_75_high,

                std_dev_2_low,
                std_dev_2_high,

                std_dev_2_25_low,
                std_dev_2_25_high,
                
                std_dev_2_50_low,
                std_dev_2_50_high,

                std_dev_2_75_low,
                std_dev_2_75_high,

                std_dev_3_low,
                std_dev_3_high,

                vol_val,
                breach_low,
                breach_high
               );
            int quantity=100;   
            if (isBuySignal && deviationId != -1) 
              {
                 orderDao.placeBuyOrder(
                    deviationId,
                    symbol,
                    bar.getLow(),
                    quantity,
                    date,
                    vol_val
                    );
              }   

           

            if (isSellSignal && deviationId != -1&&!isBuySignal) {
                int sellQty = Math.min(currentHoldings, quantity); // Sell only what you have or up to your default qty
                orderDao.placeSellOrder(
                    deviationId,
                    symbol,
                    bar.getOpen(),
                    sellQty,
                    date,
                    vol_val
                );
            }  
            System.out.println("App:101 Saved stock bar with standard deviation for " + symbol + " on " + date);
            if(i==bars.size()-1)
              {
                System.out.println("App:110 Last stock bar for " + symbol + " on " + date + ": " + bar);
                previousClose=bars.get(i).getClose();
              }
            } 
        }
     }
}
    // List<StockBar> bars = apiClient.fetchStockBars(symbol);
       
       
       //placing a market order
     /*   System.out.println("App:42 placing order for " + symbol+" for 2 shares");
       apiClient.placeMarketOrder(symbol, 2, "buy");
       System.out.println("App:21 Fetched stock bars for symbol: " + symbol);
       */

     // Fetch stockbar records from db
      /*   StockPriceDAO dao = new StockPriceDAO();
        List<StockBar> bars = dao.getAllStockBar();
        if (bars == null || bars.isEmpty()) {
            System.out.println("No stock bars found for symbol: " + symbol);
            return;
        }
        
       MovingAverageStrategy.Action signal = MovingAverageStrategy.getSignal(bars);
       int n=bars.size();
       System.out.println("App:61 Trading signal for " + symbol + " on " + bars.get(n - 1).getAddDate()+ ": " + signal);  
       */ // Uncomment the following lines to print stock bars
        /*for (StockBar bar : bars) {
            System.out.println(bar);
        }*/
        
        // Uncomment the following lines to execute a trading strategy
        /*MovingAverageStrategy.Action action = MovingAverageStrategy.getSignal(bars);
        System.out.println("Trading action for " + symbol + ": " + action);*/

       
       // Uncomment the following lines to fetch and save stock bars
        /*if (bars == null || bars.isEmpty()) {
            System.out.println("No stock bars found for symbol: " + symbol);
            return;
        }
        StockPriceDAO dao = new StockPriceDAO();
        for (StockBar bar : bars) {
            dao.saveStockBar(bar);
        }*/
        
        // Uncomment the following lines to fetch stock bars for a specific date
       /*  List<StockBar> bars = apiClient.fetchStockBarsForDay(symbol,LocalDate.of(2025, 7, 01));
        if(bars == null || bars.isEmpty()) {
            System.out.println("No stock bars found for symbol: " + symbol);
            return;
        }
        StockPriceDAO dao = new StockPriceDAO();
        for (StockBar bar : bars) {
            dao.saveStockBar(bar);
        }*/
    
    /* 

    This is a Java application prototype that fetches stock market data from the Alpaca API

    // Setting the API key and secret as constants
    private static final String API_KEY = "PKMXFOXOMDPMNUMB3TXR";
    private static final String API_SECRET = "64LdMS9gQnAf4bm8el3NIhNkuuj3slpG3lfvde6p";
    private static final String BASE_URL = "https://data.alpaca.markets/v2/stocks/bars";

    public static void main( String[] args )
    {
      String symbol = "AAPL";
      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
      ZonedDateTime lastWeek = now.minusDays(7);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    // last 7 trading days of market data.
      Map<String, String> timeWindow = getMarketTimeWindowUtc();
      String start = timeWindow.get("start");
      String end = timeWindow.get("end");
     
      //String start = formatter.format(lastWeek);
      //String end = formatter.format(now);

     String queryParams = String.format(
    "symbols=%s&timeframe=15Min&start=%s&end=%s&limit=1000&adjustment=raw&feed=sip&sort=asc",
    symbol, start, end
);     //String endpoint = "/stocks/" + symbol + "/bars?timeframe=1Day&limit=5";
       try {

            System.out.println("base url + queryParams: " + BASE_URL + "?" + queryParams);
            URL url=new URL(BASE_URL + "?" + queryParams);
            
            // Construct the full URL
//"https://data.alpaca.markets/v2/stocks/bars";
          //  URL url = new URL("https://data.alpaca.markets/v2/stocks/bars?symbols=AAPL&timeframe=15Min&start=2025-05-18T00:00:00Z&end=2025-05-25T00:00:00Z&limit=1000&adjustment=raw&feed=sip&sort=asc");
            String actual_url=url.toString();
            System.out.println("App:58 Actual URL: " +  actual_url);
           // System.out.println("are they equal? "+ actual_url.equals(BASE_URL + "?" + queryParams));
            
            //API Request Setup
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();   
           
            conn.setRequestMethod("GET");
            conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
            conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
            conn.setRequestProperty("accept", "application/json");

                        
            int responseCode = conn.getResponseCode();
            System.out.println("App:71 Response Code: " + responseCode);

            BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );

            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println("App:84 Response of static url JSON:");
            System.out.println(response.toString());

            //storing the response in a DB
            JSONObject  jsonResponse = new JSONObject(response.toString());
            JSONObject bars = jsonResponse.getJSONObject("bars");
            System.out.println("App:90 Bars JSON Object length:"+bars.length());
            
            //connection to database and storing the data
            Connection connDb=DriverManager.getConnection("jdbc:mysql://localhost:3306/stockdb", "root", "root");
            String sql_insert = "INSERT INTO stock_prices (symbol,open, high, low, close, volume,timeframe,num_trades,vwap,add_date) VALUES (?, ?, ?, ?, ?, ?, ?,?, ?, ?)";
            
            System.out.println("App:100 sql_insert raw query: " + sql_insert);
            JSONArray symbol_bars = bars.getJSONArray(symbol);
            LocalDateTime time = LocalDateTime.now(); // current date and time
            DateTimeFormatter time_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String dateTimeString = time.format(time_formatter);

            for(int i=0;i<symbol_bars.length();i++)
             {
                JSONObject bar = symbol_bars.getJSONObject(i);
                double open = bar.getDouble("o");
                double high = bar.getDouble("h");
                double low = bar.getDouble("l");
                double close = bar.getDouble("c");
                long volume = bar.getLong("v");
                String timeframe = bar.getString("t");
                int numTrades = bar.getInt("n");
                double vwap = bar.getDouble("vw");
                String addDate = dateTimeString;

                // Prepare and execute the insert statement
                try (var preparedStatement = connDb.prepareStatement(sql_insert)) {
                    preparedStatement.setString(1, symbol);
                    preparedStatement.setDouble(2, open);
                    preparedStatement.setDouble(3, high);
                    preparedStatement.setDouble(4, low);
                    preparedStatement.setDouble(5, close);
                    preparedStatement.setLong(6, volume);
                    preparedStatement.setString(7, timeframe);
                    preparedStatement.setInt(8, numTrades);
                    preparedStatement.setDouble(9, vwap);
                    preparedStatement.setString(10, addDate);

                    int rowsAffected = preparedStatement.executeUpdate();
                    System.out.println("App:123 Rows affected: " + rowsAffected);
                }
             }
        } 

     catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public static Map<String, String> getMarketTimeWindowUtc() {
        // Define the Eastern Time Zone and formatter
        ZoneId eastern = ZoneId.of("America/New_York");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        //initialize the list to hold market days
        List<ZonedDateTime> marketDays = new ArrayList<>();
        ZonedDateTime current = ZonedDateTime.now(eastern).withHour(16).withMinute(0).withSecond(0).withNano(0);

        // Step 1: Skip to previous weekday if today is Sat/Sun
        while (current.getDayOfWeek() == DayOfWeek.SATURDAY || current.getDayOfWeek() == DayOfWeek.SUNDAY) {
            current = current.minusDays(1);
        }

        // Step 2: Collect 7 previous weekdays
        while (marketDays.size() < 7) {
            if (current.getDayOfWeek().getValue() >= 1 && current.getDayOfWeek().getValue() <= 5) {
                marketDays.add(current);
            }
            current = current.minusDays(1);
        }

        // Step 3: Get earliest and latest valid market timestamps in UTC
        ZonedDateTime startET = marketDays.get(6).withHour(9).withMinute(30); // Oldest day start
        ZonedDateTime endET = marketDays.get(0).withHour(16).withMinute(0);   // Newest day end

        ZonedDateTime startUTC = startET.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime endUTC = endET.withZoneSameInstant(ZoneOffset.UTC);

        Map<String, String> result = new HashMap<>();
        result.put("start", formatter.format(startUTC));
        result.put("end", formatter.format(endUTC));

        return result;
    }*/

