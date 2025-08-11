package com.stock.api;


import com.stock.App.OcoFill;
import com.stock.model.StockBar;
import com.stock.util.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
    public LocalDate getPreviousWeekDay() {
      
        LocalDate today = LocalDate.now(ZoneId.of("America/New_York"));
        return today.minusWeeks(0).with(DayOfWeek.MONDAY); // Adjust to previous DAY
    }

public List<StockBar> fetchBarsForPreviousWeek(String symbol) {
    List<StockBar> bars = new ArrayList<>();
    try {
        LocalDate previousWeekDay = getPreviousWeekDay();
        ZonedDateTime startNY = previousWeekDay.atTime(9, 30).atZone(ZoneId.of("America/New_York"));
        ZonedDateTime endNY = previousWeekDay.atTime(16, 0).atZone(ZoneId.of("America/New_York"));
        

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
        // Convert to ISO-8601 UTC format
        String start = startNY.withZoneSameInstant(ZoneOffset.UTC).format(formatter);
        String end = endNY.withZoneSameInstant(ZoneOffset.UTC).format(formatter);


       // String start = startNY.withZoneSameInstant(ZoneOffset.UTC).toString();
        //String end = endNY.withZoneSameInstant(ZoneOffset.UTC).toString();


        //String start = previousMonday + "T09:30:00-06:00";  // NYSE open
        //String end = previousMonday + "T16:00:00-06:00";    // NYSE close

        String queryParams = String.format(
            "symbols=%s&start=%s&end=%s&feed=iex&timeframe=1Min",
            URLEncoder.encode(symbol, "UTF-8"),
            start,
            end
        );

        URL url = new URL("https://data.alpaca.markets/v2/stocks/bars?" + queryParams);
        System.out.println("AlpacaApiClient::fetchBarsForPreviousWeek:169 Request URL: " + url); 

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            System.out.println("API Error for " + symbol + ": " + responseCode);
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
        JSONObject barsObj = json.getJSONObject("bars");

        if (!barsObj.has(symbol)) {
            System.out.println("No bars found for symbol: " + symbol);
            return bars;
        }

        JSONArray symbolBars = barsObj.getJSONArray(symbol);
        for (int i = 0; i < symbolBars.length(); i++) {
            JSONObject bar = symbolBars.getJSONObject(i);
            StockBar sb = new StockBar(
                symbol,
                bar.getDouble("o"),
                bar.getDouble("h"),
                bar.getDouble("l"),
                bar.getDouble("c"),
                bar.getLong("v"),
                bar.getString("t"),
                bar.optInt("n", 0),
                bar.optDouble("vw", 0.0),
                bar.getString("t")
            );
            bars.add(sb);
        }

    } catch (Exception e) {
        System.err.println("Error fetching previous Monday bars for " + symbol);
        e.printStackTrace();
    }
    return bars;
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

        try (OutputStream os = conn.getOutputStream()) { // sends json order to alapca servers
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
    String query = String.format("status=open&symbol=%s&side=buy&limit=500", URLEncoder.encode(symbol, "UTF-8"));
   // URL url = new URL("https://paper-api.alpaca.markets/v2/orders?" + query);
    URL url = new URL(PAPER_BASE_URL+"?"+ query);
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
    //System.out.println("AlpacaApiClient::getBuyOrders:372  response: ");
    //return new JSONArray(response.toString());  // Array of order JSON objects
    JSONArray orders = new JSONArray(response.toString());
    //System.out.println("AlpacaApiClient::getBuyOrders:372  response orders length: "+orders.length());
    return orders.length(); // Return count of open buy orders for the symbol
    /*int count = 0;
    for (int i = 0; i < orders.length(); i++) {
        JSONObject order = orders.getJSONObject(i);
        if (order.getString("symbol").equalsIgnoreCase(symbol)
            && !order.getString("status").equalsIgnoreCase("filled")
        && order.getString("side").equalsIgnoreCase("buy")) {
            count++;
        }
    }
    return count;*/
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

    public int placeBuyOrder(String symbol,int qty,String order_id)
    {
        try{
            URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
          
            conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
            conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
            conn.setRequestProperty("Content-Type", "application/json");
             conn.setDoOutput(true);
            JSONObject order = new JSONObject();
            order.put("symbol", symbol);
            order.put("qty", qty);  
            order.put("side", "buy");  
            order.put("type", "market");
            order.put("time_in_force", "day");
            order.put("client_order_id",order_id);
            
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
                System.out.println("AlpacaApiClient::buyOrder:529 Order placed: " + response);
                return responseCode;
        }
        else {
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
        }
        catch(Exception e)
        {
            System.err.println("Exception placing bracket order: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }
    public double waitForFilledPrice(String clientOrderId, int maxRetries, int intervalMillis) {
    int attempts = 0;
    double price = -1;

    while (attempts < maxRetries) {
        price = getOrderFilledPrice(clientOrderId);

        if (price != -1) {
            System.out.println("AlpacaApiClientOrder::waitForFilledPrice:561 filled after " + attempts + " retries. Filled price: " + price);
            return price;
        }

        System.out.println("AlpacaApiClientOrder::waitForFilledPrice:565 filled after Waiting for order to fill... Attempt: " + (attempts + 1));
        attempts++;

        try {
            Thread.sleep(intervalMillis);  // wait before retrying
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }

    System.err.println("Order not filled after " + maxRetries + " retries.");
    return -1;
}
private JSONObject getOrderByClientIdNested(String clientOrderId) throws IOException {
    String base = "https://paper-api.alpaca.markets/v2/orders";
    String byCidUrl = base + ":by_client_order_id?client_order_id="
            + java.net.URLEncoder.encode(clientOrderId, java.nio.charset.StandardCharsets.UTF_8);

    // 1) Get the order by client_order_id
    JSONObject o = httpGetJson(byCidUrl);  // this response has fields like id, client_order_id, status, etc.

    // 2) If legs are not included, fetch again by the order UUID with nested=true
    if (!o.has("legs")) {
        if (!o.has("id")) {
            throw new IOException("Order response missing 'id' for client_order_id=" + clientOrderId + ": " + o);
        }
        String id = o.getString("id");     // <-- this is the order UUID from the first response
        String byIdUrl = base + "/" + id + "?nested=true";
        o = httpGetJson(byIdUrl);
    }
    return o;
}

public OcoFill waitForOcoFillByClientId(String ocoClientOrderId, long timeoutMs, long pollMs) throws InterruptedException {
    long start = System.currentTimeMillis();
    long backoff = pollMs;

    // 1) Resolve parent + legs once
    JSONObject parent;
    try {
        parent = getOrderByClientIdNested(ocoClientOrderId); // must include legs
    } catch (IOException e) {
        System.out.println("Failed to fetch OCO parent: " + e.getMessage());
        return null;
    }

    String tpId = parent.optString("id"); // TP is the parent
    String slId = null;
    JSONArray legs = parent.optJSONArray("legs");
    if (legs != null) {
        for (int i = 0; i < legs.length(); i++) {
            JSONObject leg = legs.getJSONObject(i);
            String type = leg.optString("type");
            if ("stop".equalsIgnoreCase(type) || "stop_limit".equalsIgnoreCase(type)) {
                slId = leg.getString("id");
            }
        }
    }
    if (slId == null || tpId == null) {
        System.out.println("OCO legs not found for client_order_id=" + ocoClientOrderId);
        return null;
    }

    // 2) Poll both legs (2–3s cadence) with 429 backoff
    while (System.currentTimeMillis() - start < timeoutMs) {
        try {
            JSONObject tp = getOrderById(tpId);
            JSONObject sl = getOrderById(slId);

            String tpStatus = tp.optString("status"); // new/accepted/partially_filled/filled/canceled
            String slStatus = sl.optString("status");

            if ("filled".equalsIgnoreCase(tpStatus)) {
                double px = tp.optDouble("filled_avg_price", Double.NaN);
                return new OcoFill("TP", px, tpId);
            }
            if ("filled".equalsIgnoreCase(slStatus)) {
                double px = sl.optDouble("filled_avg_price", Double.NaN);
                return new OcoFill("SL", px, slId);
            }

            // If SL filled, TP will be canceled by server; if TP filled, SL will be canceled.
            // You can also detect: if one is "canceled" and the other is "filled".
            if ("canceled".equalsIgnoreCase(tpStatus) && "filled".equalsIgnoreCase(slStatus)) {
                double px = sl.optDouble("filled_avg_price", Double.NaN);
                return new OcoFill("SL", px, slId);
            }
            if ("canceled".equalsIgnoreCase(slStatus) && "filled".equalsIgnoreCase(tpStatus)) {
                double px = tp.optDouble("filled_avg_price", Double.NaN);
                return new OcoFill("TP", px, tpId);
            }

            Thread.sleep(pollMs);
            backoff = pollMs; // reset after a successful cycle
        } catch (IOException e) {
            // 429 or transient: back off and retry
            Thread.sleep(backoff);
            backoff = Math.min((long)(backoff * 1.5), 15_000L);
        }
    }
    return null; // timed out
}

private JSONObject getOrderById(String orderId) throws IOException {
    return httpGetJson("https://paper-api.alpaca.markets/v2/orders/" + orderId);
}

private JSONObject httpGetJson(String url) throws IOException {
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    c.setRequestMethod("GET");
    c.setRequestProperty("APCA-API-KEY-ID", API_KEY);
    c.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
    int code = c.getResponseCode();
    InputStream is = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
    String body = is != null ? new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "";
    if (code == 429) throw new IOException("429 " + body);
    if (code >= 300) throw new IOException("Alpaca error " + code + ": " + body);
    return new org.json.JSONObject(body);
}
    public double getOrderFilledPrice(String clientOrderId) {
        try {
            URL url = new URL("https://paper-api.alpaca.markets/v2/orders:by_client_order_id?client_order_id=" + clientOrderId);
            //URL url = new URL("https://paper-api.alpaca.markets/v2/orders:by_client_order_id?client_order_id=" + clientOrderId);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
            conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());
            System.out.println("AlpacaApiClient::getOrderFilledPrice:569 Response: " + json);
         //   return json.getDouble("filled_avg_price");
            String status = json.optString("status", "");
        if (!"filled".equalsIgnoreCase(status)) {
            System.err.println("AlpacaApiClient::getOrderFilledPrice:602 Order not filled yet. Status: " + status);
            return -1;
        }

        // Check if filled_avg_price exists and is not null
        if (!json.has("filled_avg_price") || json.isNull("filled_avg_price")) {
            System.err.println("AlpacaApiClient::getOrderFilledPrice:608 filled_avg_price is not available yet.");
            return -1;
        }

        return json.getDouble("filled_avg_price");

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
public double getLastTradePrice(String symbol) throws IOException {
    String endpoint = String.format("https://data.alpaca.markets/v2/stocks/%s/trades/latest", URLEncoder.encode(symbol, "UTF-8"));
    URL url = new URL(endpoint);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
    conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
    conn.setRequestProperty("accept", "application/json");

    int responseCode = conn.getResponseCode();
    if (responseCode != 200) {
        throw new IOException("Failed to fetch last trade price. Code: " + responseCode);
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    StringBuilder response = new StringBuilder();
    String inputLine;
    while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
    }
    in.close();

    JSONObject json = new JSONObject(response.toString());
    if (!json.has("trade") || !json.getJSONObject("trade").has("p")) {
        throw new IOException("Invalid response while fetching trade price.");
    }

    return json.getJSONObject("trade").getDouble("p"); // "p" is the price field
}

public int placeLongBuyBracketOrder(String symbol, int qty, double stopLossLevel, double takeProfitLevel,String order_id) {
    try {
        double entryPrice = getLastTradePrice(symbol);
        System.out.printf("Entry price for %s: %.4f%n", symbol, entryPrice);

        // Ensure stop loss is below entry and take profit is above
        double stopPrice = Math.min(stopLossLevel, entryPrice - 0.01);
        double stopLimitPrice = stopPrice - 0.20;  // Small buffer below stop price
        double takeProfitPrice = Math.max(takeProfitLevel, entryPrice + 0.01);

        // Log adjusted levels
        System.out.printf("Adjusted STOP price: %.4f | STOP LIMIT: %.4f | TAKE PROFIT: %.4f%n",
                stopPrice, stopLimitPrice, takeProfitPrice);

        // Build JSON payload
        JSONObject take = new JSONObject();
        take.put("limit_price", takeProfitPrice);

        JSONObject stop = new JSONObject();
        stop.put("stop_price", stopPrice);
        stop.put("limit_price", stopLimitPrice);

        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);
        order.put("side", "buy");
        order.put("type", "market");
        order.put("time_in_force", "day");
        order.put("order_class", "bracket");
        order.put("take_profit", take);
        order.put("stop_loss", stop);
        order.put("client_order_id", order_id); // Add client order ID

        // Submit order
        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200 && responseCode != 201) {
            System.err.printf("Bracket order failed for %s. HTTP %d%n", symbol, responseCode);
            return -1;
        }

        System.out.printf("✅ Bracket order placed for %s: qty=%d, entry=%.2f, SL=%.2f, TP=%.2f%n",
                symbol, qty, entryPrice, stopPrice, takeProfitPrice);
        return 1;

    } catch (Exception e) {
        System.err.printf("❌ Error placing long buy bracket for %s: %s%n", symbol, e.getMessage());
        e.printStackTrace();
        return -1;
    }
}
public int placeSellBracketOrder(String symbol, int qty, double targetEntry, double stopLossHigh, double takeProfitHigh) {
    try {
        double entryPrice = getLastTradePrice(symbol);
        System.out.printf("Entry price for %s: %.4f%n", symbol, entryPrice);

        // Adjust levels relative to live price
        double stopPrice = Math.max(stopLossHigh, entryPrice + 0.01);  // must be above
        double stopLimitPrice = stopPrice + 0.20;  // small buffer
        double takeProfitPrice = Math.min(takeProfitHigh, entryPrice - 0.01);  // must be below

        // Debug
        System.out.printf("Adjusted STOP: %.4f | STOP LIMIT: %.4f | TAKE PROFIT: %.4f%n",
                stopPrice, stopLimitPrice, takeProfitPrice);

        JSONObject take = new JSONObject();
        take.put("limit_price", takeProfitPrice);

        JSONObject stop = new JSONObject();
        stop.put("stop_price", stopPrice);
        stop.put("limit_price", stopLimitPrice);

        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);
        order.put("side", "sell");
        order.put("type", "market");
        order.put("time_in_force", "gtc");
        order.put("order_class", "bracket");
        order.put("take_profit", take);
        order.put("stop_loss", stop);

        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        System.out.println("AlpacaApiClient::placeSellBracketOrder:789  responseCode: " + responseCode);
        if (responseCode != 200 && responseCode != 201) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println(line);
            }
            return -1;
        }

        return 1;

    } catch (Exception e) {
        System.err.println("Error placing sell bracket order: " + e.getMessage());
        e.printStackTrace();
        return -1;
    }
}
public int placeSellBracketOrder(String symbol, int qty, double stopLossLevel, double takeProfitLevel,String order_id) {
     try {
        double entryPrice = getLastTradePrice(symbol);
        System.out.printf("Entry price for %s: %.4f%n", symbol, entryPrice);

        // ✅ Enforce API constraint: stop >= base + 0.01, take <= base - 0.01
        double stopPrice = Math.max(stopLossLevel, entryPrice + 0.01);
        double stopLimitPrice = stopPrice + 0.20;  // buffer
        double takeProfitPrice = Math.min(takeProfitLevel, entryPrice - 0.01);

        System.out.printf("Adjusted STOP: %.4f | STOP LIMIT: %.4f | TAKE PROFIT: %.4f%n",
                stopPrice, stopLimitPrice, takeProfitPrice);

        JSONObject take = new JSONObject();
        take.put("limit_price", takeProfitPrice);

        JSONObject stop = new JSONObject();
        stop.put("stop_price", stopPrice);
        stop.put("limit_price", stopLimitPrice);

        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);
        order.put("side", "sell");
        order.put("type", "market");
        order.put("time_in_force", "gtc");
        order.put("order_class", "bracket");
        order.put("take_profit", take);
        order.put("stop_loss", stop);

        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        System.out.println("AlpacaApiClient::placeSellBracketOrder: Response Code = " + responseCode);

        if (responseCode == 200 || responseCode == 201) {
            System.out.println("✅ Sell bracket order placed successfully.");
            return 1;
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line);
            }
            System.err.println("❌ Alpaca API error (" + responseCode + "): " + error);
            return -1;
        }

    } catch (Exception e) {
        System.err.println("❌ Error placing sell bracket order: " + e.getMessage());
        e.printStackTrace();
        return -1;
    }
}


public void placeBracketOrder(String symbol, int qty, double stopLossPrice, double takeProfitPrice,String orderId) {
    try {
        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject stopLoss = new JSONObject();
        //169.09
        stopLoss.put("stop_price", stopLossPrice);
        stopLoss.put("limit_price", stopLossPrice - 0.20);  // buffer

        JSONObject takeProfit = new JSONObject();
        takeProfit.put("limit_price", takeProfitPrice);

        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);  
        order.put("side", "buy");  
        order.put("type", "market");
        order.put("time_in_force", "day");
        order.put("order_class", "bracket");
        order.put("stop_loss", stopLoss);
        order.put("take_profit", takeProfit);
        order.put("client_order_id",orderId);


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
            System.out.println("AlpacaApiClient:placeBracketOrder:909 Bracket order placed: " + response);
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
public String placeOcoBuyToClose(String symbol, int qty, double takeProfitLimitBuy, double stopLossBuy,double shortSellEntryPrice)
 {
    try{

        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        if(stopLossBuy>shortSellEntryPrice && takeProfitLimitBuy<shortSellEntryPrice)
        {
            System.out.println("AlpacaApiClient::placeOcoBuyToClose:907 Stop loss buy and takeProfit limit is in range of short sell entry price. stopLossBuy: "+stopLossBuy+" takeProfitLimitBuy: "+takeProfitLimitBuy+" shortSellEntryPrice: "+shortSellEntryPrice);
            String order_id=generateClientOrderId(symbol, "buy_to_close");
            JSONObject stopLoss = new JSONObject();
            stopLoss.put("stop_price", stopLossBuy);
            stopLoss.put("limit_price", stopLossBuy + 0.20);
            JSONObject takeProfit = new JSONObject();
            takeProfit.put("limit_price", takeProfitLimitBuy);
            
            JSONObject order = new JSONObject();
            order.put("symbol", symbol);
            order.put("qty", qty);
            order.put("side", "buy"); // SHORT SELL
            order.put("type", "limit");
            order.put("order_class", "oco");
            order.put("take_profit",takeProfit);
            order.put("stop_loss", stopLoss); // buffer above stop price    
            order.put("time_in_force", "day");
            order.put("client_order_id",order_id);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(order.toString().getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                System.out.println("AlpacaApiClient::placeOcoBuyToClose:947 Order placed successfully order_id: "+order_id);
                return order_id;
            }
            else {       
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    error.append(line);
                }
                in.close();
                System.err.println("AlpacaApiClient::placeOcoBuyToClose:959: Alpaca API error (" + responseCode + "): " + error);
                return null;
               }       
        }   
        else
        {
            System.err.println("AlpacaApiClient::placeOcoBuyToClose:964 Stop loss buy or take profit limit buy is not in range of short sell entry price. stopLossBuy: "+stopLossBuy+" takeProfitLimitBuy: "+takeProfitLimitBuy+" shortSellEntryPrice: "+shortSellEntryPrice);
            return null;
        }

    }
    catch(Exception e)
    {
        System.err.println("AlpacaApiClient::placeOcoBuyToClose:971 Exception in placeOcoBuyToClose: "+e.getMessage());
    }
    return null;
 }
public int placeShortSellOrder(String symbol, int qty,String order_id) {
    try {  

        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
            // ✅ JSON object for stop_loss
       // JSONObject stopLossJ = new JSONObject();
        //stopLossJ.put("stop_price", stopLoss);
        //stopLossJ.put("limit_price", stopLoss + 0.20);  // optional buffer above stop

        // ✅ JSON object for take_profit
        // JSONObject takeProfitJ = new JSONObject();
        // takeProfitJ.put("limit_price", takeProfit);


        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);
        order.put("side", "sell"); // SHORT SELL
        order.put("type", "market");
        order.put("time_in_force", "day");
        order.put("client_order_id",order_id);
        //order.put("order_class", "bracket");
       // order.put("stop_loss", stopLossJ);
       // order.put("take_profit", takeProfitJ );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            System.out.println("Short Sell Response: " + response);
            return status; // Return HTTP status code
        }

    } catch (Exception e) {
        System.err.println("Error placing short sell order: " + e.getMessage());
    }
    return -1; // Indicate failure
}


public int placeShortSellBracketOrder(String symbol, int qty, double stopLoss, double takeProfit) {
    try {
        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);
        order.put("side", "sell"); // SHORT SELL
        order.put("type", "market");
        order.put("time_in_force", "day");

        JSONObject take = new JSONObject();
        take.put("limit_price", takeProfit);
        
        JSONObject stop = new JSONObject();
        stop.put("stop_price", stopLoss);
        stop.put("limit_price", stopLoss + 0.25);  // buffer

        order.put("order_class", "bracket");
        order.put("take_profit", take);
        order.put("stop_loss", stop);

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
            System.out.println("Short Sell Bracket Order placed: " + response);
            return responseCode;
    }
    else {
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
        System.err.println("Exception placing short sell bracket order: " + e.getMessage());
    }
    return -1; // Indicate failure

 }
// public void coverShort(String symbol, int qty) {
//     try {

//         URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
//         HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//         conn.setRequestMethod("POST");
//         conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
//         conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
//         conn.setRequestProperty("Content-Type", "application/json");
//         conn.setDoOutput(true);

//         JSONObject order = new JSONObject();
//         order.put("symbol", symbol);
//         order.put("qty", qty);
//         order.put("side", "buy"); // BUY to cover
//         order.put("type", "market");
//         order.put("time_in_force", "day");

//         try (OutputStream os = conn.getOutputStream()) {
//             os.write(order.toString().getBytes(StandardCharsets.UTF_8));
//         }

//         int status = conn.getResponseCode();
//         InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
//         try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
//             String line;
//             StringBuilder response = new StringBuilder();
//             while ((line = br.readLine()) != null) {
//                 response.append(line);
//             }
//             System.out.println("Cover Response: " + response);
//         }


        

//     } catch (Exception e) {
//         System.err.println("Error placing cover order: " + e.getMessage());
//     }
// }
public boolean placeShortSellWithManualOCO(String symbol, int qty, double stopLossPrice, double takeProfitPrice,String order_id) {
    try {
        // 1. Market Sell to open short
        order_id=generateClientOrderId(symbol, "shortSell");
        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);
        order.put("side", "sell");
        order.put("type", "market");
        order.put("time_in_force", "day");
        order.put("client_order_id",order_id);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200 && responseCode != 201) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println(line);
            }
            return false;
        }
        System.out.println("AlpacaApiClient::placeShortSellWithManualOCO:1112 Short SELL executed. Thread: "+Thread.currentThread().getId());

        // 2. Place Stop Loss (Buy Stop Order)
        //placeStopBuy(symbol, qty, stopLossPrice);

        // 3. Place Take Profit (Buy Limit Order)
        //placeLimitBuy(symbol, qty, takeProfitPrice);

        return true;

    } catch (Exception e) {
        System.err.println("Error placing short sell order: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}
public String  placeLongEntry(String symbol, int qty,String type,double limitPrice)
 {
    if (!"market".equalsIgnoreCase(type) && !"limit".equalsIgnoreCase(type)) {
        throw new IllegalArgumentException("type must be market or limit");
    }
    if ("limit".equalsIgnoreCase(type) ) {
        throw new IllegalArgumentException("limitPrice required for limit orders");
    }
    limitPrice = Math.round(limitPrice * 100.0) / 100.0;
    String order_id=generateClientOrderId(symbol, "longEntry");
    try{
        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("side", "sell");
        order.put("type", type);
        order.put("qty", qty);
        order.put("limit_price", limitPrice);
        order.put("time_in_force", "day");
        order.put("client_order_id",order_id);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes(StandardCharsets.UTF_8));
        }
        int responseCode = conn.getResponseCode();
         if (responseCode == 200 || responseCode == 201) {
            System.out.println("AlpacaApiClient::placeShortEntry:1164 short entry order place for : " + symbol+ " order_id: " + order_id);
            return order_id; // Return order ID for further processing
         }
        else 
         {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("AlpacaApiClient::placeShortEntry:1174 Failed to place long buy order. Response code: " + responseCode+ " for symbol: " + symbol+ "order_id: " + order_id);
            return ""; 
         } 
    }
   catch(Exception e) {
        System.err.println("AlpacaApiClient::placeShortEntry:1179: Error placing short entry order: " + e.getMessage());
        e.printStackTrace();
    } 
    return "";
 }

public String  placeShortEntry(String symbol, int qty,String type,double limitPrice)
 {
    if (!"market".equalsIgnoreCase(type) && !"limit".equalsIgnoreCase(type)) {
        throw new IllegalArgumentException("type must be market or limit");
    }
    if ("limit".equalsIgnoreCase(type) ) {
        throw new IllegalArgumentException("limitPrice required for limit orders");
    }
    limitPrice = Math.round(limitPrice * 100.0) / 100.0;
    String order_id=generateClientOrderId(symbol, "shortEntry");
    try{
        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("side", "sell");
        order.put("type", type);
        order.put("qty", qty);
        order.put("limit_price", limitPrice);
        order.put("time_in_force", "day");
        order.put("client_order_id",order_id);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes(StandardCharsets.UTF_8));
        }
        int responseCode = conn.getResponseCode();
         if (responseCode == 200 || responseCode == 201) {
            System.out.println("AlpacaApiClient::placeShortEntry:1164 short entry order place for : " + symbol+ " order_id: " + order_id);
            return order_id; // Return order ID for further processing
         }
        else 
         {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("AlpacaApiClient::placeShortEntry:1174 Failed to place long buy order. Response code: " + responseCode+ " for symbol: " + symbol+ "order_id: " + order_id);
            return ""; 
         } 
    }
   catch(Exception e) {
        System.err.println("AlpacaApiClient::placeShortEntry:1179: Error placing short entry order: " + e.getMessage());
        e.printStackTrace();
    } 
    return "";
 }

public boolean placeLongBuyWithManualOCO(String symbol, int qty,String order_id) {
    try {
       
        // 1. Market Sell to open short
        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);
        order.put("side", "buy");
        order.put("type", "market");
        order.put("time_in_force", "day");
        order.put("client_order_id",order_id);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            System.out.println("AlpacaApiClient::placeLongSellWithManualOCO:1166 Long buy executed: " + symbol+ " order_id: " + order_id);
            // 2. Place Stop Loss (Buy Stop Order)
            //placeStopBuy(symbol, qty, stopLossPrice);
            // 3. Place Take Profit (Buy Limit Order)
            //placeLimitBuy(symbol, qty, takeProfitPrice);
            return true;
            
        }
        else
         {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            System.err.println("AlpacaApiClient::placeLongSellWithManualOCO:1164 Failed to place long buy order. Response code: " + responseCode+ " for symbol: " + symbol+ "order_id: " + order_id);
            return false;
         }


    } catch (Exception e) {
        System.err.println("Error placing short sell order: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}

public  String placeStopBuy(String symbol, int qty, double stopPrice) {
    int maxRetries = 5;
    int retryDelayMs = 2000; // 2 seconds delay between attempts

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
    
    try {
        stopPrice = Math.round(stopPrice * 100.0) / 100.0;

        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        String order_id=generateClientOrderId(symbol, "sl");
        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);
        order.put("side", "buy");
        order.put("type", "stop_limit");
        order.put("stop_price", stopPrice);
        order.put("limit_price", stopPrice-0.50);
        order.put("time_in_force", "day");
        order.put("client_order_id", order_id); // Add client order ID

        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes(StandardCharsets.UTF_8));
        }
        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            
            System.out.println("AlpacaApiClient:placeStopBuy:1218 Stop BUY  order placed: " + stopPrice);

            return order_id; 
        }
        else
         {
             // Log error response body
               InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                    String line;
                    System.err.println("Attempt " + attempt + " failed to place stop order (HTTP " + responseCode + "):");
                    while ((line = reader.readLine()) != null) {
                        System.err.println(line);
                    }
                } else {
                    System.err.println("Attempt " + attempt + " failed (HTTP " + responseCode + ") but no error stream available.");
                }
         }

    } catch (Exception e) {
        System.err.println("Error placing stop buy: " + e.getMessage());
        e.printStackTrace();        
    }
    if (attempt < maxRetries) {
            try {
                Thread.sleep(retryDelayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    System.err.println("All retry attempts failed for stop buy order for symbol: " + symbol);
    return ""; // or return null, based on your logic

}

public  String placeStopSellForLong(String symbol, int qty, double stopPrice) {
    int maxRetries = 5;
    int retryDelayMs = 2000; // 2 seconds delay between attempts

    for (int attempt = 1; attempt <= maxRetries; attempt++) {

    try {
        stopPrice = Math.round(stopPrice * 100.0) / 100.0;

        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        String order_id=generateClientOrderId(symbol, "stopSell");
        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);
        order.put("side", "sell");
        order.put("type", "stop_limit"); // required to include limit_price
        order.put("stop_price", stopPrice);               // triggers the order
        order.put("limit_price", stopPrice - 0.20);       // actual sell price
        order.put("time_in_force", "gtc");
        order.put("client_order_id", order_id);// Add client order ID

        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes(StandardCharsets.UTF_8));
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            System.out.println("AlpacaApiClient:placeStopBuy:1288 Stop Sell Long order placed: " + stopPrice);
            return order_id;
        } 
        else {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                String line;
                System.err.println("Attempt " + attempt + " failed to place stop order (HTTP " + responseCode + "):");
                while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                }
            } 
            else {
                System.err.println("AlpacaApiClient:placeStopSellForLong:1438:Attempt " + attempt + " failed with HTTP code " + responseCode + " but no error stream was provided.");
            }
        }
       return order_id; // Return the client order ID for tracking
    } catch (Exception e) {
        System.err.println("Error placing stop buy: " + e.getMessage());
        e.printStackTrace();        
    }
     if (attempt < maxRetries) {
            try {
                Thread.sleep(retryDelayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
        System.err.println("All retry attempts failed for stop buy order for symbol: " + symbol);
 
    return "";
}




public  String placeLimitSellForLong(String symbol, int qty, double limitPrice) {
        int maxRetries = 3;
    int retryDelayMs = 2000; // 2 seconds delay between attempts

    for (int attempt = 1; attempt <= maxRetries; attempt++) {

    
    try {
        limitPrice = Math.round(limitPrice * 100.0) / 100.0;

        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        String order_id=generateClientOrderId(symbol, "limitSell");
        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);
        order.put("side", "sell");
        order.put("type", "limit"); // required to include limit_price           
        order.put("limit_price", limitPrice);       // actual sell price
        order.put("time_in_force", "day");
        order.put("client_order_id", order_id);// Add client order ID

        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes(StandardCharsets.UTF_8));
        }
      
        int responseCode = conn.getResponseCode();
if (responseCode == 200 || responseCode == 201) {
    System.out.println("AlpacaApiClient:placeStopBuy:1288 limitSell Long order placed: " + limitPrice);
    return order_id;
} else {
    InputStream errorStream = conn.getErrorStream();
    if (errorStream != null) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
        String line;
        System.err.println("AlpacaApiClient:placeLimitSellForLong:1438: Attempt " + attempt + " failed to place stop order (HTTP " + responseCode + "):");
        while ((line = reader.readLine()) != null) {
            System.err.println(line);
        }
    } else {
        System.err.println("Attempt " + attempt + " failed with HTTP code " + responseCode + " but no error stream was provided.");
    }
}
         return order_id; // Return the client order ID for tracking
    } catch (Exception e) {
        System.err.println("Error placing stop buy: " + e.getMessage());
        e.printStackTrace();        
    }
     if (attempt < maxRetries) {
            try {
                Thread.sleep(retryDelayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
}
    System.err.println("All retry attempts failed for limit sell order for symbol: " + symbol);
    return "";
}
public boolean isOrderFilled(String clientOrderId) {
     if (clientOrderId == null || clientOrderId.trim().isEmpty()) {
        System.err.println("AlapacaApiClient:1163: Invalid clientOrderId provided for order status check.");
        return false;
    }
    try {
       // URL url = new URL("https://paper-api.alpaca.markets/v2/orders/client:" + clientOrderId);
       URL url = new URL("https://paper-api.alpaca.markets/v2/orders:by_client_order_id?client_order_id=" + clientOrderId);

       HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        JSONObject json = new JSONObject(response.toString());
        String status = json.optString("status", "");
        System.out.println("AlpacaApiClient:isOrderFilled:1179: Order status for " + clientOrderId + ": " + status);
        return "filled".equalsIgnoreCase(status);

    } catch (Exception e) {
        System.err.println("Error checking order status: " + e.getMessage());
        e.printStackTrace();
    }
    return false;
}   
public boolean cancelOrder(String orderId) {
    try {
        URL url = new URL("https://paper-api.alpaca.markets/v2/orders/" + orderId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode == 204) {
            System.out.println("AlpacaApiClient:cancelOrder:1199: Order canceled successfully: " + orderId);
            return true;
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            String line;
            System.err.println(" AlpacaApiClient:cancelOrder:1204:Failed to cancel order: " + orderId);
            while ((line = reader.readLine()) != null) {
                System.err.println(line);
            }
            return false;
        }

    } catch (Exception e) {
        System.err.println("Exception while canceling order: " + orderId);
        e.printStackTrace();
        return false;
    }
}

public boolean cancelOrderByClientId(String clientOrderId) {
    try {
        // Step 1: Get order info using client_order_id
        String getUrl = "https://paper-api.alpaca.markets/v2/orders:by_client_order_id?client_order_id=" + clientOrderId;
        HttpURLConnection getConn = (HttpURLConnection) new URL(getUrl).openConnection();
        getConn.setRequestMethod("GET");
        getConn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        getConn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        getConn.setRequestProperty("accept", "application/json");

        int getRespCode = getConn.getResponseCode();
        if (getRespCode != 200) {
            System.err.println("AlpacaApiClient:cancelOrderByClientId:1355 Failed to fetch order by client_order_id: " + clientOrderId);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(getConn.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) System.err.println(line);
            }
            return false;
        }

        // Read and parse response
        StringBuilder jsonBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getConn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) jsonBuilder.append(line);
        }

        String jsonResponse = jsonBuilder.toString();
        JSONObject orderJson = new JSONObject(jsonResponse);
        String orderId = orderJson.getString("id");

        // Step 2: Cancel the order using order_id
        URL deleteUrl = new URL("https://paper-api.alpaca.markets/v2/orders/" + orderId);
        HttpURLConnection deleteConn = (HttpURLConnection) deleteUrl.openConnection();
        deleteConn.setRequestMethod("DELETE");
        deleteConn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        deleteConn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        deleteConn.setRequestProperty("accept", "application/json");

        int deleteRespCode = deleteConn.getResponseCode();
        if (deleteRespCode == 204) {
            System.out.println("AlpacaApiClient:cancelOrderByClientId:1384 AlpacaOrder canceled successfully by client_order_id: " + clientOrderId);
            return true;
        } else {
            System.err.println("AlpacaApiClient:cancelOrderByClientId:1387 Failed to cancel order: " + orderId);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(deleteConn.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) System.err.println(line);
            }
            return false;
        }

    } catch (Exception e) {
        System.err.println("❌ Exception while canceling order by client_order_id: " + clientOrderId);
        e.printStackTrace();
        return false;
    }
}

public String generateClientOrderId(String symbol, String side) {
    String timestamp = new java.text.SimpleDateFormat("HHmmss").format(new java.util.Date());
    return String.format("%s-OCO-%s-%s", symbol, side.toUpperCase(), timestamp);
}
public String placeLimitBuy(String symbol, int qty, double limitPrice) {
      int maxRetries = 3;
    int retryDelayMs = 2000; // 2 seconds delay between attempts

    for (int attempt = 1; attempt <= maxRetries; attempt++) {

    try {
        limitPrice = Math.round(limitPrice * 100.0) / 100.0;

        URL url = new URL("https://paper-api.alpaca.markets/v2/orders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
        conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        String order_id=generateClientOrderId(symbol, "tp");
        JSONObject order = new JSONObject();
        order.put("symbol", symbol);
        order.put("qty", qty);
        order.put("side", "buy");
        order.put("type", "limit");
        order.put("limit_price", limitPrice);
        order.put("time_in_force", "day");
        order.put("client_order_id", order_id); // Add client order ID

      
        try (OutputStream os = conn.getOutputStream()) {
            os.write(order.toString().getBytes(StandardCharsets.UTF_8));
        }
          int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            System.out.println("AlpacaApiClient:placeStopBuy:1288 limitPrice Buy order placed: " + limitPrice);
            return order_id;
        } else {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                String line;
                System.err.println("Attempt " + attempt + " failed to place stop order (HTTP " + responseCode + "):");
                while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                }
            } else {
                System.err.println("Attempt " + attempt + " failed with HTTP code " + responseCode + " but no error stream was provided.");
            }
        }

       
        

       
    } catch (Exception e) {
        System.err.println("Error placing limit buy: " + e.getMessage());
        e.printStackTrace();
    }
      // Delay before retry
        if (attempt < maxRetries) {
            try {
                Thread.sleep(retryDelayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    System.err.println("All retry attempts failed for limit buy order for symbol: " + symbol);
    return "";
}
}

    

