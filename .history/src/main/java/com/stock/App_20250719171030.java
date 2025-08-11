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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down... Closing DB connections.");
            dao.close();
            orderDao.close();
        }));
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

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(symbolCloseVolMap.size());

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
            }, 0, INTERVAL_SECONDS, TimeUnit.SECONDS);
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
        computeStdDevs(previousClose, volatility);

        StockBar bar = apiClient.fetchLatest1MinBar(symbol);
        if (bar == null) {
            System.out.println("No stock bar found for symbol: " + symbol);
            return;
        }

        boolean isBuySignal = isBuySignal(bar);
        boolean isSellSignal = isSellSignal(bar, orderDao.getCurrentHoldings(symbol));
        String breachLow = detectLowBreach(bar.getLow());
        String breachHigh = detectHighBreach(bar.getHigh());

        int deviationId = dao.saveStockVolStdDev(
                bar,
                LocalDate.now(),
                stdDevs,
                volatility,
                breachLow,
                breachHigh
        );

        try {
            int alpacaHoldings = apiClient.getBuyOrders(symbol);
            System.out.println("Current holdings for " + symbol + ": " + alpacaHoldings);
            if (isBuySignal && deviationId != -1 && alpacaHoldings < MAX_HOLDINGS) {
                double stopLoss = stdDevs.get("1.77").low;
                double takeProfit = stdDevs.get("1").low;
                apiClient.placeBracketOrder(symbol, 5, stopLoss, takeProfit);

                orderDao.placeBuyOrder(deviationId, symbol, bar.getLow(), 5, LocalDate.now(), volatility);
            } else if (alpacaHoldings >= MAX_HOLDINGS) {
                System.out.println("Buy blocked: Max holdings reached for " + symbol);
            }
        } catch (Exception e) {
            System.out.println("Could not fetch holdings from Alpaca: " + e.getMessage());
        }
    }

    private void computeStdDevs(double previousClose, double volatility) {
        StandardDeviationCal cal = new StandardDeviationCal();
        double[] levels = {1, 1.25, 1.50, 1.75, 1.77, 2, 2.25, 2.50, 2.75, 3};

        for (double level : levels) {
            double dev = cal.compute(previousClose, volatility, level);
            stdDevs.put(String.valueOf(level), new StdDevRange(level, previousClose - dev, previousClose + dev));
        }
    }

    private boolean isBuySignal(StockBar bar) {
        return bar.getLow() < stdDevs.get("1.50").low;
    }

    private boolean isSellSignal(StockBar bar, int currentHoldings) {
        return bar.getOpen() > stdDevs.get("1.50").low && currentHoldings > 0;
    }

    private String detectLowBreach(double low) {
        for (String level : List.of("3", "2.75", "2.50", "2.25", "2", "1.77", "1.75", "1.50", "1.25", "1")) {
            if (low < stdDevs.get(level).low) {
                return "low_" + level + "_breach";
            }
        }
        return "none";
    }

    private String detectHighBreach(double high) {
        for (String level : List.of("3", "2.75", "2.50", "2.25", "2", "1.75", "1.50", "1.25", "1")) {
            if (high > stdDevs.get(level).high) {
                return "high_" + level + "_breach";
            }
        }
        return "none";
    }

    static class StdDevRange {
        double level;
        double low;
        double high;

        public StdDevRange(double level, double low, double high) {
            this.level = level;
            this.low = low;
            this.high = high;
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

