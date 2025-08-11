package com.stock.api;


import com.stock.model.StockBar;
import com.stock.util.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AlpacaApiClient 
{
    private static final String API_KEY = "PKMXFOXOMDPMNUMB3TXR";
    private static final String API_SECRET = "64LdMS9gQnAf4bm8el3NIhNkuuj3slpG3lfvde6p";
    private static final String BASE_URL = "https://data.alpaca.markets/v2/stocks/bars";
    private static final String PAPER_BASE_URL="https://paper-api.alpaca.markets/v2/orders";
    public List<StockBar> fetchStockBars(String symbol) {
        List<StockBar> result = new ArrayList<>();
        try {
            String queryParams = String.format(
                "symbols=%s&timeframe=1Min&start=%s&end=%s&limit=1000&adjustment=raw&feed=sip&sort=asc",
                symbol, TimeUtils.getTimeWindow().get("start"), TimeUtils.getTimeWindow().get("end")
            );

            URL url = new URL(BASE_URL + "?" + queryParams);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
            conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
            conn.setRequestProperty("accept", "application/json");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray barsArray = jsonResponse.getJSONObject("bars").getJSONArray(symbol);

            for (int i = 0; i < barsArray.length(); i++) {
                JSONObject bar = barsArray.getJSONObject(i);
                StockBar stockBar = new StockBar(
                        symbol,
                        bar.getDouble("o"),
                        bar.getDouble("h"),
                        bar.getDouble("l"),
                        bar.getDouble("c"),
                        bar.getLong("v"),
                        bar.getString("t"),
                        bar.getInt("n"),
                        bar.getDouble("vw"),
                        TimeUtils.getCurrentDateTime()
                );
                result.add(stockBar);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
    public StockBar fetchLatest1MinBar(String symbol) 
     {
        
          System.out.println("AlpacaApiClient::fetchLatest1MinBar:80 Fetching data for date: " + symbol);
          StockBar latestBar = null;
          try
           {
            String queryParams = String.format(
                    "symbols=%s&timeframe=1Min&limit=1&adjustment=raw&feed=sip&sort=asc",
                    symbol
            );
               URL url = new URL(BASE_URL + "?" + queryParams);
               HttpURLConnection conn = (HttpURLConnection) url.openConnection();
               conn.setRequestMethod("GET");
               conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
               conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
               conn.setRequestProperty("accept", "application/json");
               int responseCode = conn.getResponseCode();
               if (responseCode != 200) {
                System.out.println("API Error: " + responseCode);
                return latestBar;
                 }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray symbolBars = json.getJSONObject("bars").optJSONArray(symbol);
            if (symbolBars == null || symbolBars.length() == 0) {
                System.out.println("No data for " + symbol);
                return latestBar;
            }
            if (symbolBars.length() > 0) {
            JSONObject bar = symbolBars.getJSONObject(0);
            return new StockBar(
                        symbol,
                        bar.getDouble("o"),
                        bar.getDouble("h"),
                        bar.getDouble("l"),
                        bar.getDouble("c"),
                        bar.getLong("v"),
                        bar.getString("t"),
                        bar.getInt("n"),
                        bar.getDouble("vw"),
                        TimeUtils.getCurrentDateTime()
                );
            }
          }
          catch (Exception e) 
           {
            System.err.println("Error fetching latest 1Min bar for " + symbol);
            e.printStackTrace();
          } 
          return null;
     }
    public List<StockBar> fetchStockBarsForDay(String symbol, LocalDate date) {
        List<StockBar> bars = new ArrayList<>();
       
        // Skip weekends
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            System.out.println("Skipping weekend: " + date);
            return bars;
        }
        System.out.println("Fetching data for date: " + date);
        try {
            // Set start and end times for market hours in America/New_York
            ZoneId eastern = ZoneId.of("America/New_York");
            ZonedDateTime startET = date.atTime(9, 30).atZone(eastern);
            ZonedDateTime endET = date.atTime(16, 0).atZone(eastern);

            ZonedDateTime startUTC = startET.withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endUTC = endET.withZoneSameInstant(ZoneOffset.UTC);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            String startStr = formatter.format(startUTC);
            String endStr = formatter.format(endUTC);

            String queryParams = String.format(
                    "symbols=%s&timeframe=1Min&start=%s&end=%s&limit=1000&adjustment=raw&feed=sip&sort=asc",
                    symbol, startStr, endStr
            );

            URL url = new URL(BASE_URL + "?" + queryParams);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
            conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
            conn.setRequestProperty("accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.out.println("API Error on date " + date + ": " + responseCode);
                return bars;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray symbolBars = json.getJSONObject("bars").optJSONArray(symbol);

            if (symbolBars == null) {
                System.out.println("No data for " + symbol + " on " + date);
                return bars;
            }

            for (int i = 0; i < symbolBars.length(); i++) {
                JSONObject bar = symbolBars.getJSONObject(i);
                StockBar stockBar = new StockBar(
                        symbol,
                        bar.getDouble("o"),
                        bar.getDouble("h"),
                        bar.getDouble("l"),
                        bar.getDouble("c"),
                        bar.getLong("v"),
                        bar.getString("t"),
                        bar.getInt("n"),
                        bar.getDouble("vw"),
                        TimeUtils.getCurrentDateTime()
                );
                bars.add(stockBar);
            }

        } catch (Exception e) {
            System.err.println("Error fetching data for " + date);
            e.printStackTrace();
        }

        return bars;
    }

    public void placeMarketOrder(String symbol, int qty, String side) {
        try {
            System.out.println("Placing " + side + " order for " + qty + " shares of " + symbol);
            URL url = new URL(PAPER_BASE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
            conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // JSON body
            String jsonBody = String.format(
                    "{\"symbol\":\"%s\",\"qty\":\"%d\",\"side\":\"%s\",\"type\":\"market\",\"time_in_force\":\"day\"}",
                    symbol, qty, side.toLowerCase()
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode == 200 || responseCode == 201) {
                System.out.println("Order placed successfully.");
            } else {
                System.out.println("Failed to place order. Response Code: " + responseCode);
                conn.getErrorStream().transferTo(System.out); // show error body
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

    

