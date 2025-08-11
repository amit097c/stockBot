package com.stock.api;


import com.stock.model.StockBar;
import com.stock.util.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private static final String API_KEY = "PK79RXU9CFAQCUTLZYPA";//"PKMXFOXOMDPMNUMB3TXR";
    private static final String API_SECRET = "LGhrv4eZImBC9ilYlzuLVydUiufXruFh6aoHUeFO";//"64LdMS9gQnAf4bm8el3NIhNkuuj3slpG3lfvde6p";
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
        
          System.out.println("AlpacaApiClient::fetchLatest1MinBar:82 Fetching data for date: " + symbol);
          StockBar latestBar = null;
          try {
                String queryParams = String.format("symbols=%s&feed=iex", URLEncoder.encode(symbol, "UTF-8"));
                URL url = new URL("https://data.alpaca.markets/v2/stocks/bars/latest?" + queryParams);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
                conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
                conn.setRequestProperty("accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("API Error for " + symbol + ": " + responseCode);
                    return null;
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject json = new JSONObject(response.toString());
                JSONObject bars = json.getJSONObject("bars");

                if (!bars.has(symbol)) {
                    System.out.println("No latest bar found for symbol: " + symbol);
                    return null;
                }

                JSONObject bar = bars.getJSONObject(symbol);

                latestBar = new StockBar(
                    symbol,
                    bar.getDouble("o"),
                    bar.getDouble("h"),
                    bar.getDouble("l"),
                    bar.getDouble("c"),
                    bar.getLong("v"),
                    bar.getString("t"),         // timestamp (ISO format)
                    bar.optInt("n", 0),         // number of trades (optional)
                    bar.optDouble("vw", 0.0),   // volume-weighted price (optional)
                    TimeUtils.getCurrentDateTime()
                );

                } catch (Exception e) {
                    System.err.println("Error fetching latest bar for " + symbol);
                    e.printStackTrace();
                }
          return latestBar;
     }

    public StockBar fetchLatest1HrBar(String symbol) 
     {
        
          System.out.println("AlpacaApiClient::fetchLatest1MinBar:82 Fetching data for date: " + symbol);
          StockBar latestBar = null;
          try {
                String queryParams = String.format("symbols=%s&timeframe=1Hour&limit=1&feed=iex", URLEncoder.encode(symbol, "UTF-8"));
                URL url = new URL("https://data.alpaca.markets/v2/stocks/bars/latest?" + queryParams);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
                conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
                conn.setRequestProperty("accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("API Error for " + symbol + ": " + responseCode);
                    return null;
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject json = new JSONObject(response.toString());
                JSONObject bars = json.getJSONObject("bars");

                if (!bars.has(symbol)) {
                    System.out.println("No latest bar found for symbol: " + symbol);
                    return null;
                }

                JSONObject bar = bars.getJSONObject(symbol);

                latestBar = new StockBar(
                    symbol,
                    bar.getDouble("o"),
                    bar.getDouble("h"),
                    bar.getDouble("l"),
                    bar.getDouble("c"),
                    bar.getLong("v"),
                    bar.getString("t"),         // timestamp (ISO format)
                    bar.optInt("n", 0),         // number of trades (optional)
                    bar.optDouble("vw", 0.0),   // volume-weighted price (optional)
                    TimeUtils.getCurrentDateTime()
                );

                } catch (Exception e) {
                    System.err.println("Error fetching latest bar for " + symbol);
                    e.printStackTrace();
                }
          return latestBar;
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


    public void placePaperOrder(String symbol, int qty, String side, String type, String timeInForce) {
    try {
        URL url = new URL(PAPER_BASE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);
        order.put("side", side); // "buy" or "sell"
        order.put("type", type); // "market"
        order.put("time_in_force", timeInForce); // "gtc"

        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        System.out.println("Order placed: " + content);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
public int getBuyOrders(String symbol) throws IOException {
    String query = String.format("status=all&symbol=%s&side=buy&limit=500", URLEncoder.encode(symbol, "UTF-8"));
    URL url = new URL("https://paper-api.alpaca.markets/v2/orders?" + query);

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
    conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
    conn.setRequestProperty("accept", "application/json");

    int code = conn.getResponseCode();
    if (code != 200) throw new IOException("Failed to get orders. Code: " + code);

    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = in.readLine()) != null) response.append(line);
    in.close();
    System.out.println("AlpacaApiClient::getBuyOrders:372  response: " + response.toString());
    //return new JSONArray(response.toString());  // Array of order JSON objects
     JSONArray orders = new JSONArray(response.toString());
    int count = 0;
    for (int i = 0; i < orders.length(); i++) {
        JSONObject order = orders.getJSONObject(i);
        if (order.getString("symbol").equalsIgnoreCase(symbol)) {
            count++;
        }
    }

    return count;
}
public int getPositionQty(String symbol) throws IOException {
    URL url = new URL("https://paper-api.alpaca.markets/v2/orders" + symbol);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
    conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
    conn.setRequestProperty("accept", "application/json");

    int code = conn.getResponseCode();
    if (code == 404) return 0; // No open position
    if (code != 200) throw new IOException("Alpaca error: " + code);

    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = in.readLine()) != null) response.append(line);
    in.close();

    JSONObject json = new JSONObject(response.toString());
    return Math.abs(json.getInt("qty")); // Positive for long, negative for short
}


public void placeBracketOrder(String symbol, int qty, double stopLossPrice, double takeProfitPrice) {
    try {
        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject stopLoss = new JSONObject();
        stopLoss.put("stop_price", String.format("%.2f", stopLossPrice));
        stopLoss.put("limit_price", String.format("%.2f", stopLossPrice - 0.20)); // buffer

        JSONObject takeProfit = new JSONObject();
        takeProfit.put("limit_price", String.format("%.2f", takeProfitPrice));

        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", String.valueOf(qty));  // ✅ string, not int
        order.put("side", "buy");
        order.put("type", "market");
        order.put("time_in_force", "day");
        order.put("order_class", "bracket");
        order.put("stop_loss", stopLoss);
        order.put("take_profit", takeProfit);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            System.out.println("✅ Bracket order placed: " + response);
        } else {
            // 🔍 Read error response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                error.append(line);
            }
            in.close();
            System.err.println("Alpaca API error (" + responseCode + "): " + error);
        }

    } catch (Exception e) {
        System.err.println("Exception placing bracket order: " + e.getMessage());
        e.printStackTrace();
    }
}

}

    

