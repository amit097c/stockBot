package com.stock;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Driver;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.stock.api.AlpacaApiClient;
import com.stock.dao.StockPriceDAO;
import com.stock.model.StockBar;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Hello world!
 *
 */
public class App 
{

    public static void main(String[] args) {
        String symbol = "AAPL";

        AlpacaApiClient apiClient = new AlpacaApiClient();
        List<StockBar> bars = apiClient.fetchStockBars(symbol);

        StockPriceDAO dao = new StockPriceDAO();
        for (StockBar bar : bars) {
            dao.saveStockBar(bar);
        }
    }
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
}
